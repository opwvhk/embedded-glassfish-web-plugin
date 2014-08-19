package nl.opk.io;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class UnionFileTreeWalkerTest extends FileBasedTestBase
{
	private Path testDirectory1;
	private Path testDirectory2;
	private Path file5;
	private Path emptyPath;


	@Before
	public void initialize() throws IOException
	{
		Path targetDirectory = findTargetDirectory();
		testDirectory1 = Files.createTempDirectory(targetDirectory, getClass().getSimpleName());
		testDirectory2 = Files.createTempDirectory(targetDirectory, getClass().getSimpleName());
		emptyPath = testDirectory1.relativize(testDirectory1);

		Path dir1 = Files.createDirectory(testDirectory1.resolve("dir1"));
		Path dir2a = Files.createDirectory(testDirectory1.resolve("dir2"));

		Files.createFile(testDirectory1.resolve("file1"));
		Files.createFile(dir1.resolve("file2"));
		Files.createFile(dir2a.resolve("file3"));
		file5 = emptyPath.resolve("file5");
		Files.createFile(testDirectory1.resolve(file5));
		Files.createSymbolicLink(testDirectory1.resolve("link"), file5);

		Path dir2b = Files.createDirectory(testDirectory2.resolve("dir2"));

		Files.createFile(dir2b.resolve("file3"));
		Files.createFile(dir2b.resolve("file4"));
		Files.createFile(testDirectory2.resolve("file6"));

	}


	@Test
	public void testWalkDirectory() throws Exception
	{
		StringBuilder buffer = new StringBuilder();
		PathVisitor visitor = createPathVisitor(buffer);

		new UnionFileTreeWalker(Arrays.asList(testDirectory1, testDirectory2)).walk(emptyPath, visitor);

		assertEquals(">>dir1=dir1/file2<dir1>dir2=dir2/file3=dir2/file4<dir2=file1=file5=file6<", buffer.toString());
	}


	@Test
	public void testWalkFile() throws Exception
	{
		StringBuilder buffer = new StringBuilder();
		PathVisitor visitor = createPathVisitor(buffer);

		new UnionFileTreeWalker(Arrays.asList(testDirectory1, testDirectory2)).walk(file5, visitor);

		assertEquals("=file5", buffer.toString());
	}


	private PathVisitor createPathVisitor(final StringBuilder buffer)
	{
		return new PathVisitor()
			{
				@Override
				public void preVisitDirectory(Path directory) throws IOException
				{
					buffer.append('>').append(directory.toString().replace('\\', '/'));
				}


				@Override
				public void visitFile(Path file) throws IOException
				{
					buffer.append('=').append(file.toString().replace('\\', '/'));
				}


				@Override
				public void postVisitDirectory(Path directory) throws IOException
				{
					buffer.append('<').append(directory.toString().replace('\\', '/'));
				}
			};
	}


	@After
	public void cleanup() throws Exception
	{
		FileVisitor<Path> deleter = new SimpleFileVisitor<Path>()
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
				Files.deleteIfExists(directory);
				return FileVisitResult.CONTINUE;
			}
		};

		Files.walkFileTree(testDirectory1, deleter);
		Files.walkFileTree(testDirectory2, deleter);
	}
}
