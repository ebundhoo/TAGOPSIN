package uniprot;
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
 * This class parses a DAT file. It retrieves UniProt ACs of interest, plus their UniProt ID, 
 * function, sub-cellular location and information about their AA sequence, and inserts 
 * the data into PostgreSQL.
 * 
 * @author 	Eshan Bundhoo and Anisah W. Ghoorah, University of Mauritius
 * @since	2019-02-19
 * @version	1.0
 *
 */

public class Protein {

	public static void parseFile(String filename) throws FileNotFoundException {
		
		Scanner scanner = new Scanner(new FileInputStream(filename));
		UniProt u = new UniProt();
		
		String[] array = DBqueries.getCdsUniProt();
		
		String flag = "";

		//Start reading the file
		while (scanner.hasNextLine()) {
			
			String line = scanner.nextLine();
			
			if (line.startsWith("ID")) {
				String id = extract_id(line); 
				u.setId(id);
				int length = extract_length(line); 
				u.setLength(length);
			}
			if (line.startsWith("AC")) {
				String ac = extract_ac(line); 
				u.setAc(ac);
			}
			if (line.startsWith("CC   -!-")) {
				flag = "";
			}
			if (line.startsWith("CC   -!- FUNCTION:")) {
				String function = extract_function(line);  
				u.setFunction(function);
				flag = "FUNCTION";
			}
			if (line.startsWith("CC   -!- SUBCELLULAR LOCATION:")) {
				String sclocation = extract_sclocation(line); 
				u.setScLocation(sclocation);
				flag = "LOCATION";
			}
			if (line.startsWith("CC       ")) {
				String temp = line.replaceAll("CC       ", "");
				if (flag.equals("FUNCTION")) {
					u.setFunction(u.getFunction() + " " + temp);
				}
				else if (flag.equals("LOCATION")) {
					u.setScLocation(u.getScLocation() + " " + temp);
				}
			}
			if (line.startsWith("//")) {
				if (Utility.isIDofInterest(array, u.getAc())) {
					
					//Insert into PostgreSQL "protein" table
					String SQL = "INSERT INTO protein(uniprot_ac, function, sc_location, aa_seq_length, uniprot_id) " + 
							"VALUES(?,?,?,?,?)";
					
					try (Connection conn = ConnectDB.connect();
							PreparedStatement pstmt = conn.prepareStatement(SQL)) {
						pstmt.setString(1, u.getAc());
						pstmt.setString(2, u.getFunction());
						pstmt.setString(3, u.getScLocation());
						pstmt.setInt(4, u.getLength());
						pstmt.setString(5, u.getId());
						
						pstmt.executeUpdate();
						conn.close();

					} catch (SQLException ex) {
						System.out.println(ex.getMessage());
					}
					
				}
				u = new UniProt();
				flag = "";
			}
		}
		scanner.close();
		
	}
	
	public static String extract_id(String line) {
		
		String tline = line.replaceAll("\\s{2,}", " ");
		return (tline.split(" "))[1];		
	}
	
	public static String extract_ac(String line) {
		
		String tline = line.replaceAll("\\s{2,}", " ");
		return ((tline.split(" "))[1]).replace(";", "");
	}
	
	public static int extract_length(String line) {
		
		String tline = line.replaceAll("\\s{2,}", " ");
		return Integer.parseInt((tline.split(" "))[3]);
	}
	
	public static String extract_function(String line) {
		
		return line.replaceAll("CC   -!- FUNCTION: ", "");
	}
	
	public static String extract_sclocation(String line) {
		
		return line.replaceAll("CC   -!- SUBCELLULAR LOCATION: ", "");
	}

}
