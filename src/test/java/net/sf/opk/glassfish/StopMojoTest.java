package net.sf.opk.glassfish;

import org.junit.Test;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;


/**
 * Test class for the class {@link StopMojo}.
 *
 * @author <a href="mailto:oscar@westravanholthe.nl">Oscar Westra van Holthe - Kind</a>
 */
public class StopMojoTest extends MojoTestBase
{
	@Test
	public void testExecute() throws Exception
	{
		EmbeddedGlassFish mockGlassFish = createMock(EmbeddedGlassFish.class);

		mockGlassFish.shutdown();
		expectLastCall().once();
		replay(mockGlassFish);

		StopMojo mojo = configureMojo(new StopMojo(), HTTP_PORT, HTTPS_PORT, mockGlassFish);
		mojo.execute();

		verify(mockGlassFish);
	}
}
