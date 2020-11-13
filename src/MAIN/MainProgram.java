package main;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Paths;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Formatter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import genbank.EukaryotesAC;
import genbank.GenBank;
import genbank.ProkaryotesAC;
import genbank.VirusesAC;
import go.GO;
import mapping.GenBank2UniProt;
import mapping.UniProt2GO;
import mapping.UniProt2PDB;
import pdb.PDB_Chain_Taxa;
import pdb.PDB_Entry;
import pdb.PDB_SeqRes;
import pfam.Pfam;
import taxa.Taxonomy;
import uniprot.Protein;
import uniprot.ReadFastaProtein;
import util.ConnectParam;
import util.CreateDB;
import util.DBqueries;
import util.History;
import util.SQL_DDL;
import util.StdFiles;
import util.Utility;

/**
 * This class is the main program of TAGOPSIN. It accepts user input and contains a number of functions 
 * to download and process data files needed by the program, check dependencies and connect to PostgreSQL.
 * This version adds:
 * 1) functionality for eukaryotes and viruses,
 * 2) use of shell scripting to parse the GenBank flat files,
 * 3) automatic creation of a relational database in PostgreSQL,
 * 4) automatic creation of the table structures in that database,
 * 5) program history check to avoid redundant file downloads.
 * 
 * @author 	Eshan Bundhoo, University of Mauritius
 * @since	2020-01-03
 * @version	1.2
 *
 */

public class MainProgram {
	
	private static ConnectParam cparam;
	private static File file;
	private static Formatter output;
	private static Runtime rt = Runtime.getRuntime();
	private static Scanner input = new Scanner(System.in);
	private static String dir;
	private static String wgetLoc;
	private static URL url;

