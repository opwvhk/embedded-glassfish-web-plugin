package net.sf.opk.glassfish;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;


/**
 * MOJO to stop the embedded GlassFish instance that was started with this plugin.
 *
 * @author <a href="mailto:oscar.westra@42.nl">Oscar Westra van Holthe - Kind</a>
 * @goal stop
 * @phase post-integration-test
 * @requiresDependencyResolution test
 * @threadSafe false
 */
public class StopMojo extends EmbeddedGlassFishMojo
{
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		shutdown();
	}
}
