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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.glassfish.embeddable.GlassFishException;


/**
 * MOJO to start an embedded GlassFish instance with the artifact deployed in it.
 *
 * @author <a href="mailto:oscar@westravanholthe.nl">Oscar Westra van Holthe - Kind</a>
 * @goal start
 * @phase pre-integration-test
 * @requiresDependencyResolution test
 * @threadSafe false
 */
public class StartMojo extends ConfiguredEmbeddedGlassFishMojo
{
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		configureLogging();

		try
		{
			startupWithArtifact(createScatteredArchive());
		}
		catch (GlassFishException e)
		{
			shutdown();
			throw new MojoFailureException("GlassFish failed to do our bidding.", e);
		}
		catch (IOException e)
		{
			shutdown();
			throw new MojoFailureException("I/O failure.", e);
		}
	}
}
