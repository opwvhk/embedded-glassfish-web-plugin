package net.sf.opk.glassfish;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.LogManager;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.archive.ScatteredArchive;


/**
 * Abstract MOJO to control an embedded GlassFish instance with the current project as scattered archive. Note that this
 * class keeps track of its embedded GlassFish instance, and hence is <strong>NOT</strong> thread-safe.
 *
 * @author <a href="mailto:oscar@westravanholthe.nl">Oscar Westra van Holthe - Kind</a>
 */
public abstract class ConfiguredEmbeddedGlassFishMojo extends net.sf.opk.glassfish.EmbeddedGlassFishMojo
{
	/**
	 * The maven project.
	 *
	 * @parameter default-value="${executedProject}"
	 * @required
	 * @readonly
	 */
	@SuppressWarnings({"UnusedDeclaration"})
	private MavenProject project;
	/**
	 * The context root to deploy the application at. Defaults to the artifact.
	 *
	 * @parameter expression="${project.build.finalName}" default-value="${project.build.finalName}"
	 */
	@SuppressWarnings({"UnusedDeclaration"})
	private String applicationName;
	/**
	 * The context root to deploy the application at. Defaults to the artifact.
	 *
	 * @parameter default-value="/${project.artifactId}"
	 */
	@SuppressWarnings({"UnusedDeclaration"})
	private String contextRoot;
	/**
	 * If true, the &lt;testClassesDirectory&gt; will be put on the runtime classpath before &lt;classesDirectory&gt;
	 * and the dependencies of &lt;scope&gt;test&lt;scope&gt; will be put on the runtime classpath as well.
	 *
	 * @parameter default-value="false"
	 */
	@SuppressWarnings({"UnusedDeclaration"})
	private boolean useTestClasspath;
	/**
	 * The directory where classes are compiled to.
	 *
	 * @parameter expression="${project.build.outputDirectory}" default-value="${project.build.outputDirectory}"
	 * @required
	 */
	@SuppressWarnings({"UnusedDeclaration"})
	private File classesDirectory;
	/**
	 * The directory where classes test are compiled to.
	 *
	 * @parameter expression="${project.build.testOutputDirectory}" default-value="${project.build.testOutputDirectory}"
	 * @required
	 */
	@SuppressWarnings({"UnusedDeclaration"})
	private File testClassesDirectory;
	/**
	 * The directory that contains the resources of the web application.
	 *
	 * @parameter default-value="${project.basedir}/src/main/webapp"
	 * @required
	 */
	@SuppressWarnings({"UnusedDeclaration"})
	private File webAppSourceDirectory;
	/**
	 * A file to configure <code>java.util.logging</code>, which is the logging system used by GlassFish.
	 *
	 * @parameter
	 */
	@SuppressWarnings({"UnusedDeclaration"})
	private File loggingProperties;
	/**
	 * A resource file that defines the external resources required by the web application. Similar to
	 * <code>${webAppSourceDirectory}/WEB-INF/glassfish-resources.xml</code>, but some environments require database
	 * passwords etc. to be kept outside your application. This simulates that by loading the resources before the
	 * application is deployed.
	 *
	 * @parameter
	 */
	@SuppressWarnings({"UnusedDeclaration"})
	private File glassFishResources;
	/**
	 * The file realms to create prior to deploying the application. The predefined realms &quot;file&quot; and
	 * &quot;admin-realm&quot; are recognized and not created anew, though the users you define are added. The
	 * predefined realm &quot;certificate&quot; is also recognized, but will generate an error (it is not a file realm).
	 *
	 * @parameter
	 */
	@SuppressWarnings({"UnusedDeclaration", "MismatchedReadAndWriteOfArray"})
	private FileRealm[] fileRealms;
	/**
	 * Commands to <code>asadmin</code> to execute prior to deploying the application. The commands required for the
	 * properties <code>glassFishResources</code> and <code>fileRealms</code> will already be executed.
	 *
	 * @parameter
	 */
	@SuppressWarnings({"UnusedDeclaration", "MismatchedReadAndWriteOfArray"})
	private Command[] extraCommands;
	/**
	 * The HTTP port GlassFish should listen on. Defaults to 8080.
	 *
	 * @parameter default-value="8080"
	 */
	@SuppressWarnings({"UnusedDeclaration"})
	private int httpPort;
	/**
	 * The HTTPS port GlassFish should listen on. Defaults to 8443.
	 *
	 * @parameter default-value="8443"
	 */
	@SuppressWarnings({"UnusedDeclaration"})
	private int httpsPort;
	/**
	 * All dependencies, by type, in the iteration order of {@link MavenProject#getArtifacts()}.
	 */
	private Map<String, List<Artifact>> dependenciesByType = null;


	protected EmbeddedGlassFish createEmbeddedGlassFish() throws GlassFishException
	{
		return new EmbeddedGlassFish(httpPort, httpsPort);
	}


