package util;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * This class lists all the database queries used by TAGOPSIN (versions 1.0 and 1.1).
 * 
 * @author 	Eshan Bundhoo and Anisah W. Ghoorah, University of Mauritius
 * @since	2019-02-19
 * @version	1.0
 *
 */

public class DBqueries1_0 {
	
	//get number of taxIDs excluding taxIDs with one-word species name
	public static int getSpeciesCount() {
			
		String SQL = "SELECT COUNT (taxonomy_id) FROM organism WHERE " +
				"array_length(string_to_array(species, ' '), 1)  > 1";
		int count = 0;
			
		try (Connection conn = ConnectDB.connect();
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(SQL)) {
			rs.next();
			count = rs.getInt(1);
			conn.close();
		} catch (SQLException ex) {
			System.out.println(ex.getMessage());
		}
		
		return count;
	}
		
	//function to retrieve list of taxIDs
	public static int[] getTaxIDs() {
			
		int[] array = new int[getSpeciesCount()];
		int i = 0;
			
		String SQL = "SELECT DISTINCT taxonomy_id FROM organism WHERE " + 
				"array_length(string_to_array(species, ' '), 1)  > 1";
			
		try {
			Connection conn = ConnectDB.connect();
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(SQL);
				 
			while (rs.next()) {
			    array[i] = rs.getInt(1);
			    i++;
			}
			conn.close();
		} catch (SQLException ex) {
			System.out.println(ex.getMessage());
		}
		
		Arrays.sort(array);
		return array;		
	}
	
	//get number of OIDs in Organism table
	public static int getOidCount() {
			
		String SQL = "SELECT COUNT (oid) FROM organism WHERE " +
				"array_length(string_to_array(species, ' '), 1)  > 1";
		int count = 0;
			
		try (Connection conn = ConnectDB.connect();
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(SQL)) {
			rs.next();
			count = rs.getInt(1);
			conn.close();
		} catch (SQLException ex) {
			System.out.println(ex.getMessage());
		}
		
		return count;
	}
	
	//function to retrieve list of organism names from Organism table
	public static String[] getOrgName() {
			
		String[] array = new String[getOidCount()];
		int i = 0;
		
		String SQL = "SELECT DISTINCT species FROM organism WHERE " + 
				"array_length(string_to_array(species, ' '), 1)  > 1";
		
		try {
			Connection conn = ConnectDB.connect();
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(SQL);
			 
			while (rs.next()) {
			    array[i] = rs.getString(1);
			    i++;
			}
			conn.close();
		} catch (SQLException ex) {
			System.out.println(ex.getMessage());
		}
		
		Arrays.sort(array);
		return array;
	}
	
	//function to retrieve list of distinct protein_id with genome_ac from CDS table
	public static Map<String, HashSet<String>> getCdsProt() {
		
		Map<String, HashSet<String>> map = new HashMap<>();
		
		String SQL = "SELECT DISTINCT protein_id, genome_ac FROM cds "		
				+ "WHERE protein_id IS NOT NULL "
				+ "AND uniprot_ac IS NULL";
		
		try {
			Connection conn = ConnectDB.connect();
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(SQL);
			 
			while (rs.next()) {
				String refseq = rs.getString(1);
				String genome_ac = rs.getString(2);
				
				if (map.containsKey(refseq)) {
					HashSet<String> set = map.get(refseq);
					set.add(genome_ac);
					map.put(refseq, set);
				}
				else {
					HashSet<String> set = new HashSet<>();
					set.add(genome_ac);
					map.put(refseq, set);
				}				
			}
			conn.close();
		} catch (SQLException ex) {
			System.out.println(ex.getMessage());
		}
		
		return map;		
	}
	
	//get number of non-null UniProt ACs in CDS table
	public static int getCdsUniProtCnt() {
		
		String SQL = "SELECT COUNT (DISTINCT uniprot_ac) FROM cds "		
				+ "WHERE uniprot_ac IS NOT NULL";							
		int count = 0;
			
		try (Connection conn = ConnectDB.connect();
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(SQL)) {
			rs.next();
			count = rs.getInt(1);
			conn.close();
		} catch (SQLException ex) {
			System.out.println(ex.getMessage());
		}
		
		return count;	
	}
	
	//function to retrieve list of non-null UniProt ACs from CDS table
	public static String[] getCdsUniProt() {
		
		String[] array = new String[getCdsUniProtCnt()];
		int i = 0;
		
		String SQL = "SELECT DISTINCT uniprot_ac FROM cds "
				+ "WHERE uniprot_ac IS NOT NULL";
		
		try {
			Connection conn = ConnectDB.connect();
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(SQL);
			 
			while (rs.next()) {
			    array[i] = rs.getString(1);
			    i++;
			}
			conn.close();
		} catch (SQLException ex) {
			System.out.println(ex.getMessage());
		}
		
		Arrays.sort(array);
		return array;
		
	}
	
	//get number of proteins in Protein table
	public static int getProtCount() {
			
		String SQL = "SELECT COUNT (uniprot_ac) FROM protein";
		int count = 0;
			
		try (Connection conn = ConnectDB.connect();
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(SQL)) {
			rs.next();
			count = rs.getInt(1);
			conn.close();
		} catch (SQLException ex) {
			System.out.println(ex.getMessage());
		}
		
		return count;
	}
		
	//function to retrieve list of UniProtACs from Protein table
	public static String[] getUniProtACs() {
		
		String[] array = new String[getProtCount()];
		int i = 0;
		
		String SQL = "SELECT uniprot_ac FROM protein";
		
		try {
			Connection conn = ConnectDB.connect();
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(SQL);
			 
			while (rs.next()) {
			    array[i] = rs.getString(1);
			    i++;
			}
			conn.close();
		} catch (SQLException ex) {
			System.out.println(ex.getMessage());
		}
		
		Arrays.sort(array);
		return array;
	}
	
