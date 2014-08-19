package nl.opk.io;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Class to synchronize web resources. Does not synchronize the directories with classpath entries.
 *
 * @author <a href="mailto:oscar@westravanholthe.nl">Oscar Westra van Holthe - Kind</a>
 */
public class WebResourcesSynchronizer implements DirectoryEventHandler
{
	/**
	 * Logger for this class.
	 */
	private static final Logger LOGGER = Logger.getLogger(WebResourcesSynchronizer.class.getName());
	/**
	 * The directory we're synchronizing into.
	 */
	private final Path targetDirectory;
	/**
	 * Our source web resources directory.
	 */
	private final Path sourceDirectory;
	/**
	 * The path {@code /WEB-INF/classes} in {@link #sourceDirectory}.
	 */
	private final Path webinfClasses;
	/**
	 * The path {@code /WEB-INF/lib} in {@link #sourceDirectory}.
	 */
	private final Path webinfLib;
	/**
	 * <p>{@link FileVisitor FileVisitor} that recursively copies a path to its destination <strong>on the same
	 * filesystem</strong> by hardlinking the files. Excludes {@link #webinfClasses} and {@link #webinfLib}.</p>
	 */
	private final FileVisitor<Path> recursiveWebResourceCopier;
	/**
	 * <p>{@link FileVisitor FileVisitor} that recursively deletes target web resources. Excludes {@link #webinfClasses}
	 * and {@link #webinfLib}.</p>
	 */
	private final FileVisitor<Path> recursiveWebResourceDeleter;


	public WebResourcesSynchronizer(final Path sourceDirectory, final Path targetDirectory) throws IOException
	{
		requireDirectory(sourceDirectory);
		requireDirectory(targetDirectory);
		this.targetDirectory = targetDirectory.toAbsolutePath();
		this.sourceDirectory = sourceDirectory.toAbsolutePath();

		Path webinf = sourceDirectory.resolve("WEB-INF");
		webinfClasses = sourceDirectory.relativize(webinf.resolve("classes"));
		webinfLib = sourceDirectory.relativize(webinf.resolve("lib"));

		recursiveWebResourceCopier = new SimpleFileVisitor<Path>()
		{
			@Override
			public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) throws IOException
			{
				Path relativePath = sourceDirectory.relativize(directory);
				if (!isWebResourcePath(relativePath))
				{
					return FileVisitResult.SKIP_SUBTREE;
				}
				Path directoryInTarget = targetDirectory.resolve(relativePath);

				Files.createDirectories(directoryInTarget);
				LOGGER.log(Level.FINE, "Created {}", directoryInTarget);
				return FileVisitResult.CONTINUE;
			}


			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException
			{
				Path relativePath = sourceDirectory.relativize(file);
				if (isWebResourcePath(relativePath))
				{
					Path fileInTarget = targetDirectory.resolve(relativePath);
					Files.createDirectories(fileInTarget.getParent());
					final Path linkTarget = file.toAbsolutePath();
					Files.createLink(fileInTarget, linkTarget);
					LOGGER.log(Level.FINE, "Linked {0} to {1}", new Object[]{fileInTarget, linkTarget});
				}

				return FileVisitResult.CONTINUE;
			}
		};

		recursiveWebResourceDeleter = new SimpleFileVisitor<Path>()
		{
			@Override
			public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) throws IOException
			{
				Path relativePath = targetDirectory.relativize(directory);
				if (!isWebResourcePath(relativePath))
				{
					LOGGER.log(Level.FINE, "Skipping {0}: it is not a web resource path", directory);
					return FileVisitResult.SKIP_SUBTREE;
				}
				return FileVisitResult.CONTINUE;
			}


			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException
			{
				Path relativePath = targetDirectory.relativize(file);
				if (isWebResourcePath(relativePath))
				{
					LOGGER.log(Level.FINE, "Deleting {0}", file);
					Files.deleteIfExists(file);
				}
				return FileVisitResult.CONTINUE;
			}


			@Override
			public FileVisitResult postVisitDirectory(Path directory, IOException exception) throws IOException
			{
				// Our superclass handles any directory traversal exception.
				super.postVisitDirectory(directory, exception);

				if (shouldDeleteDirectory(directory))
				{
					LOGGER.log(Level.FINE, "Deleting {0}", directory);
					Files.deleteIfExists(directory);
				}
				return FileVisitResult.CONTINUE;
			}
		};

		LOGGER.log(Level.CONFIG, "Clearing {0}", this.targetDirectory);
		clearTargetDirectory();
		LOGGER.log(Level.CONFIG, "Copying web resources from {0} into {1}",
		           new Object[]{sourceDirectory, targetDirectory});
		Files.walkFileTree(sourceDirectory, recursiveWebResourceCopier);
		LOGGER.log(Level.CONFIG, "Created {0} for {1}", new Object[]{toString(), this.targetDirectory});
	}


	private void requireDirectory(Path directory)
	{
		if (!Files.isDirectory(directory))
		{
			throw new IllegalArgumentException(directory + " is not a directory.");
		}
	}


	private boolean isWebResourcePath(Path relativePath)
	{
		return !relativePath.startsWith(webinfClasses) && !relativePath.startsWith(webinfLib);
	}


	/**
	 * Determine whether to delete a directory or not. The default implementation tests if the directory is empty (if
	 * so, it should be deleted).
	 *
	 * @param directory a directory
	 * @return whether to delete the directory
	 * @throws IOException when the directory cannot be read
	 */
	private boolean shouldDeleteDirectory(Path directory) throws IOException
	{
		// We never delete the target directory.

		if (directory.equals(targetDirectory))
		{
			return false;
		}

		// We can only delete empty directories.

		try (DirectoryStream directoryStream = Files.newDirectoryStream(directory))
		{
			final boolean empty = !directoryStream.iterator().hasNext();
			LOGGER.log(Level.FINE, "Directory {0} {1} empty: {2}",
			           new Object[]{directory, empty ? "is" : "is not", empty ? "deleting" : "skipping"});
			return empty;
		}
	}


	private void clearTargetDirectory() throws IOException
	{
		Files.walkFileTree(targetDirectory, new SimpleFileVisitor<Path>()
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
				if (!targetDirectory.equals(directory))
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
	public void handle(WatchEvent.Kind<?> event, Path root, Path sourcePath) throws IOException
	{
		if (!sourceDirectory.equals(root))
		{
			throw new IllegalArgumentException(
					String.format("Wrong source root: this WebResourcesSynchronizer is for %s, not %s", root,
					              sourceDirectory));
		}

		Path visitedPath;
		FileVisitor<Path> visitor;
		if (StandardWatchEventKinds.ENTRY_CREATE.equals(event) && isWebResourcePath(sourcePath))
		{
			visitedPath = sourceDirectory.resolve(sourcePath);
			visitor = recursiveWebResourceCopier;
		}
		else if (StandardWatchEventKinds.ENTRY_DELETE.equals(event) && isWebResourcePath(sourcePath))
		{
			visitedPath = targetDirectory.resolve(sourcePath);
			visitor = recursiveWebResourceDeleter;
		}
		else
		{
			// Unknown event type, or not a web resource: exit.
			return;
		}

		Files.walkFileTree(visitedPath, visitor);
	}
}
