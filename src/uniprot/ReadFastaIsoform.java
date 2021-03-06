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
 * This class reads a FASTA-format file to extract UniProt ACs and amino acid sequences of protein isoforms.
 * 
 * @author 	Anisah W. Ghoorah, University of Mauritius
 * @since	2021-02-19
 * @version	1.0
 *
 */

public class ReadFastaIsoform {

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
			
			//System.out.println(desc.get(i));
			String uniprotAc = get_uniprotAc(desc.get(i));
			String isoformAc = get_isoformAc(desc.get(i));
			
			if (Utility.isIDofInterest(array, uniprotAc)) {
				
				String SQL = "INSERT INTO protein_isoform VALUES(?,?,?)";
				
				try (Connection conn = ConnectDB.connect();
						PreparedStatement pstmt = conn.prepareStatement(SQL)) {
					
					pstmt.setString(1, isoformAc);
					pstmt.setString(2, uniprotAc);
					pstmt.setString(3, seq.get(i));
					
					pstmt.executeUpdate();
					
					conn.close();
						
				} catch (SQLException ex) {
					System.out.println(ex.getMessage());
				}
				
			}
			//System.out.println(uniprotac);
			
			//System.out.println(seq.get(i));
		}
		in.close();
		
    }
	
	public static String get_isoformAc(String header) {
		
		String[] tokens = header.split("\\|");
		return tokens[1];
	}
	
	public static String get_uniprotAc(String header) {
			
		String[] tokens = header.split("\\|");
		return tokens[1].split("-")[0];
	}

}
