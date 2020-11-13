package mapping;

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
 * This class parses a TSV file (pdb_chain_uniprot.tsv) from EBI SIFTS to map UniProt ACs 
 * onto PDB IDs, and updates PostgreSQL accordingly.
 * 
 * @author 	Eshan Bundhoo, University of Mauritius
 * @since	2019-02-19
 * @version	1.0
 *
 */

public class UniProt2PDB {

	public static void parseFile(String filename) throws FileNotFoundException {
		
		Scanner scanner = new Scanner(new FileInputStream(filename));
		
		String[] array = DBqueries.getUniProtACs();
		
		//Start reading the file pdb_chain_uniprot.tsv found on SIFTS website
		while (scanner.hasNextLine()) {
			
			String line = scanner.nextLine();
			
			if (line.startsWith("#") || line.startsWith("PDB"))
				continue;
			
			String[] tokens = line.split("\t");
				
			String swissprot = tokens[2];
			
			if (Utility.isIDofInterest(array, swissprot)) {
				
				String pdbid = tokens[0];
				String chain = tokens[1]; 
				String pdbstart = tokens[5];
				String pdbstop = tokens[6];		
				int spstart = Integer.parseInt(tokens[7]);
				int spstop = Integer.parseInt(tokens[8]);
				
				//Insert into database
				String SQL = "INSERT INTO protein2pdb(uniprot_ac, sp_start, sp_stop, pdb_id, chain, pdb_start, pdb_stop) " + 
						"VALUES (?,?,?,?,?,?,?)";
				
				try (Connection conn = ConnectDB.connect();
						PreparedStatement pstmt = conn.prepareStatement(SQL)) {
					pstmt.setString(1, swissprot);
					pstmt.setInt(2, spstart);
					pstmt.setInt(3, spstop);
					pstmt.setString(4, pdbid);
					pstmt.setString(5, chain);
					pstmt.setString(6, pdbstart);
					pstmt.setString(7, pdbstop);
					
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
