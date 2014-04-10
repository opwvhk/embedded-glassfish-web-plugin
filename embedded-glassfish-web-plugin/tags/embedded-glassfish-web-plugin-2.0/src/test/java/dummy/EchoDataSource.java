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
package dummy;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;


/**
 * Testing servlet that echos the database product name.
 *
 * @author <a href="mailto:oscar@westravanholthe.nl">Oscar Westra van Holthe - Kind</a>
 */
public class EchoDataSource extends HttpServlet
{
	@Resource(name = "jdbc/hsqlDataSource")
	private DataSource dataSource;


	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		try
		{
			response.setContentType("text/plain");
			response.getWriter().printf("Database: %s\n", getDatabaseName());
		}
		catch (SQLException e)
		{
			throw new ServletException(e);
		}
	}


	private String getDatabaseName() throws SQLException
	{
		Connection connection = null;
		try
		{
			connection = dataSource.getConnection();
			return connection.getMetaData().getDatabaseProductName();
		}
		finally
		{
			if (connection != null)
			{
				connection.close();
			}
		}
	}
}
