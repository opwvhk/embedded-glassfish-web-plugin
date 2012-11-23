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

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.archive.ScatteredArchive;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * Test class for the GlassFish facade.
 *
 * @author <a href="mailto:oscar@westravanholthe.nl">Oscar Westra van Holthe - Kind</a>
 */
public class EmbeddedGlassFishTest
{
	@Test
	public void testSequence() throws GlassFishException, IOException, URISyntaxException
	{
		int httpPort = findUnusedPort();
		int httpsPort = findUnusedPort();

		EmbeddedGlassFish embeddedGlassFish = new EmbeddedGlassFish(httpPort, httpsPort);
		try
		{
			File resources = getResourceAsFile("/resources.xml");
			File warFile = getResourceAsFile("/minimal_war.war");

			User user1 = new User();
			user1.setUsername("test1");
			user1.setPassword("pass1");
			user1.setRoles(new String[]{"role1", "role2"});
			User user2 = new User();
			user2.setUsername("test2");
			user2.setPassword("pass2");
			user2.setRoles(new String[]{"role1", "role3"});
			FileRealm fileRealm = new FileRealm();
			fileRealm.setRealmName("testRealm");
			fileRealm.setUsers(new User[]{user1, user2});

			ScatteredArchive artifact = createScatteredArchive();
			embeddedGlassFish.startup();
			embeddedGlassFish.addResources(resources);
			embeddedGlassFish.addFileRealm(fileRealm);
			embeddedGlassFish.deployApplication(warFile);
			embeddedGlassFish.deployArtifact(artifact, "/test");

			checkResult(httpPort, "/minimal_war/index.html", HttpServletResponse.SC_OK);
			checkResult(httpPort, "/test", HttpServletResponse.SC_OK);

			embeddedGlassFish.undeployLastArtifact();

			checkResult(httpPort, "/minimal_war/index.html", HttpServletResponse.SC_OK);
			checkResult(httpPort, "/test", HttpServletResponse.SC_NOT_FOUND);

			embeddedGlassFish.deployArtifact(artifact, "/test");

			checkResult(httpPort, "/minimal_war/index.html", HttpServletResponse.SC_OK);
			checkResult(httpPort, "/test", HttpServletResponse.SC_OK);

			embeddedGlassFish.undeployAllApplications();

			checkResult(httpPort, "/minimal_war/index.html", HttpServletResponse.SC_NOT_FOUND);
			checkResult(httpPort, "/test", HttpServletResponse.SC_NOT_FOUND);
		}
		finally
		{
			embeddedGlassFish.shutdown();
		}
	}


	private int findUnusedPort() throws IOException
	{
		ServerSocket socket = null;
		try
		{
			socket = new ServerSocket(0);
			return socket.getLocalPort();
		}
		finally
		{
			if (socket != null)
			{
				socket.close();
			}
		}
	}


	private File getResourceAsFile(String resource) throws URISyntaxException
	{
		URL resourceURL = getClass().getResource(resource);
		return new File(resourceURL.toURI());
	}


	private void checkResult(int port, String location, int responseCode) throws IOException, URISyntaxException
	{
		DefaultHttpClient httpclient = new DefaultHttpClient();

		URI uri = new URI("http", null, "localhost", port, location, null, null);
		HttpGet httpget = new HttpGet(uri);
		HttpResponse response = httpclient.execute(httpget);
		assertEquals(responseCode, response.getStatusLine().getStatusCode());
	}


	protected ScatteredArchive createScatteredArchive() throws IOException, URISyntaxException
	{

		ScatteredArchive archive = new ScatteredArchive("testapp", ScatteredArchive.Type.WAR, getResourceAsFile(
				"/dummyapp"));
		File servlet = getResourceAsFile("/dummy/EchoDataSource.class");
		File classpath = new File(servlet, "../..").getCanonicalFile();
		archive.addClassPath(classpath);

		return archive;
	}


	@Test(expected = IllegalStateException.class)
	public void testSequenceError1() throws GlassFishException, IOException
	{
		int httpPort = findUnusedPort();
		int httpsPort = findUnusedPort();

		EmbeddedGlassFish embeddedGlassFish = new EmbeddedGlassFish(httpPort, httpsPort);
		try
		{
			embeddedGlassFish.addResources(null);
		}
		finally
		{
			embeddedGlassFish.shutdown();
		}
	}


	@Test(expected = IllegalStateException.class)
	public void testSequenceError2() throws IOException, GlassFishException
	{
		int httpPort = findUnusedPort();
		int httpsPort = findUnusedPort();

		EmbeddedGlassFish embeddedGlassFish = new EmbeddedGlassFish(httpPort, httpsPort);
		try
		{
			embeddedGlassFish.startup();
			embeddedGlassFish.startup();
		}
		finally
		{
			embeddedGlassFish.shutdown();
		}
	}
}
