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

import java.io.Serializable;
import java.util.Arrays;


/**
 * A command for the {@code asadmin} command in GlassFish.
 *
 * @author <a href="mailto:oscar@westravanholthe.nl">Oscar Westra van Holthe - Kind</a>
 */
public class Command implements Serializable
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


	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (o == null || getClass() != o.getClass())
		{
			return false;
		}

		Command that = (Command)o;
		return this.command.equals(that.command) && Arrays.equals(this.parameters, that.parameters);
	}


	@Override
	public int hashCode()
	{
		int result = command.hashCode();
		result = 31 * result + Arrays.hashCode(parameters);
		return result;
	}
}
