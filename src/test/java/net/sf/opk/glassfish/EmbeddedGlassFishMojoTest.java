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

import org.glassfish.embeddable.GlassFishException;
import org.junit.Test;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertNotNull;


/**
 * Test class for the base class {@link EmbeddedGlassFishMojo}.
 *
 * @author <a href="mailto:oscar@westravanholthe.nl">Oscar Westra van Holthe - Kind</a>
 */
public class EmbeddedGlassFishMojoTest extends MojoTestBase
{
	@Test
	public void testGlassfishInitialization() throws Exception
	{
		EmbeddedGlassFish glassfish = null;
		try
		{
			EmbeddedGlassFishMojo mojo = createAndConfigureMojo(null);
			glassfish = mojo.getEmbeddedGlassFish();

			assertNotNull(glassfish); // We test the class itself in a dedicated test case.
		}
		finally
		{
			if (glassfish != null)
			{
				glassfish.shutdown();
			}
		}
	}


	@Test
	public void testStartup() throws Exception
	{
		EmbeddedGlassFish mockGlassFish = createMock(EmbeddedGlassFish.class);
		mockGlassFish.startup();
		expectLastCall().once();
		replay(mockGlassFish);

		EmbeddedGlassFishMojo mojo = createAndConfigureMojo(mockGlassFish);
		mojo.startup();

		verify(mockGlassFish);
	}


	@Test
	public void testShutdown1() throws Exception
	{
		EmbeddedGlassFish mockGlassFish = createMock(EmbeddedGlassFish.class);

		mockGlassFish.shutdown();
		expectLastCall().once();
		replay(mockGlassFish);

		EmbeddedGlassFishMojo mojo = createAndConfigureMojo(mockGlassFish);
		mojo.shutdown();

		verify(mockGlassFish);
	}


	@Test
	public void testShutdown2() throws Exception
	{
		EmbeddedGlassFish mockGlassFish = createMock(EmbeddedGlassFish.class);

		mockGlassFish.shutdown();
		expectLastCall().andThrow(new GlassFishException("Test")).once();
		replay(mockGlassFish);

		EmbeddedGlassFishMojo mojo = createAndConfigureMojo(mockGlassFish);
		mojo.shutdown();

		verify(mockGlassFish);
	}
}
