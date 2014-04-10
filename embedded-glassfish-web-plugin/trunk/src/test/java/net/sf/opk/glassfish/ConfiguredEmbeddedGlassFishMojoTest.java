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
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.hamcrest.core.StringContains;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * Test class for the base class {@link ConfiguredEmbeddedGlassFishMojo}.
 *
 * @author <a href="mailto:oscar@westravanholthe.nl">Oscar Westra van Holthe - Kind</a>
 */
@SuppressWarnings({"ObjectEqualsNull", "EqualsBetweenInconvertibleTypes"})
public class ConfiguredEmbeddedGlassFishMojoTest extends MojoTestBase
{
	private static final String APP_PATH = "/test";
	private static final int HTTP_PORT = 8180;
	private static final int HTTPS_PORT = 8543;
	private static final File BASE_DIRECTORY = PathUtil.getBaseDirectory();
	private static final File CLASSES_DIRECTORY = new File(BASE_DIRECTORY, "target/classes");
	private static final File TEST_CLASSES_DIRECTORY = new File(BASE_DIRECTORY, "target/test-classes");
	private static final File NONEXISTING_DIRECTORY = new File(BASE_DIRECTORY, "target/dfskjhdfkjfghdfghkjdf");
	private static final File WEBAPP_DIRECTORY = PathUtil.resource("/dummyapp");
	private static final File JAR_ARTIFACT_C = PathUtil.resource("/minimal_jar1.jar");
	private static final File JAR_ARTIFACT_R = PathUtil.resource("/minimal_jar2.jar");
	private static final File JAR_ARTIFACT_T = PathUtil.resource("/minimal_jar3.jar");
	private static final File WAR_ARTIFACT_R = PathUtil.resource("/minimal_war.war");
	private static final String WAR_ARTIFACT_PATH = "/minimal_war/index.html";

	private static final File LOGGING_CONFIGURATION_FILE = PathUtil.resource("/logging.properties");
	private static final File RESOURCES_FILE = PathUtil.resource("/resources.xml");
	private static final List<File> PLUGIN_CLASSPATH = PathUtil.findClasspath(org.glassfish.embeddable.GlassFish.class,
	                                                                          GlassFishWebPluginRunner.class,
	                                                                          org.hsqldb.jdbc.JDBCDataSource.class);
	@Rule
	public ExpectedException expectedException = ExpectedException.none();


	@Test
	public void testConfigurationWrongClasspath() throws Exception
	{
		expectedException.expect(MojoExecutionException.class);
		expectedException.expectCause(isA(ClassNotFoundException.class));

		getConfigurationFromMojo(HTTP_PORT, HTTPS_PORT, LOGGING_CONFIGURATION_FILE, RESOURCES_FILE, APP_PATH,
		                         WEBAPP_DIRECTORY, NONEXISTING_DIRECTORY, true, NONEXISTING_DIRECTORY,
		                         Collections.<File>emptyList(), Collections.<FileRealm>emptyList(),
		                         Collections.<Command>emptyList());
	}


	@Test
	public void testConfigurationMinimal() throws Exception
	{
		GlassFishConfiguration configuration = getConfigurationFromMojo(HTTP_PORT, 0, null, null, APP_PATH,
		                                                                WEBAPP_DIRECTORY, NONEXISTING_DIRECTORY, true,
		                                                                NONEXISTING_DIRECTORY, PLUGIN_CLASSPATH,
		                                                                Collections.<FileRealm>emptyList(),
		                                                                Collections.<Command>emptyList());

		assertEquals(HTTP_PORT, configuration.getHttpPort());
		assertNull(configuration.getHttpsPort());
		assertEquals(Collections.<File>emptyList(), configuration.getLoggingProperties());
		assertEquals(Collections.<File>emptyList(), configuration.getGlassFishResources());
		assertEquals(APP_PATH, configuration.getContextRoot());
		assertEquals(WEBAPP_DIRECTORY, configuration.getWebApplicationSourceDirectory());
		List<File> webappClasspath = Arrays.asList(JAR_ARTIFACT_C, JAR_ARTIFACT_R, JAR_ARTIFACT_T);
		assertEquals(webappClasspath, configuration.getWebApplicationClassPath());
		assertEquals(Collections.singletonList(WAR_ARTIFACT_R), configuration.getExtraApplications());
		assertEquals(Collections.<FileRealm>emptyList(), configuration.getFileRealms());
		assertEquals(Collections.<Command>emptyList(), configuration.getExtraCommands());
	}


