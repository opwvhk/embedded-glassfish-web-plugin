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

import java.util.concurrent.Callable;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.glassfish.embeddable.GlassFishException;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Test class for the base class {@link EmbeddedGlassFishMojo}.
 *
 * @author <a href="mailto:oscar@westravanholthe.nl">Oscar Westra van Holthe - Kind</a>
 */
public class EmbeddedGlassFishMojoTest
{
    private EmbeddedGlassFishMojo mojo;

    @Before
    public void initialize() throws Exception {

        mojo = new EmbeddedGlassFishMojo() {
            @Override
            public void execute() throws MojoExecutionException, MojoFailureException {

            }
        };
    }

	@Test
	public void testShutdownWithNull() throws Exception
	{
		mojo.setGlassFishShutdownHook(null);

        // Should not throw.
		mojo.shutdown();
	}


	@Test
	public void testShutdownThrowing() throws Exception
	{
        Callable<Void> shutdownHook = mock(Callable.class);

        // Set shutdown hook from different MOJO (happens with start and stop goals too).
        new EmbeddedGlassFishMojo() {
            @Override
            public void execute() throws MojoExecutionException, MojoFailureException {

            }
        }.setGlassFishShutdownHook(shutdownHook);

        when(shutdownHook.call()).thenThrow(new GlassFishException("Test"));

        // Should not throw.
        mojo.shutdown();

		verify(shutdownHook, times(1)).call();
	}


	@Test
	public void testShutdownNormal() throws Exception
	{
        Callable<Void> shutdownHook = mock(Callable.class);

        // Set shutdown hook from different MOJO (happens with start and stop goals too).
        new EmbeddedGlassFishMojo() {
            @Override
            public void execute() throws MojoExecutionException, MojoFailureException {

            }
        }.setGlassFishShutdownHook(shutdownHook);

		when(shutdownHook.call()).thenReturn(null);

        // Should not throw.
        mojo.shutdown();
        // Second call should not do anything.
        mojo.shutdown();

		verify(shutdownHook, times(1)).call();
	}
}
