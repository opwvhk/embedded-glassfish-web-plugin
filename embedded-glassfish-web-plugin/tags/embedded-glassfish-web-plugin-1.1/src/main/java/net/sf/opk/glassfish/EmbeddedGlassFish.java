package net.sf.opk.glassfish;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import org.glassfish.embeddable.BootstrapProperties;
import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.embeddable.Deployer;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.GlassFishProperties;
import org.glassfish.embeddable.GlassFishRuntime;
import org.glassfish.embeddable.archive.ScatteredArchive;


/**
 * Facade for an embedded GlassFish server. Note that although this class starts threads with the {@link #startup()}
 * method, it is NOT thread-safe.
 *
 * @author <a href="mailto:oscar.westra@42.nl">Oscar Westra van Holthe - Kind</a>
 */
public class EmbeddedGlassFish
{
	private GlassFishRuntime runtime;
	private GlassFish glassfish;
	private Deployer deployer;
	private CommandRunner commandRunner;
	private Stack<String> deployedArtifacts;


	/**
	 * Create and initialize an embedded GlassFish instance.
	 *
	 * @param httpPort  the port to use to listen to HTTP requests
	 * @param httpsPort the port to use to listen to HTTPS requests
	 * @throws GlassFishException when the server cannot be initialized
	 */
	public EmbeddedGlassFish(int httpPort, int httpsPort) throws GlassFishException
	{
		deployer = null;
		commandRunner = null;
		deployedArtifacts = new Stack<String>();

		System.setProperty("glassfish.embedded.tmpdir", "target");

		BootstrapProperties bootstrapProperties = new BootstrapProperties();
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		runtime = GlassFishRuntime.bootstrap(bootstrapProperties, classLoader);

		GlassFishProperties glassFishProperties = new GlassFishProperties();
		glassFishProperties.setPort("http-listener", httpPort);
		glassFishProperties.setPort("https-listener", httpsPort);
		glassfish = runtime.newGlassFish(glassFishProperties);
	}


	/**
	 * Check the status of the embedded GlassFish instance. If incorrect, an {@code IllegalStateException} is thrown.
	 *
	 * @param running whether the embedded GlassFish instance should be running or not
	 * @throws GlassFishException when the GlassFish status is unavailable
	 */
	private void checkStatus(boolean running) throws GlassFishException
	{
		GlassFish.Status status = glassfish.getStatus();
		if (running && status != GlassFish.Status.STARTED)
		{
			throw new IllegalStateException(String.format(
					"The embedded GlassFish instance is not running (status=%s).", status));
		}
		if (!running && status != GlassFish.Status.INIT && status != GlassFish.Status.STOPPED)
		{
			throw new IllegalStateException(String.format(
					"The embedded GlassFish instance is not ready to run (status=%s).", status));
		}
	}


	/**
	 * Start the embedded GlassFish instance.
	 *
	 * @throws GlassFishException when the server cannot be started
	 */
	public void startup() throws GlassFishException
	{
		checkStatus(false);

		glassfish.start();
		deployer = glassfish.getDeployer();
		commandRunner = glassfish.getCommandRunner();

		// TODO: Add method to add a user here.
		// TODO: Test form login.
		// TODO: Add configuration option to add users to ConfiguredEmbeddedGlassFishMojo.
		// TODO: Increase branch coverage to 100% (line coverage is 100%).
		//asadmin("list-auth-realms");
		//asadmin("list-file-users");
		//asadmin("create-file-user", "--passwordfile", "passwds.txt", "--groups", "users:administrators", "oscar");
		//asadmin("list-file-users");
	}

	private void asadmin(String command, String... arguments)
	{
		System.out.printf("GlassFish asadmin: %s %s\n", command, java.util.Arrays.<String>asList(arguments));
		CommandResult result = commandRunner.run(command, arguments);
		System.out.printf("GlassFish [%s] %s\n", result.getExitStatus(), result.getOutput());
		if (result.getFailureCause() != null)
		{
			result.getFailureCause().printStackTrace();
		}
	}



	/**
	 * Add resources from a resource file to the embedded GlassFish instance.
	 *
	 * @param resourceFile the resource file
	 * @return a command result that tells whether adding the resources succeeded
	 */
	public CommandResult addResources(File resourceFile) throws GlassFishException
	{
		checkStatus(true);

		return commandRunner.run("add-resources", resourceFile.getPath());
	}


	/**
	 * Deploy an application from a file. Undeploying is only possible with {@link #undeployAllApplications()}.
	 *
	 * @param file the file to deploy
	 * @throws GlassFishException when deployment fails
	 */
	public void deployApplication(File file) throws GlassFishException
	{
		checkStatus(true);

		deployer.deploy(file);
	}


	/**
	 * Deploy en artifact. The last deployed artifact can be undeployed with {@link #undeployLastArtifact()}.
	 *
	 * @param artifact    the artifact to deploy
	 * @param contextRoot the context root to use
	 * @throws IOException        when the artifact is unavailable
	 * @throws GlassFishException when deployment fails
	 */
	public void deployArtifact(ScatteredArchive artifact, String contextRoot) throws IOException, GlassFishException
	{
		checkStatus(true);

		URI location = artifact.toURI();
		String application = deployer.deploy(location, "--contextroot", contextRoot, "--createtables", "true");
		deployedArtifacts.push(application);
	}


	/**
	 * Undeploy the artifact that was the last one deployed with {@link #deployArtifact(ScatteredArchive, String)}.
	 * Calling this method again undeploys the artifact deployed before that, etc.
	 *
	 * @throws GlassFishException when undeployment fails
	 */
	public void undeployLastArtifact() throws GlassFishException
	{
		if (!deployedArtifacts.empty())
		{
			deployer.undeploy(deployedArtifacts.pop(), "--droptables", "true");
		}
	}


	/**
	 * Undeploys all applications, artifacts first.
	 *
	 * @throws GlassFishException when undeployment fails
	 */
	public void undeployAllApplications() throws GlassFishException
	{
		while (!deployedArtifacts.empty())
		{
			deployer.undeploy(deployedArtifacts.pop(), "--droptables", "true");
		}

		List<String> deployedApplications = new ArrayList<String>(deployer.getDeployedApplications());
		Collections.reverse(deployedApplications);
		for (String application : deployedApplications)
		{
			deployer.undeploy(application);
		}
	}


	/**
	 * Stop and dispose of the embedded GlassFish instance.
	 *
	 * @throws GlassFishException when the server cannot be shutdown
	 */
	public void shutdown() throws GlassFishException
	{
		if (deployer != null)
		{
			undeployAllApplications();
			deployer = null;
		}
		if (glassfish.getStatus() == GlassFish.Status.STARTED)
		{
			glassfish.stop();
		}
		if (glassfish.getStatus() != GlassFish.Status.DISPOSED)
		{
			glassfish.dispose();
		}
		runtime.shutdown();
	}
}
