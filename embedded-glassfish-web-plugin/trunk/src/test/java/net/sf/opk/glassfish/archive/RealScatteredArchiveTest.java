package net.sf.opk.glassfish.archive;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class RealScatteredArchiveTest extends FileBasedTestBase
{
	private Path testRoot;

	private Path tempDir;
	private Path webapp;
	private Set<Path> classpath;

	private Path classes;
	private Path library;

	private Path webappPathIgnored1;
	private Path webappPathIgnored2;
	private Path webappPathInitial;
	private Path webappPathExtra;
	private Path classesPathInitial;
	private Path classesPathExtra;

	private Path resultWebInfClasses;
	private Path resultWebInfLib;


	@Before
	public void createTestDirectories() throws IOException
	{
		Path targetDirectory = Files.createDirectories(findTargetDirectory());
		testRoot = Files.createTempDirectory(targetDirectory, getClass().getSimpleName());

		// Main paths

		tempDir = Files.createDirectory(testRoot.resolve("result"));
		webapp = Files.createDirectory(testRoot.resolve("webapp"));
		classes = Files.createDirectory(testRoot.resolve("classes"));
		library = Files.createFile(testRoot.resolve("used.jar"));
		classpath = new HashSet<>(Arrays.asList(classes, library));

		// Setup resource directory, class and result paths

		Path webappWebInfClassesMetaInf = webapp.resolve("WEB-INF").resolve("classes").resolve("META-INF");
		webappPathIgnored1 = Files.createDirectories(webappWebInfClassesMetaInf);
		Path webappWebInfLib = Files.createDirectories(webapp.resolve("WEB-INF").resolve("lib"));
		webappPathIgnored2 = Files.createFile(webappWebInfLib.resolve("ignored.jar"));
		webappPathInitial = Files.createFile(webapp.resolve("initial"));

		classesPathInitial = Files.createFile(classes.resolve("initial"));

		resultWebInfClasses = tempDir.relativize(tempDir.resolve("WEB-INF").resolve("classes"));
		resultWebInfLib = tempDir.relativize(tempDir.resolve("WEB-INF").resolve("lib"));

		// Extra resource & 'class'

		webappPathExtra = webapp.resolve("extra");
		classesPathExtra = classes.resolve("extra");
	}


	@Test
	public void testArchive() throws IOException, InterruptedException
	{
		try (RealScatteredArchive archive = new RealScatteredArchive(tempDir, webapp, classpath))
		{
			Path result = archive.toPath();

			// Verify result

			assertFalse(existsMapped(webappPathIgnored1, webapp, result));
			assertFalse(existsMapped(webappPathIgnored2, webapp, result));
			assertTrue(existsMapped(webappPathInitial, webapp, result));
			assertTrue(existsMapped(classesPathInitial, classes, result.resolve(resultWebInfClasses)));
			assertFalse(existsMapped(webappPathExtra, webapp, result));
			assertFalse(existsMapped(classesPathExtra, classes, result.resolve(resultWebInfClasses)));
			assertTrue(Files.exists(result.resolve(resultWebInfLib).resolve(library.getFileName())));

			// Update sources

			pause(1500);
			Files.createFile(webappPathExtra);
			Files.createFile(classesPathExtra);
			pause(1500);

			// Verify changes in result

			assertFalse(existsMapped(webappPathIgnored1, webapp, result));
			assertFalse(existsMapped(webappPathIgnored2, webapp, result));
			assertTrue(existsMapped(webappPathInitial, webapp, result));
			assertTrue(existsMapped(classesPathInitial, classes, result.resolve(resultWebInfClasses)));
			assertTrue(existsMapped(webappPathExtra, webapp, result));
			assertTrue(existsMapped(classesPathExtra, classes, result.resolve(resultWebInfClasses)));
			assertTrue(Files.exists(result.resolve(resultWebInfLib).resolve(library.getFileName())));
		}
	}


	private static boolean existsMapped(Path path, Path source, Path target)
	{
		return Files.exists(target.resolve(source.relativize(path)));
	}


	private static void pause(long millis) throws InterruptedException
	{
		Thread.sleep(millis);
	}


	public void printDirs() throws IOException
	{
		printDir(tempDir);
		printDir(webapp);
		printDir(classes);
		//printDir(testClasses);
		printDir(library);
	}


	@Test
	public void testMakeUniqueSuccessWithoutExtension() throws IOException
	{
		Path input = testRoot.resolve("someFile");
		List<Path> expected = new ArrayList<>();
		expected.add(input);
		expected.add(testRoot.resolve("someFile.01"));
		expected.add(testRoot.resolve("someFile.02"));
		expected.add(testRoot.resolve("someFile.03"));
		expected.add(testRoot.resolve("someFile.04"));
		expected.add(testRoot.resolve("someFile.05"));
		expected.add(testRoot.resolve("someFile.06"));
		expected.add(testRoot.resolve("someFile.07"));
		expected.add(testRoot.resolve("someFile.08"));
		expected.add(testRoot.resolve("someFile.09"));
		expected.add(testRoot.resolve("someFile.10"));

		List<Path> actual = new ArrayList<>();
		try
		{
			for (Path ignored : expected)
			{
				actual.add(Files.createFile(RealScatteredArchive.makeUnique(input, 2)));
			}
			assertEquals(expected, actual);
		}
		finally
		{
			for (Path path : actual)
			{
				Files.deleteIfExists(path);
			}
		}
	}


	@Test
	public void testMakeUniqueSuccessWithExtension() throws IOException
	{
		Path input = testRoot.resolve("someFile.txt");
		List<Path> expected = new ArrayList<>();
		expected.add(input);
		expected.add(testRoot.resolve("someFile.1.txt"));
		expected.add(testRoot.resolve("someFile.2.txt"));
		expected.add(testRoot.resolve("someFile.3.txt"));
		expected.add(testRoot.resolve("someFile.4.txt"));
		expected.add(testRoot.resolve("someFile.5.txt"));
		expected.add(testRoot.resolve("someFile.6.txt"));
		expected.add(testRoot.resolve("someFile.7.txt"));
		expected.add(testRoot.resolve("someFile.8.txt"));
		expected.add(testRoot.resolve("someFile.9.txt"));
		expected.add(testRoot.resolve("someFile.10.txt"));

		List<Path> actual = new ArrayList<>();
		try
		{
			for (Path ignored : expected)
			{
				actual.add(Files.createFile(RealScatteredArchive.makeUnique(input, 1)));
			}
			assertEquals(expected, actual);
		}
		finally
		{
			for (Path path : actual)
			{
				Files.deleteIfExists(path);
			}
		}
	}
}
