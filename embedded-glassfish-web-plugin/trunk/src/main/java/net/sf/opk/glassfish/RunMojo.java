package net.sf.opk.glassfish;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.archive.ScatteredArchive;


/**
 * MOJO to run the artifact in an embedded GlassFish instance.
 *
 * @author <a href="mailto:oscar.westra@42.nl">Oscar Westra van Holthe - Kind</a>
 * @goal run
 * @execute phase="test-compile"
 * @ phase pre-integration-test
 * @requiresDependencyResolution test
 * @threadSafe false
 */
public class RunMojo extends ConfiguredEmbeddedGlassFishMojo
{
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		configureLogging();

		try
		{
			ScatteredArchive archive = createScatteredArchive();

			startupWithArtifact(archive);

			boolean continueRunning = true;
			BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
			while (continueRunning)
			{
				//noinspection UseOfSystemOutOrSystemErr
				System.out.printf("\n\nPress ENTER to redeploy the artifact, or 'X' + ENTER to exit.\n");

				String lineFromConsole = stdin.readLine();
				if ("X".equalsIgnoreCase(lineFromConsole))
				{
					// This will exit the loop.
					continueRunning = false;
				}
				else
				{
					redeploy(archive);
				}
			}
		}
		catch (GlassFishException e)
		{
			throw new MojoFailureException("GlassFish failed to do our bidding.", e);
		}
		catch (IOException e)
		{
			throw new MojoFailureException("I/O failure.", e);
		}
		finally
		{
			shutdown();
		}
	}
}
