package genbank;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Formatter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import util.ConnectDB;
import util.DBqueries;
import util.Scripts;
import util.Utility;


/**
 * This class parses a file in the GenBank flat file format, extracts information relating to 
 * the organism (name, strain, sub-strain) and the "CDS" feature (name, RefSeq ID, AA sequence, 
 * NT sequence etc.), and inserts it into PostgreSQL.  
 * This version adds functionality for eukaryotes and uses shell scripting to extract coding 
 * sequences from the genome.
 * 
 * @author 	Eshan Bundhoo and Anisah W. Ghoorah, University of Mauritius
 * @since	2019-12-17
 * @version	1.1
 *
 */

public class GenBank {

	public static void parseFiles(String dirPath1, String orgType, String dirPath2) throws IOException, InterruptedException {
		
		//Write all scripts used by this class to external files
		Formatter outfile = new Formatter("scripts/getseq1.bash");
		outfile.format("%s", Scripts.getSeq1());
		outfile.close();
		
		outfile = new Formatter("scripts/getseq2.bash");
		outfile.format("%s", Scripts.getSeq2());
		outfile.close();
		
		outfile = new Formatter("scripts/subseq.bash");
		outfile.format("%s", Scripts.subseq());
		outfile.close();
		
		outfile = new Formatter("scripts/getseqlen.bash");
		outfile.format("%s", Scripts.getSeqLen());
		outfile.close();
		
		outfile = new Formatter("scripts/getwholeseq.bash");
		outfile.format("%s", Scripts.getWholeSeq());  
		outfile.close();
		
		//Store all files of the directory in a linked list
		List<String> fileList = Utility.getFilesInDirectory(dirPath1);
		
		int count = 1;
        
		//Read the files of the list one by one
		for (String filename : fileList) {
			
			//long t1 = System.nanoTime();
			
			if ((Paths.get(filename).getFileName().toString()).startsWith("."))
				continue;
			
			System.out.printf("Processing %d of %d\n", count, fileList.size());
		
			Scanner scanner = new Scanner(new FileInputStream (filename));
			
			//Get genome AC of file
			Genome g = new Genome();
			String fname = Paths.get(filename).getFileName().toString().split("\\.")[0];
			g.setGenAc(fname);
			
			boolean flagOrganism = false;
			boolean flagOrgContd = false;
			boolean flagStrain = false;
			boolean flagCDS = false;
			boolean flagProduct = false;
			boolean flagTranslation = false;
			//boolean flagOrigin = false;
			
			String orgName="";
			int lineNumber=0;
			
			//Start reading file line by line until keyword ORIGIN is encountered
			while (scanner.hasNextLine()) {
				
				lineNumber = lineNumber + 1;
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
					lineNumber = parseStartEnd(line, g, scanner, lineNumber);
				}
				
				else if (flagCDS && line.startsWith("                     /gene=")) {
					String gene = extract_keyword(line);
					g.getStackCDS().peek().setGene(gene);
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
			
				else if (line.startsWith("     gene            ") || 
						line.startsWith("     mRNA            ") ||
						line.startsWith("     misc_RNA        ") ||
						line.startsWith("     ncRNA           ")) {
					flagOrganism = true;
					flagCDS = false;
				}

				//Feature "ORIGIN"
				else if (line.startsWith("ORIGIN")) {
					flagCDS = false;
					flagTranslation = false;
					//flagOrigin = true;
					//System.out.println("Line number where ORIGIN occurs: "+lineNumber);
					break;
				}
			}
			scanner.close();
			
			if (orgType.equalsIgnoreCase("P") || orgType.equalsIgnoreCase("V"))
			    g.setGenomeSeq( getWholeSequence(filename, lineNumber) );
			else
			    g.setGenomeSeq( null );   		//for "E"
			
			
			String directory = dirPath2;
			List<String> seqBin = prepareSequenceBins(filename, lineNumber, directory);
			
			//Set nucleotide sequence
			Iterator<CDS> it = g.getStackCDS().iterator();	
			while (it.hasNext()) {

				CDS cds = it.next();
				Iterator<NtSeq> it2 = cds.getListNtSeq().iterator();
				
				while (it2.hasNext()) {
					NtSeq nts = it2.next();
					long start = nts.getStart();
					long stop = nts.getStop();
					String seq = extractCDS(seqBin, start, stop);
					nts.setSeq( seq );
				}
			}
			add_info_db(g);
			count++;	
			
			//long t2 = System.nanoTime();
			//System.out.printf("#min to process %s %.2f\n", fname, (t2-t1)*1.6667e-11);
		} 
	
	} 
	
