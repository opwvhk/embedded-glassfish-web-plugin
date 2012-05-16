package net.sf.opk.glassfish;

/**
 * Simple JavaBean to represent a user.
 *
 * @author <a href="mailto:oscar@westravanholthe.nl">Oscar Westra van Holthe - Kind</a>
 */
public class User
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
}
