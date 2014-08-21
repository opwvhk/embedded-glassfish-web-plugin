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
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;


/**
 * Abstract MOJO to control an embedded GlassFish instance with the current project as scattered archive. Note that this
 * class keeps track of its embedded GlassFish instance, and hence is <strong>NOT</strong> thread-safe.
 *
 * @author <a href="mailto:oscar@westravanholthe.nl">Oscar Westra van Holthe - Kind</a>
 */
public abstract class ConfiguredEmbeddedGlassFishMojo extends net.sf.opk.glassfish.EmbeddedGlassFishMojo
{
	/**
	 * This plugin, as configured.
	 */
	@Parameter(defaultValue = "${plugin}", required = true, readonly = true)
	private PluginDescriptor plugin;
	/**
	 * The maven project.
	 */
	@Parameter(defaultValue = "${executedProject}", required = true, readonly = true)
	private MavenProject project;
	/**
	 * The context root to deploy the application at. Defaults to the artifact.
	 */
	@Parameter(defaultValue = "/${project.artifactId}")
	private String contextRoot;
	/**
	 * If true, the &lt;testClassesDirectory&gt; will be put on the runtime classpath before &lt;classesDirectory&gt;
	 * and the dependencies of &lt;scope&gt;test&lt;scope&gt; will be put on the runtime classpath as well.
	 */
	@Parameter(defaultValue = "false")
	private boolean useTestClasspath;
	/**
	 * The target directory.
	 */
	@Parameter(property = "project.build.directory", required = true, readonly = true)
	private File targetDirectory;
	/**
	 * The directory where classes are compiled to.
	 */
	@Parameter(property = "project.build.outputDirectory", required = true)
	private File classesDirectory;
	/**
	 * The directory where classes test are compiled to.
	 */
	@Parameter(property = "project.build.testOutputDirectory", required = true)
	private File testClassesDirectory;
	/**
	 * The directory that contains the resources of the web application.
	 */
	@Parameter(defaultValue = "${project.basedir}/src/main/webapp", required = true)
	private File webAppSourceDirectory;
	/**
	 * A file to configure <code>java.util.logging</code>, which is the logging system used by GlassFish.
	 */
	@Parameter
	private File loggingProperties;
	/**
	 * A resource file that defines the external resources required by the web application. Similar to
	 * <code>${webAppSourceDirectory}/WEB-INF/glassfish-resources.xml</code>, but some environments require database
	 * passwords etc. to be kept outside your application. This simulates that by loading the resources before the
	 * application is deployed.
	 */
	@Parameter
	private File glassFishResources;
	/**
	 * The file realms to create prior to deploying the application. The predefined realms &quot;file&quot; and
	 * &quot;admin-realm&quot; are recognized and not created anew, though the users you define are added. The
	 * predefined realm &quot;certificate&quot; is also recognized, but will generate an error (it is not a file
	 * realm).
	 */
	@Parameter
	private FileRealm[] fileRealms;
	/**
	 * Commands to <code>asadmin</code> to execute prior to deploying the application. The commands required for the
	 * properties <code>glassFishResources</code> and <code>fileRealms</code> will already be executed.
	 */
	@Parameter
	private Command[] extraCommands;
	/**
	 * The HTTP port GlassFish should listen on. Defaults to 8080.
	 */
	@Parameter(defaultValue = "8080")
	private int httpPort;
	/**
	 * The HTTPS port GlassFish should listen on. Defaults to 8443, 0 means "none".
	 */
	@Parameter(defaultValue = "8443")
	private int httpsPort;
	/**
	 * All dependencies, by type, in the iteration order of {@link MavenProject#getArtifacts()}.
	 */
	private Map<String, List<Artifact>> dependenciesByType = null;
	/**
	 * The GlassFish web plugin runner.
	 */
	private Callable<Void> glassFishWebPluginRunner = null;
	/**
	 * Redeploy hook for our web application.
	 */
	private Callable<Void> webApplicationRedeployHook = null;


	/**
	 * Configures and starts GlassFish.
	 */
	protected void startup() throws MojoExecutionException
	{
		Callable<Void> glassFishWebPluginRunner = getGlassFishWebPluginRunner();

		try
		{
			glassFishWebPluginRunner.call();
			setGlassFishShutdownHook((Callable<?>)callAccessor(glassFishWebPluginRunner, "getShutdownHook"));
			webApplicationRedeployHook = callAccessor(glassFishWebPluginRunner, "getRedeployHook");
		}
		catch (Exception e)
		{
			throw new MojoExecutionException("Failed to start GlassFish.", e);
		}
	}


	Callable<Void> getGlassFishWebPluginRunner() throws MojoExecutionException
	{
		try
		{
			if (glassFishWebPluginRunner == null)
			{
				byte[] configurationBytes = buildConfiguration();
				glassFishWebPluginRunner = createGlassFishWebPluginRunner(configurationBytes);
			}
			return glassFishWebPluginRunner;
		}
		catch (Exception e)
		{
			throw new MojoExecutionException("Failed to create the plugin runner.", e);
		}
	}


