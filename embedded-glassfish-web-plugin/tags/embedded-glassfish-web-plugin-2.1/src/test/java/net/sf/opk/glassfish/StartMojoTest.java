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

import org.apache.maven.plugin.MojoExecutionException;
import org.glassfish.embeddable.GlassFishException;
import org.hamcrest.core.Is;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * Test class for the class {@link StartMojo}.
 *
 * @author <a href="mailto:oscar@westravanholthe.nl">Oscar Westra van Holthe - Kind</a>
 */
public class StartMojoTest extends MojoTestBase {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testStartupNormal() throws Exception {
        expect(glassFishWebPluginRunner.call()).andReturn(null);
        replay(glassFishWebPluginRunner, redeployHook, shutdownHook);

        StartMojo mojo = configureMojo(new StartMojo(), glassFishWebPluginRunner);
        mojo.execute();

        assertSame(redeployHook, getField(ConfiguredEmbeddedGlassFishMojo.class, "webApplicationRedeployHook").get(mojo));
        assertSame(shutdownHook, getField(EmbeddedGlassFishMojo.class, "glassFishShutdownHook").get(null));

        verify(glassFishWebPluginRunner, redeployHook, shutdownHook);
    }

    @Test
    public void testStartupFailure() throws Exception {
        GlassFishException oops = new GlassFishException("Oops");
        expect(glassFishWebPluginRunner.call()).andThrow(oops);
        replay(glassFishWebPluginRunner, redeployHook, shutdownHook);

        expectedException.expect(MojoExecutionException.class);
        expectedException.expectCause(Is.is(oops));

        StartMojo mojo = configureMojo(new StartMojo(), glassFishWebPluginRunner);
        mojo.execute();

        assertNull(ConfiguredEmbeddedGlassFishMojo.class.getDeclaredField("webApplicationRedeployHook").get(mojo));
        assertNull(EmbeddedGlassFishMojo.class.getDeclaredField("glassFishShutdownHook").get(null));

        verify(glassFishWebPluginRunner, redeployHook, shutdownHook);
    }
}