	@Test
	public void testConfigurationBasic() throws Exception
	{
		GlassFishConfiguration configuration = getConfigurationFromMojo(HTTP_PORT, HTTPS_PORT,
		                                                                LOGGING_CONFIGURATION_FILE, RESOURCES_FILE,
		                                                                APP_PATH, WEBAPP_DIRECTORY, CLASSES_DIRECTORY,
		                                                                false, NONEXISTING_DIRECTORY, PLUGIN_CLASSPATH,
		                                                                Collections.<FileRealm>emptyList(),
		                                                                Collections.<Command>emptyList());

		assertEquals(HTTP_PORT, configuration.getHttpPort());
		assertEquals((Integer)HTTPS_PORT, configuration.getHttpsPort());
		assertEquals(Collections.singletonList(LOGGING_CONFIGURATION_FILE), configuration.getLoggingProperties());
		assertEquals(Collections.singletonList(RESOURCES_FILE), configuration.getGlassFishResources());
		assertEquals(APP_PATH, configuration.getContextRoot());
		assertEquals(WEBAPP_DIRECTORY, configuration.getWebApplicationSourceDirectory());
		List<File> webappClasspath = Arrays.asList(CLASSES_DIRECTORY, JAR_ARTIFACT_C, JAR_ARTIFACT_R);
		assertEquals(webappClasspath, configuration.getWebApplicationClassPath());
		assertEquals(Collections.singletonList(WAR_ARTIFACT_R), configuration.getExtraApplications());
		assertEquals(Collections.<FileRealm>emptyList(), configuration.getFileRealms());
		assertEquals(Collections.<Command>emptyList(), configuration.getExtraCommands());
	}


	@Test
	public void testConfigurationFull() throws Exception
	{
		List<FileRealm> fileRealms = createFileRealms();
		List<Command> extraCommands = createCommands();

		GlassFishConfiguration configuration = getConfigurationFromMojo(HTTP_PORT, HTTPS_PORT,
		                                                                LOGGING_CONFIGURATION_FILE, RESOURCES_FILE,
		                                                                APP_PATH, WEBAPP_DIRECTORY, CLASSES_DIRECTORY,
		                                                                true, TEST_CLASSES_DIRECTORY, PLUGIN_CLASSPATH,
		                                                                fileRealms, extraCommands);

		assertEquals(HTTP_PORT, configuration.getHttpPort());
		assertEquals((Integer)HTTPS_PORT, configuration.getHttpsPort());
		assertEquals(Collections.singletonList(LOGGING_CONFIGURATION_FILE), configuration.getLoggingProperties());
		assertEquals(Collections.singletonList(RESOURCES_FILE), configuration.getGlassFishResources());
		assertEquals(APP_PATH, configuration.getContextRoot());
		assertEquals(WEBAPP_DIRECTORY, configuration.getWebApplicationSourceDirectory());
		List<File> webappClasspath = Arrays.asList(CLASSES_DIRECTORY, TEST_CLASSES_DIRECTORY, JAR_ARTIFACT_C,
		                                           JAR_ARTIFACT_R, JAR_ARTIFACT_T);
		assertEquals(webappClasspath, configuration.getWebApplicationClassPath());
		assertEquals(Collections.singletonList(WAR_ARTIFACT_R), configuration.getExtraApplications());
		assertEquals(fileRealms, configuration.getFileRealms());
		assertEquals(extraCommands, configuration.getExtraCommands());
	}


