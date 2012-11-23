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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.archive.ScatteredArchive;
import org.junit.Test;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/**
 * Test class for the base class {@link ConfiguredEmbeddedGlassFishMojo}.
 *
 * @author <a href="mailto:oscar@westravanholthe.nl">Oscar Westra van Holthe - Kind</a>
 */
public class ConfiguredEmbeddedGlassFishMojoTest extends MojoTestBase
{
	protected static final File LOGGING_CONFIGURATION_FILE = resource("/logging.properties");
	private static final File RESOURCES_FILE = new File(BASE_DIRECTORY, "src/main/config/glassfish-resources.xml");


	@Test
	public void testLogging1() throws Exception
	{
		LogManager.getLogManager().reset();

		EmbeddedGlassFish mockGlassFish = createMock(EmbeddedGlassFish.class);
		replay(mockGlassFish);

		ConfiguredEmbeddedGlassFishMojo mojo = createAndConfigureMojo(false, null, null, mockGlassFish);
		mojo.configureLogging();

		verify(mockGlassFish);
		assertNull(Logger.getLogger("net.sf.opk").getLevel());
	}


	@Test
	public void testLogging2() throws Exception
	{
		LogManager.getLogManager().reset();

		EmbeddedGlassFish mockGlassFish = createMock(EmbeddedGlassFish.class);
		replay(mockGlassFish);

		ConfiguredEmbeddedGlassFishMojo mojo = createAndConfigureMojo(false, null, LOGGING_CONFIGURATION_FILE,
		                                                              mockGlassFish);
		mojo.configureLogging();

		verify(mockGlassFish);
		assertNotNull(Logger.getLogger("net.sf.opk").getLevel());
	}


	@Test
	public void testLogging3() throws Exception
	{
		LogManager.getLogManager().reset();

		EmbeddedGlassFish mockGlassFish = createMock(EmbeddedGlassFish.class);
		replay(mockGlassFish);

		ConfiguredEmbeddedGlassFishMojo mojo = createAndConfigureMojo(false, null, NONEXISTING_PATH, mockGlassFish);
		mojo.configureLogging();

		verify(mockGlassFish);
		assertNull(Logger.getLogger("net.sf.opk").getLevel());
	}


	@Test
	public void testResultLogging() throws Exception
	{
		EmbeddedGlassFish mockGlassFish = createMock(EmbeddedGlassFish.class);
		replay(mockGlassFish);

		ConfiguredEmbeddedGlassFishMojo mojo = createAndConfigureMojo(false, null, null, mockGlassFish);

		mojo.logCommandResult(new CommandResultStub(CommandResult.ExitStatus.SUCCESS, null, "Success"));
		mojo.logCommandResult(new CommandResultStub(CommandResult.ExitStatus.WARNING, null, "Warning"));
		mojo.logCommandResult(new CommandResultStub(CommandResult.ExitStatus.FAILURE, new RuntimeException(), "Ouch"));

		verify(mockGlassFish);
	}


	@Test
	public void testScatteredArchive1() throws Exception
	{
		EmbeddedGlassFish mockGlassFish = createMock(EmbeddedGlassFish.class);
		replay(mockGlassFish);

		ConfiguredEmbeddedGlassFishMojo mojo = createAndConfigureMojo(false, null, null, mockGlassFish);
		ScatteredArchive archive = mojo.createScatteredArchive();

		verify(mockGlassFish);

		List<String> archiveContents = listContents(archive);
		// Web archive content
		assertTrue(archiveContents.contains("index.jsp"));
		assertTrue(archiveContents.contains("WEB-INF/web.xml"));
		// Compile classpath
		assertTrue(archiveContents.contains("WEB-INF/classes/dummy/EchoDataSource.class"));
		assertTrue(archiveContents.contains("WEB-INF/lib/minimal_jar1.jar"));
		// Runtime classpath
		assertTrue(archiveContents.contains("WEB-INF/lib/minimal_jar2.jar"));
		// Test classpath
		assertFalse(archiveContents.contains(
				"WEB-INF/classes/net/sf/opk/glassfish/ConfiguredEmbeddedGlassFishMojo.class"));
		assertFalse(archiveContents.contains("WEB-INF/lib/minimal_jar3.jar"));
	}