	//get number of GO IDs in Protein2GO table
	public static int getProtGoIdCnt() {
		
		String SQL = "SELECT COUNT (DISTINCT go_id) FROM protein2go";		
		int count = 0;
			
		try (Connection conn = ConnectDB.connect();
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(SQL)) {
			rs.next();
			count = rs.getInt(1);
			conn.close();
		} catch (SQLException ex) {
			System.out.println(ex.getMessage());
		}
		
		return count;
	}
	
	//function to retrieve list of GO IDs from Protein2GO table
	public static String[] getProtGoIds() {
		
		String[] array = new String[getProtGoIdCnt()];
		int i = 0;
		
		String SQL = "SELECT DISTINCT go_id FROM protein2go";
		
		try {
			Connection conn = ConnectDB.connect();
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(SQL);
			 
			while (rs.next()) {
			    array[i] = rs.getString(1);
			    i++;
			}
			conn.close();
		} catch (SQLException ex) {
			System.out.println(ex.getMessage());
		}
		
		Arrays.sort(array);
		return array;
	}
	
	//get count of all GO IDs in GO_Parent table
	public static int getCntAllGoIds() {
		
		String SQL = "SELECT COUNT (*) FROM (" + 		
				"(SELECT DISTINCT go_id FROM go_parent) " + 
				"UNION (SELECT DISTINCT go_id_parent FROM go_parent)) " + 
				"AS num_entries";		
		int count = 0;
			
		try (Connection conn = ConnectDB.connect();
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(SQL)) {
			rs.next();
			count = rs.getInt(1);
			conn.close();
		} catch (SQLException ex) {
			System.out.println(ex.getMessage());
		}
		
		return count;		
	}
		
	//function to retrieve all GO IDs from GO_Parent table
	public static String[] getAllGoIds() {
		
		String[] array = new String[getCntAllGoIds()];
		int i = 0;
		
		String SQL = "(SELECT DISTINCT go_id FROM go_parent) " + 
				"UNION (SELECT DISTINCT go_id_parent FROM go_parent)";
		
		try {
			Connection conn = ConnectDB.connect();
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(SQL);
			
			while (rs.next()) {
				array[i] = rs.getString(1);
				i++;
			}
			conn.close();
		} catch (SQLException ex) {
			System.out.println(ex.getMessage());
		}
		
		Arrays.sort(array);
		return array;
	}
	
	//get number of Pfam ACs in Protein2Pfam table
	public static int getPfamAcCnt() {
		
		String SQL = "SELECT COUNT (DISTINCT pfam_ac) FROM protein2pfam";		
		int count = 0;
			
		try (Connection conn = ConnectDB.connect();
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(SQL)) {
			rs.next();
			count = rs.getInt(1);
			conn.close();
		} catch (SQLException ex) {
			System.out.println(ex.getMessage());
		}
		
		return count;
	}
	
	//function to retrieve list of Pfam ACs from Protein2Pfam table
	public static String[] getPfamACs() {
		
		String[] array = new String[getPfamAcCnt()];
		int i = 0;
		
		String SQL = "SELECT DISTINCT pfam_ac FROM protein2pfam";
		
		try {
			Connection conn = ConnectDB.connect();
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(SQL);
			 
			while (rs.next()) {
			    array[i] = rs.getString(1);
			    i++;
			}
			conn.close();
		} catch (SQLException ex) {
			System.out.println(ex.getMessage());
		}
		
		Arrays.sort(array);
		return array;
	}
	
	//get count distinct PDB ID from table PDB_Chain
	public static int getNumberPdb() {
		
		String SQL = "SELECT COUNT (DISTINCT pdb_id) FROM pdb_chain";
		int count = 0;
			
		try (Connection conn = ConnectDB.connect();
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(SQL)) {
			rs.next();
			count = rs.getInt(1);
			conn.close();
		} catch (SQLException ex) {
			System.out.println(ex.getMessage());
		}
		
		return count;	
	}
	
	//function to retrieve list of PDB IDs from table PDB_Chain
	public static String[] getPDBIDs() {
		
		String[] array = new String[getNumberPdb()];
		int i = 0;
		
		String SQL = "SELECT DISTINCT pdb_id FROM pdb_chain";
		
		try {
			Connection conn = ConnectDB.connect();
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(SQL);
			 
			while (rs.next()) {
			    array[i] = rs.getString(1);
			    i++;
			}
			conn.close();
		} catch (SQLException ex) {
			System.out.println(ex.getMessage());
		}
		
		Arrays.sort(array);
		return array;
	}
	
	//function to retrieve list of distinct PDB IDs with chain label from table Protein2Pdb
	public static Map<String, HashSet<String>> getPdbIDsWithChain() {
		
		Map<String, HashSet<String>> map = new HashMap<>();
		
		String SQL = "SELECT DISTINCT pdb_id, chain FROM protein2pdb";
		
		try {
			Connection conn = ConnectDB.connect();
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(SQL);
			 
			while (rs.next()) {
				String pdbid = rs.getString(1);
				String chain = rs.getString(2);
				
				if (map.containsKey(pdbid)) {
					HashSet<String> set = map.get(pdbid);
					set.add(chain);
					map.put(pdbid, set);
				}
				else {
					HashSet<String> set = new HashSet<>();
					set.add(chain);
					map.put(pdbid, set);
				}				
			}
			conn.close();
		} catch (SQLException ex) {
			System.out.println(ex.getMessage());
		}
		
		return map;		
	}

}