	/**
	 * Configure logging for GlassFish using the specified {@link #loggingProperties} (if any).
	 */
	protected void configureLogging()
	{
		if (loggingProperties != null)
		{
			try
			{
				LogManager.getLogManager().readConfiguration(new FileInputStream(loggingProperties));
				getLog().info("Initialized logging using: " + loggingProperties.getPath());
			}
			catch (IOException e)
			{
				getLog().warn(String.format("Logging not initialized; failed to read %s (%s)", loggingProperties,
				                            e.getMessage()));
			}
		}
	}


	protected void startupWithArtifact(ScatteredArchive archive) throws GlassFishException, IOException
	{
		startup();
		EmbeddedGlassFish instance = getEmbeddedGlassFish();

		// Deploy any resources

		if (glassFishResources != null)
		{
			logCommandResult(instance.addResources(glassFishResources));
		}

		// Add any file realms

		if (fileRealms != null)
		{
			for (FileRealm fileRealm : fileRealms)
			{
				instance.addFileRealm(fileRealm);
			}
		}

		// Execute any other asadmin commands.

		if (extraCommands != null)
		{
			for (Command command : extraCommands)
			{
				logCommandResult(instance.asadmin(command.getCommand(), command.getParameters()));
			}
		}

		// We deploy both war and ear dependencies. The latter are not supported by the JavaEE Web Profile, but are
		// supported by a full JavaEE embedded GlassFish (which can be used by substituting the plugin dependency).

		for (Artifact artifact : findDependencies("war", "ear"))
		{
			File file = artifact.getFile();
			getLog().debug("Deploying dependency " + file.getName());
			instance.deployApplication(file);
			getLog().info("Deployed dependency " + file.getName());
		}

		// Deploy the web application.

		instance.deployArtifact(archive, contextRoot);
	}


	protected void logCommandResult(CommandResult result)
	{
		switch (result.getExitStatus())
		{
			case WARNING:
				getLog().warn(result.getOutput());
				break;
			case FAILURE:
				//noinspection ThrowableResultOfMethodCallIgnored
				getLog().error(result.getOutput(), result.getFailureCause());
				break;
			default:
				getLog().info(result.getOutput());
				break;
		}
	}


	/**
	 * Finds all dependencies of given types that are in a valid dependency scope.
	 *
	 * @param types the dependency types, e.g. &quot;jar&quot;, &quot;war&quot;, ...
	 * @return all dependencies of the given type
	 */
	protected List<Artifact> findDependencies(String... types)
	{
		if (dependenciesByType == null)
		{
			resolveValidDependencies();
		}

		List<Artifact> result = new ArrayList<Artifact>();
		for (String type : types)
		{
			List<Artifact> dependencies = dependenciesByType.get(type);
			if (dependencies != null)
			{
				result.addAll(dependencies);
			}
		}
		return result;
	}


	private void resolveValidDependencies()
	{
		Set<String> validScopes = new HashSet<String>();
		validScopes.add(Artifact.SCOPE_COMPILE);
		validScopes.add(Artifact.SCOPE_RUNTIME);
		if (useTestClasspath)
		{
			validScopes.add(Artifact.SCOPE_TEST);
		}

		dependenciesByType = new HashMap<String, List<Artifact>>();
		//noinspection unchecked
		for (Artifact artifact : (Set<Artifact>)project.getArtifacts())
		{
			if (validScopes.contains(artifact.getScope()))
			{
				List<Artifact> dependencies = dependenciesByType.get(artifact.getType());
				if (dependencies == null)
				{
					dependencies = new ArrayList<Artifact>();
					dependenciesByType.put(artifact.getType(), dependencies);
				}
				dependencies.add(artifact);
			}
		}
	}


	protected void redeploy(ScatteredArchive archive) throws GlassFishException, IOException
	{
		EmbeddedGlassFish instance = getEmbeddedGlassFish();
		instance.undeployLastArtifact();
		instance.deployArtifact(archive, contextRoot);
	}


	protected ScatteredArchive createScatteredArchive() throws IOException
	{
		ScatteredArchive archive = new ScatteredArchive(applicationName, ScatteredArchive.Type.WAR,
		                                                webAppSourceDirectory);
		if (classesDirectory.exists())
		{
			archive.addClassPath(classesDirectory);
			getLog().info("Adding directory " + classesDirectory + " to WEB-INF/classes");
		}
		if (useTestClasspath && testClassesDirectory.exists())
		{
			archive.addClassPath(testClassesDirectory);
			getLog().info("Adding directory " + testClassesDirectory + " to WEB-INF/classes");
		}

		//noinspection unchecked
		for (Artifact artifact : findDependencies("jar"))
		{
			archive.addClassPath(artifact.getFile());
			getLog().info("Adding artifact " + artifact.getFile().getName() + " to WEB-INF/lib");
		}
		return archive;
	}
}
