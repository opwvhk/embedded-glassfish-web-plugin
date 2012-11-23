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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;

import org.apache.maven.plugin.MojoExecutionException;
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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * Test class for {@link RunMojo}.
 *
 * @author <a href="mailto:oscar@westravanholthe.nl">Oscar Westra van Holthe - Kind</a>
 */
public class RunMojoTest extends MojoTestBase
{
	private static final InputStream SYSTEM_IN = System.in;
	private static final PrintStream SYSTEM_OUT = System.out;


	@Test(timeout = 1000)
	public void testExecute1() throws Exception
	{
		try
		{
			// Redirect stdin & stdout

			PipedOutputStream pipe = new PipedOutputStream();
			PrintStream toIn = new PrintStream(pipe);
			PipedInputStream in = new PipedInputStream(pipe);
			System.setIn(in);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			System.setOut(new PrintStream(out));

			// Create mock.

			EmbeddedGlassFish mockGlassFish = createMock(EmbeddedGlassFish.class);

			// Expectations for startup
			mockGlassFish.startup();
			expectLastCall().once();
			mockGlassFish.deployApplication(WAR_ARTIFACT);
			expectLastCall().once();
			mockGlassFish.deployArtifact(anyObject(ScatteredArchive.class), eq(APP_PATH));
			expectLastCall().once();
			// Expectations for redeploy
			mockGlassFish.undeployLastArtifact();
			expectLastCall().once();
			mockGlassFish.deployArtifact(anyObject(ScatteredArchive.class), eq(APP_PATH));
			expectLastCall().once();
			// Expectation for shutdown
			mockGlassFish.shutdown();
			expectLastCall().once();

			replay(mockGlassFish);

			// Create and test mojo.

			final RunMojo mojo = configureMojo(new RunMojo(), mockGlassFish);
			Thread testedProcess = new Thread(new Runnable()
			{
				public void run()
				{
					try
					{
						mojo.execute();
					}
					catch (MojoExecutionException e)
					{
						fail("Unexpected exception: " + e.getMessage());
					}
					catch (MojoFailureException e)
					{
						fail("Unexpected exception: " + e.getMessage());
					}
				}
			});
			testedProcess.start();

			toIn.println();
			toIn.println("X");

			testedProcess.join();

			verify(mockGlassFish);
			assertTrue(out.size() > 0);
		}
		finally
		{
			System.setIn(SYSTEM_IN);
			System.setOut(SYSTEM_OUT);
		}
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

			RunMojo mojo = configureMojo(new RunMojo(), mockGlassFish);
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

			RunMojo mojo = configureMojo(new RunMojo(), mockGlassFish);
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

			RunMojo mojo = configureMojo(new RunMojo(), mockGlassFish);
			mojo.execute();
		}
		finally
		{
			verify(mockGlassFish);
		}
	}
}
