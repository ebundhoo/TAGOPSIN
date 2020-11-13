package genbank;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import util.ConnectDB;
import util.DBqueries;
import util.Utility;


/**
 * This class parses a file in the GenBank flat file format, extracts data of interest 
 * and inserts them into PostgreSQL.
 * 
 * @author 	Eshan Bundhoo and Anisah W. Ghoorah, University of Mauritius
 * @since	2019-02-19
 * @version	1.0
 *
 */

public class GenBank1_0 {

	public static void parseFiles(String dirPath) throws IOException {
		
		//Store all files of the directory in a list
		List<String> fileList = Utility.getFilesInDirectory(dirPath);
		
		int count = 1;
        
		//Read the files one by one
		for (String filename : fileList) {
			
			if ((Paths.get(filename).getFileName().toString()).startsWith("."))
				continue;
			
			System.out.printf("Processing %d of %d\n", count, fileList.size());
		
			Scanner scanner = new Scanner(new FileInputStream (filename));
			
			//Get the genome AC of the file
			Genome g = new Genome();
			String fname = Paths.get(filename).getFileName().toString().split("\\.")[0];
			g.setGenAc(fname);
			
			boolean flagOrganism = false;
			boolean flagOrgContd = false;
			boolean flagStrain = false;
			boolean flagCDS = false;
			boolean flagProduct = false;
			boolean flagTranslation = false;
			boolean flagOrigin = false;
			
			String orgName="";
			
			//Start reading file line by line
			while (scanner.hasNextLine()) {
				
				String line = scanner.nextLine();
				
				if (!flagOrganism && line.startsWith("                     /organism=") && line.endsWith("\"")) {
					String species = extract_keyword(line);
					orgName = orgName + species;
					g.setOrganism(orgName);
					flagOrgContd = false;
				}
				
				else if (!flagOrganism && line.startsWith("                     /organism=")) {
					String species = extract_keyword(line);
					orgName = orgName + species;
					g.setOrganism(orgName);
					flagOrgContd = true;
				}
				
				else if (!flagOrganism && flagOrgContd && line.endsWith("\"")) {
					line = line.trim();
					line = line.replaceAll("\"", "");
					g.setOrganism( g.getOrganism() + " " + line );
					flagOrgContd = false;
				}
				
				else if (!flagOrganism && (line.startsWith("                     /strain="))) {
					flagStrain = true;
					String strain = extract_keyword(line);
					if (strain.contains(g.getOrganism()))
						g.setOrganism(strain);
					else if (parseOrganism( g.getOrganism(), strain ))
						g.setOrganism( g.getOrganism() );
					else
						g.setOrganism( g.getOrganism() + " " + strain );
				}
				
				else if (!flagOrganism && (line.startsWith("                     /sub_strain="))) {
					String substrain = extract_keyword(line);
					if (substrain.contains(g.getOrganism()))
						g.setOrganism(substrain);
					else if (parseOrganism( g.getOrganism(), substrain ))
						g.setOrganism( g.getOrganism() );
					else
						g.setOrganism( g.getOrganism() + " substr. " + substrain );
				}
				
				else if (!flagOrganism && !flagStrain && line.startsWith("                     /isolate=")) {
					String isolate = extract_keyword(line);
					if (isolate.contains(g.getOrganism()))
						g.setOrganism(isolate);
					else if (parseOrganism( g.getOrganism(), isolate ))
						g.setOrganism( g.getOrganism() );
					else
						g.setOrganism( g.getOrganism() + " " + isolate );
				}
				
				else if (!flagOrganism && line.startsWith("                     /db_xref")) {
					flagOrganism = true;
				}
				
				//Feature "CDS"
				else if (line.startsWith("     CDS             ")) {
					flagCDS = true;
					boolean flagCDScontd = parseStartEnd(line, g);
					if ( !flagCDScontd ) {						
						line = scanner.nextLine();
						line = line.replaceAll("\\s{2,}", "");
						line = line.replaceAll("\\)\\)", "");
						String[] tokens = line.split(",");
						for (String startend : tokens) { 
							if (startend.contains("..")) {
								int start = Integer.parseInt((startend.split("\\.."))[0]);
								int stop = Integer.parseInt((startend.split("\\.."))[1]);
								NtSeq nts = new NtSeq(start, stop);
								g.getStackCDS().peek().addNtSeq(nts);
							}
						}
					}
				}
				
				else if (flagCDS && line.startsWith("                     /locus_tag=")) {
					String locustag = extract_keyword(line);
					g.getStackCDS().peek().setLocusTag(locustag);
				}
				
				else if (flagCDS && line.startsWith("                     /product=") && line.endsWith("\"")) {
					String product = extract_keyword(line);
					g.getStackCDS().peek().setProduct(product);
					flagProduct = false;
				}
				
				else if (flagCDS && line.startsWith("                     /product=")) {
					String product = extract_keyword(line);
					g.getStackCDS().peek().setProduct(product);
					flagProduct = true;
				}
				
				else if (flagCDS && flagProduct && line.endsWith("\"")) {
					line = line.trim();
					line = line.replaceAll("\"", "");
					g.getStackCDS().peek().setProduct( g.getStackCDS().peek().getProduct() + " " + line );
					flagProduct = false;
				}
				
				else if (flagCDS && line.startsWith("                     /protein_id=")) {
					String prot_id = extract_keyword(line);
					g.getStackCDS().peek().setProteinID(prot_id);
				}
				
				else if (flagCDS && line.startsWith("                     /db_xref=\"GOA:")) {
					String uniprotac = extract_uniprotac(line);
					g.getStackCDS().peek().setUniProtAC(uniprotac);
				}
				
				else if (flagCDS && line.startsWith("                     /translation=") && line.endsWith("\"")) {
					String prot_seq = extract_keyword(line);
					g.getStackCDS().peek().setProtSeq(prot_seq);
					flagTranslation = false;
				}
				
				else if (flagCDS && line.startsWith("                     /translation=")) {
					String prot_seq = extract_keyword(line);
					g.getStackCDS().peek().setProtSeq(prot_seq);
					flagTranslation = true;
				}
				
				else if (flagCDS && flagTranslation && line.endsWith("\"")) {
					line = line.trim();
					line = line.replaceAll("\"", "");
					g.getStackCDS().peek().setProtSeq( g.getStackCDS().peek().getProtSeq() + line );
					flagTranslation = false;
				}
				
				else if (flagCDS && flagTranslation) {
					line = line.trim();
					g.getStackCDS().peek().setProtSeq( g.getStackCDS().peek().getProtSeq() + line );
				}
			
				else if (line.startsWith("     gene            ")) {
					flagOrganism = true;
					flagCDS = false;
				}
				
				//Feature "ORIGIN"
				else if (line.startsWith("ORIGIN")) {
					flagCDS = false;
					flagTranslation = false;
					flagOrigin = true;
				}
				
				if (flagOrigin) {
					line = line.replaceAll("\\s{2,}", " ");
					String[] tokens = line.split(" ");
					String seq = "";
					for (int j=2; j<tokens.length; j++)
						seq = seq + tokens[j];
					
					g.setGenomeSeq( g.getGenomeSeq() + seq );
				}
				
			} 
			scanner.close();
			
			//Extract nucleotide sequence
			if (g.getGenomeSeq() != null) {
				Iterator<CDS> it = g.getStackCDS().iterator();			
				
				while (it.hasNext()) {
					CDS cds = it.next();
					Iterator<NtSeq> it2 = cds.getListNtSeq().iterator();
					
					while (it2.hasNext()) {
						NtSeq nts = it2.next();
						nts.setSeq( g.getGenomeSeq().substring(nts.getStart()-1, nts.getStop()) ); 		
					}
				}
			}
			add_info_db(g);
			count++;	
		} 
		
	} 
	
