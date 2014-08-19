package nl.opk.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;


/**
 * ...
 *
 * @author <a href="mailto:owestra@bol.com">Oscar Westra van Holthe - Kind</a>
 */
public class FileBasedTestBase
{
	protected Path findTargetDirectory()
	{
		String basedirName = System.getProperty("basedir", ".");
		Path basedir = new File(basedirName).toPath().toAbsolutePath().normalize();
		return basedir.resolve("target");
	}


	public void printDir(Path path) throws IOException
	{
		SimpleFileVisitor<Path> recursiveList = new SimpleFileVisitor<Path>()
		{
			private int depth = 0;


			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
			{
				printFinalSegmentIndented(dir);
				depth++;
				return FileVisitResult.CONTINUE;
			}


			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
			{
				printFinalSegmentIndented(file);
				return FileVisitResult.CONTINUE;
			}


			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
			{
				depth--;
				return super.postVisitDirectory(dir, exc);
			}


			private void printFinalSegmentIndented(Path path)
			{
				for (int i = 0; i < depth; i++)
				{
					System.out.print("  ");
				}
				System.out.print("* ");
				System.out.println(path.getFileName());
			}
		};

		Files.walkFileTree(path, recursiveList);
	}
}
