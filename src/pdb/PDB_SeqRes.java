package pdb;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import util.ConnectDB;
import util.DBqueries;
import util.Utility;

/**
 * This class reads a file in FASTA format to extract amino acid sequences for PDB entries 
 * of interest, and updates PostgreSQL accordingly.
 * 
 * @author 	Eshan Bundhoo and Anisah W. Ghoorah, University of Mauritius
 * @since	2019-02-19
 * @version	1.0
 *
 */

public class PDB_SeqRes {

	public static void parseFile(String file) throws IOException {		
		
		List<String> desc = new ArrayList<>();
		List<String> seq = new ArrayList<>();
		
		String[] array = DBqueries.getPDBIDs();
			
        BufferedReader in = new BufferedReader(new FileReader(file));
        StringBuffer buffer = new StringBuffer();
        String line = in.readLine();
     
        if (line == null)
        	throw new IOException ( file + " is an empty file" );
     
        if (line.charAt(0) != '>')
            throw new IOException ( "First line of " + file + " should start with '>'" );
        
        else
            desc.add(line);
        
        for (line = in.readLine().trim(); line != null; line = in.readLine()) {
            if (line.length() > 0 && line.charAt(0) == '>') {
				seq.add(buffer.toString());
				buffer = new StringBuffer();
				desc.add(line);
            } 
            else
            	buffer.append(line.trim());
        }  
        
        if (buffer.length() != 0)
        	seq.add(buffer.toString());
	
		for (int i=0; i<seq.size(); i++) {
			
			String pdbid = get_pdbid(desc.get(i));
			String chain = get_chain(desc.get(i));
			
			if (Utility.isIDofInterest(array, pdbid)) {
				
				String SQL = "UPDATE pdb_chain " + 
						"SET aa_sequence = ? " +
						"WHERE pdb_id = ? AND chain = ?";
				
				try (Connection conn = ConnectDB.connect();
						PreparedStatement pstmt = conn.prepareStatement(SQL)) {
					pstmt.setString(1, seq.get(i));
					pstmt.setString(2, pdbid);
					pstmt.setString(3, chain);
					
					pstmt.executeUpdate();
					conn.close();
						
				} catch (SQLException ex) {
					System.out.println(ex.getMessage());
				}
				
			}
			
		}
		in.close();
    }
	
	public static String get_pdbid(String header) {
		
		String[] tokens = header.split(" ");
		
		return tokens[0].split("_")[0].replaceAll(">", "");
	}
	
	public static String get_chain(String header) {
		
		String[] tokens = header.split(" ");
		
		return tokens[0].split("_")[1];
	}

}
