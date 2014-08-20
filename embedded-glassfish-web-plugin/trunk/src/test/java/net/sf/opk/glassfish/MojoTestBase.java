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

import java.lang.reflect.Field;
import java.util.concurrent.Callable;

import org.junit.After;
import org.junit.Before;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;


public class MojoTestBase
{
	protected Callable<Void> glassFishWebPluginRunner;
	protected Callable<Void> shutdownHook;
	protected Callable<Void> redeployHook;


	@Before
	public void initialize() throws Exception
	{
		// Should not be called.
		shutdownHook = createMock(Callable.class);

		redeployHook = createMock(Callable.class);

		GlassFishWebPluginRunner mockRunner = createMock(GlassFishWebPluginRunner.class);
		expect(mockRunner.getShutdownHook()).andStubReturn(shutdownHook);
		expect(mockRunner.getRedeployHook()).andStubReturn(redeployHook);
		glassFishWebPluginRunner = mockRunner;
	}


	@After
	public void allowGarbageCollection()
	{
		glassFishWebPluginRunner = null;
		shutdownHook = null;
		redeployHook = null;
		System.gc();
	}


	protected <M extends ConfiguredEmbeddedGlassFishMojo> M configureMojo(M mojo,
	                                                                      Callable<Void> glassFishWebPluginRunner)
			throws Exception
	{
		Class<ConfiguredEmbeddedGlassFishMojo> mojoClass = ConfiguredEmbeddedGlassFishMojo.class;
		getField(mojoClass, "glassFishWebPluginRunner").set(mojo, glassFishWebPluginRunner);
		return mojo;
	}


	protected Field getField(Class type, String fieldName) throws Exception
	{
		Field field = type.getDeclaredField(fieldName);
		field.setAccessible(true);
		return field;
	}
}