	public static boolean parseOrganism(String org, String strain) {
		
		String[] tokens = strain.split(" ");
		
		for (String strainToken : tokens) {
			if (org.contains(strainToken))
				return true;
		}
		return false;
	}
	
	public static String extract_keyword(String line) {
		
		String[] tokens = line.split("=\"");
		return tokens[1].replaceAll("\"", "");
	}
	
	public static String extract_uniprotac(String line) {
		
		String[] tokens = line.split("GOA:");
		return tokens[1].replaceAll("\"", "");
	}
	
	public static boolean parseStartEnd(String line, Genome g) {
		
		line = line.replaceAll("\\s{2,}", " ").trim();
		line = line.replaceAll("CDS ", "");
		line = line.replaceAll(">", "");
		line = line.replaceAll("<", "");
		
		//Type
		if (line.contains("complement(join(") && line.endsWith("))")) { 
			line = line.replaceAll("complement\\(join\\(", "");
			line = line.replaceAll("\\)\\)", ""); 
			String[] tokens = line.split(",");
			CDS cds = new CDS();
			cds.setType("complement_join");
			for (String startend : tokens) {
				if (startend.contains("..")) {
					int start = Integer.parseInt((startend.split("\\.."))[0]);
					int stop = Integer.parseInt((startend.split("\\.."))[1]);
					NtSeq nts = new NtSeq(start, stop);
					cds.addNtSeq(nts);
				}
			}
			g.addCDS(cds);
			return true;
		}
		
		else if (line.contains("complement(join(") && line.endsWith(",")) { 
			line = line.replaceAll("complement\\(join\\(", "");
			line = line.replaceAll("\\)\\)", "");
			String[] tokens = line.split(",");
			CDS cds = new CDS();
			cds.setType("complement_join");
			for (String startend : tokens) { 
				if (startend.contains("..")) {
					int start = Integer.parseInt((startend.split("\\.."))[0]);
					int stop = Integer.parseInt((startend.split("\\.."))[1]);
					NtSeq nts = new NtSeq(start, stop);
					cds.addNtSeq(nts);
				}
			}
			g.addCDS(cds);
			return false;
		}
		
		else if (line.contains("complement")) {
			line = line.replaceAll("complement\\(", "");
			line = line.replaceAll("\\)", "");    					
			int start = Integer.parseInt((line.split("\\.."))[0]);
			int stop = Integer.parseInt((line.split("\\.."))[1]);
			CDS cds = new CDS();
			cds.setType("complement");
			NtSeq nts = new NtSeq(start, stop);
			cds.addNtSeq(nts);
			g.addCDS(cds);
			return true;
		}
		
		else if (line.contains("join")) { 
			line = line.replaceAll("join\\(", "");
			line = line.replaceAll("\\)", "");    			
			String[] tokens = line.split(",");
			CDS cds = new CDS();
			cds.setType("join");
			for (String startend : tokens) {  				
				if (startend.contains("..")) {
					int start = Integer.parseInt((startend.split("\\.."))[0]);
					int stop = Integer.parseInt((startend.split("\\.."))[1]);
					NtSeq nts = new NtSeq(start, stop);
					cds.addNtSeq(nts);
				}
			}
			g.addCDS(cds);
			return true;
		}
		
		else {
			int start = Integer.parseInt((line.split("\\.."))[0]);
			int stop = Integer.parseInt((line.split("\\.."))[1]);
			CDS cds = new CDS();
			cds.setType("default");
			NtSeq nts = new NtSeq(start, stop);
			cds.addNtSeq(nts);
			g.addCDS(cds);
			return true;
		}
		
	}
	
