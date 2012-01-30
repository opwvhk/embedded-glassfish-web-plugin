package net.sf.opk.glassfish;

/**
 * Simple JavaBean to represent a FileRealm.
 *
 * @author <a href="mailto:oscar@westravanholthe.nl">Oscar Westra van Holthe - Kind</a>
 */
public class FileRealm
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
}
