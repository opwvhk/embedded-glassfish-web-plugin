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

/**
 * A command for the {@code asadmin} command in GlassFish.
 *
 * @author <a href="mailto:oscar@westravanholthe.nl">Oscar Westra van Holthe - Kind</a>
 */
public class Command
{
	/**
	 * The command to execute.
	 */
	private String command;
	/**
	 * Parameters for the command to execute.
	 */
	private String[] parameters;


	public String getCommand()
	{
		return command;
	}


	public void setCommand(String command)
	{
		this.command = command;
	}


	public String[] getParameters()
	{
		return parameters;
	}


	public void setParameters(String[] parameters)
	{
		this.parameters = parameters;
	}
}
