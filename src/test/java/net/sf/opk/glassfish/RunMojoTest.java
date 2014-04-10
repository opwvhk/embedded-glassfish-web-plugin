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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.easymock.IAnswer;
import org.glassfish.embeddable.GlassFishException;
import org.junit.Test;

import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link RunMojo}.
 *
 * @author <a href="mailto:oscar@westravanholthe.nl">Oscar Westra van Holthe - Kind</a>
 */
public class RunMojoTest extends MojoTestBase {
    private static final InputStream SYSTEM_IN = System.in;
    private static final PrintStream SYSTEM_OUT = System.out;

    @Test
    public void testExecuteWithSuccessfulRedeploy() throws Exception {
        try {
            final List<Throwable> thrown = new ArrayList<>();

            // Redirect stdin & stdout

            PipedOutputStream pipe = new PipedOutputStream();
            PrintStream toIn = new PrintStream(pipe);
            PipedInputStream in = new PipedInputStream(pipe);
            System.setIn(in);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            System.setOut(new PrintStream(out));

            // Create mock.

            expect(glassFishWebPluginRunner.call()).andAnswer(new IAnswer<Void>() {
                @Override
                public Void answer() throws Throwable {
                    System.out.println("Startup");
                    return null;
                }
            });
            expect(redeployHook.call()).andAnswer(new IAnswer<Void>() {
                @Override
                public Void answer() throws Throwable {
                    System.out.println("Redeployed");
                    return null;
                }
            });
            expect(shutdownHook.call()).andAnswer(new IAnswer<Void>() {
                @Override
                public Void answer() throws Throwable {
                    System.out.println("Shutdown");
                    return null;
                }
            });

            replay(glassFishWebPluginRunner, redeployHook, shutdownHook);

            // Create and test mojo.

            final RunMojo mojo = configureMojo(new RunMojo(), glassFishWebPluginRunner);
            Thread testedProcess = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mojo.execute();
                    } catch (Throwable e) {
                        thrown.add(e);
                    }
                }
            });
            testedProcess.start();

            toIn.println();
            toIn.println("X");

            testedProcess.join();

            verify(glassFishWebPluginRunner, redeployHook, shutdownHook);
            assertTrue(thrown.isEmpty());
            assertEquals("Startup\n" +
                    "Press ENTER to redeploy the artifact, or 'X' + ENTER to exit.\n" +
                    "Redeployed\n" +
                    "Press ENTER to redeploy the artifact, or 'X' + ENTER to exit.\n" +
                    "Shutdown\n", out.toString().replace("\r\n", "\n").replaceAll("\\n\\n+", "\n"));
        } finally {
            System.setIn(SYSTEM_IN);
            System.setOut(SYSTEM_OUT);
        }
    }

    @Test
    public void testExecuteWithFailedRedeploy() throws Exception {
        try {
            final List<Throwable> thrown = new ArrayList<>();

            // Redirect stdin & stdout

            PipedOutputStream pipe = new PipedOutputStream();
            PrintStream toIn = new PrintStream(pipe);
            PipedInputStream in = new PipedInputStream(pipe);
            System.setIn(in);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            System.setOut(new PrintStream(out));

            // Create mock.

            expect(glassFishWebPluginRunner.call()).andAnswer(new IAnswer<Void>() {
                @Override
                public Void answer() throws Throwable {
                    System.out.println("Startup");
                    return null;
                }
            });
            expect(redeployHook.call()).andAnswer(new IAnswer<Void>() {
                @Override
                public Void answer() throws Throwable {
                    System.out.println("Redeploy Failing");
                    throw new GlassFishException("Oops");
                }
            });
            expect(shutdownHook.call()).andAnswer(new IAnswer<Void>() {
                @Override
                public Void answer() throws Throwable {
                    System.out.println("Shutdown");
                    return null;
                }
            });

            replay(glassFishWebPluginRunner, redeployHook, shutdownHook);

            // Create and test mojo.

            final RunMojo mojo = configureMojo(new RunMojo(), glassFishWebPluginRunner);
            Thread testedProcess = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mojo.execute();
                    } catch (Throwable e) {
                        thrown.add(e);
                    }
                }
            });
            testedProcess.start();

            toIn.println();
            toIn.println("X");

            testedProcess.join();

            verify(glassFishWebPluginRunner, redeployHook, shutdownHook);
            assertEquals(1, thrown.size());
            assertTrue(thrown.get(0) instanceof MojoExecutionException);
            assertEquals("Startup\n" +
                    "Press ENTER to redeploy the artifact, or 'X' + ENTER to exit.\n" +
                    "Redeploy Failing\n" +
                    "Shutdown\n", out.toString().replace("\r\n", "\n").replaceAll("\\n\\n+", "\n"));
        } finally {
            System.setIn(SYSTEM_IN);
            System.setOut(SYSTEM_OUT);
        }
    }

    @Test
    public void testExecuteWithIOFailure() throws Exception {
        try {
            final List<Throwable> thrown = new ArrayList<>();

            // Redirect stdin & stdout

            InputStream mockIn = createMock(InputStream.class);
            expect(mockIn.read(anyObject(byte[].class), anyInt(), anyInt())).andThrow(new IOException("oops"));
            replay(mockIn);
            System.setIn(mockIn);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            System.setOut(new PrintStream(out));

            // Create mock.

            expect(glassFishWebPluginRunner.call()).andAnswer(new IAnswer<Void>() {
                @Override
                public Void answer() throws Throwable {
                    System.out.println("Startup");
                    return null;
                }
            });
            expect(shutdownHook.call()).andAnswer(new IAnswer<Void>() {
                @Override
                public Void answer() throws Throwable {
                    System.out.println("Shutdown");
                    return null;
                }
            });

            replay(glassFishWebPluginRunner, redeployHook, shutdownHook);

            // Create and test mojo.

            final RunMojo mojo = configureMojo(new RunMojo(), glassFishWebPluginRunner);
            Thread testedProcess = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mojo.execute();
                    } catch (Throwable e) {
                        thrown.add(e);
                    }
                }
            });
            testedProcess.start();

            //toIn.println();
            //toIn.println("X");

            testedProcess.join();

            verify(glassFishWebPluginRunner, redeployHook, shutdownHook);
            assertEquals(1, thrown.size());
	        assertTrue(thrown.get(0) instanceof MojoExecutionException);
            assertEquals("Startup\n" +
                    "Press ENTER to redeploy the artifact, or 'X' + ENTER to exit.\n" +
                    "Shutdown\n", out.toString().replace("\r\n", "\n").replaceAll("\\n\\n+", "\n"));
        } finally {
            System.setIn(SYSTEM_IN);
            System.setOut(SYSTEM_OUT);
        }
    }
}
