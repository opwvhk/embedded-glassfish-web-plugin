package net.sf.opk.glassfish.archive;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;


public class WebResourcesSynchronizerTest extends FileBasedTestBase
{
	private Path srcFile;
	private Path src;
	private Path srcDir1;
	private Path srcDir1File;
	private Path srcWebinf;
	private Path srcWebinfFile1;
	private Path srcWebinfFile2;
	private Path srcWebinfDir;
	private Path srcWebinfClasses;
	private Path srcWebinfClassesFile;
	private Path srcWebinfLib;
	private Path srcWebinfLibFile;
	private Path dest;


	@Before
	public void initialize() throws IOException
	{
		Path targetDirectory = findTargetDirectory();
		Files.createDirectories(targetDirectory);
		Path parentDirectory = Files.createTempDirectory(targetDirectory, getClass().getSimpleName());

		srcFile = Files.createFile(parentDirectory.resolve("file"));

		src = Files.createDirectory(parentDirectory.resolve("src"));
		srcDir1 = Files.createDirectory(src.resolve("dir1"));
		srcDir1File = Files.createFile(srcDir1.resolve("file"));
		srcWebinf = Files.createDirectory(src.resolve("WEB-INF"));
		srcWebinfFile1 = Files.createFile(srcWebinf.resolve("file1"));
		srcWebinfFile2 = Files.createFile(srcWebinf.resolve("file2"));
		srcWebinfDir = Files.createDirectory(srcWebinf.resolve("dir"));
		srcWebinfClasses = Files.createDirectory(srcWebinf.resolve("classes"));
		srcWebinfClassesFile = Files.createFile(srcWebinfClasses.resolve("file"));
		srcWebinfLib = Files.createDirectory(srcWebinf.resolve("lib"));
		srcWebinfLibFile = Files.createFile(srcWebinfLib.resolve("file"));

		dest = Files.createDirectory(parentDirectory.resolve("dest"));
	}


	@Test(expected = IllegalArgumentException.class)
	public void canOnlyCreateForDirectories1() throws IOException
	{
		new WebResourcesSynchronizer(srcFile, dest);
	}


	@Test(expected = IllegalArgumentException.class)
	public void canOnlyCreateForDirectories2() throws IOException
	{
		new WebResourcesSynchronizer(src, srcFile);
	}


	@Test
	public void creatingInitializesTargetDirectory() throws IOException
	{
		Files.createFile(dest.resolve("file1a.txt"));
		Files.createDirectory(dest.resolve("dir1b"));
		Path dir1c = Files.createDirectory(dest.resolve("dir1c"));
		Files.createFile(dir1c.resolve("file1d.txt"));
		Files.createFile(dir1c.resolve("file1e.txt"));

		new WebResourcesSynchronizer(src, dest);

		assertDestinationContents(Arrays.asList(srcDir1, srcWebinf, srcWebinfDir),
		                          Arrays.asList(srcDir1File, srcWebinfFile1, srcWebinfFile2));
	}


	private void assertDestinationContents(Collection<Path> sourceDirectories, Collection<Path> sourceFiles)
			throws IOException
	{
		final Set<Path> actualDirectories = new HashSet<>();
		final Set<Path> actualFiles = new HashSet<>();
		Files.walkFileTree(dest, new SimpleFileVisitor<Path>()
		{
			@Override
			public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) throws IOException
			{
				actualDirectories.add(directory);
				return FileVisitResult.CONTINUE;
			}


			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException
			{
				actualFiles.add(file);
				return FileVisitResult.CONTINUE;
			}
		});

		Set<Path> expectedDirectories = new HashSet<>();
		// The visitor also visits the directory itself, but the method parameters only specify the contents.
		expectedDirectories.add(dest);
		for (Path path : sourceDirectories)
		{
			expectedDirectories.add(dest.resolve(src.relativize(path)));
		}
		Set<Path> expectedFiles = new HashSet<>();
		for (Path path : sourceFiles)
		{
			expectedFiles.add(dest.resolve(src.relativize(path)));
		}