	public static void add_info_db(Genome g) {
		
		addOrganismInfo(g);
		
		addGenomeInfo(g);
		
		addCDS_NtSeqInfo(g);
			
	}	
	
	//Insert into database
	public static void addOrganismInfo(Genome g) {
		
		String[] array = DBqueries.getOrgName();
		
		String SQLs = "INSERT INTO organism(species) VALUES (?)";
		
		try (Connection conn = ConnectDB.connect();
				PreparedStatement pstmt = conn.prepareStatement(SQLs)) {

			if (!Utility.isIDofInterest(array, g.getOrganism())) {
				
				pstmt.setString(1, g.getOrganism());
				pstmt.executeUpdate();
				
				conn.close();
			}
		} catch (SQLException ex) {
			System.out.println(ex.getMessage());
		}		
		
	}
	
	public static void addGenomeInfo(Genome g) {
		
		String SQL = "SELECT oid FROM organism WHERE species = ?"; 
		
		String SQLg = "INSERT INTO genome(ac, nt_sequence, oid) VALUES(?,?,?)";
		
		try (Connection conn = ConnectDB.connect();
				PreparedStatement pstmt1 = conn.prepareStatement(SQL);
				PreparedStatement pstmt2 = conn.prepareStatement(SQLg)) {
			
			pstmt1.setString(1, g.getOrganism());
			
			ResultSet rs = pstmt1.executeQuery();
			rs.next();
			int oid = rs.getInt(1);
			
			pstmt2.setString(1, g.getGenAc());
			pstmt2.setString(2, g.getGenomeSeq());
			pstmt2.setInt(3, oid);
			
			pstmt2.executeUpdate();
			
			conn.close();
			
		} catch (SQLException ex) {
			System.out.println(ex.getMessage());
		}	
			
	}
	