	public static String extractCDS(List<String> seqBin, long start, long stop) 
			throws IOException, InterruptedException {
		
		//Given start and stop, obtain corresponding seqbin file(s) 
		String seqBinFile = "";
		
		for (String sbf : seqBin) {
			String range = Paths.get(sbf).getFileName().toString(); 
			long rangeStart = Long.parseLong( (range.split("-"))[0] );
			long rangeStop  = Long.parseLong( (range.split("-"))[1] );
			
			if (start>=rangeStart && stop<=rangeStop) {
				seqBinFile = sbf;
				break;
			}
		}
		
		if (seqBinFile.equals("")) {				//meaning two seqbin files are required
			
			String seqBinFile1="";
			String seqBinFile2="";
			
			for (String sbf : seqBin) {
				String range = Paths.get(sbf).getFileName().toString(); 
				long rangeStart = Long.parseLong( (range.split("-"))[0] );
				long rangeStop  = Long.parseLong( (range.split("-"))[1] );
				
				if (start>=rangeStart && start<=rangeStop) {
					seqBinFile1 = sbf;
					break;
				}
			}
			
			for (String sbf : seqBin) {
				String range = Paths.get(sbf).getFileName().toString(); 
				long rangeStart = Long.parseLong( (range.split("-"))[0] );
				long rangeStop  = Long.parseLong( (range.split("-"))[1] );
				
				if (stop>=rangeStart && stop<=rangeStop) {
					seqBinFile2 = sbf;
					break;
				}
			}
			
			String range1 = Paths.get(seqBinFile1).getFileName().toString(); 
			//String range2 = Paths.get(seqBinFile2).getFileName().toString(); 
			long rangeStart = Long.parseLong( (range1.split("-"))[0] );
			if (rangeStart == 1) {
				//System.out.printf("%d,%d file %s %s => %d,%d\n", start, stop, range1, range2, start, stop);
				return getSeqTwoFiles(seqBinFile1, seqBinFile2, start, stop);
			}
			else {
				//System.out.printf("%d,%d file %s %s => %d,%d\n", start, stop, range1, range2, start-(rangeStart-1), stop-(rangeStart-1));
				return getSeqTwoFiles(seqBinFile1, seqBinFile2, start-(rangeStart-1), stop-(rangeStart-1));
			}
		}
		
		else {
			
			String range = Paths.get(seqBinFile).getFileName().toString(); 
			long rangeStart = Long.parseLong( (range.split("-"))[0] );
			
			if (rangeStart == 1) {
				//System.out.printf("%d,%d file %s => %d,%d\n", start, stop, range, start, stop);
				return getSeqOneFile(seqBinFile, start, stop);
			}
			else {
				//System.out.printf("%d,%d file %s => %d,%d\n", start, stop, range, start-(rangeStart-1), stop-(rangeStart-1));
				return getSeqOneFile(seqBinFile, start-(rangeStart-1), stop-(rangeStart-1));
			}
		}
	}
	
	public static String getSeqOneFile(String seqBinFile, long start, long stop) 
			throws IOException, InterruptedException {
		
		//Execute shell script to extract sequence available in a single file
		String script = "scripts/getseq1.bash";
		
		String[] cmdArray = new String[5];
		cmdArray[0] = "bash";
		cmdArray[1] = script;
		cmdArray[2] = seqBinFile;
		cmdArray[3] = ""+start;
		cmdArray[4] = ""+stop;
		
		ProcessBuilder pb = new ProcessBuilder(cmdArray);
		Process proc = pb.start();
		BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		String seq = reader.readLine();
		proc.waitFor();
		
		return seq;
	}
	
	public static String getSeqTwoFiles(String seqBinFile1, String seqBinFile2, long start, long stop) 
			throws IOException, InterruptedException {
		
		//Execute shell script to extract sequence encompassing two files
		String script = "scripts/getseq2.bash";
		
		String[] cmdArray = new String[6];
		cmdArray[0] = "bash";
		cmdArray[1] = script;
		cmdArray[2] = seqBinFile1;
		cmdArray[3] = seqBinFile2;
		cmdArray[4] = ""+start;
		cmdArray[5] = ""+stop;
		
		ProcessBuilder pb = new ProcessBuilder(cmdArray);
		Process proc = pb.start();
		BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		String seq = reader.readLine();
		proc.waitFor();
		
		return seq;
	}
	
	public static List<String> prepareSequenceBins(String genbankFile, int lineNumber, String directory) 
			throws IOException, InterruptedException {
		
		long seqlen = getWholeSequenceLength(genbankFile, lineNumber);
		//System.out.println("Sequence length is " + seqlen);
		
		long count=1;
		List<String> list = new LinkedList<>();
		
		while (count < seqlen) {
			long start = count;
			long stop = count+85000-1;
			//System.out.printf("From %d to %d\n", start, stop);
			String range = start+"-"+stop;
			String fileSeqRange = directory+"/"+range;
			writeSequenceBin(genbankFile, lineNumber, start, stop, fileSeqRange);
			list.add(fileSeqRange);
			count=count+85000;
		}
		return list;
	}
	
	public static void writeSequenceBin(String genbankFile, int lineNumber, long start, long stop, String fileSeqRange) 
			throws IOException, InterruptedException {
		
		//Execute shell script to extract subsequence
		String script = "scripts/subseq.bash";
		
		String[] cmdArray = new String[7];
		cmdArray[0] = "bash";
		cmdArray[1] = script;
		cmdArray[2] = genbankFile;
		cmdArray[3] = ""+lineNumber;
		cmdArray[4] = ""+start;
		cmdArray[5] = ""+stop;
		cmdArray[6] = fileSeqRange;
		
		ProcessBuilder pb = new ProcessBuilder(cmdArray);
		Process proc = pb.start();
		proc.waitFor();
	}
	
