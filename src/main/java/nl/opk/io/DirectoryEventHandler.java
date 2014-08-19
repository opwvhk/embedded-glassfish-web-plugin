package nl.opk.io;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;


/**
 * Event handler for directory events.
 *
 * @author <a href="mailto:oscar@westravanholthe.nl">Oscar Westra van Holthe - Kind</a>
 */
public interface DirectoryEventHandler
{
	/**
	 * Report which kinds of events can be handled.
	 */
	WatchEvent.Kind<?>[] handledEvents();

	/**
	 * Called to handle an event. Overflow events may not have an associated context.
	 *
	 * @param event the event that happened; it's always one returned by {@link #handledEvents()} or {@link
	 *              StandardWatchEventKinds#OVERFLOW}
	 * @param root  the registered root where the event occurred
	 * @param path  the relative path that is the cause of the event
	 * @throws IOException when handling the event fails
	 */
	public void handle(WatchEvent.Kind<?> event, Path root, Path path) throws IOException;
}