	byte[] buildConfiguration() throws IOException
	{
		Integer actualHttpsPort = null;
		if (httpsPort != 0)
		{
			actualHttpsPort = httpsPort;
		}
		GlassFishConfiguration configuration =
				new GlassFishConfiguration(httpPort, actualHttpsPort, contextRoot, webAppSourceDirectory,
				                           targetDirectory);
		if (classesDirectory.exists())
		{
			configuration.addToWebApplicationClassPath(classesDirectory);
			getLog().info("Adding directory " + classesDirectory + " to WEB-INF/classes");
		}
		if (useTestClasspath && testClassesDirectory.exists())
		{
			configuration.addToWebApplicationClassPath(testClassesDirectory);
			getLog().info("Adding directory " + testClassesDirectory + " to WEB-INF/classes");
		}
		for (Artifact artifact : findDependencies("jar"))
		{
			configuration.addToWebApplicationClassPath(artifact.getFile());
			getLog().info("Adding artifact " + artifact.getFile().getName() + " to WEB-INF/lib");
		}

		if (loggingProperties != null)
		{
			configuration.addLoggingProperties(loggingProperties);
		}
		if (glassFishResources != null)
		{
			configuration.addGlassFishResources(glassFishResources);
		}
		configuration.addFileRealms(fileRealms);
		configuration.addExtraCommands(extraCommands);

		for (Artifact artifact : findDependencies("war", "ear"))
		{
			File file = artifact.getFile();
			getLog().debug("Deploying dependency " + file.getName());
			configuration.addExtraApplication(file);
			getLog().info("Deployed dependency " + file.getName());
		}

		return configuration.toByteArray();
	}


	private List<Artifact> findDependencies(String... types)
	{
		if (dependenciesByType == null)
		{
			resolveValidDependencies();
		}

		List<Artifact> result = new ArrayList<>();
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
		Set<String> validScopes = new HashSet<>();
		validScopes.add(Artifact.SCOPE_COMPILE);
		validScopes.add(Artifact.SCOPE_RUNTIME);
		if (useTestClasspath)
		{
			validScopes.add(Artifact.SCOPE_TEST);
		}

		dependenciesByType = new HashMap<>();
		for (Artifact artifact : project.getArtifacts())
		{
			if (validScopes.contains(artifact.getScope()))
			{
				List<Artifact> dependencies = dependenciesByType.get(artifact.getType());
				if (dependencies == null)
				{
					dependencies = new ArrayList<>();
					dependenciesByType.put(artifact.getType(), dependencies);
				}
				dependencies.add(artifact);
			}
		}
	}


	private Callable<Void> createGlassFishWebPluginRunner(byte[] configurationBytes)
			throws MalformedURLException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
			       InvocationTargetException, InstantiationException
	{
		// The JVM always has these ClassLoaders:
		// - bootstrap - all Java classes
		// - extended  - bootstrap, plus all extended (jre/lib/ext) classes
		// - system    - extended, plus the command line classpath

		// We need everything from the JVM, but nothing more.

		ClassLoader extendedClassLoader = ClassLoader.getSystemClassLoader().getParent();

		URLClassLoader glassFishClassLoader = new URLClassLoader(getPluginClassPathWithoutMaven(), extendedClassLoader);
		Thread.currentThread().setContextClassLoader(glassFishClassLoader);

		Class<?> glassFishWebPluginRunnerClass =
				glassFishClassLoader.loadClass(GlassFishWebPluginRunner.class.getName());
		Constructor<?> constructor = glassFishWebPluginRunnerClass.getConstructor(byte[].class);
		//noinspection PrimitiveArrayArgumentToVariableArgMethod
		return (Callable<Void>)constructor.newInstance(configurationBytes);
	}


	private URL[] getPluginClassPathWithoutMaven() throws MalformedURLException
	{
		List<URL> classPath = new ArrayList<>();
		// PluginDescriptor#getArtifacts() returns the entire classpath of the plugin, except for the Maven classes.
		for (Artifact artifact : plugin.getArtifacts())
		{
			classPath.add(artifact.getFile().toURI().toURL());
		}

		return classPath.toArray(new URL[classPath.size()]);
	}


	static <T> T callAccessor(Object bean, String accessorName)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
	{
		Method accessor = bean.getClass().getMethod(accessorName);
		return (T)accessor.invoke(bean);
	}


	protected void redeploy() throws MojoExecutionException
	{
		try
		{
			webApplicationRedeployHook.call();
		}
		catch (Exception e)
		{
			throw new MojoExecutionException("Failed to redeploy the web application.", e);
		}
	}
}
