package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import main.MainProgram;

/**
 * This class establishes a connection to the PostgreSQL relational database management system.
 * 
 * @author 	Eshan Bundhoo, University of Mauritius
 * @since	2019-02-19
 * @version	1.0
 *
 */

public class ConnectDB {
	
	private static ConnectParam cparam = MainProgram.access_conn_param();
	
	private static final String url = cparam.getUrl();
	private static final String user = cparam.getUsername();
	private static final String password = cparam.getPassword();
	
	public static Connection connect() {
		
		Connection conn = null;
		try {
			conn = DriverManager.getConnection(url, user, password);
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		
		return conn;
	}

}
