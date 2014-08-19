package nl.opk.io;

import java.io.IOException;
import java.nio.file.FileVisitor;
import java.nio.file.Path;


/**
 * Path visitor. Basically, this is a simplified version of {@link FileVisitor FileVisitor&lt;Path&gt;}.
 *
 * @author <a href="mailto:owestra@bol.com">Oscar Westra van Holthe - Kind</a>
 */
public interface PathVisitor
{
	/**
	 * Invoked for a directory before any entries in the directory are visited.
	 *
	 * @param directory the path to the directory
	 * @throws IOException when I/O fails
	 */
	void preVisitDirectory(Path directory) throws IOException;

	/**
	 * Invoked for a file.
	 *
	 * @param file the path to the file
	 * @throws IOException when I/O fails
	 */
	void visitFile(Path file) throws IOException;

	/**
	 * Invoked for a directory after all entries in the directory are visited.
	 *
	 * @param directory the path to the directory
	 * @throws IOException when I/O fails
	 */
	void postVisitDirectory(Path directory) throws IOException;
}
