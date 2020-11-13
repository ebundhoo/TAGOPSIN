package uniprot;
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
 * This class reads a FASTA-format file to extract UniProt protein names and amino acid sequences.
 * 
 *  @author		Anisah W. Ghoorah, University of Mauritius
 *  @since		2019-02-19
 *  @version	1.0
 */

public class ReadFastaProtein {

	public static void parseFile(String file) throws IOException {
		
		List<String> desc = new ArrayList<>();
		List<String> seq = new ArrayList<>();
		
		String[] array = DBqueries.getUniProtACs();
			
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
			
			String ac = get_ac(desc.get(i));
			String name = get_name(desc.get(i));
	
			if (Utility.isIDofInterest(array, ac)) {
				
				String SQL = "UPDATE protein " + 
						"SET name = ?, aa_sequence = ? " +
						"WHERE uniprot_ac = ?";
				
				try (Connection conn = ConnectDB.connect();
						PreparedStatement pstmt = conn.prepareStatement(SQL)) {
					pstmt.setString(1, name);
					pstmt.setString(2, seq.get(i));
					pstmt.setString(3, ac);
					
					pstmt.executeUpdate();
					conn.close();
						
				} catch (SQLException ex) {
					System.out.println(ex.getMessage());
				}
				
			}
			
		}
		in.close();
		
    }
	
	public static String get_ac(String header) {
		
		String[] tokens = header.split("\\|");
		return tokens[1];
	}
	
	public static String get_name(String header) {
		
		String[] tokens = header.split("\\|");
		String[] tok = tokens[2].split(" ");
		
		String name = tok[1];
		for (int i=2; i<tok.length; i++) {
			
			if (tok[i].startsWith("OS=")) {
				break;
			}
			name = name + " " + tok[i];
		}
		return name;
	}

}
