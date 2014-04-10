package net.sf.opk.glassfish;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Simple class to hold the configuration for GlassFish. Capable of initializing itzelf from and serializing itself to a byte array. This class performs no
 * validation.
 *
 * @author <a href="mailto:owestra@bol.com">Oscar Westra van Holthe - Kind</a>
 */
public class GlassFishConfiguration implements Serializable
{
	/**
	 * The HTTP port GlassFish should listen on.
	 */
	private final int httpPort;
	/**
	 * The HTTPS port GlassFish should listen on, if any.
	 */
	private final Integer httpsPort;
	/**
	 * (Optional) files to configure <code>java.util.logging</code>, which is the logging system used by GlassFish. Note that adding more than one file is not
	 * useful.
	 */
	private final List<File> loggingProperties;
	/**
	 * (Optional) resource files that defines the external resources required by the web application. Similar to
	 * <code>${webAppSourceDirectory}/WEB-INF/glassfish-resources.xml</code>, but some environments require database passwords etc. to be kept outside your
	 * application. This simulates that by loading the resources before the application is deployed.
	 */
	private final List<File> glassFishResources;
	/**
	 * The file realms (if any) to create prior to deploying the application. The predefined realms &quot;file&quot; and &quot;admin-realm&quot; are recognized
	 * and not created anew, though the users you define are added. The predefined realm &quot;certificate&quot; is also recognized, but will generate an error
	 * (it is not a file realm).
	 */
	private final List<FileRealm> fileRealms;
	/**
	 * Commands (if any) to <code>asadmin</code> to execute prior to deploying the application. The commands required for the properties
	 * <code>glassFishResources</code> and <code>fileRealms</code> will already be executed.
	 */
	private final List<Command> extraCommands;
	/**
	 * Extra {@code .war} / {@code .ear} files to deploy (if any).
	 */
	private final List<File> extraApplications;
	/**
	 * The context root to deploy the web application at. Must start with a {@code /}.
	 */
	private final String contextRoot;
	/**
	 * The web application sources directory. Usually this is the content of {@code src/main/webapp}.
	 */
	private final File webApplicationSourceDirectory;
	/**
	 * The classpath entries for the web application to deploy.
	 */
	private final List<File> webApplicationClassPath;


	public GlassFishConfiguration(int httpPort, Integer httpsPort, String contextRoot, File webApplicationSourceDirectory)
	{
		this.httpPort = httpPort;
		this.httpsPort = httpsPort;
		this.contextRoot = contextRoot;
		this.webApplicationSourceDirectory = webApplicationSourceDirectory;

		this.loggingProperties = new ArrayList<>();
		this.glassFishResources = new ArrayList<>();
		this.fileRealms = new ArrayList<>();
		this.extraCommands = new ArrayList<>();
		this.extraApplications = new ArrayList<>();
		this.webApplicationClassPath = new ArrayList<>();
	}


	public byte[] toByteArray() throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(this);
		return baos.toByteArray();
	}


	public static GlassFishConfiguration fromByteArray(byte[] rawData) throws IOException, ClassNotFoundException
	{
		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(rawData));
		return (GlassFishConfiguration)ois.readObject();
	}


	public int getHttpPort()
	{
		return httpPort;
	}


	public Integer getHttpsPort()
	{
		return httpsPort;
	}


	public List<File> getLoggingProperties()
	{
		return loggingProperties;
	}


	public void addLoggingProperties(File loggingProperties)
	{
		this.loggingProperties.add(loggingProperties);
	}


	public List<File> getGlassFishResources()
	{
		return glassFishResources;
	}


	public void addGlassFishResources(File glassFishResources)
	{
		this.glassFishResources.add(glassFishResources);
	}


	public List<FileRealm> getFileRealms()
	{
		return fileRealms;
	}


	public void addFileRealms(FileRealm[] fileRealms)
	{
		if (fileRealms != null)
		{
			this.fileRealms.addAll(Arrays.asList(fileRealms));
		}
	}


	public List<Command> getExtraCommands()
	{
		return extraCommands;
	}


	public void addExtraCommands(Command[] extraCommands)
	{
		if (extraCommands != null)
		{
			this.extraCommands.addAll(Arrays.asList(extraCommands));
		}
	}


	public List<File> getExtraApplications()
	{
		return extraApplications;
	}


	public void addExtraApplication(File extraApplication)
	{
		this.extraApplications.add(extraApplication);
	}


	public String getContextRoot()
	{
		return contextRoot;
	}


	public File getWebApplicationSourceDirectory()
	{
		return webApplicationSourceDirectory;
	}


	public List<File> getWebApplicationClassPath()
	{
		return webApplicationClassPath;
	}


	public void addToWebApplicationClassPath(File classPathentries)
	{
		this.webApplicationClassPath.add(classPathentries);
	}
}
