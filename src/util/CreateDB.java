package util;

/**
 * This class contains the SQL statement used by TAGOPSIN for automatically 
 * creating the local database "tagopsin" in PostgreSQL.
 * 
 * @author 	Eshan Bundhoo, University of Mauritius
 * @since	2020-02-01
 * @version	1.0
 *
 */

public class CreateDB {
	
	public static String getSQL() {
		
		String sql = "";
		
		sql += "CREATE DATABASE tagopsin\n";
		sql += "    WITH\n";
		sql += "    OWNER = postgres\n";
		sql += "    ENCODING = 'UTF8'\n";
		//sql += "    LC_COLLATE = 'C'\n";
		//sql += "    LC_CTYPE = 'C'\n";
		sql += "    TABLESPACE = pg_default\n";
		sql += "    CONNECTION LIMIT = -1;";
		
		return sql;
	}

}
