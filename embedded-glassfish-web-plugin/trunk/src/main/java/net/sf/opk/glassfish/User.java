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
 * Simple JavaBean to represent a user.
 *
 * @author <a href="mailto:oscar@westravanholthe.nl">Oscar Westra van Holthe - Kind</a>
 */
public class User implements Serializable
{
	/**
	 * The name of the user.
	 */
	private String username;
	/**
	 * The users password.
	 */
	private String password;
	/**
	 * The roles of the user.
	 */
	private String[] roles;


	public String getUsername()
	{
		return username;
	}


	public void setUsername(String username)
	{
		this.username = username;
	}


	public String getPassword()
	{
		return password;
	}


	public void setPassword(String password)
	{
		this.password = password;
	}


	public String[] getRoles()
	{
		return roles;
	}


	public void setRoles(String[] roles)
	{
		this.roles = roles;
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

		User user = (User)o;

		return username.equals(user.username) && password.equals(user.password) && Arrays.equals(roles, user.roles);
	}


	@Override
	public int hashCode()
	{
		int result = username.hashCode();
		result = 31 * result + password.hashCode();
		result = 31 * result + Arrays.hashCode(roles);
		return result;
	}
}
