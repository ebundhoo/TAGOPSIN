package mapping;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import util.ConnectDB;
import util.DBqueries;
import util.Utility;

/**
 * This class parses a DAT file to map RefSeq protein IDs onto UniProt ACs for a 
 * given RefSeq genome AC (prokaryotes, viruses) or a given locus name (eukaryotes), 
 * and updates PostgreSQL accordingly. 
 * 
 * @author 	Eshan Bundhoo and Anisah W. Ghoorah, University of Mauritius
 * @since	2020-06-18
 * @version	1.1
 *
 */

public class GenBank2UniProt {

	public static void parseFile(String filename, String orgType, String orgName) throws FileNotFoundException {
		
		create_index_cds();
		
		Scanner scanner = new Scanner(new FileInputStream(filename));
		
		Map<String, HashSet<String>> map = DBqueries.getCdsProt(orgType);
		
		String[] array = DBqueries.getCdsProtHuman();
		
		GBMapping gbm = null;
		
		//Read the default file "idmapping.dat" or the by_organism idmapping file found on UniProt FTP server
		while (scanner.hasNextLine()) {
			
			String line = scanner.nextLine();
			
			String[] tokens = line.split("\t");
			String dbxref = tokens[1];
			
			if (dbxref.equals("Gene_OrderedLocusName") && orgType.equalsIgnoreCase("E") 
					&& !orgName.equalsIgnoreCase("Homo sapiens")) {
				
				String locus_name = tokens[2];
				
				if (map.containsKey(locus_name.toUpperCase())) {
					gbm = new GBMapping();
					gbm.setLocusName(locus_name.toUpperCase());
				}
					
			}
			else if (dbxref.equals("RefSeq")) {
				
				String uniprot = tokens[0];
				String refseq = tokens[2];
				
				if (orgType.equalsIgnoreCase("E")) {
					
					if (orgName.equalsIgnoreCase("Homo sapiens")) {
						
						if (Utility.isIDofInterest(array, refseq)) {
							gbm = new GBMapping();
							gbm.setRefseq(refseq);
							gbm.setUniprotAc(uniprot);
							db_update_human(gbm.getUniprotAc(), gbm.getRefseq());
						}
					}
					else {
						if (gbm!=null && map.containsKey(gbm.getLocusName())) { 
							
							Set<String> set = map.get(gbm.getLocusName());
							
							if (set.contains(refseq)) {
								gbm.setRefseq(refseq);	
								gbm.setUniprotAc(uniprot);
								db_update_euk(gbm.getUniprotAc(), gbm.getRefseq(), gbm.getLocusName());
							}
						}
					}
				}
				else {							//for orgType "P" and "V"
					if (map.containsKey(refseq)) {
						gbm = new GBMapping();
						gbm.setRefseq(refseq);
						gbm.setUniprotAc(uniprot);
					}
				}
			}
			else if (dbxref.equals("RefSeq_NT") && !orgType.equalsIgnoreCase("E")) {
				
				String genome_ac = tokens[2].split("\\.")[0];
				
				if (gbm!=null && map.containsKey(gbm.getRefseq())) {
				
					Set<String> set = map.get(gbm.getRefseq());
					
					if (set.contains(genome_ac)) {
						gbm.setGenomeAc(genome_ac);
						db_update_prok(gbm.getUniprotAc(), gbm.getRefseq(), gbm.getGenomeAc());
					}
				}
			}
			else if (dbxref.equals("UniProtKB-ID"))
				gbm = null;
		}
		scanner.close();
	}
	
	//Create index on "cds" table
	public static void create_index_cds() {
		
		String SQL = "CREATE INDEX prot_id_idx ON cds (protein_id)";
		
		try {
			Connection conn = ConnectDB.connect();
			Statement stmt = conn.createStatement();
			stmt.executeUpdate(SQL);
			conn.close();
		} catch (SQLException ex) {
			System.out.println(ex.getMessage());
		}
		
		SQL = "CREATE INDEX gen_ac_idx ON cds (genome_ac)";
		
		try {
			Connection conn = ConnectDB.connect();
			Statement stmt = conn.createStatement();
			stmt.executeUpdate(SQL);
			conn.close();
		} catch (SQLException ex) {
			System.out.println(ex.getMessage());
		}
		
	}
	
	//Update PostgreSQL for prokaryotes and viruses
	public static void db_update_prok(String uniprot_ac, String refseq, String genome_ac) {
		
		String SQLp = "UPDATE cds SET uniprot_ac = ? "
				+ "WHERE protein_id = ? "
				+ "AND genome_ac = ? "
				+ "AND uniprot_ac IS NULL";
		
		try (Connection conn = ConnectDB.connect();
				PreparedStatement pstmt = conn.prepareStatement(SQLp)) {
			pstmt.setString(1, uniprot_ac);
			pstmt.setString(2, refseq);
			pstmt.setString(3, genome_ac);
			
			pstmt.executeUpdate();
			
			conn.close();	
			
		} catch (SQLException ex) {
			System.out.println(ex.getMessage());
		}
	}
	
	//Update PostgreSQL for eukaryotes
	public static void db_update_euk(String uniprot_ac, String refseq, String locus_tag) {
	
		String SQLe = "UPDATE cds SET uniprot_ac = ? "
				+ "WHERE protein_id = ? "
				+ "AND locus_tag = ? "
				+ "AND uniprot_ac IS NULL";
			
		try (Connection conn = ConnectDB.connect();
				PreparedStatement pstmt = conn.prepareStatement(SQLe)) {
			pstmt.setString(1, uniprot_ac);
			pstmt.setString(2, refseq);
			pstmt.setString(3, locus_tag);
			
			pstmt.executeUpdate();
			
			conn.close();	
			
		} catch (SQLException ex) {
			System.out.println(ex.getMessage());
		}
	}
	
	//Update PostgreSQL for human (eukaryote)
	public static void db_update_human(String uniprot_ac, String refseq) {
		
		String SQL = "UPDATE cds SET uniprot_ac = ? "
				+ "WHERE protein_id = ? "
				+ "AND uniprot_ac IS NULL";
			
		try (Connection conn = ConnectDB.connect();
				PreparedStatement pstmt = conn.prepareStatement(SQL)) {
			pstmt.setString(1, uniprot_ac);
			pstmt.setString(2, refseq);
			
			pstmt.executeUpdate();
			
			conn.close();	
			
		} catch (SQLException ex) {
			System.out.println(ex.getMessage());
		}
	}

}
