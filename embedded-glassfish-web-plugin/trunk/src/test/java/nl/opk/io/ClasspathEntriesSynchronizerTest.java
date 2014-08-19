package nl.opk.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;


public class ClasspathEntriesSynchronizerTest extends FileBasedTestBase
{
	private Path srcFile;
	private Path src1;
	private Path src1Dir1;
	private Path src1Dir1File;
	private Path src1Dir2;
	private Path src1Dir2File;
	private Path src2;
	private Path src2Dir1;
	private Path src2Dir1File;
	private Path src2Dir3;
	private Path src2Dir3File;
	private List<Path> src;
	private Path dest;


	@Before
	public void initialize() throws IOException
	{
		Path targetDirectory = findTargetDirectory();
		Files.createDirectories(targetDirectory);
		Path parentDirectory = Files.createTempDirectory(targetDirectory, getClass().getSimpleName());

		srcFile = Files.createFile(parentDirectory.resolve("file"));

		src1 = Files.createDirectory(parentDirectory.resolve("src1"));
		src1Dir1 = Files.createDirectory(src1.resolve("dir1"));
		src1Dir1File = writeFileContents(src1Dir1.resolve("file"), "src1Dir1File");
		src1Dir2 = Files.createDirectory(src1.resolve("dir2"));
		src1Dir2File = writeFileContents(src1Dir2.resolve("file"), "src1Dir2File");
		src2 = Files.createDirectory(parentDirectory.resolve("src2"));
		src2Dir1 = Files.createDirectory(src2.resolve("dir1"));
		src2Dir1File = writeFileContents(src2Dir1.resolve("file"), "src2Dir1File");
		src2Dir3 = Files.createDirectory(src2.resolve("dir3"));
		src2Dir3File = writeFileContents(src2Dir3.resolve("file"), "src2Dir3File");

		src = Arrays.asList(src1, src2);
		dest = Files.createDirectory(parentDirectory.resolve("dest"));
	}


	private Path writeFileContents(Path file, String contents) throws IOException
	{
		Charset utf8 = Charset.forName("UTF-8");
		try (BufferedWriter writer = Files.newBufferedWriter(file, utf8, TRUNCATE_EXISTING, CREATE))
		{
			writer.write(contents);
		}
		return file;
	}


	private String readFileContents(Path file) throws IOException
	{
		StringBuilder contents = new StringBuilder();

		Charset utf8 = Charset.forName("UTF-8");
		for (String line : Files.readAllLines(file, utf8))
		{
			contents.append(line).append('\n');
		}
		return contents.toString();
	}


	@Test(expected = IllegalArgumentException.class)
	public void canOnlyCreateForDirectories1() throws IOException
	{
		new ClasspathEntriesSynchronizer(Collections.singletonList(srcFile), dest);
	}


	@Test(expected = IllegalArgumentException.class)
	public void canOnlyCreateForDirectories2() throws IOException
	{
		new ClasspathEntriesSynchronizer(Collections.singletonList(src1), srcFile);
	}


	@Test
	public void creatingInitializesTargetDirectory() throws IOException
	{
		Files.createFile(dest.resolve("file1a.txt"));
		Files.createDirectory(dest.resolve("dir1b"));
		Path dir1c = Files.createDirectory(dest.resolve("dir1c"));
		Files.createFile(dir1c.resolve("file1d.txt"));
		Files.createFile(dir1c.resolve("file1e.txt"));

		new ClasspathEntriesSynchronizer(src, dest);

		assertDestinationContents(
				Arrays.asList(src1.relativize(src1Dir1), src1.relativize(src1Dir2), src2.relativize(src2Dir3)),
				Arrays.asList(src1.relativize(src1Dir1File), src1.relativize(src1Dir2File),
				              src2.relativize(src2Dir3File)));
		assertEquals("src1Dir1File\n", readFileContents(dest.resolve(src1.relativize(src1Dir1File))));
		assertEquals("src1Dir2File\n", readFileContents(dest.resolve(src1.relativize(src1Dir2File))));
		assertEquals("src2Dir3File\n", readFileContents(dest.resolve(src2.relativize(src2Dir3File))));
	}


	private void assertDestinationContents(Collection<Path> relativeDirectories, Collection<Path> relativeFiles)
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
		for (Path path : relativeDirectories)
		{
			expectedDirectories.add(dest.resolve(path));
		}
		Set<Path> expectedFiles = new HashSet<>();
		for (Path path : relativeFiles)
		{
			expectedFiles.add(dest.resolve(path));
		}

		assertEquals(contents(expectedDirectories), contents(actualDirectories));
		assertEquals(contents(expectedFiles), contents(actualFiles));
	}


	private String contents(Collection<Path> paths)
	{
		StringBuilder buffer = new StringBuilder();

		List<Path> pathList = new ArrayList<>(paths);
		Collections.sort(pathList);
		for (Path path : pathList)
		{
			buffer.append(path).append('\n');
		}

		return buffer.toString();
	}


	@Test
	public void handlesCreateAndDeleteEvents() throws IOException
	{
		ClasspathEntriesSynchronizer synchronizer = new ClasspathEntriesSynchronizer(src, dest);

		assertArrayEquals(
				new WatchEvent.Kind<?>[]{StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE},
				synchronizer.handledEvents());
	}


	@Test
	public void handleAnyEvent() throws IOException
	{
		ClasspathEntriesSynchronizer synchronizer = new ClasspathEntriesSynchronizer(src, dest);

		Files.delete(src1Dir2File);
		Files.delete(src1Dir2);
		synchronizer.handle(StandardWatchEventKinds.ENTRY_DELETE, src1, src1.relativize(src1Dir2File));
		synchronizer.handle(StandardWatchEventKinds.ENTRY_DELETE, src1, src1.relativize(src1Dir2));

		writeFileContents(src2Dir1File, "Changed content");
		writeFileContents(src2Dir3File, "Changed content");

		assertDestinationContents(
				Arrays.asList(src1.relativize(src1Dir1), src2.relativize(src2Dir3)),
				Arrays.asList(src1.relativize(src1Dir1File), src2.relativize(src2Dir3File)));
		assertEquals("src1Dir1File\n", readFileContents(dest.resolve(src1.relativize(src1Dir1File))));
		//assertEquals("src1Dir2File\n", readFileContents(dest.resolve(src1.relativize(src1Dir2File))));
		assertEquals("Changed content\n", readFileContents(dest.resolve(src2.relativize(src2Dir3File))));
	}
}