	public static void addCDS_NtSeqInfo(Genome g) {
		
		String SQLc = "INSERT INTO cds(locus_tag, type, product, protein_id, uniprot_ac, prot_aa_seq, genome_ac) " + 
				"VALUES(?,?,?,?,?,?,?)";
		
		String SQLn = "INSERT INTO cds_ntseq(start, stop, seq, cdsid) " + 
				"VALUES(?,?,?,?)";
			
		//write to table "cds"
		
        Iterator<CDS> it = g.getStackCDS().iterator();
		
		while (it.hasNext()) {
			CDS cds = it.next();
			
			try (Connection conn = ConnectDB.connect();
					PreparedStatement pstmt3 = conn.prepareStatement(SQLc)) {
			
			pstmt3.setString(1, cds.getLocusTag());
			pstmt3.setString(2, cds.getType());
			pstmt3.setString(3, cds.getProduct());
			pstmt3.setString(4, cds.getProteinID());
			pstmt3.setString(5, cds.getUniProtAC());
			pstmt3.setString(6, cds.getProtSeq());
			pstmt3.setString(7, g.getGenAc());
			
			pstmt3.executeUpdate();
			
			conn.close();
					
		} catch (SQLException ex) {
			System.out.println(ex.getMessage());
		}
			
			//write to table "cds_ntseq"
			
			Iterator<NtSeq> it2 = cds.getListNtSeq().iterator();
			
			while (it2.hasNext()) {
				NtSeq nts = it2.next();
				
				try (Connection conn = ConnectDB.connect();
						PreparedStatement pstmt4 = conn.prepareStatement(SQLn)) {
					
					String SQLm = "SELECT MAX(cdsid) FROM cds";
					
					Statement stmt = conn.createStatement();
					ResultSet rs = stmt.executeQuery(SQLm);
					rs.next();
					int cdsid = rs.getInt(1);
			
					pstmt4.setInt(1, nts.getStart());
					pstmt4.setInt(2, nts.getStop());
					pstmt4.setString(3, nts.getSeq());
					pstmt4.setInt(4, cdsid);
					
					pstmt4.executeUpdate();
					
					conn.close();
					
				} catch (SQLException ex) {
					System.out.println(ex.getMessage());
				}
			}
		}	
	
	}
	
}