	private List<String> listContents(ScatteredArchive archive) throws IOException
	{
		List<String> archiveContents = new ArrayList<String>();
		File webapp = new File(archive.toURI());
		ZipFile zipFile = null;
		try
		{
			zipFile = new ZipFile(webapp);
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements())
			{
				ZipEntry entry = entries.nextElement();
				if (entry.isDirectory())
				{
					archiveContents.add(entry.getName() + '/');
				}
				else
				{
					archiveContents.add(entry.getName());
				}
			}
		}
		finally
		{
			if (zipFile != null)
			{
				zipFile.close();
			}
		}
		Collections.sort(archiveContents);
		return archiveContents;
	}


	@Test
	public void testScatteredArchive2() throws Exception
	{
		net.sf.opk.glassfish.EmbeddedGlassFish mockGlassFish = createMock(EmbeddedGlassFish.class);
		replay(mockGlassFish);

		ConfiguredEmbeddedGlassFishMojo mojo = createAndConfigureMojo(true, null, null, mockGlassFish);
		ScatteredArchive archive = mojo.createScatteredArchive();

		verify(mockGlassFish);

		List<String> archiveContents = listContents(archive);
		// Web archive content
		assertTrue(archiveContents.contains("index.jsp"));
		assertTrue(archiveContents.contains("WEB-INF/web.xml"));
		// Compile classpath
		assertTrue(archiveContents.contains("WEB-INF/classes/dummy/EchoDataSource.class"));
		assertTrue(archiveContents.contains("WEB-INF/lib/minimal_jar1.jar"));
		// Runtime classpath
		assertTrue(archiveContents.contains("WEB-INF/lib/minimal_jar2.jar"));
		// Test classpath
		assertTrue(archiveContents.contains(
				"WEB-INF/classes/net/sf/opk/glassfish/ConfiguredEmbeddedGlassFishMojo.class"));
		assertTrue(archiveContents.contains("WEB-INF/lib/minimal_jar3.jar"));
	}


	@Test
	public void testStartup1() throws Exception
	{
		ScatteredArchive mockArchive = createMock(ScatteredArchive.class);
		net.sf.opk.glassfish.EmbeddedGlassFish mockGlassFish = createMock(EmbeddedGlassFish.class);
		mockGlassFish.startup();
		expectLastCall().once();
		CommandResultStub commandResult = new CommandResultStub(CommandResult.ExitStatus.SUCCESS, null, "Success");
		expect(mockGlassFish.addResources(RESOURCES_FILE)).andReturn(commandResult).once();
		mockGlassFish.deployApplication(WAR_ARTIFACT);
		expectLastCall().once();
		mockGlassFish.deployArtifact(mockArchive, APP_PATH);
		expectLastCall().once();
		replay(mockGlassFish);

		ConfiguredEmbeddedGlassFishMojo mojo = createAndConfigureMojo(false, RESOURCES_FILE, null, mockGlassFish);
		mojo.startupWithArtifact(mockArchive);

		verify(mockGlassFish);
	}


	@Test
	public void testStartup2() throws Exception
	{
		ScatteredArchive mockArchive = createMock(ScatteredArchive.class);
		EmbeddedGlassFish mockGlassFish = createMock(net.sf.opk.glassfish.EmbeddedGlassFish.class);
		mockGlassFish.startup();
		expectLastCall().once();
		mockGlassFish.deployApplication(WAR_ARTIFACT);
		expectLastCall().once();
		mockGlassFish.deployArtifact(mockArchive, APP_PATH);
		expectLastCall().once();
		replay(mockGlassFish);

		ConfiguredEmbeddedGlassFishMojo mojo = createAndConfigureMojo(false, null, null, mockGlassFish);
		mojo.startupWithArtifact(mockArchive);

		verify(mockGlassFish);
	}


	@Test
	public void testRedeploy() throws Exception
	{
		ScatteredArchive mockArchive = createMock(ScatteredArchive.class);
		EmbeddedGlassFish mockGlassFish = createMock(net.sf.opk.glassfish.EmbeddedGlassFish.class);

		mockGlassFish.undeployLastArtifact();
		expectLastCall().once();
		mockGlassFish.deployArtifact(mockArchive, APP_PATH);
		expectLastCall().once();
		replay(mockGlassFish);

		ConfiguredEmbeddedGlassFishMojo mojo = createAndConfigureMojo(false, null, null, mockGlassFish);
		mojo.redeploy(mockArchive);

		verify(mockGlassFish);
	}
}
