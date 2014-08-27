package net.sf.opk.glassfish.archive;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Class to synchronize exploded classpath entries.
 *
 * @author <a href="mailto:oscar@westravanholthe.nl">Oscar Westra van Holthe - Kind</a>
 */
public class ClasspathEntriesSynchronizer implements DirectoryEventHandler
{
	/**
	 * Logger for this class.
	 */
	private static final Logger LOGGER = Logger.getLogger(ClasspathEntriesSynchronizer.class.getName());
	/**
	 * The file tree walker to do the synchronization with.
	 */
	private final UnionFileTreeWalker fileTreeWalker;
	/**
	 * The visitor that does the synchronization.
	 */
	private final PathVisitor classpathEntriesSynchronizingVisitor;


	/**
	 * Create a synchronizer for classpath entries.
	 *
	 * @param sourceDirectories the exploded classpath entries (directories), in order of precedence (highest first)
	 * @param targetDirectory   the directory into which to synchronize the classpath entries
	 * @throws IOException when initial synchronisation fails
	 */
	public ClasspathEntriesSynchronizer(final List<Path> sourceDirectories, Path targetDirectory)
			throws IOException
	{
		List<Path> pathRoots = new ArrayList<>(sourceDirectories.size() + 1);
		for (Path sourceDirectory : sourceDirectories)
		{
			pathRoots.add(requireDirectory(sourceDirectory.toAbsolutePath()));
		}
		final Path absoluteTarget = targetDirectory.toAbsolutePath();
		pathRoots.add(requireDirectory(absoluteTarget));
		fileTreeWalker = new UnionFileTreeWalker(pathRoots);

		classpathEntriesSynchronizingVisitor = new PathVisitor()
		{
			@Override
			public void preVisitDirectory(Path directory) throws IOException
			{
				if (findHighestPrecedenceSource(directory) != null)
				{
					Files.createDirectories(absoluteTarget.resolve(directory));
				}
			}


			@Override
			public void visitFile(Path file) throws IOException
			{
				Path source = findHighestPrecedenceSource(file);
				Path target = absoluteTarget.resolve(file);

				Files.deleteIfExists(target);
				if (source != null)
				{
					Files.createLink(target, source);
				}
			}


			@Override
			public void postVisitDirectory(Path directory) throws IOException
			{
				if (findHighestPrecedenceSource(directory) == null)
				{
					Files.deleteIfExists(absoluteTarget.resolve(directory));
				}
			}


			private Path findHighestPrecedenceSource(Path relativePath)
			{
				for (Path sourceDirectory : sourceDirectories)
				{
					Path path = sourceDirectory.resolve(relativePath);
					if (Files.exists(path))
					{
						return path;
					}
				}
				return null;
			}
		};

		LOGGER.log(Level.CONFIG, "Clearing {0}", targetDirectory.toAbsolutePath());
		clearDirectory(absoluteTarget);
		LOGGER.log(Level.CONFIG, "Synchronizing classes from into {0}", targetDirectory);
		fileTreeWalker.walk(absoluteTarget.relativize(absoluteTarget), classpathEntriesSynchronizingVisitor);
		LOGGER.log(Level.CONFIG, "Created {0} for {1}", new Object[]{toString(), absoluteTarget});
	}


	private Path requireDirectory(Path directory)
	{
		if (!Files.isDirectory(directory))
		{
			throw new IllegalArgumentException(directory + " is not a directory.");
		}
		return directory;
	}


	private void clearDirectory(final Path target) throws IOException
	{
		Files.walkFileTree(target, new SimpleFileVisitor<Path>()
		{
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException
			{
				Files.deleteIfExists(file);
				return FileVisitResult.CONTINUE;
			}


			@Override
			public FileVisitResult postVisitDirectory(Path directory, IOException exception) throws IOException
			{
				if (!target.equals(directory))
				{
					Files.deleteIfExists(directory);
				}
				return FileVisitResult.CONTINUE;
			}
		});
	}


	@Override
	public WatchEvent.Kind<?>[] handledEvents()
	{
		return new WatchEvent.Kind<?>[]{StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE};
	}


	@Override
	public void handle(WatchEvent.Kind<?> event, Path root, Path path) throws IOException
	{
		fileTreeWalker.walk(path, classpathEntriesSynchronizingVisitor);
	}
}
