package net.sf.opk.glassfish.archive;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * <p>A source for events on a set of directories (and their descendants) that can be handled by the {@link
 * DirectoryEventHandler} of each added root.</p>
 *
 * <p>Requires all directories to be on the same filesystem, as given during construction. This is not much of a
 * restriction, as the Java implementations of {@link WatchService} seem to be OS specific.</p>
 *
 * @author <a href="mailto:oscar@westravanholthe.nl">Oscar Westra van Holthe - Kind</a>
 */
public class DirectoryEventSource implements Runnable
{
	/**
	 * Logger for this class.
	 */
	private static final Logger LOGGER = Logger.getLogger(DirectoryEventSource.class.getName());
	private final FileSystem fileSystem;
	private final Map<Path, Pair<DirectoryEventHandler, WatchEvent.Kind<?>[]>> sourceDirectories;
	private final Map<WatchKey, Path> watchedRoots;
	private final Executor eventExecutor;
	private Thread runningThread;


	public DirectoryEventSource(FileSystem fileSystem) throws IOException
	{
		this(fileSystem, Executors.newSingleThreadExecutor());
	}


	public DirectoryEventSource(FileSystem fileSystem, Executor eventExecutor) throws IOException
	{
		this.fileSystem = fileSystem;
		this.eventExecutor = eventExecutor;

		sourceDirectories = new HashMap<>();
		watchedRoots = new HashMap<>();

		runningThread = null;

		LOGGER.log(Level.CONFIG, "Created {0}", toString());
	}


	/**
	 * Register a directory (recursively) for events.
	 *
	 * @param directory    the directory to watch
	 * @param eventHandler the event handler for the directory
	 */
	public void register(Path directory, DirectoryEventHandler eventHandler)
	{
		if (!Files.isDirectory(directory))
		{
			throw new IllegalArgumentException(directory + " is not a directory");
		}
		if (!fileSystem.equals(directory.getFileSystem()))
		{
			throw new IllegalArgumentException(
					directory + " is not on the filesystem watched by this DirectoryEventSource");
		}
		for (Path path : sourceDirectories.keySet())
		{
			if (path.startsWith(directory))
			{
				throw new IllegalArgumentException(directory + " contains an already registered path: " + path +
				                                   " (this is not supported)");
			}
			if (directory.startsWith(path))
			{
				throw new IllegalArgumentException(directory + " is part of an already registered path: " + path +
				                                   " (this is not supported)");
			}
		}

		sourceDirectories.put(directory, new Pair<>(eventHandler, eventHandler.handledEvents()));
	}


	private void watch(final WatchService watchService, final Path root, Path directory) throws IOException
	{
		Pair<DirectoryEventHandler, WatchEvent.Kind<?>[]> pair = sourceDirectories.get(root);
		final WatchEvent.Kind[] events = pair.getValue();

		Files.walkFileTree(directory, new SimpleFileVisitor<Path>()
		{
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
			{
				WatchKey key = dir.register(watchService, events);
				watchedRoots.put(key, root);
				return FileVisitResult.CONTINUE;
			}
		});
	}


	@Override
	public void run()
	{
		if (runningThread != null)
		{
			throw new IllegalStateException(getClass().getSimpleName() +
			                                ".run() is already running (and NOT thread-safe).");
		}
		try (WatchService watchService = FileSystems.getDefault().newWatchService())
		{
			runningThread = Thread.currentThread();

			LOGGER.info(toString() + " initializes");
			for (Path root : sourceDirectories.keySet())
			{
				watch(watchService, root, root);
			}

			LOGGER.info(toString() + " starts watching its sources");
			while (runningThread != null)
			{
				WatchKey watchKey = watchService.take();
				final Path root = watchedRoots.get(watchKey);

				for (WatchEvent<?> watchEvent : watchKey.pollEvents())
				{
					final WatchEvent.Kind<?> event = watchEvent.kind();
					Path path = determineEventPath(watchKey, watchEvent);
					LOGGER.log(Level.FINE, "Event: {0} for {1}", new Object[]{event.name(), path});

					boolean isCreateEvent = StandardWatchEventKinds.ENTRY_CREATE.equals(event);
					boolean isDirectory = path != null && Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS);
					if (isCreateEvent && isDirectory)
					{
						LOGGER.log(Level.FINE, "Watching {} (recursively)", path);
						watch(watchService, root, path);
					}
					final DirectoryEventHandler eventHandler = sourceDirectories.get(root).getKey();
					final Path relative = root.relativize(path);
					LOGGER.log(Level.FINE, "Using {1} for root {2} to handle event {0} of {3}",
					           new Object[]{event.name(), eventHandler, root, relative});
					eventExecutor.execute(new Runnable()
					{
						@Override
						public void run()
						{
							try
							{
								eventHandler.handle(event, root, relative);
							}
							catch (Exception e)
							{
								String message =
										String.format("Failed to handle event %s by %s for root %s and path %s",
										              event.name(), eventHandler, root, relative);
								LOGGER.log(Level.WARNING, message, e);
							}
						}
					});
				}

				if (!watchKey.reset())
				{
					watchedRoots.remove(watchKey);
					if (watchedRoots.isEmpty())
					{
						LOGGER.info(toString() + " has no more sources to watch");
						runningThread = null;
					}
				}
			}
		}
		catch (InterruptedException ignored)
		{
			// Ignore: if we're interrupted, we should exit.
		}
		catch (IOException e)
		{
			LOGGER.log(Level.SEVERE, "Watcher process for " + toString() + " has crashed:", e);
		}
		finally
		{
			LOGGER.info(toString() + " has stopped watching its sources");
			runningThread = null;
		}
	}


	static Path determineEventPath(WatchKey watchKey, WatchEvent<?> watchEvent)
	{
		Path context = (Path)watchEvent.context();
		Path path = null;
		if (context != null)
		{
			Path watchable = (Path)watchKey.watchable();
			path = watchable.resolve(context);
		}
		return path;
	}


	public void stop() throws IOException
	{
		if (runningThread != null)
		{
			runningThread.interrupt();
		}
		runningThread = null;
		LOGGER.log(Level.CONFIG, "Signalled {0} to stop.", toString());
	}


	private class Pair<K, V>
	{
		private final K key;
		private final V value;


		private Pair(K key, V value)
		{
			this.key = key;
			this.value = value;
		}


		public K getKey()
		{
			return key;
		}


		public V getValue()
		{
			return value;
		}
	}
}