	public static void main(String[] args) throws IOException, ParseException, SQLException, InterruptedException {
		
		long t1 = System.nanoTime();
		
        printWelcomeMessage();
        check_wget();
        create_database();
        create_db_relations();
        dir = getDataDirFromHistory();
        
        boolean dataFilesExist = History.checkStandardFiles(dir);
        
        if (!dataFilesExist) {
        	System.out.println("\nAt least one of the files required by TAGOPSIN is missing. Checking all files and "
        			+ "directories in \"" + dir + "\"...");	
        	downloadStandardFiles(dir);
        }
		else
			System.out.println("\nAll required data files are present. Proceeding now...");
        
        check_database_conn();
        
        //NCBI TAXONOMY DATASET
        //Retrieve data from names.dmp for a specified organism, and insert into PostgreSQL
		System.out.println("Depending on the objective of your project, organism name can be either Mycobacterium "
				+ "or Mycobacterium bovis for example.");
		System.out.print("Input name of organism of interest: ");
		input.nextLine();
		String organism = input.nextLine();
		System.out.print("Please specify whether this organism is eukaryotic (E), prokaryotic (P) or viral (V): ");
		String type = input.next();
		while (!type.equalsIgnoreCase("E") && !type.equalsIgnoreCase("P") && !type.equalsIgnoreCase("V")) { 
			System.out.print("Invalid key! Please enter E, P or V: ");
		    type = input.next();
	    }
		System.out.println("Retrieving taxonomy IDs and scientific names from names.dmp...");
		Taxonomy.parseFile(dir+"/taxdump/names.dmp", organism);
		System.out.println("Saved to \"organism\" relation");
		
		
		//NCBI GENBANK DATASET
		//Get RefSeq genome ACs from eukaryotes.txt, prokaryotes.txt or viruses.txt, and write EFetch URLs
		file = new File(dir + "/genbank");
		if (file.exists())
			rt.exec("rm -r " + dir + "/genbank");
		rt.exec("mkdir " + dir + "/genbank");
		rt.exec("mkdir " + dir + "/genbank/genomes");
		rt.exec("mkdir " + dir + "/genbank/seqbins");
		rt.exec("mkdir scripts");
		Set<String> setGBACs = null;
		
		if (type.equalsIgnoreCase("E")) {
			System.out.println("Downloading ftp://ftp.ncbi.nlm.nih.gov/genomes/GENOME_REPORTS/eukaryotes.txt...");
			url = new URL("ftp://ftp.ncbi.nlm.nih.gov/genomes/GENOME_REPORTS/eukaryotes.txt");
			FTP.downloadFile(url, dir+"/genbank/eukaryotes.txt");
			System.out.println("Done");
			System.out.println("Retrieving genome ACs from eukaryotes.txt and building EFetch URLs...");
			setGBACs = EukaryotesAC.parseFile(dir, dir+"/genbank/eukaryotes.txt", dir+"/genbank/wget_genomes.sh", wgetLoc);
		}
		else if (type.equalsIgnoreCase("P")) {
			System.out.println("Downloading ftp://ftp.ncbi.nlm.nih.gov/genomes/GENOME_REPORTS/prokaryotes.txt...");
			url = new URL("ftp://ftp.ncbi.nlm.nih.gov/genomes/GENOME_REPORTS/prokaryotes.txt");
			FTP.downloadFile(url, dir+"/genbank/prokaryotes.txt");
			System.out.println("Done");
			System.out.println("Retrieving genome ACs from prokaryotes.txt and building EFetch URLs...");
			setGBACs = ProkaryotesAC.parseFile(dir, dir+"/genbank/prokaryotes.txt", dir+"/genbank/wget_genomes.sh", wgetLoc);
		}
		else if (type.equalsIgnoreCase("V")) {
			System.out.println("Downloading ftp://ftp.ncbi.nlm.nih.gov/genomes/GENOME_REPORTS/viruses.txt...");
			url = new URL("ftp://ftp.ncbi.nlm.nih.gov/genomes/GENOME_REPORTS/viruses.txt");
			FTP.downloadFile(url, dir+"/genbank/viruses.txt");
			System.out.println("Done");
			System.out.println("Retrieving genome ACs from viruses.txt and building EFetch URLs...");
			setGBACs = VirusesAC.parseFile(dir, dir+"/genbank/viruses.txt", dir+"/genbank/wget_genomes.sh", wgetLoc);
		}
		System.out.println("Successful");
		
		//Download genomes in GenBank flat file format from NCBI server using E-utilities
		System.out.println("Downloading genomic data files from NCBI via E-utilities...");
		try {
			Process proc = rt.exec("bash " + dir + "/genbank/wget_genomes.sh");
			proc.waitFor();
		} catch (InterruptedException e) {
			System.out.println("Process interrupted. Terminating now...");
			System.exit(1);
		}
		
		//Check number of genomes downloaded, and restart previous process if necessary 
		List<String> gbFileList = Utility.getFilesInDirectory(dir+"/genbank/genomes"); 
		Set<String> setGBFiles = new HashSet<>();
		Set<String> setGBACs_cp = new HashSet<>();
		
		while (setGBACs.size() != gbFileList.size()) {
			
			System.out.printf("Number of files downloaded is %d of %d\n", gbFileList.size(), setGBACs.size());
			System.out.println("Retrieving missing file(s) from NCBI via E-utilities...");
			
			for (String filePath : gbFileList) {
				String fname = Paths.get(filePath).getFileName().toString().split("\\.")[0];
				setGBFiles.add(fname);
			}
			
			//make a copy of the original set of genome ACs
			setGBACs_cp.addAll(setGBACs);
			
			//remove already downloaded files from the duplicate 
			setGBACs_cp.removeAll(setGBFiles);
	
			//write new script
			String wgetFileNew = dir + "/genbank/wget_genomes_new.sh";		

	        output = new Formatter(wgetFileNew);
			String baseURL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=nuccore&rettype=gbwithparts&retmode=text&id=";
			String baseURLf = dir + "/efetch.fcgi?db=nuccore&rettype=gbwithparts&retmode=text&id="; 	
			String baseURLg = dir + "/genbank/genomes/";
			
			for (String ac : setGBACs_cp) {
				output.format(wgetLoc+" -q \"%s\" -P %s\n", baseURL+ac, dir);
				output.format("mv \"%s\" \"%s\"\n", baseURLf+ac, baseURLg+ac+".txt");	
			}
			output.close();
            
			//try downloading again
			try {
				Process proc = rt.exec("bash " + wgetFileNew);
				proc.waitFor();
			} catch (InterruptedException e) {
				System.out.println("Process interrupted. Terminating now...");
				System.exit(1);
			}
			gbFileList = Utility.getFilesInDirectory(dir+"/genbank/genomes");
		}
		System.out.println("Saved in \"genomes\"");
		
		//Parse GenBank flat files found in directory "genomes", and insert data into PostgreSQL
		System.out.println("Getting coding sequence information from genomic data files in directory \"genomes\"...");
		try {
			GenBank.parseFiles(dir+"/genbank/genomes", type, dir+"/genbank/seqbins");
		} catch (InterruptedException e) {
			System.out.println("Error parsing GenBank files. Terminating now...");
			System.exit(1);
		}
		System.out.println("Saved to \"cds\", \"cds_ntseq\" and \"genome\" relations");
		
		
		//UNIPROT DATASET
		//Download UniProt idmapping file, map RefSeq protein IDs onto UniProt ACs, and insert into PostgreSQL
		if (type.equalsIgnoreCase("E")) {
			
			String fName = getIdMappingFilename();
			
			if (fName!=null)
				
				downloadAndOrParseByOrganismFile(fName, type, organism);
				
			else
				downloadAndOrParseDefaultIdMappingFile(type, organism);
		}
		else {
			
			downloadAndOrParseDefaultIdMappingFile(type, organism);
			
			if (organism.equalsIgnoreCase("Escherichia coli")) {
				
				String fName = getIdMappingFilename();
				
				if (fName!=null) {
					
					System.out.println("Additional data are available in " + fName.substring(0, fName.length()-3));		//for taxID 83333
					
					downloadAndOrParseByOrganismFile(fName, type, organism);
				}	
			}
		}
		
		//Get Swiss-Prot information of interest, and insert into PostgreSQL
		System.out.println("Retrieving Swiss-Prot information from uniprot_sprot.dat for UniProt ACs of interest...");		
		Protein.parseFile(dir+"/uniprot/uniprot_sprot.dat");
		System.out.println("Saved to \"protein\" relation");
		
		//Retrieve protein names and amino acid sequences, and update Protein relation in PostgreSQL
		System.out.println("Reading uniprot_sprot.fasta to retrieve amino acid sequences and protein names...");		 
		ReadFastaProtein.parseFile(dir+"/uniprot/uniprot_sprot.fasta");		
		System.out.println("Saved to \"protein\" relation");
		
		//Map UniProt ACs onto GO IDs, and insert into PostgreSQL
		System.out.println("Mapping UniProt ACs onto GO IDs...");
		UniProt2GO.parseFile(dir+"/uniprot/uniprot_sprot.dat");
		System.out.println("Saved to \"protein2go\" relation");
		
		
		//GO DATASET
		//Retrieve Gene Ontology terms for GO IDs of interest, and insert into PostgreSQL
		System.out.println("Retrieving all Gene Ontology terms from go-basic.obo...");  		
		GO.parseFile(dir+"/gene_ontology/go-basic.obo");
		System.out.println("Saved to \"go\" and \"go_parent\" relations");
		
		
		//PFAM DATASET
		//Retrieve Pfam data of interest and insert into corresponding relations in PostgreSQL
		System.out.println("Mapping UniProt ACs onto Pfam ACs, and retrieving Pfam entries of interest from Pfam-A.full.uniprot...");
		Pfam.parseFile(dir+"/pfam/Pfam-A.full.uniprot");	 		
		System.out.println("Saved to \"protein2pfam\" and \"pfam\" relations");
		
		
		//PDB DATASET
		//Map UniProt ACs onto PDB IDs and chains and taxa IDs, and insert into PostgreSQL
		System.out.println("Mapping UniProt ACs onto PDB IDs and chains...");
		UniProt2PDB.parseFile(dir+"/pdb/pdb_chain_uniprot.tsv");
		System.out.println("Saved to \"protein2pdb\" relation");
		System.out.println("Getting taxonomy information from pdb_chain_taxonomy.tsv for PDB IDs of interest...");
		PDB_Chain_Taxa.parseFile(dir+"/pdb/pdb_chain_taxonomy.tsv");
		System.out.println("Saved to \"pdb_chain\" relation");
		
		//Retrieve information of interest and insert into PostgreSQL
		System.out.println("Retrieving PDB entries of interest from entries.idx...");
		PDB_Entry.parseFile(dir+"/pdb/entries.idx");
		System.out.println("Saved to \"pdb\" relation");
		System.out.println("Getting amino acid sequences from pdb_seqres.txt for PDB IDs of interest...");
		PDB_SeqRes.parseFile(dir+"/pdb/pdb_seqres.txt");
		System.out.println("Saved");
		
		long t2 = System.nanoTime();
		System.out.printf("Runtime = %.2f min\n", (t2-t1)*1.6667e-11);
		
		input.close();

	}//end MAIN