	public static long getWholeSequenceLength(String filename, int lineNumber) throws IOException, InterruptedException {
		
		//Execute shell script to obtain length of whole sequence
		String script = "scripts/getseqlen.bash";
		
		String[] cmdArray = new String[4];
		cmdArray[0] = "bash";
		cmdArray[1] = script;
		cmdArray[2] = filename;
		cmdArray[3] = ""+lineNumber;
		
		ProcessBuilder pb = new ProcessBuilder(cmdArray);
		Process proc = pb.start();
		BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		String seqlen = reader.readLine();
		proc.waitFor();

		return Long.parseLong(seqlen);
	}
	
	public static String getWholeSequence(String filename, int lineNumber) throws IOException, InterruptedException {

		//Execute shell script to extract whole sequence (genome)
		String script = "scripts/getwholeseq.bash";

		String[] cmdArray = new String[4];
		cmdArray[0] = "bash";
		cmdArray[1] = script;
		cmdArray[2] = filename;
		cmdArray[3] = ""+lineNumber;

		ProcessBuilder pb = new ProcessBuilder(cmdArray);
		Process proc = pb.start();
		BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		String seq = reader.readLine();
		proc.waitFor();
		
		return seq;
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
	
	public static int parseStartEnd(String line, Genome g, Scanner scanner, int lineNumber) {
		
		//Get type: default, join, complement, complement_join
		String type = null;
		CDS cds = new CDS();
		
		if (line.contains("complement(join("))
			type = "complement_join";
		else if (line.contains("complement"))
			type = "complement";
		else if (line.contains("join"))
			type = "join";
		else
			type = "default";
		
		//cleaning
		line = line.replaceAll("\\s{2,}", " ").trim();
		line = line.replaceAll("CDS ", "");
		line = line.replaceAll(">", "");
		line = line.replaceAll("<", "");
		line = line.replaceAll("complement\\(join\\(", "");
		line = line.replaceAll("complement\\(", "");
		line = line.replaceAll("join\\(", "");
		line = line.replaceAll("\\)\\)", "");
		line = line.replaceAll("\\)", "");  
		
		while (line.endsWith(",") ) {

			String[] tokens = line.split(",");
			for (String startend : tokens) { 
				if (startend.contains("..")) {
					int start = Integer.parseInt((startend.split("\\.."))[0]);
					int stop = Integer.parseInt((startend.split("\\.."))[1]);
					NtSeq nts = new NtSeq(start, stop);
					cds.addNtSeq(nts);
				}
			}
			line = scanner.nextLine();
			
			//cleaning
			line = line.replaceAll("\\s{2,}", " ").trim();
			line = line.replaceAll("CDS ", "");
			line = line.replaceAll(">", "");
			line = line.replaceAll("<", "");
			line = line.replaceAll("complement\\(join\\(", "");
			line = line.replaceAll("complement\\(", "");
			line = line.replaceAll("join\\(", "");
			line = line.replaceAll("\\)\\)", "");
			line = line.replaceAll("\\)", "");
			
			lineNumber = lineNumber + 1;
		}
		
		if (line.contains(",")) {
			String[] tokens = line.split(",");
			for (String startend : tokens) { 
				if (startend.contains("..")) {
					int start = Integer.parseInt((startend.split("\\.."))[0]);
					int stop = Integer.parseInt((startend.split("\\.."))[1]);
					NtSeq nts = new NtSeq(start, stop);
					cds.addNtSeq(nts);
				}
			}
	    }
		else if (line.contains("..")){
			int start = Integer.parseInt((line.split("\\.."))[0]);
			int stop = Integer.parseInt((line.split("\\.."))[1]);
			NtSeq nts = new NtSeq(start, stop);
			cds.addNtSeq(nts);
		}
		cds.setType(type);  
		g.addCDS(cds);
		
		return lineNumber;
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
		
		String SQLc = "INSERT INTO cds(gene, locus_tag, type, product, protein_id, uniprot_ac, prot_aa_seq, genome_ac) " + 
				"VALUES(?,?,?,?,?,?,?,?)";
		
		String SQLn = "INSERT INTO cds_ntseq(start, stop, seq, cdsid) " + 
				"VALUES(?,?,?,?)";
			
		//write to table "cds"
		
        Iterator<CDS> it = g.getStackCDS().iterator();
		
		while (it.hasNext()) {
			CDS cds = it.next();
			
			try (Connection conn = ConnectDB.connect();
					PreparedStatement pstmt3 = conn.prepareStatement(SQLc)) {
			
				pstmt3.setString(1, cds.getGene());
				pstmt3.setString(2, cds.getLocusTag());
				pstmt3.setString(3, cds.getType());
				pstmt3.setString(4, cds.getProduct());
				pstmt3.setString(5, cds.getProteinID());
				pstmt3.setString(6, cds.getUniProtAC());
				pstmt3.setString(7, cds.getProtSeq());
				pstmt3.setString(8, g.getGenAc());
				
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