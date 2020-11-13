package pfam;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Scanner;

import util.ConnectDB;
import util.DBqueries;
import util.Utility;

/**
 * This class parses a file in the Stockholm format, retrieves IDs, ACs, descriptions 
 * and UniProt domains of interest, and inserts them into PostgreSQL.
 * 
 * @author 	Eshan Bundhoo and Anisah W. Ghoorah, University of Mauritius
 * @since	2019-02-19
 * @version	1.0
 *
 */

public class Pfam {

	public static void parseFile(String filename) throws IOException {
		
		PfamEntry pf = new PfamEntry();
		
		String[] array = DBqueries.getUniProtACs();
		
		Scanner scanner = new Scanner(new FileInputStream(filename));
		
		//Start reading file Pfam-A.full.uniprot found on Pfam FTP server
		while (scanner.hasNextLine()) {
			
			String line = scanner.nextLine();
			
			if (line.startsWith("#=GF ID")) {
				pf.setId(extract_id(line));
			}
			
			else if (line.startsWith("#=GF AC")) {
				pf.setAc(extract_ac(line));
			}
			
			else if (line.startsWith("#=GF DE")) {
				pf.setDescription(extract_desc(line));
			}
			
			else if ( ! line.startsWith("#") && ! line.startsWith("//")) {
				pf.addUniProtDomain(extract_uniprot(line));
			}
			
			else if (line.startsWith("//")) {
				
				//Insert into database
				for (UniProtDomain u : pf.getListUniProt()) {
					
					if (Utility.isIDofInterest(array, u.getUniProtAc()))		
						
						insert_prot2pfam(u.getUniProtAc(), u.getStart(), u.getStop(), pf.getAc());
				}
				
				String[] arrayPfAcs = DBqueries.getPfamACs();
				
				if (Utility.isIDofInterest(arrayPfAcs, pf.getAc()))
					
					insert_pfam(pf.getAc(), pf.getId(), pf.getDescription());
				
				pf = new PfamEntry();
			}
			
		}
		scanner.close();
		
	}
	
	public static String extract_id(String line) {
		
		return line.split("ID")[1].trim();	
	}
	
	public static String extract_ac(String line) {
		
		return (line.split("AC")[1].trim()).split("\\.")[0];	   
	}
	
	public static String extract_desc(String line) {
		
		return line.split("DE")[1].trim();	
	}
	
	public static UniProtDomain extract_uniprot(String line) {
		
		String nline = line.replace("\\s{2,}", " ");
		String[] tokens = nline.split(" ");
		String[] tokens2 = tokens[0].split("/");       		
		String uniprotac = tokens2[0].split("\\.")[0];    	  
		
        int start = Integer.parseInt(tokens2[1].split("-")[0]);   	
        int stop = Integer.parseInt(tokens2[1].split("-")[1]);
		
		return new UniProtDomain(uniprotac, start, stop);	
	}
	
	public static void insert_prot2pfam(String uniprot_ac, int start, int stop, String pfam_ac) {
		
		String SQL = "INSERT INTO protein2pfam VALUES(?,?,?,?)";
		
		try (Connection conn = ConnectDB.connect();
				PreparedStatement pstmt = conn.prepareStatement(SQL)) {
			pstmt.setString(1, uniprot_ac);
			pstmt.setInt(2, start);
			pstmt.setInt(3, stop);
			pstmt.setString(4, pfam_ac);
			
			pstmt.executeUpdate();
			conn.close();

		} catch (SQLException ex) {
			System.out.println(ex.getMessage());
		}
	}
	
	public static void insert_pfam(String pfam_ac, String pfam_id, String descr) {
		
		String SQL = "INSERT INTO pfam VALUES(?,?,?)";
		
		try (Connection conn = ConnectDB.connect();
				PreparedStatement pstmt = conn.prepareStatement(SQL)) {
			pstmt.setString(1, pfam_ac);
			pstmt.setString(2, pfam_id);
			pstmt.setString(3, descr);
			
			pstmt.executeUpdate();
			conn.close();

		} catch (SQLException ex) {
			System.out.println(ex.getMessage());
		}
	}

}