	private GlassFishConfiguration getConfigurationFromMojo(int httpPort, int httpsPort, File loggingConfiguration,
	                                                        File resources, String contextRoot,
	                                                        File webAppSourceDirectory, File classesDirectory,
	                                                        boolean useTestClassPath, File testClassesDirectory,
	                                                        List<File> pluginClasspath, List<FileRealm> fileRealms,
	                                                        List<Command> extraCommands) throws Exception
	{
		ConfiguredEmbeddedGlassFishMojo mojo = createAndconfigureMojo(httpPort, httpsPort, loggingConfiguration,
		                                                              resources, contextRoot, webAppSourceDirectory,
		                                                              classesDirectory, useTestClassPath,
		                                                              testClassesDirectory, pluginClasspath, fileRealms,
		                                                              extraCommands);

		Callable<Void> glassFishWebPluginRunner = mojo.getGlassFishWebPluginRunner();
		Object configuration = ConfiguredEmbeddedGlassFishMojo.callAccessor(glassFishWebPluginRunner,
		                                                                    "getConfiguration");
		byte[] rawData = ConfiguredEmbeddedGlassFishMojo.callAccessor(configuration, "toByteArray");
		return GlassFishConfiguration.fromByteArray(rawData);
	}


	private ConfiguredEmbeddedGlassFishMojo createAndconfigureMojo(int httpPort, int httpsPort,
	                                                               File loggingConfiguration, File resources,
	                                                               String contextRoot, File webAppSourceDirectory,
	                                                               File classesDirectory, boolean useTestClassPath,
	                                                               File testClassesDirectory,
	                                                               List<File> pluginClasspath,
	                                                               List<FileRealm> fileRealms,
	                                                               List<Command> extraCommands) throws Exception
	{
		ConfiguredEmbeddedGlassFishMojo mojo = new ConfiguredEmbeddedGlassFishMojo()
		{
			@Override
			public void execute() throws MojoExecutionException, MojoFailureException
			{
				// Nothing to do.
			}
		};

		Set<Artifact> projectArtifacts = new LinkedHashSet<>();
		projectArtifacts.add(new ArtifactStub(Artifact.SCOPE_COMPILE, JAR_ARTIFACT_C));
		projectArtifacts.add(new ArtifactStub(Artifact.SCOPE_RUNTIME, JAR_ARTIFACT_R));
		projectArtifacts.add(new ArtifactStub(Artifact.SCOPE_TEST, JAR_ARTIFACT_T));
		projectArtifacts.add(new ArtifactStub(Artifact.SCOPE_RUNTIME, WAR_ARTIFACT_R));
		MavenProject project = new MavenProject();
		project.setArtifacts(projectArtifacts);

		List<Artifact> pluginArtifacts = new ArrayList<>();
		for (File classpathEntry : pluginClasspath)
		{
			pluginArtifacts.add(new ArtifactStub(Artifact.SCOPE_COMPILE, classpathEntry));
		}
		PluginDescriptor plugin = new PluginDescriptor();
		plugin.setArtifacts(pluginArtifacts);

		getField(ConfiguredEmbeddedGlassFishMojo.class, "project").set(mojo, project);
		getField(ConfiguredEmbeddedGlassFishMojo.class, "plugin").set(mojo, plugin);
		getField(ConfiguredEmbeddedGlassFishMojo.class, "httpPort").setInt(mojo, httpPort);
		getField(ConfiguredEmbeddedGlassFishMojo.class, "httpsPort").setInt(mojo, httpsPort);
		getField(ConfiguredEmbeddedGlassFishMojo.class, "loggingProperties").set(mojo, loggingConfiguration);
		getField(ConfiguredEmbeddedGlassFishMojo.class, "glassFishResources").set(mojo, resources);
		getField(ConfiguredEmbeddedGlassFishMojo.class, "contextRoot").set(mojo, contextRoot);
		getField(ConfiguredEmbeddedGlassFishMojo.class, "webAppSourceDirectory").set(mojo, webAppSourceDirectory);
		getField(ConfiguredEmbeddedGlassFishMojo.class, "classesDirectory").set(mojo, classesDirectory);
		getField(ConfiguredEmbeddedGlassFishMojo.class, "useTestClasspath").setBoolean(mojo, useTestClassPath);
		getField(ConfiguredEmbeddedGlassFishMojo.class, "testClassesDirectory").set(mojo, testClassesDirectory);
		getField(ConfiguredEmbeddedGlassFishMojo.class, "fileRealms").set(mojo, toArray(FileRealm.class, fileRealms));
		getField(ConfiguredEmbeddedGlassFishMojo.class, "extraCommands").set(mojo, toArray(Command.class,
		                                                                                   extraCommands));
		return mojo;
	}


