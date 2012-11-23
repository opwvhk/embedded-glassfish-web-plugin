/*
 * Copyright 2012 Oscar Westra van Holthe - Kind
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
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.stubs.ArtifactStub;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.project.MavenProject;
import org.glassfish.embeddable.CommandResult;


public class MojoTestBase
{
	private static final String APP_NAME = "testApp";
	protected static final String APP_PATH = "/test";
	protected static final int HTTP_PORT = 8180;
	protected static final int HTTPS_PORT = 8543;
	protected static final File NONEXISTING_PATH = new File("/tmp/ksgfjhgfjksgfjsd");
	protected static final File BASE_DIRECTORY = getBaseDirectory();
	// Note: the next two paths are correct, as the class dummy.EchoDataSource should not be included with the plugin.
	private static final File CLASSES_DIRECTORY = new File(BASE_DIRECTORY, "target/test-classes");
	private static final File TEST_CLASSES_DIRECTORY = new File(BASE_DIRECTORY, "target/classes");
	private static final File WEBAPP_DIRECTORY = resource("/dummyapp");
	private static final File JAR_ARTIFACT_1 = resource("/minimal_jar1.jar");
	private static final File JAR_ARTIFACT_2 = resource("/minimal_jar2.jar");
	private static final File JAR_ARTIFACT_3 = resource("/minimal_jar3.jar");
	protected static final File WAR_ARTIFACT = resource("/minimal_war.war");


	/**
	 * Locate the base directory.
	 *
	 * @return the base directory
	 */
	private static File getBaseDirectory()
	{
		try
		{
			return new File(resource("/dummyapp"), "../../../").getCanonicalFile();
		}
		catch (IOException e)
		{
			throw new IllegalStateException("Unable to locate the base directory.", e);
		}
	}


	/**
	 * Get a file reference to a resource on the classpath.
	 *
	 * @param name the name of the resource
	 * @return a File referencing the resource
	 */
	protected static File resource(String name)
	{
		try
		{
			URL location = MojoTestBase.class.getResource(name);
			if (location == null)
			{
				throw new IllegalArgumentException(String.format(
						"The resource \"%s\" cannot be found on the " + "classpath", name));
			}
			return new File(location.toURI());
		}
		catch (URISyntaxException ignored)
		{
			throw new IllegalArgumentException(String.format("The resource \"%s\" cannot be referenced as a URI",
			                                                 name));
		}
	}


	protected EmbeddedGlassFishMojo createAndConfigureMojo(EmbeddedGlassFish embeddedGlassFish) throws Exception
	{
		EmbeddedGlassFishMojo mojo = new ConfiguredEmbeddedGlassFishMojo()
		{
			@Override
			public void execute() throws MojoExecutionException, MojoFailureException
			{
				// Nothing to do.
			}
		};
		return configureBaseMojo(mojo, embeddedGlassFish);
	}


	protected ConfiguredEmbeddedGlassFishMojo createAndConfigureMojo(boolean useTestClassPath, File resources,
	                                                                 File loggingConfiguration,
	                                                                 EmbeddedGlassFish embeddedGlassFish)
			throws Exception
	{
		ConfiguredEmbeddedGlassFishMojo mojo = new ConfiguredEmbeddedGlassFishMojo()
		{
			@Override
			public void execute() throws MojoExecutionException, MojoFailureException
			{
				// Nothing to do.
			}
		};
		mojo = configureMojo(mojo, embeddedGlassFish);
		return configureMojo(mojo, APP_NAME, APP_PATH, CLASSES_DIRECTORY,
		                     useTestClassPath ? TEST_CLASSES_DIRECTORY : null, WEBAPP_DIRECTORY, resources,
		                     loggingConfiguration, HTTP_PORT, HTTPS_PORT);
	}


	protected <M extends ConfiguredEmbeddedGlassFishMojo> M configureMojo(M mojo, EmbeddedGlassFish embeddedGlassFish)
			throws Exception
	{
		mojo = configureBaseMojo(mojo, embeddedGlassFish);
		return configureMojo(mojo, APP_NAME, APP_PATH, CLASSES_DIRECTORY, null, WEBAPP_DIRECTORY, null, null, HTTP_PORT,
		                     HTTPS_PORT);
	}


	protected <M extends EmbeddedGlassFishMojo> M configureBaseMojo(M mojo, EmbeddedGlassFish embeddedGlassFish)
			throws Exception
	{
		getField(EmbeddedGlassFishMojo.class, "glassfish").set(null, embeddedGlassFish);

		return mojo;
	}


	private <M extends ConfiguredEmbeddedGlassFishMojo> M configureMojo(M mojo, String name, String path,
	                                                                    File classesDirectory,
	                                                                    File testClassesDirectory, File webAppDirectory,
	                                                                    File resources, File loggingConfiguration,
	                                                                    int httpPort, int httpsPort) throws Exception
	{
		MavenProject project = new MavenProjectStub()
		{
			@Override
			public Set<Artifact> getArtifacts()
			{
				//noinspection unchecked
				return new HashSet<Artifact>(getAttachedArtifacts());
			}
		};
		project.addAttachedArtifact(createArtifact(Artifact.SCOPE_COMPILE, JAR_ARTIFACT_1));
		project.addAttachedArtifact(createArtifact(Artifact.SCOPE_RUNTIME, JAR_ARTIFACT_2));
		project.addAttachedArtifact(createArtifact(Artifact.SCOPE_TEST, JAR_ARTIFACT_3));
		project.addAttachedArtifact(createArtifact(Artifact.SCOPE_RUNTIME, WAR_ARTIFACT));

		boolean useTestClassPath = false;
		if (testClassesDirectory == null)
		{
			testClassesDirectory = NONEXISTING_PATH;
		}
		else
		{
			useTestClassPath = true;
		}

		getField(ConfiguredEmbeddedGlassFishMojo.class, "project").set(mojo, project);
		getField(ConfiguredEmbeddedGlassFishMojo.class, "applicationName").set(mojo, name);
		getField(ConfiguredEmbeddedGlassFishMojo.class, "contextRoot").set(mojo, path);
		getField(ConfiguredEmbeddedGlassFishMojo.class, "useTestClasspath").setBoolean(mojo, useTestClassPath);
		getField(ConfiguredEmbeddedGlassFishMojo.class, "classesDirectory").set(mojo, classesDirectory);
		getField(ConfiguredEmbeddedGlassFishMojo.class, "testClassesDirectory").set(mojo, testClassesDirectory);
		getField(ConfiguredEmbeddedGlassFishMojo.class, "webAppSourceDirectory").set(mojo, webAppDirectory);
		getField(ConfiguredEmbeddedGlassFishMojo.class, "glassFishResources").set(mojo, resources);
		getField(ConfiguredEmbeddedGlassFishMojo.class, "loggingProperties").set(mojo, loggingConfiguration);
		getField(ConfiguredEmbeddedGlassFishMojo.class, "httpPort").setInt(mojo, httpPort);
		getField(ConfiguredEmbeddedGlassFishMojo.class, "httpsPort").setInt(mojo, httpsPort);

		return mojo;
	}


	private Artifact createArtifact(String scope, File file)
	{
		String type = "jar";
		if (file != null)
		{
			String fileName = file.getName();
			int dotPos = fileName.indexOf('.');
			if (dotPos != -1)
			{
				type = fileName.substring(dotPos + 1, fileName.length());
			}
		}
		ArtifactStub artifact = new ArtifactStub();
		artifact.setGroupId("group");
		artifact.setArtifactId("artifact");
		artifact.setVersion("1-SNAPSHOT");
		artifact.setScope(scope);
		artifact.setType(type);
		artifact.setFile(file);
		return artifact;
	}


	private Field getField(Class type, String fieldName) throws Exception
	{
		Field field = type.getDeclaredField(fieldName);
		field.setAccessible(true);
		return field;
	}


	protected class CommandResultStub implements CommandResult
	{
		private final ExitStatus exitStatus;
		private final Throwable failureCause;
		private final String output;


		protected CommandResultStub(ExitStatus exitStatus, Throwable failureCause, String output)
		{
			this.exitStatus = exitStatus;
			this.failureCause = failureCause;
			this.output = output;
		}


		@Override
		public ExitStatus getExitStatus()
		{
			return exitStatus;
		}


		@Override
		public Throwable getFailureCause()
		{
			return failureCause;
		}


		@Override
		public String getOutput()
		{
			return output;
		}
	}
}
