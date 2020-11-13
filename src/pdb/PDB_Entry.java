package pdb;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Scanner;

import util.ConnectDB;
import util.DBqueries;
import util.Utility;

/**
 * This class reads the file entries.idx found on the RCSB PDB FTP server and retrieves 
 * the name, experimental method and resolution of PDB IDs of interest. These data are 
 * then inserted into PostgreSQL.
 * 
 * @author 	Eshan Bundhoo, University of Mauritius
 * @since	2019-02-19
 * @version	1.0
 *
 */

public class PDB_Entry {

	public static void parseFile(String filename) throws FileNotFoundException {
		
		Scanner scanner = new Scanner(new FileInputStream(filename));
		
		String[] array = DBqueries.getPDBIDs();
		
		while (scanner.hasNextLine()) {
			
			String line = scanner.nextLine();
			
			if (line.startsWith("IDCODE") || line.startsWith("-"))		//skip header
				continue;
			
			String[] tokens = line.split("\t");
			String pdbid = tokens[0];
			
			if (Utility.isIDofInterest(array, pdbid.toLowerCase())) {
				
				String name = tokens[3];
				String resolution = tokens[6];
				String method = tokens[7];
				
				//Insert into database
				String SQL = "INSERT INTO pdb VALUES (?,?,?,?)";
				
				try (Connection conn = ConnectDB.connect();
						PreparedStatement pstmt = conn.prepareStatement(SQL)) {
					pstmt.setString(1, pdbid.toLowerCase());
					pstmt.setString(2, name);
					pstmt.setString(3, method);
					pstmt.setString(4, resolution);
					
					pstmt.executeUpdate();
					conn.close();	
					
				} catch (SQLException ex) {
					System.out.println(ex.getMessage());
				}
								
			}
			
		}
		scanner.close();

	}

}
