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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * MOJO to run the artifact in an embedded GlassFish instance.
 *
 * @author <a href="mailto:oscar@westravanholthe.nl">Oscar Westra van Holthe - Kind</a>
 */
@Mojo(name = "run", requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.TEST_COMPILE)
public class RunMojo extends ConfiguredEmbeddedGlassFishMojo
{
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		try
		{
			startup();

			boolean continueRunning = true;
			BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
			while (continueRunning)
			{
				//noinspection UseOfSystemOutOrSystemErr
				System.out.printf("\n\nPress ENTER to redeploy the artifact, or 'X' + ENTER to exit.\n");

				String lineFromConsole = stdin.readLine();
				if ("X".equalsIgnoreCase(lineFromConsole))
				{
					// This will exit the loop.
					continueRunning = false;
				}
				else
				{
					redeploy();
				}
			}
		}
		catch (IOException e)
		{
			throw new MojoExecutionException("I/O failure.", e);
		}
		finally
		{
			shutdown();
		}
	}
}