		assertEquals(expectedDirectories, actualDirectories);
		assertEquals(expectedFiles, actualFiles);
	}


	@Test
	public void handlesCreateAndDeleteEvents() throws IOException
	{
		WebResourcesSynchronizer synchronizer = new WebResourcesSynchronizer(src, dest);

		assertArrayEquals(
				new WatchEvent.Kind<?>[]{StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE},
				synchronizer.handledEvents());
	}


	@Test(expected = IllegalArgumentException.class)
	public void cannotHandleEventsForUnmappedDirectories() throws IOException
	{
		WebResourcesSynchronizer synchronizer = new WebResourcesSynchronizer(src, dest);
		// We use a subdirectory, but any path other than src will do.
		synchronizer.handle(StandardWatchEventKinds.ENTRY_CREATE, srcDir1, srcDir1.relativize(srcDir1File));
	}


	@Test
	public void handleCreateEvents() throws IOException
	{
		WebResourcesSynchronizer synchronizer = new WebResourcesSynchronizer(src, dest);

		Path path1 = Files.createFile(src.resolve("newfile"));
		Path path2 = Files.createDirectory(srcWebinf.resolve("newfile"));
		Path path3 = Files.createDirectory(srcWebinfClasses.resolve("newfile"));
		Path path4 = Files.createFile(srcWebinfLib.resolve("newfile"));

		synchronizer.handle(StandardWatchEventKinds.ENTRY_CREATE, src, src.relativize(path1));
		synchronizer.handle(StandardWatchEventKinds.ENTRY_CREATE, src, src.relativize(path2));
		synchronizer.handle(StandardWatchEventKinds.ENTRY_CREATE, src, src.relativize(path3));
		synchronizer.handle(StandardWatchEventKinds.ENTRY_CREATE, src, src.relativize(path4));

		// Verify end point

		assertDestinationContents(Arrays.asList(srcDir1, srcWebinf, srcWebinfDir, path2),
		                          Arrays.asList(srcDir1File, srcWebinfFile1, srcWebinfFile2, path1));
	}


	@Test
	public void handleDeleteEvents() throws IOException
	{
		// Setup

		WebResourcesSynchronizer synchronizer = new WebResourcesSynchronizer(src, dest);

		// The test case

		synchronizer.handle(StandardWatchEventKinds.ENTRY_DELETE, src, src.relativize(srcDir1File));
		synchronizer.handle(StandardWatchEventKinds.ENTRY_DELETE, src, src.relativize(srcWebinfDir));
		synchronizer.handle(StandardWatchEventKinds.ENTRY_DELETE, src, src.relativize(srcWebinfClassesFile));
		synchronizer.handle(StandardWatchEventKinds.ENTRY_DELETE, src, src.relativize(srcWebinfLibFile));

		// Verify end point

		assertDestinationContents(Arrays.asList(srcDir1, srcWebinf), Arrays.asList(srcWebinfFile1, srcWebinfFile2));
	}


	@Test
	public void handleDeleteEventForSourceDirectoryWithClasspathInDestination() throws IOException
	{
		WebResourcesSynchronizer synchronizer = new WebResourcesSynchronizer(src, dest);

		Files.createDirectory(dest.resolve(src.relativize(srcWebinfClasses)));
		Files.createDirectory(dest.resolve(src.relativize(srcWebinfLib)));
		Files.createFile(dest.resolve(src.relativize(srcWebinfClassesFile)));
		Files.createFile(dest.resolve(src.relativize(srcWebinfLibFile)));

		synchronizer.handle(StandardWatchEventKinds.ENTRY_DELETE, src, src.relativize(src));

		// Verify end point

		assertDestinationContents(Arrays.asList(srcWebinf, srcWebinfClasses, srcWebinfLib),
		                          Arrays.asList(srcWebinfClassesFile, srcWebinfLibFile));
	}


	@Test
	public void handleModificationEvent() throws IOException
	{
		WebResourcesSynchronizer synchronizer = new WebResourcesSynchronizer(src, dest);

		synchronizer.handle(StandardWatchEventKinds.ENTRY_MODIFY, src, src.relativize(srcDir1File));

		// Verify end point --> no difference with initialized state, as it is a noop

		assertDestinationContents(Arrays.asList(srcDir1, srcWebinf, srcWebinfDir),
		                          Arrays.asList(srcDir1File, srcWebinfFile1, srcWebinfFile2));
	}
}
