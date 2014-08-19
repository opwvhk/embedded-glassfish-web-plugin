package nl.opk.io;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;


/**
 * <p>Class similar to {@link java.nio.file.FileTreeWalker} to walk the union of several directories. The directories
 * are assumed to contain the same structure, i.e. if a path in one directory is (not) a file, it is also (not) a file
 * in all other directories.</p>
 *
 * <p>Differences with {@code java.nio.file.FileTreeWalker}:</p>
 *
 * <ul>
 *
 * <li>Uses a {@link PathVisitor} instead of a {@link java.nio.file.FileVisitor FileVisitor&lt;<Path&gt;}.</li>
 *
 * <li>The {@link PathVisitor} will be given a path relative to the starting point.</li>
 *
 * <li>Can only handle files and directories.</li>
 *
 * <li>As a result, links will not be followed (they are assumed to not exist).</li>
 *
 * <li>Crashes if access is denied instead of ignoring the path.</li>
 *
 * <li>There is no maximum traversal depth.</li>
 *
 * </ul>
 *
 * @author <a href="mailto:oscar@westravanholthe.nl">Oscar Westra van Holthe - Kind</a>
 */
public class UnionFileTreeWalker
{
	private final List<Path> roots;


	public UnionFileTreeWalker(List<Path> roots)
	{
		this.roots = Collections.unmodifiableList(new ArrayList<>(roots));
	}


	/**
	 * Walk the root directory trees starting at the given relative path, using a visitor.
	 *
	 * @param relativePath the relative path below all roots to start walking the directory trees
	 * @param visitor      the visitor to walk the directory trees with
	 */
	public void walk(Path relativePath, PathVisitor visitor) throws IOException
	{
		PathType type = determinePathType(relativePath);
		if (type == PathType.FILE)
		{
			visitor.visitFile(relativePath);
		}
		else if (type == PathType.DIRECTORY)
		{
			walkDirectory(relativePath, visitor);
		}
	}


	private void walkDirectory(Path relativePath, PathVisitor visitor) throws IOException
	{
		// Start walking directory path

		visitor.preVisitDirectory(relativePath);

		// Discover the children

		Set<Path> children = new TreeSet<>();
		for (Path root : roots)
		{
			children.addAll(findChildren(root, relativePath));
		}

		// Walk the children

		for (Path child: children)
		{
			walk(child, visitor);
		}

		// Stop walking the directory path
		visitor.postVisitDirectory(relativePath);
	}


	private Set<Path> findChildren(Path root, Path relativePath) throws IOException
	{
		Set<Path> children = new HashSet<>();

		Path path = root.resolve(relativePath);
		if (Files.exists(path, NOFOLLOW_LINKS))
		{
			try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path))
			{
				for (Path child : directoryStream)
				{
					children.add(root.relativize(child));
				}
			}
		}

		return children;
	}


	private enum PathType
	{
		/**
		 * Denotes a file.
		 */
		FILE,
		/**
		 * Denotes a directory.
		 */
		DIRECTORY,
		/**
		 * Denotes a non-existing path, a symbolic link, or a special file (for example a device node).
		 */
		OTHER
	}


	private PathType determinePathType(Path relativePath) throws IOException
	{
		for (Path root : roots)
		{
			Path path = root.resolve(relativePath);
			try
			{
				BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class, NOFOLLOW_LINKS);
				if (attributes.isDirectory())
				{
					return PathType.DIRECTORY;
				}
				else if (attributes.isRegularFile())
				{
					return PathType.FILE;
				}
			}
			catch (IOException ignored)
			{
				// If an exception occurs, the path doesn't exist (and it's neither a file nor a directory).
				// Note: this is also the way the various methods in java.nio.file.Files work...
			}
		}
		// The path exists neither as a file nor as a directory in any of the roots.
		return PathType.OTHER;
	}
}
