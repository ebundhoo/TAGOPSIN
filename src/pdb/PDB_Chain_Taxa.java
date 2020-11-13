package pdb;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

import util.ConnectDB;
import util.DBqueries;

/**
 * This class reads a TSV file (pdb_chain_taxonomy.tsv) from EBI SIFTS to map 
 * PDB IDs onto NCBI taxonomy IDs, and updates PostgreSQL accordingly.
 *  
 * @author 	Eshan Bundhoo and Anisah W. Ghoorah, University of Mauritius
 * @since	2019-02-19
 * @version	1.0
 *
 */

public class PDB_Chain_Taxa {

	public static void parseFile(String filename) throws FileNotFoundException {
		
		HashMap<String, Integer> mapFile = readPdbChainTaxaFile(filename);  	
		
		Map<String, HashSet<String>> mapDB = DBqueries.getPdbIDsWithChain();
		
		Set<Entry<String, HashSet<String>>> entrySet = mapDB.entrySet();
		
		Iterator<Entry<String, HashSet<String>>> it = entrySet.iterator();
		
		while (it.hasNext()) {
			
			Entry<String, HashSet<String>> entry = it.next();
			String pdbcode = entry.getKey();
			HashSet<String> chainlabels = entry.getValue();
			
			for (String chain : chainlabels) {
				
				//Update database
				String SQL = "INSERT INTO pdb_chain(pdb_id, chain) VALUES (?,?)";
				
				String SQLt = "UPDATE pdb_chain SET taxonomy_id = ? "
						+ "WHERE pdb_id = ? AND chain = ?";
				
				try (Connection conn = ConnectDB.connect();
						PreparedStatement pstmt1 = conn.prepareStatement(SQL);
						PreparedStatement pstmt2 = conn.prepareStatement(SQLt)) {
					
					pstmt1.setString(1, pdbcode);
					pstmt1.setString(2, chain);
					
					pstmt1.executeUpdate();
				
					String pdbchain = pdbcode + "_" + chain;
					
					if (mapFile.containsKey(pdbchain)) {
						
						Integer taxaid = mapFile.get(pdbchain);
						
						pstmt2.setInt(1, taxaid);
						pstmt2.setString(2, pdbcode);
						pstmt2.setString(3, chain);
						
						pstmt2.executeUpdate();
						conn.close();	
							
					}
					
				} catch (SQLException ex) {
					System.out.println(ex.getMessage());
				}
				
			}
			
		}		

	}
	
	public static HashMap<String, Integer> readPdbChainTaxaFile(String filename) throws FileNotFoundException {
		
		Scanner scanner = new Scanner(new FileInputStream(filename));
		
		HashMap<String, Integer> map = new HashMap<>();
		
		//Start reading file
		while (scanner.hasNextLine()) {
			
			String line = scanner.nextLine();
			
			if (line.startsWith("#") || line.startsWith("PDB"))		//skip header
				continue;
			
			String[] tokens = line.split("\t");
			String pdbchain = tokens[0] + "_" + tokens[1];
			
			//Store data in HashMap
			if (! map.containsKey(pdbchain))
				map.put(pdbchain, Integer.parseInt(tokens[2]));
		}
		scanner.close();
		
		return map;		
	}

}
