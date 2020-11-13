package mapping;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import util.ConnectDB;
import util.DBqueries1_0;

/**
 * This class parses a DAT file to map RefSeq protein IDs onto UniProt ACs for a 
 * given RefSeq genome AC, and updates PostgreSQL accordingly. 
 * 
 * @author 	Eshan Bundhoo, University of Mauritius
 * @since	2019-02-19
 * @version	1.0
 *
 */

public class GenBank2UniProt1_0 {

	public static void parseFile(String filename) throws FileNotFoundException {
		
		Scanner scanner = new Scanner(new FileInputStream(filename));
		
		Map<String, HashSet<String>> map = DBqueries1_0.getCdsProt();
		
		GBMapping1_0 gbm = null;
		
		//Start reading the file idmapping.dat found on UniProt FTP server
		while (scanner.hasNextLine()) {
			
			String line = scanner.nextLine();
			
			String[] tokens = line.split("\t");
			String db = tokens[1];
			
			if (db.equals("RefSeq")) {
				 
				gbm = new GBMapping1_0();
				
				String uniprot = tokens[0];
				String refseq = tokens[2];
				
				if (map.containsKey(refseq)) {
					gbm.setRefseq(refseq);
					gbm.setUniprotAc(uniprot);
				}
			}
			
			else if (db.equals("RefSeq_NT")) {
				
				String genome_ac = tokens[2].split("\\.")[0];
				
				if (map.containsKey(gbm.getRefseq())) {
					Set<String> set = map.get(gbm.getRefseq());
					if (set.contains(genome_ac)) {
						gbm.setGenomeAc(genome_ac);
						db_update(gbm.getRefseq(),gbm.getGenomeAc(),gbm.getUniprotAc());
					}
					
				}
				
			}
			
		}
		scanner.close();

	}
	
	//Update PostgreSQL
	public static void db_update(String refseq, String genome_ac, String uniprot_ac) {
		
		String SQL = "UPDATE cds SET uniprot_ac = ? "
				+ "WHERE protein_id = ? "
				+ "AND genome_ac = ?";
		
		try (Connection conn = ConnectDB.connect();
				PreparedStatement pstmt = conn.prepareStatement(SQL)) {
			pstmt.setString(1, uniprot_ac);
			pstmt.setString(2, refseq);
			pstmt.setString(3, genome_ac);
			
			pstmt.executeUpdate();
			
			conn.close();	
			
		} catch (SQLException ex) {
			System.out.println(ex.getMessage());
		}
	}

}
