package mapping;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Scanner;

import uniprot.Protein;
import uniprot.UniProt;
import util.ConnectDB;
import util.DBqueries;
import util.Utility;

/**
 * This class parses a DAT file to map UniProt ACs onto GO IDs, and updates 
 * PostgreSQL accordingly.
 * 
 * @author 	Eshan Bundhoo, University of Mauritius
 * @since	2019-02-19
 * @version	1.0
 *
 */

public class UniProt2GO {

	public static void parseFile(String filename) throws FileNotFoundException {
		
		Scanner scanner = new Scanner(new FileInputStream(filename));
		UniProt u = new UniProt();
		
		String[] array = DBqueries.getUniProtACs();

		//Start reading the file uniprot_sprot.dat found on UniProt FTP server
		while (scanner.hasNextLine()) {
			
			String line = scanner.nextLine();
			
			if (line.startsWith("AC")) {
				String ac = Protein.extract_ac(line); 
				u.setAc(ac);
			}
			if (line.startsWith("DR   GO;")) {
				String goid = extract_goid(line);
				u.addGoId(goid);
			}
			
			if (line.startsWith("//")) {
				if (Utility.isIDofInterest(array, u.getAc())) {
					
					for (String goid : u.getGoId()) {
						
						//Insert into database
						String SQL = "INSERT INTO protein2go(uniprot_ac, go_id) VALUES(?,?)";
						
						try (Connection conn = ConnectDB.connect();
								PreparedStatement pstmt = conn.prepareStatement(SQL)) {
							pstmt.setString(1, u.getAc());
							pstmt.setString(2, goid);
							
							pstmt.executeUpdate();
							conn.close();

						} catch (SQLException ex) {
							System.out.println(ex.getMessage());
						}
						
					}
						
				}
				u = new UniProt();
			}
		}
		scanner.close();

	}
	
	public static String extract_goid(String line) {
		
		String tline = line.replaceAll("\\s{2,}", " ");
		return (tline.split(" ")[2]).replace(";", "");
	}

}