	private <T> T[] toArray(Class<T> arrayType, Collection<? extends T> elements)
	{
		if (elements == null)
		{
			return null;
		}
		else
		{
			T[] array = (T[])Array.newInstance(arrayType, elements.size());
			return elements.toArray(array);
		}
	}


	private List<FileRealm> createFileRealms()
	{
		FileRealm realm1 = new FileRealm();
		realm1.setRealmName("file");
		realm1.setUsers(createUser("test", "user", "role1", "role2"));

		FileRealm realm2 = new FileRealm();
		realm2.setRealmName("admin-realm");
		realm2.setUsers(createUser("admin", "secret", "god"));

		FileRealm realm3 = new FileRealm();
		realm3.setRealmName("extra");
		realm3.setUsers(createUser("test", "user", "role3", "role4"));

		FileRealm realm4 = new FileRealm();
		realm4.setRealmName("certificate");

		List<FileRealm> fileRealms = new ArrayList<>();
		fileRealms.add(realm1);
		fileRealms.add(realm2);
		fileRealms.add(realm3);
		fileRealms.add(realm4);

		return fileRealms;
	}


	private User[] createUser(String username, String password, String... roles)
	{
		User user = new User();
		user.setUsername(username);
		user.setPassword(password);
		user.setRoles(roles);

		return new User[]{user};
	}


	private List<Command> createCommands()
	{
		Command command = new Command();
		command.setCommand("list");
		command.setParameters(new String[]{"*root*"});

		List<Command> extraCommands = new ArrayList<>();
		extraCommands.add(command);

		return extraCommands;
	}


	@Test
	public void testLongSequenceWithHttps() throws Exception
	{
		LogManager.getLogManager().reset();
		assertNull(Logger.getLogger("net.sf.opk").getLevel());

		int httpPort = findUnusedPort();
		int httpsPort = findUnusedPort();

		ConfiguredEmbeddedGlassFishMojo mojo = createAndconfigureMojo(httpPort, httpsPort, LOGGING_CONFIGURATION_FILE,
		                                                              RESOURCES_FILE, APP_PATH, WEBAPP_DIRECTORY,
		                                                              CLASSES_DIRECTORY, true, TEST_CLASSES_DIRECTORY,
		                                                              PLUGIN_CLASSPATH, createFileRealms(),
		                                                              createCommands());

		try
		{
			assertConnectionError(httpPort);

			mojo.startup();

			checkResultIsTextContaining(httpPort, APP_PATH, "Database: HSQL Database Engine");
			checkResultIsTextContaining(httpPort, WAR_ARTIFACT_PATH, "Hello World!");

			mojo.redeploy();

			checkResultIsTextContaining(httpPort, APP_PATH, "Database: HSQL Database Engine");
			checkResultIsTextContaining(httpPort, WAR_ARTIFACT_PATH, "Hello World!");

			mojo.shutdown();

			assertConnectionError(httpPort);
		}
		finally
		{
			// Just in case.
			mojo.shutdown();
		}

		assertEquals(Level.FINE, Logger.getLogger("net.sf.opk").getLevel());
	}


