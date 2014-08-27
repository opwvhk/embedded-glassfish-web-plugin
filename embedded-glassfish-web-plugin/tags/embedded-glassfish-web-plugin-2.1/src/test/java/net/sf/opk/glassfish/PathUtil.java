package net.sf.opk.glassfish;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


final class PathUtil
{
	/**
	 * Locate the base directory.
	 *
	 * @return the base directory
	 */
	static File getBaseDirectory()
	{
		try
		{
			String className = ConfiguredEmbeddedGlassFishMojoTest.class.getName();
			String classResource = '/' + className.replace('.', '/') + ".class";
			String packageNameWithDot = className.replaceAll("(?<=\\.)[^\\.]+$", "");
			String ancestorPath = "../../../" + packageNameWithDot.replaceAll("[^\\.]+\\.", "../");
			return new File(resource(classResource), ancestorPath).getCanonicalFile();
		}
		catch (IOException e)
		{
			throw new IllegalStateException("Unable to locate the base directory.", e);
		}
	}


	/**
	 * Get a file reference to a resource on the classpath.
	 *
	 * @param name the name of the resource
	 * @return a File referencing the resource
	 */
	static File resource(String name)
	{
		try
		{
			URL location = MojoTestBase.class.getResource(name);
			if (location == null)
			{
				throw new IllegalArgumentException(String.format(
						"The resource \"%s\" cannot be found on the " + "classpath", name));
			}
			return new File(location.toURI());
		}
		catch (URISyntaxException ignored)
		{
			throw new IllegalArgumentException(String.format("The resource \"%s\" cannot be referenced as a URI",
			                                                 name));
		}
	}


	/**
	 * Find the classpath entries that contains specific classes.
	 *
	 * @param classes the classes we'll need to load
	 * @return the classpath containing the classes
	 */
	static List<File> findClasspath(Class<?>... classes)
	{
		List<File> classpath = new ArrayList<>();
		for (Class<?> clazz : classes)
		{
			try
			{
				classpath.add(new File(clazz.getProtectionDomain().getCodeSource().getLocation().toURI()));
			}
			catch (URISyntaxException e)
			{
				throw new IllegalStateException("Cannot find classpath entry for " + clazz, e);
			}
		}
		return classpath;
	}


	private PathUtil()
	{
		// Nothing to do.
	}
}
