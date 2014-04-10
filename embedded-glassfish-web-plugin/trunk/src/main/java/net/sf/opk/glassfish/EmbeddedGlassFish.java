/*
 * Copyright 2012-2014 Oscar Westra van Holthe - Kind
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */
package net.sf.opk.glassfish;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.embeddable.BootstrapProperties;
import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.embeddable.Deployer;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.GlassFishProperties;
import org.glassfish.embeddable.GlassFishRuntime;
import org.glassfish.embeddable.archive.ScatteredArchive;
import org.glassfish.security.common.SSHA;


/**
 * Facade for an embedded GlassFish server, FOR INTERNAL USE ONLY. It is NOT thread-safe.
 *
 * @author <a href="mailto:oscar@westravanholthe.nl">Oscar Westra van Holthe - Kind</a>
 */
public class EmbeddedGlassFish
{
	/**
	 * Logger for this class.
	 */
	private static final Logger LOGGER = Logger.getLogger(EmbeddedGlassFish.class.getName());
	private static final Map<CommandResult.ExitStatus, Level> ASADMIN_RESULT_LOG_LEVELS;
	private static final String DEFAULT_REALM_FILE = "file";
	private static final String DEFAULT_REALM_ADMIN = "admin-realm";
	private static final String DEFAULT_REALM_CERTIFICATE = "certificate";
	private static final SecureRandom rng = new SecureRandom();
	private File configDir;
	private GlassFishRuntime runtime;
	private GlassFish glassfish;
	private Deployer deployer;
	private CommandRunner commandRunner;
	private Deque<String> deployedArtifacts;


	static
	{
		EnumMap<CommandResult.ExitStatus, Level> asadminResultLogLevels = new EnumMap<>(CommandResult.ExitStatus.class);
		asadminResultLogLevels.put(CommandResult.ExitStatus.SUCCESS, Level.INFO);
		asadminResultLogLevels.put(CommandResult.ExitStatus.WARNING, Level.WARNING);
		asadminResultLogLevels.put(CommandResult.ExitStatus.FAILURE, Level.SEVERE);
		ASADMIN_RESULT_LOG_LEVELS = Collections.unmodifiableMap(asadminResultLogLevels);
	}


	/**
	 * Create and initialize an embedded GlassFish instance.
	 *
	 * @param httpPort  the port to use to listen to HTTP requests
	 * @param httpsPort the port to use to listen to HTTPS requests, if any
	 * @throws GlassFishException when the server cannot be initialized
	 */
	public EmbeddedGlassFish(int httpPort, Integer httpsPort) throws GlassFishException
	{
		deployer = null;
		commandRunner = null;
		deployedArtifacts = new ArrayDeque<>();

		System.setProperty("glassfish.embedded.tmpdir", "target");

		BootstrapProperties bootstrapProperties = new BootstrapProperties();
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		runtime = GlassFishRuntime.bootstrap(bootstrapProperties, classLoader);

		GlassFishProperties glassFishProperties = new GlassFishProperties();
		glassFishProperties.setPort("http-listener", httpPort);
		if (httpsPort != null)
		{
			glassFishProperties.setPort("https-listener", httpsPort);
		}
		glassfish = runtime.newGlassFish(glassFishProperties);

		// Workaround for a bug: GlassFish can't always find its own JAAS config...
		System.setProperty("java.security.auth.login.config", getClass().getResource("/config/login.conf").toString());
		javax.security.auth.login.Configuration.getConfiguration().refresh();

		// Bootstrapping GlassFish points this system property a directory. The mkdirs() thus always succeeds.
		configDir = new File(System.getProperty("com.sun.aas.installRoot"), "config");
		configDir.mkdirs();

		glassfish.start();
		deployer = glassfish.getDeployer();
		commandRunner = glassfish.getCommandRunner();
	}


