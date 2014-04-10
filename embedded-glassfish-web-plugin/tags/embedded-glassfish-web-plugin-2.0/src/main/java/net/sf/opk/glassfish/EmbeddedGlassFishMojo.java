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

import org.apache.maven.plugin.AbstractMojo;


/**
 * Abstract MOJO to control (shutdown) a single embedded GlassFish instance.
 *
 * @author <a href="mailto:oscar@westravanholthe.nl">Oscar Westra van Holthe - Kind</a>
 */
public abstract class EmbeddedGlassFishMojo extends AbstractMojo
{
	/**
	 * Shutdown hook for the embedded GlassFish instance.
	 */
	private static Callable<?> glassFishShutdownHook = null;


	protected void setGlassFishShutdownHook(Callable<?> glassFishShutdownHook)
	{
		EmbeddedGlassFishMojo.glassFishShutdownHook = glassFishShutdownHook;
	}


	protected void shutdown()
	{
		try
		{
			if (glassFishShutdownHook != null)
			{
				glassFishShutdownHook.call();
				glassFishShutdownHook = null;
			}
		}
		catch (Exception e)
		{
			getLog().error("GlassFish failed to shutdown. Please stop the JVM.", e);
		}
	}
}
