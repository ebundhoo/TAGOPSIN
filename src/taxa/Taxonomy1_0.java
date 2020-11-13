package taxa;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Scanner;

import util.ConnectDB;

/**
 * This class parses a DMP file, retrieves scientific names and taxonomy IDs, and inserts 
 * them into PostgreSQL.
 * 
 * @author 	Eshan Bundhoo and Anisah W. Ghoorah, University of Mauritius
 * @since	2019-02-19
 * @version	1.0
 *
 */

public class Taxonomy1_0 {

	public static void parseFile(String filename, String organism) throws IOException {
		
		Scanner scanner = new Scanner(new FileInputStream(filename));
		
		while (scanner.hasNextLine()) {
			
			String line = scanner.nextLine();
			line = line.replaceAll("\\s{2,}", " ");
			line = line.replaceAll("\t", "");
		
			String[] tokens = line.split("\\|");
			
			int taxid = Integer.parseInt(tokens[0]);
			String nametxt = tokens[1];
			String nameclass = tokens[3];
	       
			if (nameclass != null && nameclass.equals("scientific name")) {
        		if (nametxt.startsWith(organism)) {							
        			
        			String SQL = "INSERT INTO organism(species, taxonomy_id) VALUES(?,?)";
					
					try (Connection conn = ConnectDB.connect();
							PreparedStatement pstmt = conn.prepareStatement(SQL)) {
						pstmt.setString(1, nametxt);
						pstmt.setInt(2, taxid);
						
						pstmt.executeUpdate();
						conn.close();

					} catch (SQLException ex) {
						System.out.println(ex.getMessage());
						System.exit(0);
					}
						
				}
	        }
			
		}
		scanner.close();
		
	}

}