	/**
	 * Add a file realm to the GlassFish instance.
	 *
	 * @param fileRealm the realm to add
	 * @throws IOException        when the temporary password file (a GlassFish oddity) cannot be written
	 * @throws GlassFishException when the realm cannot be added
	 */
	public void addFileRealmWithUsers(FileRealm fileRealm) throws IOException, GlassFishException
	{
		String realmName = fileRealm.getRealmName();
		if (DEFAULT_REALM_CERTIFICATE.equals(realmName))
		{
			LOGGER.warning(String.format("Cannot add users to the realm '%s': it is not a file realm. Skipping it.",
			                             DEFAULT_REALM_CERTIFICATE));
			return;
		}

		File keyFile;
		boolean createRealm = false;
		if (DEFAULT_REALM_FILE.equals(realmName))
		{
			keyFile = new File(configDir, "keyfile");
		}
		else if (DEFAULT_REALM_ADMIN.equals(realmName))
		{
			keyFile = new File(configDir, "admin-keyfile");
		}
		else
		{
			keyFile = new File(writeStringToConfigFile("keyfile", ""));
			createRealm = true;
		}

		try (PrintWriter writer = new PrintWriter(new FileWriter(keyFile, true)))
		{
			for (User user : fileRealm.getUsers())
			{
				byte[] salt = new byte[8];
				rng.nextBytes(salt);
				byte[] password = user.getPassword().getBytes(Charset.defaultCharset());
				byte[] hash = SSHA.compute(salt, password, "SHA");
				String ssha = SSHA.encode(salt, hash, "SHA");
				// anonymous;{SSHA}w9WBMj/jphXlgDWPgozFSSzUgy5Fd/ONd7nPtw==;asadmin

				writer.println(user.getUsername() + ';' + ssha + ';' + join(user.getRoles(), ","));
			}
		}
		if (createRealm)
		{
			String keyFilePath = keyFile.getPath().replace("\\", "/").replace(":", "\\:");
			asadmin("create-auth-realm", "--classname", "com.sun.enterprise.security.auth.realm.file.FileRealm",
			        "--property", "file=" + keyFilePath + ":jaas-context=fileRealm", realmName);
		}
	}


	/**
	 * Write a String to a temporary file, and return the path to the file.
	 *
	 * @param prefix the prefix for the temporary file; can be used to identify the type of content
	 * @param text   the String to write
	 * @return the path to the temporary file
	 * @throws IOException when the file cannot be written
	 */
	private String writeStringToConfigFile(String prefix, String text) throws IOException
	{
		File file = File.createTempFile(prefix, "", configDir);
		file.deleteOnExit();

		try (PrintWriter writer = new PrintWriter(new FileWriter(file), true))
		{
			writer.println(text);
		}

		return file.getPath();
	}


	/**
	 * Execute an asadmin command and log the result.
	 *
	 * @param command   the command to execute
	 * @param arguments any arguments for the command
	 */
	public void asadmin(String command, String... arguments)
	{
		CommandResult result = commandRunner.run(command, arguments);
		Level level = ASADMIN_RESULT_LOG_LEVELS.get(result.getExitStatus());
		LOGGER.log(level, result.getOutput(), result.getFailureCause());
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
		StringBuilder buffer = new StringBuilder();
		for (String element : array)
		{
			buffer.append(separator).append(element);
		}
		return buffer.substring(separator.length());
	}


	/**
	 * Add resources from a resource file to the embedded GlassFish instance.
	 *
	 * @param resourceFile the resource file
	 */
	public void addResources(File resourceFile) throws GlassFishException
	{
		asadmin("add-resources", resourceFile.getPath());
	}


	/**
	 * Deploy an application from a file. Undeploying is only done by {@link #shutdown()}.
	 *
	 * @param file the file to deploy
	 * @throws GlassFishException when deployment fails
	 */
	public void deployApplication(File file) throws GlassFishException
	{
		deployer.deploy(file);
	}


	/**
	 * Deploy en artifact. The last deployed artifact can be undeployed with {@link #undeployArtifacts()}.
	 *
	 * @param artifact    the artifact to deploy
	 * @param contextRoot the context root to use
	 * @throws IOException        when the artifact is unavailable
	 * @throws GlassFishException when deployment fails
	 */
	public void deployArtifact(ScatteredArchive artifact, String contextRoot) throws IOException, GlassFishException
	{
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
	public void undeployArtifacts() throws GlassFishException
	{
		for (String deployedArtifact : deployedArtifacts)
		{
			deployer.undeploy(deployedArtifact, "--droptables", "true");
		}
	}


	/**
	 * Stop and dispose of the embedded GlassFish instance. Should only be called last, and only once.
	 *
	 * @throws GlassFishException when the server cannot be shutdown
	 */
	public void shutdown() throws GlassFishException
	{
		deployer = null;
		glassfish.stop();
		glassfish.dispose();
		runtime.shutdown();
	}
}