	private int findUnusedPort() throws IOException
	{
		try (ServerSocket socket = new ServerSocket(0))
		{
			return socket.getLocalPort();
		}
	}


	private void assertConnectionError(int port) throws IOException, URISyntaxException
	{
		try (CloseableHttpClient httpClient = HttpClientBuilder.create().build())
		{
			//URI uri = new URI("http", null, "localhost", port, "/", null, null);
			URI uri = new URI("http://localhost:" + port + "/");
			HttpGet httpget = new HttpGet(uri);
			httpClient.execute(httpget);
			fail("The server is available, but should not be.");
		}
		catch (SocketException ignored)
		{
			// Ignore: we want this to happen.
		}
	}


	private void checkResultIsTextContaining(int port, String location, String text)
			throws IOException, URISyntaxException
	{
		try (CloseableHttpClient httpClient = HttpClientBuilder.create().build())
		{
			//URI uri = new URI("http", null, "localhost", port, location, null, null);
			URI uri = new URI("http://localhost:" + port + location);
			HttpGet httpget = new HttpGet(uri);
			HttpResponse response = httpClient.execute(httpget);
			assertEquals(location + " is not available, but should be.", HttpServletResponse.SC_OK,
			             response.getStatusLine().getStatusCode());
			assertNotNull(response.getEntity().getContent());
			String content = EntityUtils.toString(response.getEntity());
			assertThat(content, StringContains.containsString(text));
		}
		catch (SocketException ignored)
		{
			fail(location + " is not available, but should be.");
		}
	}


	@Test
	public void testShortSequenceWithoutHttps() throws Exception
	{
		LogManager.getLogManager().reset();
		assertNull(Logger.getLogger("net.sf.opk").getLevel());

		int httpPort = findUnusedPort();

		ConfiguredEmbeddedGlassFishMojo mojo = createAndconfigureMojo(httpPort, 0, LOGGING_CONFIGURATION_FILE,
		                                                              RESOURCES_FILE, APP_PATH, WEBAPP_DIRECTORY,
		                                                              CLASSES_DIRECTORY, true, TEST_CLASSES_DIRECTORY,
		                                                              PLUGIN_CLASSPATH, createFileRealms(),
		                                                              createCommands());

		try
		{
			assertConnectionError(httpPort);

			mojo.startup();

			checkResultIsTextContaining(httpPort, APP_PATH, "Database: HSQL Database Engine");
			checkResultIsTextContaining(httpPort, WAR_ARTIFACT_PATH, "Hello World!");

			mojo.redeploy();

			checkResultIsTextContaining(httpPort, APP_PATH, "Database: HSQL Database Engine");
			checkResultIsTextContaining(httpPort, WAR_ARTIFACT_PATH, "Hello World!");

			mojo.shutdown();

			assertConnectionError(httpPort);
		}
		finally
		{
			// Just in case.
			mojo.shutdown();
		}

		assertEquals(Level.FINE, Logger.getLogger("net.sf.opk").getLevel());
	}


	@Test
	public void testEqualityContractForClassCommand()
	{
		Command command1 = new Command();
		command1.setCommand("abc");
		command1.setParameters(new String[0]);

		Command command2 = new Command();
		command2.setCommand("def");
		command2.setParameters(new String[0]);

		Command command3 = new Command();
		command3.setCommand("abc");
		command3.setParameters(new String[]{"p"});

		Command command4 = new Command();
		command4.setCommand("abc");
		command4.setParameters(new String[0]);

		assertTrue(command1.equals(command1));

		assertFalse(command1.equals(null));
		assertFalse(command1.equals(""));
		assertFalse("".equals(command1));

		assertFalse(command1.equals(command2));
		assertFalse(command2.equals(command1));
		assertFalse(command1.equals(command3));
		assertFalse(command3.equals(command1));

		assertTrue(command1.equals(command4));
		assertTrue(command4.equals(command1));

		assertNotEquals(command1.hashCode(), command2.hashCode());
		assertNotEquals(command1.hashCode(), command3.hashCode());
		assertEquals(command1.hashCode(), command4.hashCode());
	}


