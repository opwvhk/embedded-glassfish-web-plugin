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
 * Simple JavaBean to represent a FileRealm.
 *
 * @author <a href="mailto:oscar@westravanholthe.nl">Oscar Westra van Holthe - Kind</a>
 */
public class FileRealm implements Serializable
{
	/**
	 * The name of the realm as used in the {@code web.xml} of a web application.
	 */
	private String realmName;
	/**
	 * The users to add to the realm.
	 */
	private User[] users;


	public String getRealmName()
	{
		return realmName;
	}


	public void setRealmName(String realmName)
	{
		this.realmName = realmName;
	}


	public User[] getUsers()
	{
		return users;
	}


	public void setUsers(User[] users)
	{
		this.users = users;
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

		FileRealm that = (FileRealm)o;
		return this.realmName.equals(that.realmName) && Arrays.equals(this.users, that.users);
	}


	@Override
	public int hashCode()
	{
		int result = realmName.hashCode();
		result = 31 * result + Arrays.hashCode(users);
		return result;
	}
}
