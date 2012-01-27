package net.sf.opk.glassfish;

import org.apache.maven.plugin.AbstractMojo;
import org.glassfish.embeddable.GlassFishException;


/**
 * Abstract MOJO to control a single embedded GlassFish instance. Note that this class keeps track of its embedded
 * GlassFish instance in, and hence is <strong>NOT</strong> thread-safe.
 *
 * @author <a href="mailto:oscar.westra@42.nl">Oscar Westra van Holthe - Kind</a>
 */
public abstract class EmbeddedGlassFishMojo extends AbstractMojo
{
	/**
	 * The HTTP port GlassFish should listen on. Defaults to 8080.
	 *
	 * @parameter default-value="8080"
	 */
	private int httpPort;
	/**
	 * The HTTPS port GlassFish should listen on. Defaults to 8443.
	 *
	 * @parameter default-value="8443"
	 */
	private int httpsPort;
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
			glassfish = new EmbeddedGlassFish(httpPort, httpsPort);
		}
		return glassfish;
	}


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
