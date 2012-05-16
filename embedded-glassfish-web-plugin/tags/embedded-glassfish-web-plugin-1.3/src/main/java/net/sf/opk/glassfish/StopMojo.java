package net.sf.opk.glassfish;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.glassfish.embeddable.GlassFishException;


/**
 * MOJO to stop the embedded GlassFish instance that was started with this plugin.
 *
 * @author <a href="mailto:oscar@westravanholthe.nl">Oscar Westra van Holthe - Kind</a>
 * @goal stop
 * @phase post-integration-test
 * @requiresDependencyResolution test
 * @threadSafe false
 */
public class StopMojo extends EmbeddedGlassFishMojo
{
	@Override
	protected EmbeddedGlassFish createEmbeddedGlassFish() throws GlassFishException
	{
		// Nothing useful is possible, and this will throw an NPE.
		return null;
	}


	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		shutdown();
	}
}
