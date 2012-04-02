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