	@Test
	public void testEqualityContractForClassUser()
	{
		User user1 = new User();
		user1.setUsername("abc");
		user1.setPassword("def");
		user1.setRoles(new String[0]);

		User user2 = new User();
		user2.setUsername("ghi");
		user2.setPassword("jkl");
		user2.setRoles(new String[]{"r"});

		User user3 = new User();
		user3.setUsername("abc");
		user3.setPassword("jkl");
		user3.setRoles(new String[]{"r"});

		User user4 = new User();
		user4.setUsername("abc");
		user4.setPassword("def");
		user4.setRoles(new String[]{"r"});

		User user5 = new User();
		user5.setUsername("abc");
		user5.setPassword("def");
		user5.setRoles(new String[0]);

		assertTrue(user1.equals(user1));

		assertFalse(user1.equals(null));
		assertFalse(user1.equals(""));
		assertFalse("".equals(user1));

		assertFalse(user1.equals(user2));
		assertFalse(user2.equals(user1));
		assertFalse(user1.equals(user3));
		assertFalse(user3.equals(user1));
		assertFalse(user1.equals(user4));
		assertFalse(user4.equals(user1));

		assertTrue(user1.equals(user5));
		assertTrue(user5.equals(user1));

		assertNotEquals(user1.hashCode(), user2.hashCode());
		assertNotEquals(user1.hashCode(), user3.hashCode());
		assertNotEquals(user1.hashCode(), user4.hashCode());
		assertEquals(user1.hashCode(), user5.hashCode());
	}


	@Test
	public void testEqualityContractForClassFileRealm()
	{
		User user = new User();
		user.setUsername("abc");
		user.setPassword("def");
		user.setRoles(new String[0]);

		FileRealm fileRealm1 = new FileRealm();
		fileRealm1.setRealmName("abc");
		fileRealm1.setUsers(new User[0]);

		FileRealm fileRealm2 = new FileRealm();
		fileRealm2.setRealmName("ghi");
		fileRealm2.setUsers(new User[]{user});

		FileRealm fileRealm3 = new FileRealm();
		fileRealm3.setRealmName("abc");
		fileRealm3.setUsers(new User[]{user});

		FileRealm fileRealm4 = new FileRealm();
		fileRealm4.setRealmName("abc");
		fileRealm4.setUsers(new User[0]);

		assertTrue(fileRealm1.equals(fileRealm1));

		assertFalse(fileRealm1.equals(null));
		assertFalse(fileRealm1.equals(""));
		assertFalse("".equals(fileRealm1));

		assertFalse(fileRealm1.equals(fileRealm2));
		assertFalse(fileRealm2.equals(fileRealm1));
		assertFalse(fileRealm1.equals(fileRealm3));
		assertFalse(fileRealm3.equals(fileRealm1));

		assertTrue(fileRealm1.equals(fileRealm4));
		assertTrue(fileRealm4.equals(fileRealm1));

		assertNotEquals(fileRealm1.hashCode(), fileRealm2.hashCode());
		assertNotEquals(fileRealm1.hashCode(), fileRealm3.hashCode());
		assertEquals(fileRealm1.hashCode(), fileRealm4.hashCode());
	}


	@SuppressWarnings("deprecation")
	private class ArtifactStub implements Artifact
	{
		private final String scope;
		private final String type;
		private final File file;


