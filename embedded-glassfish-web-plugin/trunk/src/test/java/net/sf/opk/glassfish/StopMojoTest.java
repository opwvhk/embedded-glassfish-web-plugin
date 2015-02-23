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

import org.glassfish.embeddable.GlassFishException;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Test class for the class {@link StopMojo}.
 *
 * @author <a href="mailto:oscar@westravanholthe.nl">Oscar Westra van Holthe - Kind</a>
 */
public class StopMojoTest {

    @Test
    public void testShutdownNormal() throws Exception {

        // Setup

        Callable<Void> shutdownHook = mock(Callable.class);
        when(shutdownHook.call()).thenReturn(null);

        // Set shutdown hook from different MOJO (happens with start and stop goals too).
        new StartMojo().setGlassFishShutdownHook(shutdownHook);

        // Perform test

        StopMojo mojo = new StopMojo();
        mojo.execute();

        verify(shutdownHook, times(1)).call();
    }

    @Test
    public void testShutdownFailure() throws Exception {

        // Setup

        GlassFishException oops = new GlassFishException("Oops");

        Callable<Void> shutdownHook = mock(Callable.class);
        when(shutdownHook.call()).thenThrow(oops);

        // Set shutdown hook from different MOJO (happens with start and stop goals too).
        new StartMojo().setGlassFishShutdownHook(shutdownHook);

        // Perform test

        StopMojo mojo = new StopMojo();
        // Should still not throw!
        mojo.execute();

        verify(shutdownHook, times(1)).call();
    }
}
