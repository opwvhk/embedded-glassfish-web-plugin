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
	@Resource(name = "jdbc/myDataSource")
	private DataSource dataSource;


	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,
	                                                                                      IOException
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
