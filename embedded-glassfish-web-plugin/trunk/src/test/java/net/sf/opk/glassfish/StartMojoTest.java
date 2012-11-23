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

import java.io.IOException;

import org.apache.maven.plugin.MojoFailureException;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.archive.ScatteredArchive;
import org.junit.Test;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;


/**
 * Test class for the class {@link StartMojo}.
 *
 * @author <a href="mailto:oscar@westravanholthe.nl">Oscar Westra van Holthe - Kind</a>
 */
public class StartMojoTest extends MojoTestBase
{
	@Test
	public void testExecute1() throws Exception
	{
		EmbeddedGlassFish mockGlassFish = createMock(EmbeddedGlassFish.class);
		mockGlassFish.startup();
		expectLastCall().once();
		mockGlassFish.deployApplication(WAR_ARTIFACT);
		expectLastCall().once();
		mockGlassFish.deployArtifact(anyObject(ScatteredArchive.class), eq(APP_PATH));
		expectLastCall().once();
		replay(mockGlassFish);

		StartMojo mojo = configureMojo(new StartMojo(), mockGlassFish);
		mojo.execute();

		verify(mockGlassFish);
	}


	@Test(expected = MojoFailureException.class)
	public void testExecute2() throws Exception
	{
		EmbeddedGlassFish mockGlassFish = createMock(EmbeddedGlassFish.class);
		try
		{
			mockGlassFish.startup();
			expectLastCall().once();
			mockGlassFish.deployApplication(WAR_ARTIFACT);
			expectLastCall().andThrow(new GlassFishException("Test")).once();
			mockGlassFish.shutdown();
			expectLastCall().once();
			replay(mockGlassFish);

			StartMojo mojo = configureMojo(new StartMojo(), mockGlassFish);
			mojo.execute();
		}
		finally
		{
			verify(mockGlassFish);
		}
	}


	@Test(expected = MojoFailureException.class)
	public void testExecute3() throws Exception
	{
		EmbeddedGlassFish mockGlassFish = createMock(EmbeddedGlassFish.class);
		try
		{
			mockGlassFish.startup();
			expectLastCall().once();
			mockGlassFish.deployApplication(WAR_ARTIFACT);
			expectLastCall().once();
			mockGlassFish.deployArtifact(anyObject(ScatteredArchive.class), eq(APP_PATH));
			expectLastCall().andThrow(new GlassFishException("Test")).once();
			mockGlassFish.shutdown();
			expectLastCall().once();
			replay(mockGlassFish);

			StartMojo mojo = configureMojo(new StartMojo(), mockGlassFish);
			mojo.execute();
		}
		finally
		{
			verify(mockGlassFish);
		}
	}


	@Test(expected = MojoFailureException.class)
	public void testExecute4() throws Exception
	{
		EmbeddedGlassFish mockGlassFish = createMock(EmbeddedGlassFish.class);
		try
		{
			mockGlassFish.startup();
			expectLastCall().once();
			mockGlassFish.deployApplication(WAR_ARTIFACT);
			expectLastCall().once();
			mockGlassFish.deployArtifact(anyObject(ScatteredArchive.class), eq(APP_PATH));
			expectLastCall().andThrow(new IOException("Test")).once();
			mockGlassFish.shutdown();
			expectLastCall().once();
			replay(mockGlassFish);

			StartMojo mojo = configureMojo(new StartMojo(), mockGlassFish);
			mojo.execute();
		}
		finally
		{
			verify(mockGlassFish);
		}
	}
}