	public static void printWelcomeMessage() {
		
		System.out.println("TAGOPSIN - TAxonomy, Gene, Ontology, Protein, Structure INtegrated\n"
				+ "version 1.2");
		System.out.println("E. Bundhoo et al. (2020), Journal, vv(ii): pp-pp, doi: xxx");
		System.out.println("Licensed under the GNU General Public License");
		
		System.out.println();
		System.out.println("This program will retrieve data from the following public databases:\n"
				+ "NCBI Taxonomy, NCBI Nucleotide, UniProtKB, Gene Ontology, Pfam, EBI SIFTS and RCSB PDB.\n");
		System.out.println("TAGOPSIN uses the NCBI E-utilities. Please see NCBI's Disclaimer and Copyright "
				+ "Notice at https://www.ncbi.nlm.nih.gov/About/disclaimer.html.\n");
	}
	
	public static void check_wget() throws IOException {
		
		System.out.print("Do you have GNU Wget already installed? Y/N: ");
		String hasWget = input.next();
		
        while (!hasWget.equalsIgnoreCase("N") && !hasWget.equalsIgnoreCase("Y")) { 
			System.out.print("Invalid key! Please enter Y or N: ");
		    hasWget = input.next();
	    }
		if (hasWget.equalsIgnoreCase("N")) {
			System.out.println("GNU Wget is needed. Please install and come back.");
			System.exit(0);
		}
		else if (hasWget.equalsIgnoreCase("Y")) {
			//Get Unix path to wget
			Process proc = rt.exec("which wget");
			BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null)
            	wgetLoc = inputLine;
            in.close();
		}
		
	}
	
	public static void create_database() throws IOException, SQLException {
		
		System.out.print("Do you have PostgreSQL already installed? Y/N: ");
		String hasPg = input.next();
		
		while (!hasPg.equalsIgnoreCase("N") && !hasPg.equalsIgnoreCase("Y")) { 
			System.out.print("Invalid key! Please enter Y or N: ");
			hasPg = input.next();
	    }
		if (hasPg.equalsIgnoreCase("N")) {		
			System.out.println("PostgreSQL is needed. Please install and come back.");
			System.exit(0);
		}
		else if (hasPg.equalsIgnoreCase("Y")) {
			//Create database "tagopsin" in PostgreSQL
			System.out.println("Creating database \"tagopsin\" in PostgreSQL...");
			
			try {
				//write the SQL code in temporary file "create_db.sql"
				output = new Formatter("create_db.sql");
				output.format("%s", CreateDB.getSQL());
				output.close();
				
				//execute "create_db.sql" using psql command with default username
				Process proc = rt.exec("psql -U postgres -a -f create_db.sql");
				proc.waitFor();
				System.out.println("Done");
				
				//remove "create_db.sql"
				rt.exec("rm create_db.sql");
			} catch (Exception e) {
				System.out.println("Database creation failed. Please check for psql command.");
				System.exit(1);
			}
		}
	}
	
	public static void create_db_relations() {
		
		//Create relations (table structures) in PostgreSQL
		System.out.println("Creating relations in database \"tagopsin\"...");
		
		try {
			//write the SQL code in temporary file "SQL_DDL.sql"
			output = new Formatter("SQL_DDL.sql");
			output.format("%s", SQL_DDL.getSQL());
			output.close();
			
			//execute "SQL_DDL.sql" using psql command with default username
			Process proc = rt.exec("psql -U postgres -d tagopsin -a -f SQL_DDL.sql");
			proc.waitFor();
			System.out.println("Done");
			
			//remove "SQL_DDL.sql"
			rt.exec("rm SQL_DDL.sql");
		} catch (Exception e) {
			System.out.println("Unable to create relations in database \"tagopsin\". Please check for psql command.");
			System.exit(1);
		}
	}
	
	public static String getDataDirFromHistory() throws IOException {
		
		boolean hasHistory = History.historyExists();
		
		if (!hasHistory) {				//TAGOPSIN is either being run for the first time or history log file has been removed
		
			System.out.print("Enter full path to directory where you would like data files to be downloaded: ");
			dir = input.next();
			
			//Check whether directory path is valid
			file = new File(dir);
			System.out.println("Checking \"" + dir + "\"...");
			if (file.exists())
				System.out.println("OK");
			else {
				while (!file.exists()) {
					System.out.print("Not found. Please enter a valid directory path: ");
					dir = input.next();
					file = new File(dir);
					System.out.println("Checking \"" + dir + "\"...");
				}
				System.out.println("OK");
			}
			History.writeLogFile(dir);
		}
		
		else { 							//TAGOPSIN was run earlier and history log file exists
			dir = History.readLogFile();
			file = new File(dir);
			
			if (file.exists())			//data file directory exists
				System.out.println("\"" + dir + "\" was last used by TAGOPSIN");
			
			else {						//data file directory has been removed
				System.out.println("\"" + dir + "\" was last used by TAGOPSIN and could have been removed");
				
				System.out.print("Enter full path to directory where you would like data files to be downloaded: ");
				dir = input.next();
				
				//Check whether directory path is valid
				file = new File(dir);
				System.out.println("Checking \"" + dir + "\"...");
				if (file.exists())
					System.out.println("OK");
				else {
					while (!file.exists()) {
						System.out.print("Not found. Please enter a valid directory path: ");
						dir = input.next();
						file = new File(dir);
						System.out.println("Checking \"" + dir + "\"...");
					}
					System.out.println("OK");
				}
				History.writeLogFile(dir);
			}
		}
		return dir;
	}
	
	public static void wgetFile(URL url,String filename,String dir) throws IOException {
		
	    try {
	    	Process proc = rt.exec(wgetLoc + " " + url + " -O " + dir + "/" + filename);
	    	
	    	BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
	    	
	        String inputLine;
	        while ((inputLine = in.readLine()) != null)            	
	            System.out.println(inputLine);
	        in.close();  
		} catch (Exception e) {
			System.out.println("\nDownload failed. Please check Internet connection or Wget functionality and try again.");
			System.exit(1);
		}
	}
	
	public static void check_database_conn() throws IOException {
		
		//Download JDBC JAR and extract files
		System.out.println("Downloading https://jdbc.postgresql.org/download/postgresql-42.2.16.jar...");
		wgetFile(new URL("https://jdbc.postgresql.org/download/postgresql-42.2.16.jar"),"postgresql-42.2.16.jar",dir);	
		System.out.println("Done");
		
		System.out.println("Extracting files from postgresql-42.2.16.jar...");
		rt.exec("jar xf " + dir + "/postgresql-42.2.16.jar");
		System.out.println("Done");
		
		Console console = System.console();
		
		//Establish connection to database "tagopsin" in PostgreSQL
		System.out.println("Connecting to database \"tagopsin\" in PostgreSQL...");
		
		String url,username,password;
		
		System.out.print("Enter URL location of JDBC driver: ");
		url = input.next();
		
		System.out.print("Enter username: ");
		username = input.next();
		
		char[] pswd = console.readPassword("Enter password: ");	
		password = String.valueOf(pswd);
		
		cparam = new ConnectParam(url, username, password);
		
		try {
			DriverManager.getConnection(url, username, password);
			System.out.println("Connection successful\n");
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}
		
	}
	
	public static ConnectParam access_conn_param() {
		return cparam;
	}
	
	public static void downloadStandardFiles(String dir) throws IOException {
		
		if (! History.checkTaxonomyFile(dir))
		    
			StdFiles.downloadTaxonomy(dir);
		
		if (! History.checkUniProtFiles(dir))
			
			StdFiles.downloadUniProt(dir);
		
		if (! History.checkGOfile(dir))
			
			StdFiles.downloadGO(dir);
		
		if (! History.checkPfamFile(dir))
			
			StdFiles.downloadPfam(dir);
		
		if (! History.checkPDBfiles(dir))
			
			StdFiles.downloadPDB(dir);
		
	}
	
	public static String getIdMappingFilename() throws IOException {
		
		//Download RELEASE.metalink from UniProt FTP server
		System.out.println("Downloading "
				+ "ftp://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/idmapping/by_organism/RELEASE.metalink...");
		
		url = new URL("ftp://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/idmapping/by_organism/RELEASE.metalink");
		
		FTP.downloadFile(url, dir+"/uniprot/RELEASE.metalink");
		
		//Parse file RELEASE.metalink, and return mapping filename of interest, otherwise null
		Scanner scanner = new Scanner(new FileInputStream(dir+"/uniprot/RELEASE.metalink"));
		
		List<String> listOfFilenames = new LinkedList<>();
		int[] array = DBqueries.getTaxIDs();
		
		while (scanner.hasNextLine()) {
			
			String line = scanner.nextLine();
			
			if (line.startsWith("  <file name=\"") && line.endsWith("idmapping.dat.gz\">")) {
				line = line.replaceAll("  <file name=\"", "");
				line = line.replaceAll("\">", "");
				String mappingFilename = line;
				listOfFilenames.add(mappingFilename);
			}
			
		}
		scanner.close();
		
		for (String fName : listOfFilenames) {
			String[] tokens = fName.split("_");
			if (Utility.isIDofInterest(array, Integer.parseInt(tokens[1])))
				return fName;
		}
		return null;
		
	}
	
	public static void downloadAndOrParseByOrganismFile(String fName, String type, String organism) throws IOException {
		
		if (! History.dataFileIsPresent(dir+"/uniprot/"+fName.substring(0, fName.length()-3))) {
			
			//Download by_organism idmapping file from UniProt FTP and decompress
			System.out.println("Downloading "
					+ "ftp://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/idmapping/by_organism/"+fName+"...");
			
			url = new URL("ftp://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/idmapping/by_organism/"+fName);
			
			FTP.downloadFile(url, dir+"/uniprot/"+fName);
			
			System.out.println("Decompressing " + fName + "...");
			try {
				Process proc = rt.exec("gunzip -d " + dir + "/uniprot/"+fName);
				proc.waitFor();
				System.out.println("Done");				
			} catch (Exception e) {
				System.out.println("Process interrupted. Terminating now...");
				System.exit(1);
			}
		}
		//Parse by_organism idmapping file to map protein IDs onto UniProt ACs
		System.out.println("Mapping RefSeq protein IDs onto UniProt ACs...");
		
		GenBank2UniProt.parseFile(dir+"/uniprot/"+fName.substring(0, fName.length()-3), type, organism);
		
		System.out.println("Saved to \"cds\" relation");
		
	}
	
	public static void downloadAndOrParseDefaultIdMappingFile(String type, String organism) throws IOException {
		
		if (! History.dataFileIsPresent(dir+"/uniprot/idmapping.dat")) {
			
			//Download idmapping.dat.gz from UniProt FTP and decompress
			System.out.println("Downloading "
					+ "ftp://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/idmapping/idmapping.dat.gz...");
			
			url = new URL("ftp://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/idmapping/idmapping.dat.gz");
			
			FTP.downloadFile(url, dir+"/uniprot/idmapping.dat.gz");
			
			System.out.println("Decompressing idmapping.dat.gz...");
			try {
				Process proc = rt.exec("gunzip -d " + dir + "/uniprot/idmapping.dat.gz");
				proc.waitFor();
			} catch (InterruptedException e) {
				System.out.println("Process interrupted. Terminating now...");
				System.exit(1);
			}
			System.out.println("Done");
		}
		//Parse idmapping.dat to map protein IDs onto UniProt ACs
		System.out.println("Mapping RefSeq protein IDs onto UniProt ACs...");
		
		GenBank2UniProt.parseFile(dir+"/uniprot/idmapping.dat", type, organism);	
		
		System.out.println("Saved to \"cds\" relation");
		
	}

}
