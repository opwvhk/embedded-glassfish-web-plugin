package net.sf.opk.glassfish;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import javax.security.auth.login.Configuration;

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
	private static final String DEFAULT_REALM_FILE        = "file";
	private static final String DEFAULT_REALM_ADMIN       = "admin-realm";
	private static final String DEFAULT_REALM_CERTIFICATE = "certificate";
	private GlassFishRuntime runtime;
	private GlassFish        glassfish;
	private Deployer         deployer;
	private CommandRunner    commandRunner;
	private Stack<String>    deployedArtifacts;


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

		// Workaround for a bug: GlassFish can't always find its own JAAS config...
		Configuration jaasConfiguration = Configuration.getConfiguration();
		if (jaasConfiguration.getAppConfigurationEntry("fileRealm") == null)
		{
			System.setProperty("java.security.auth.login.config", getClass().getResource("/config/login.conf")
					.toString());
			javax.security.auth.login.Configuration.getConfiguration().refresh();
		}
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
					"The embedded GlassFish instance is not running " + "(status=%s).", status));
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
	}


	/**
	 * Add a file realm to the GlassFish instance
	 *
	 * @param fileRealm the realm to add
	 * @throws IOException        when the temporary password file (a GlassFish oddity) cannot be written
	 * @throws GlassFishException when the realm cannot be added
	 */
	public void addFileRealm(FileRealm fileRealm) throws IOException, GlassFishException
	{
		String realmName = fileRealm.getRealmName();
		if (DEFAULT_REALM_CERTIFICATE.equals(realmName))
		{
			throw new GlassFishException(String.format("Cannot add users to the realm '%s': it is not a file realm.",
			                                           DEFAULT_REALM_CERTIFICATE));
		}
		if (!DEFAULT_REALM_FILE.equals(realmName) && !DEFAULT_REALM_ADMIN.equals(realmName))
		{
			String keyfile = writeString("keyfile", "");
			asadminInternal("create-auth-realm", "--classname", "com.sun.enterprise.security.auth.realm.file.FileRealm",
			                "--property", "file=" + keyfile + ":jaas-context=fileRealm", realmName);
		}

		for (User user : fileRealm.getUsers())
		{
			addUser(realmName, user);
		}
		// TODO: Increase line and branch coverage to 100%.
	}


	/**
	 * Write a String to a temporary file, and return the path to the file.
	 *
	 * @param prefix the prefix for the temporary file; can be used to identify the type of content
	 * @param text   the String to write
	 * @return the path to the temporary file
	 * @throws IOException when the file cannot be written
	 */
	private String writeString(String prefix, String text) throws IOException
	{
		File file = File.createTempFile(prefix, "");
		file.deleteOnExit();

		FileWriter writer = new FileWriter(file);
		writer.write(text);
		writer.write("\n");
		writer.close();

		return file.getAbsolutePath();
	}


	private void asadminInternal(String command, String... arguments)
	{
		CommandResult result = commandRunner.run(command, arguments);
		//noinspection ThrowableResultOfMethodCallIgnored
		Throwable failureCause = result.getFailureCause();
		if (failureCause != null)
		{
			failureCause.printStackTrace();
		}
	}


	/**
	 * Execute an asadmin command.
	 *
	 * @param command   the command to execute
	 * @param arguments any arguments for the command
	 * @return the command result
	 */
	public CommandResult asadmin(String command, String... arguments)
	{
		return commandRunner.run(command, arguments);
	}


	/**
	 * Create a user for a file realm.
	 *
	 * @param realmName the name of the file realm to add a user to
	 * @param user      the user to add
	 * @throws IOException        when the temporary password file (a GlassFish oddity) cannot be written
	 * @throws GlassFishException when the user cannot be added
	 */
	private void addUser(String realmName, User user) throws IOException, GlassFishException
	{
		String roles = join(user.getRoles(), ":");
		if (roles == null || roles.isEmpty())
		{
			roles = "user";
		}

		String passwordFilePath = writeString("passwd", "AS_ADMIN_USERPASSWORD=" + user.getPassword());

		asadminInternal("create-file-user", "--authrealmname", realmName, "--groups", roles, "--passwordfile",
		                passwordFilePath, user.getUsername());
	}


	/**
	 * Join the elements of a String array using a separator.
	 *
	 * @param array     a String array
	 * @param separator the separator to use
	 * @return the joined String
	 */
	private static String join(String[] array, String separator)
	{
		String result = null;
		if (array != null)
		{
			StringBuilder buffer = new StringBuilder();
			for (String element : array)
			{
				buffer.append(separator).append(element);
			}
			result = buffer.substring(separator.length());
		}
		return result;
	}


	/**
	 * Add resources from a resource file to the embedded GlassFish instance.
	 *
	 * @param resourceFile the resource file
	 * @return a command result that tells whether adding the resources succeeded
	 * @throws GlassFishException when the resources cannot be added
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
	 * Undeploy the artifact that was the last one deployed with {@link #deployArtifact(ScatteredArchive, String)}. Calling
	 * this method again undeploys the artifact deployed before that, etc.
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
