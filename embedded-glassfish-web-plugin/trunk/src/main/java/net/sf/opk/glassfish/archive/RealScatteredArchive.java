package net.sf.opk.glassfish.archive;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * <p>A better ScatteredArchive than the {@link org.glassfish.embeddable.archive.ScatteredArchive ScatteredArchive} from
 * GlassFish. This class provides a realtime view of the web resources and classes, and a static view of all
 * libraries.</p>
 *
 * <p><strong>ASSUMPTION:</strong> all sources and build results reside on the same filesystem.</p>
 *
 * @author <a href="mailto:oscar@westravanholthe.nl">Oscar Westra van Holthe - Kind</a>
 */
public class RealScatteredArchive implements AutoCloseable
{
	private static final String ARCHIVE_ROOT_PREFIX = "webapp-";
	private static final String WEBINF = "WEB-INF";
	private static final String CLASSES = "classes";
	private static final String LIB = "lib";

	private final DirectoryEventSource directoryEventSource;
	private final Path archiveRoot;


	public RealScatteredArchive(Path tempDir, Path webResourcesPath, Iterable<Path> classpath)
			throws IOException
	{
		Path webResourcesRoot = webResourcesPath.toAbsolutePath();
		directoryEventSource = new DirectoryEventSource(webResourcesRoot.getFileSystem());
		archiveRoot = Files.createTempDirectory(tempDir, ARCHIVE_ROOT_PREFIX);

		WebResourcesSynchronizer webResources = new WebResourcesSynchronizer(webResourcesRoot, archiveRoot);
		directoryEventSource.register(webResourcesPath, webResources);

		final Path archiveClasses = Files.createDirectory(archiveRoot.resolve(WEBINF).resolve(CLASSES));
		final Path archiveLib = Files.createDirectory(archiveClasses.resolveSibling(LIB));

		List<Path> classpathEntries = new ArrayList<>();
		for (final Path classpathEntry : classpath)
		{
			if (Files.isDirectory(classpathEntry))
			{
				classpathEntries.add(classpathEntry);
			}
			else
			{
				Path library = makeUnique(archiveLib.resolve(classpathEntry.getFileName()), 1);
				Files.copy(classpathEntry, library);
			}
		}
		ClasspathEntriesSynchronizer classpathSynchronizer =
				new ClasspathEntriesSynchronizer(classpathEntries, archiveClasses);
		for (Path classpathEntry : classpathEntries)
		{
			directoryEventSource.register(classpathEntry, classpathSynchronizer);
		}

		new Thread(directoryEventSource).start();
	}


	public Path toPath()
	{
		return archiveRoot;
	}


	@Override
	public void close() throws IOException
	{
		directoryEventSource.stop();
	}


	/**
	 * Ensure a unique path, adding an n-digit sequence number if needed. The sequence number is added before the file
	 * extension, if it exists. If needed, the sequence number will have more than the specified number of digits.
	 *
	 * @param path      the path to make unique
	 * @param numDigits the minimum number of sequence number digits
	 * @return a path that doesn't exist yet
	 */
	public static Path makeUnique(Path path, int numDigits)
	{
		if (!Files.exists(path))
		{
			return path;
		}

		Path directory = path.getParent();
		String fileName = path.getFileName().toString();

		String prefix, extension;
		int lastDot = fileName.lastIndexOf('.');
		if (lastDot == -1)
		{
			prefix = fileName;
			extension = "";
		}
		else
		{
			prefix = fileName.substring(0, lastDot);
			extension = fileName.substring(lastDot);
		}

		int n = 1;
		String format = "%s.%0" + numDigits + "d%s";
		Path result;
		do
		{
			result = directory.resolve(String.format(format, prefix, n++, extension));
		}
		while(Files.exists(result));

		return result;
	}
}