		private ArtifactStub(String scope, File file)
		{
			this.scope = scope;
			this.file = file;

			String type = "jar";
			if (file != null && !file.isDirectory())
			{
				String fileName = file.getName();
				int dotPos = fileName.indexOf('.');
				if (dotPos != -1)
				{
					type = fileName.substring(dotPos + 1, fileName.length());
				}
			}
			this.type = type;
		}


		@Override
		public String getGroupId()
		{
			return null;
		}


		@Override
		public String getArtifactId()
		{
			return null;
		}


		@Override
		public String getVersion()
		{
			return null;
		}


		@Override
		public void setVersion(String version)
		{

		}


		@Override
		public String getScope()
		{
			return scope;
		}


		@Override
		public String getType()
		{
			return type;
		}


		@Override
		public String getClassifier()
		{
			return null;
		}


		@Override
		public boolean hasClassifier()
		{
			return false;
		}


		@Override
		public File getFile()
		{
			return file;
		}


		@Override
		public void setFile(File destination)
		{

		}


		@Override
		public String getBaseVersion()
		{
			return null;
		}


		@Override
		public void setBaseVersion(String baseVersion)
		{

		}


		@Override
		public String getId()
		{
			return null;
		}


		@Override
		public String getDependencyConflictId()
		{
			return null;
		}


		@Override
		public void addMetadata(ArtifactMetadata metadata)
		{

		}


		@Override
		public Collection<ArtifactMetadata> getMetadataList()
		{
			return null;
		}


		@Override
		public void setRepository(ArtifactRepository remoteRepository)
		{

		}


		@Override
		public ArtifactRepository getRepository()
		{
			return null;
		}


		@Override
		public void updateVersion(String version, ArtifactRepository localRepository)
		{

		}


		@Override
		public String getDownloadUrl()
		{
			return null;
		}


		@Override
		public void setDownloadUrl(String downloadUrl)
		{

		}


		@Override
		public ArtifactFilter getDependencyFilter()
		{
			return null;
		}


		@Override
		public void setDependencyFilter(ArtifactFilter artifactFilter)
		{

		}


		@Override
		public ArtifactHandler getArtifactHandler()
		{
			return null;
		}


		@Override
		public List<String> getDependencyTrail()
		{
			return null;
		}


		@Override
		public void setDependencyTrail(List<String> dependencyTrail)
		{

		}


		@Override
		public void setScope(String scope)
		{

		}


		@Override
		public VersionRange getVersionRange()
		{
			return null;
		}


		@Override
		public void setVersionRange(VersionRange newRange)
		{

		}


		@Override
		public void selectVersion(String version)
		{

		}


		@Override
		public void setGroupId(String groupId)
		{

		}


		@Override
		public void setArtifactId(String artifactId)
		{

		}


		@Override
		public boolean isSnapshot()
		{
			return false;
		}


		@Override
		public void setResolved(boolean resolved)
		{

		}


		@Override
		public boolean isResolved()
		{
			return false;
		}


		@Override
		public void setResolvedVersion(String version)
		{

		}


		@Override
		public void setArtifactHandler(ArtifactHandler handler)
		{

		}


		@Override
		public boolean isRelease()
		{
			return false;
		}


		@Override
		public void setRelease(boolean release)
		{

		}


		@Override
		public List<ArtifactVersion> getAvailableVersions()
		{
			return null;
		}


		@Override
		public void setAvailableVersions(List<ArtifactVersion> versions)
		{

		}


		@Override
		public boolean isOptional()
		{
			return false;
		}


		@Override
		public void setOptional(boolean optional)
		{

		}


		@Override
		public ArtifactVersion getSelectedVersion() throws OverConstrainedVersionException
		{
			return null;
		}


		@Override
		public boolean isSelectedVersionKnown() throws OverConstrainedVersionException
		{
			return false;
		}


		@Override
		public int compareTo(Artifact o)
		{
			return 0;
		}
	}
}
