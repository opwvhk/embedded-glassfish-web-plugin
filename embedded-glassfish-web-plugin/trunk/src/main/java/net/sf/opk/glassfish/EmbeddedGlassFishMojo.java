package net.sf.opk.glassfish;

import org.apache.maven.plugin.AbstractMojo;
import org.glassfish.embeddable.GlassFishException;


/**
 * Abstract MOJO to control a single embedded GlassFish instance. Note that this class keeps track of its embedded
 * GlassFish instance in, and hence is <strong>NOT</strong> thread-safe.
 *
 * @author <a href="mailto:oscar@westravanholthe.nl">Oscar Westra van Holthe - Kind</a>
 */
public abstract class EmbeddedGlassFishMojo extends AbstractMojo
{
	/**
	 * The ebmedded GlassFish instance.
	 */
	private static EmbeddedGlassFish glassfish = null;


	/**
	 * Get the embedded GlassFish instance. Initialize one if this is the firast call.
	 *
	 * @return an embedded GlassFish instance
	 * @throws GlassFishException when initialization fails
	 */
	protected EmbeddedGlassFish getEmbeddedGlassFish() throws GlassFishException
	{
		if (glassfish == null)
		{
			glassfish = createEmbeddedGlassFish();
		}
		return glassfish;
	}


	/**
	 * Create the embedded GlassFish instance.
	 *
	 * @return an embedded GlassFish instance
	 * @throws GlassFishException when initialization fails
	 */
	protected abstract EmbeddedGlassFish createEmbeddedGlassFish() throws GlassFishException;


	protected void startup() throws GlassFishException
	{
		EmbeddedGlassFish instance = getEmbeddedGlassFish();
		instance.startup();
	}


	protected void shutdown()
	{
		try
		{
			if (glassfish != null)
			{
				glassfish.shutdown();
				//noinspection UnusedAssignment
				glassfish = null;
			}
		}
		catch (GlassFishException e)
		{
			getLog().error("GlassFish failed to shutdown. Please stop the JVM.", e);
		}
	}
}
