package net.sf.opk.glassfish.archive;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class DirectoryEventSourceTest extends FileBasedTestBase
{
	private Path parentDirectory;
	private Path subDirectory;


	@Before
	public void createTestDirectories() throws IOException
	{
		Path targetDirectory = findTargetDirectory();
		Files.createDirectories(targetDirectory);

		parentDirectory = Files.createTempDirectory(targetDirectory, getClass().getSimpleName());
		subDirectory = parentDirectory.resolve("subdirectory");

		Files.createDirectories(subDirectory);
		assertTrue(Files.isDirectory(parentDirectory));
		assertTrue(Files.isDirectory(subDirectory));
	}


	@After
	public void deleteTestDirectories() throws IOException
	{
		reallyTryToDelete(3, parentDirectory);
	}


	public void reallyTryToDelete(int maxRetries, Path path)
	{
		try
		{
			Files.walkFileTree(path, new SimpleFileVisitor<Path>()
			{
				@Override
				public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes)
						throws IOException
				{
					try
					{
						Set<PosixFilePermission> permissions =
								Files.getPosixFilePermissions(directory, LinkOption.NOFOLLOW_LINKS);
						try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory))
						{
							for (Path path : stream)
							{
								Files.setPosixFilePermissions(path, permissions);
							}
						}
					}
					catch (UnsupportedOperationException ignored)
					{
						List<AclEntry> aclEntries = Files.getFileAttributeView(directory, AclFileAttributeView.class,
						                                                       LinkOption.NOFOLLOW_LINKS).getAcl();
						try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory))
						{
							for (Path path : stream)
							{
								Files.getFileAttributeView(path, AclFileAttributeView.class).setAcl(aclEntries);
							}
						}
					}
					return FileVisitResult.CONTINUE;
				}


				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException
				{
					Files.deleteIfExists(file);
					return FileVisitResult.CONTINUE;
				}


				@Override
				public FileVisitResult postVisitDirectory(Path directory, IOException exception) throws IOException
				{
					Files.deleteIfExists(directory);
					return FileVisitResult.CONTINUE;
				}
			});
		}
		catch (IOException e)
		{
			if (maxRetries > 0)
			{
				reallyTryToDelete(maxRetries - 1, path);
			}
		}
	}


	@Test(expected = IllegalArgumentException.class)
	public void canOnlyRegisterDirectories() throws IOException, URISyntaxException
	{
		DirectoryEventSource eventSource = new DirectoryEventSource(FileSystems.getDefault());
		eventSource.register(Paths.get("sjglajlkfghjdlakhjglkadf"), null);
	}


	@Test(expected = IllegalArgumentException.class)
	public void cannotRegisterForDifferentFileSystem() throws IOException, URISyntaxException
	{
		// A mocked FileSystem never equals a real FileSystem.
		FileSystem fileSystem = mock(FileSystem.class);

		DirectoryEventSource eventSource = new DirectoryEventSource(fileSystem);
		eventSource.register(parentDirectory, null);
	}


	@Test(expected = IllegalArgumentException.class)
	public void cannotRegisterSubdirectories() throws IOException, URISyntaxException
	{
		DirectoryEventHandler handler = mock(DirectoryEventHandler.class);
		when(handler.handledEvents()).thenReturn(
				new WatchEvent.Kind<?>[]{StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE});
		doNothing().when(handler).handle(any(WatchEvent.Kind.class), any(Path.class), any(Path.class));

		DirectoryEventSource eventSource = new DirectoryEventSource(FileSystems.getDefault());
		eventSource.register(parentDirectory, handler);
		eventSource.register(subDirectory, handler);
	}


	@Test(expected = IllegalArgumentException.class)
	public void cannotRegisterParentDirectories() throws IOException, URISyntaxException
	{
		DirectoryEventHandler handler = mock(DirectoryEventHandler.class);
		when(handler.handledEvents()).thenReturn(
				new WatchEvent.Kind<?>[]{StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE});
		doNothing().when(handler).handle(any(WatchEvent.Kind.class), any(Path.class), any(Path.class));

		DirectoryEventSource eventSource = new DirectoryEventSource(FileSystems.getDefault());
		eventSource.register(subDirectory, handler);
		eventSource.register(parentDirectory, handler);
	}


	@Test
	public void determineOverflowedEventPath()
	{
		// This case is tricky / nearly impossible to cause, but can occur on an event overflow.

		WatchKey key = mock(WatchKey.class);
		WatchEvent<?> event = mock(WatchEvent.class);

		when(event.context()).thenReturn(null);

		assertNull(DirectoryEventSource.determineEventPath(key, event));
	}


	@Test
	public void testEventsAndStop() throws IOException, InterruptedException
	{
		final List<Map.Entry<? extends WatchEvent.Kind<?>, Path>> handledEvents = new ArrayList<>();
		DirectoryEventHandler handler = new DirectoryEventHandler()
		{
			boolean handledAnything = false;

			@Override
			public WatchEvent.Kind<?>[] handledEvents()
			{
				return new WatchEvent.Kind<?>[]{StandardWatchEventKinds.ENTRY_CREATE,
				                                StandardWatchEventKinds.ENTRY_MODIFY,
				                                StandardWatchEventKinds.ENTRY_DELETE};
			}


			@Override
			public void handle(WatchEvent.Kind<?> event, Path root, Path path) throws IOException
			{
				Map.Entry<? extends WatchEvent.Kind<?>, Path> entry =
						new AbstractMap.SimpleEntry<>(event, root.resolve(path));
				handledEvents.add(entry);

				// Exceptions should not crash our event source.
				if (!handledAnything)
				{
					handledAnything = true;
					throw new IOException("Oops");
				}
			}
		};

		DirectoryEventSource eventSource = new DirectoryEventSource(FileSystems.getDefault());
		eventSource.register(parentDirectory, handler);

		Thread watchThread = new Thread(eventSource);
		watchThread.start();

		Path childDir = subDirectory.resolve("childDir");
		Path testFile = childDir.resolve("File.txt");

		pause();
		Files.createDirectory(childDir);
		pause();
		Files.createFile(testFile);
		pause();
		try (FileWriter writer = new FileWriter(testFile.toFile(), true))
		{
			writer.write("Hi there!\n");
		}
		pause();
		Files.delete(testFile);
		pause();
		Files.delete(childDir);
		pause();

		eventSource.stop();

		pause();
		assertFalse(watchThread.isAlive());

        List<Map.Entry<? extends WatchEvent.Kind<?>, Path>> expected = new ArrayList<>();
		expected.add(new AbstractMap.SimpleEntry<>(StandardWatchEventKinds.ENTRY_MODIFY, subDirectory));
		expected.add(new AbstractMap.SimpleEntry<>(StandardWatchEventKinds.ENTRY_CREATE, childDir));
		expected.add(new AbstractMap.SimpleEntry<>(StandardWatchEventKinds.ENTRY_MODIFY, childDir));
        expected.add(new AbstractMap.SimpleEntry<>(StandardWatchEventKinds.ENTRY_CREATE, testFile));
        expected.add(new AbstractMap.SimpleEntry<>(StandardWatchEventKinds.ENTRY_MODIFY, testFile));
        expected.add(new AbstractMap.SimpleEntry<>(StandardWatchEventKinds.ENTRY_DELETE, testFile));
        expected.add(new AbstractMap.SimpleEntry<>(StandardWatchEventKinds.ENTRY_MODIFY, childDir));
        expected.add(new AbstractMap.SimpleEntry<>(StandardWatchEventKinds.ENTRY_MODIFY, subDirectory));
        expected.add(new AbstractMap.SimpleEntry<>(StandardWatchEventKinds.ENTRY_DELETE, childDir));
		assertEquals(expected, handledEvents);
	}


	@Test
	public void removingAllWatchedDirectoriesStopsWatcher() throws IOException, InterruptedException
	{
		DirectoryEventSource eventSource = new DirectoryEventSource(FileSystems.getDefault());
		eventSource.register(subDirectory, new DirectoryEventHandler()
		{
			@Override
			public WatchEvent.Kind<?>[] handledEvents()
			{
				return new WatchEvent.Kind<?>[]{StandardWatchEventKinds.ENTRY_CREATE,
				                                StandardWatchEventKinds.ENTRY_MODIFY,
				                                StandardWatchEventKinds.ENTRY_DELETE};
			}


			@Override
			public void handle(WatchEvent.Kind<?> event, Path root, Path path)
			{
				// noop
			}
		});

		Thread watchThread = new Thread(eventSource);
		watchThread.start();

		pause();
		Files.delete(subDirectory);

		pause();
		assertFalse(watchThread.isAlive());
	}


	@Test
	public void exceptionsStopWatcher() throws IOException, InterruptedException
	{
		DirectoryEventHandler handler = mock(DirectoryEventHandler.class);
		when(handler.handledEvents()).thenReturn(
				new WatchEvent.Kind<?>[]{StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE});
		doNothing().when(handler).handle(any(WatchEvent.Kind.class), any(Path.class), any(Path.class));

		DirectoryEventSource eventSource = new DirectoryEventSource(FileSystems.getDefault());
		eventSource.register(parentDirectory, handler);

		Thread watchThread = new Thread(eventSource);
		watchThread.start();

		try
		{
			Files.setPosixFilePermissions(subDirectory, PosixFilePermissions.fromString("---------"));
		}
		catch (UnsupportedOperationException ignored)
		{
			AclFileAttributeView aclView = Files.getFileAttributeView(subDirectory, AclFileAttributeView.class);
			List<AclEntry> acl = Collections.emptyList();
			aclView.setAcl(acl);
		}

		pause();
		assertFalse(watchThread.isAlive());
	}


	@Test(expected = IllegalStateException.class)
	public void cannotStartWatcherTwice() throws IOException, InterruptedException
	{
		DirectoryEventHandler handler = mock(DirectoryEventHandler.class);
		when(handler.handledEvents()).thenReturn(
				new WatchEvent.Kind<?>[]{StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE});
		doNothing().when(handler).handle(any(WatchEvent.Kind.class), any(Path.class), any(Path.class));

		DirectoryEventSource eventSource = new DirectoryEventSource(FileSystems.getDefault());
		eventSource.register(parentDirectory, handler);

		Thread watchThread = new Thread(eventSource);
		watchThread.start();
		try
		{
			pause();
			eventSource.run();
		}
		finally
		{
			eventSource.stop();
		}
	}


	@Test
	public void stopWhenNotRunningIsNoop() throws IOException, InterruptedException
	{
		DirectoryEventSource eventSource = new DirectoryEventSource(FileSystems.getDefault());
		eventSource.stop();

		// Where should be no failure.
	}


	private static void pause() throws InterruptedException
	{
		Thread.sleep(2000);
	}
}
