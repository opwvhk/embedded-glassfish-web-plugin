package net.sf.opk.glassfish;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.LogManager;

import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.archive.ScatteredArchive;


/**
 * Callable to handle everything the plugin needs from GlassFish: startup (and deploy), redeploy and shutdown.
 *
 * @author <a href="mailto:oscar@westravanholthe.nl">Oscar Westra van Holthe - Kind</a>
 */
public class GlassFishWebPluginRunner implements Callable<Void>
{
	/**
	 * Logger for this class.
	 */
	private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(
			GlassFishWebPluginRunner.class.getName());

	private GlassFishConfiguration configuration;

	private Callable<Void> shutdownHook;
	private Callable<Void> redeployHook;


	public GlassFishWebPluginRunner(byte[] configurationBytes) throws IOException, ClassNotFoundException, GlassFishException
	{
		this.configuration = GlassFishConfiguration.fromByteArray(configurationBytes);
	}


	@Override
	public Void call() throws GlassFishException, IOException
	{
		// Startup and configure GlassFish.

		configureLogging();

		final EmbeddedGlassFish glassFish = new EmbeddedGlassFish(configuration.getHttpPort(),
		                                                          configuration.getHttpsPort());

		deployResources(glassFish);
		addFileRealms(glassFish);
		executeExtraCommands(glassFish);

		// Start the extra applications.

		deployExtraApplications(glassFish);

		// Deploy the web application.

		File webApplicationSourceDirectory = configuration.getWebApplicationSourceDirectory();
		final ScatteredArchive webApplicationArchive = new ScatteredArchive("projectWebapp", ScatteredArchive.Type.WAR,
		                                                                    webApplicationSourceDirectory);
		for (File classpathEntry : configuration.getWebApplicationClassPath())
		{
			webApplicationArchive.addClassPath(classpathEntry);
		}
		glassFish.deployArtifact(webApplicationArchive, configuration.getContextRoot());

		// Configure the hooks to handle GlassFish after we exit.
		shutdownHook = new Callable<Void>()
		{
			@Override
			public Void call() throws GlassFishException
			{
				glassFish.shutdown();
				//LogManager.getLogManager().reset();
				//Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());
				return null;
			}
		};
		redeployHook = new Callable<Void>()
		{
			@Override
			public Void call() throws GlassFishException, IOException
			{
				glassFish.undeployArtifacts();
				glassFish.deployArtifact(webApplicationArchive, configuration.getContextRoot());
				return null;
			}
		};
		return null;
	}


	private void configureLogging() throws IOException
	{
		for (File loggingProperties : configuration.getLoggingProperties())
		{
			LogManager.getLogManager().readConfiguration(new FileInputStream(loggingProperties));
			LOGGER.log(Level.INFO, "(Re)initialized logging using: {0}", loggingProperties.getPath());
		}
	}


	private void deployResources(EmbeddedGlassFish glassFish) throws GlassFishException
	{
		for (File glassFishResources : configuration.getGlassFishResources())
		{
			glassFish.addResources(glassFishResources);
		}
	}


	private void addFileRealms(EmbeddedGlassFish glassFish) throws IOException, GlassFishException
	{
		for (FileRealm fileRealm : configuration.getFileRealms())
		{
			glassFish.addFileRealmWithUsers(fileRealm);
		}
	}


	private void executeExtraCommands(EmbeddedGlassFish glassFish)
	{
		for (Command command : configuration.getExtraCommands())
		{
			glassFish.asadmin(command.getCommand(), command.getParameters());
		}
	}


	private void deployExtraApplications(EmbeddedGlassFish glassFish) throws GlassFishException
	{
		for (File file : configuration.getExtraApplications())
		{
			LOGGER.log(Level.FINE, "Deploying dependency {0}", file.getName());
			glassFish.deployApplication(file);
			LOGGER.log(Level.INFO, "Deployed dependency {0}", file.getName());
		}
	}


	@SuppressWarnings("UnusedDeclaration")
	public GlassFishConfiguration getConfiguration()
	{
		return configuration;
	}


	public Callable<Void> getShutdownHook()
	{
		return shutdownHook;
	}


	public Callable<Void> getRedeployHook()
	{
		return redeployHook;
	}
}
