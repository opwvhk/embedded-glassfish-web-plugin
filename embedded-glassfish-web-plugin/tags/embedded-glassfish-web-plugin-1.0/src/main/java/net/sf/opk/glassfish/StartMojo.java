package net.sf.opk.glassfish;

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.glassfish.embeddable.GlassFishException;


/**
 * MOJO to start an embedded GlassFish instance with the artifact deployed in it.
 *
 * @author <a href="mailto:oscar.westra@42.nl">Oscar Westra van Holthe - Kind</a>
 * @goal start
 * @phase pre-integration-test
 * @requiresDependencyResolution test
 * @threadSafe false
 */
public class StartMojo extends ConfiguredEmbeddedGlassFishMojo
{
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		configureLogging();

		try
		{
			startupWithArtifact(createScatteredArchive());
		}
		catch (GlassFishException e)
		{
			shutdown();
			throw new MojoFailureException("GlassFish failed to do our bidding.", e);
		}
		catch (IOException e)
		{
			shutdown();
			throw new MojoFailureException("I/O failure.", e);
		}
	}
}
