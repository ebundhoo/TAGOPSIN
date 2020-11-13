package main;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Paths;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Formatter;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import genbank.GenBank1_0;
import genbank.ProkaryotesAC;
import go.GO;
import mapping.GenBank2UniProt1_0;
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
import util.Utility;

/**
 * This class is the main program of TAGOPSIN. It accepts user input, downloads and processes data 
 * files needed by the program and contains a number of functions to check dependencies and connect 
 * to PostgreSQL.
 * This version adds iterative downloading of missing files from NCBI Nucleotide in order to get a 
 * complete GenBank data set.
 * 
 * @author 	Eshan Bundhoo, University of Mauritius
 * @since	2019-09-25
 * @version	1.1
 *
 */

public class MainProgram1_1 {
	
	private static Scanner input = new Scanner(System.in);
	private static Runtime rt = Runtime.getRuntime();
	private static String wgetLoc;
	private static String dir;
	private static File file;
	private static ConnectParam cparam;

	public static void main(String[] args) throws IOException, ParseException {
		
        printWelcomeMessage();
        String retrPfam = getPfamDataset();
        System.out.println();
        check_wget();
        check_database_conn();
        
        //NCBI TAXONOMY DATASET
        //Retrieve data from NCBI Taxonomy for a specified organism, and insert into PostgreSQL
        rt.exec("mkdir " + dir + "/taxdump");
		System.out.println("Downloading ftp://ftp.ncbi.nlm.nih.gov/pub/taxonomy/taxdump.tar.gz...");
		URL url = new URL("ftp://ftp.ncbi.nlm.nih.gov/pub/taxonomy/taxdump.tar.gz");
		FTP.downloadFile(url, dir+"/taxdump/taxdump.tar.gz");
		System.out.println("Decompressing taxdump.tar.gz...");
		rt.exec("tar -xf " + dir + "/taxdump/taxdump.tar.gz -C " + dir + "/taxdump");		
		System.out.println("Done");
		System.out.println("Depending on the objective of your project, organism name can be either Mycobacterium "
				+ "or Mycobacterium bovis for example.");
		System.out.print("Input name of organism of interest: ");
		input.nextLine();
		String organism = input.nextLine();
		System.out.println("Retrieving taxonomy IDs and scientific names from names.dmp in taxdump...");
		Taxonomy.parseFile(dir+"/taxdump/names.dmp", organism);
		System.out.println("Saved to PostgreSQL");
		
		
		//NCBI GENBANK DATASET
		//Get RefSeq genome ACs from prokaryotes.txt, and write EFetch URLs
		rt.exec("mkdir " + dir + "/genbank");
		rt.exec("mkdir " + dir + "/genbank/genomes");
		Set<String> setGBACs = null;
		
		System.out.println("Downloading ftp://ftp.ncbi.nlm.nih.gov/genomes/GENOME_REPORTS/prokaryotes.txt...");
		url = new URL("ftp://ftp.ncbi.nlm.nih.gov/genomes/GENOME_REPORTS/prokaryotes.txt");
		FTP.downloadFile(url, dir+"/genbank/prokaryotes.txt");
		System.out.println("Done");
		System.out.println("Retrieving genome ACs from prokaryotes.txt in genbank and building EFetch URLs...");
		setGBACs = ProkaryotesAC.parseFile(dir, dir+"/genbank/prokaryotes.txt", dir+"/genbank/wget_genomes.sh", wgetLoc);
		System.out.println("Successful");
		
		//Download genomes in GenBank flat file format from NCBI server using E-utilities
		System.out.println("Downloading genomic data files from NCBI via E-utilities...");
		try {
			Process proc = rt.exec("bash " + dir + "/genbank/wget_genomes.sh");
			proc.waitFor();
		} catch (InterruptedException e) {
			System.out.println("Process interrupted. Terminating...");
			System.exit(0);
		}
		
		//Check number of genomes downloaded, and restart previous process if necessary 
		List<String> gbFileList = Utility.getFilesInDirectory(dir+"/genbank/genomes"); 
		Set<String> setGBFiles = new HashSet<>();
		Set<String> setGBACs_cp = new HashSet<>();
		
		while (setGBACs.size() != gbFileList.size()) {
			
			System.out.printf("Number of files downloaded is %d of %d\n", gbFileList.size(), setGBACs.size());
			System.out.println("Retrieving remaining files from NCBI via E-utilities...");
			
			for (String path : gbFileList) {
				String fname = Paths.get(path).getFileName().toString().split("\\.")[0];
				setGBFiles.add(fname);
			}
			
			//make a copy of the original set of genome ACs
			setGBACs_cp.addAll(setGBACs);
			
			//remove already downloaded files from the duplicate 
			setGBACs_cp.removeAll(setGBFiles);								
	
			//write new script
			String wgetFileNew = dir + "/genbank/wget_genomes_new.sh";		

	        Formatter output = new Formatter(wgetFileNew);
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
				System.out.println("Process interrupted. Terminating...");
				System.exit(0);
			}
			gbFileList = Utility.getFilesInDirectory(dir+"/genbank/genomes");
		}
		System.out.println("Saved in genomes");
		
		//Parse GenBank flat files found in directory genomes, and insert data into PostgreSQL
		System.out.println("Getting coding sequence information from genomic data files in genomes...");
		GenBank1_0.parseFiles(dir+"/genbank/genomes");		
		System.out.println("Saved to PostgreSQL");
		
		
		//UNIPROT DATASET
		//Map RefSeq protein IDs onto UniProt ACs, and insert into PostgreSQL
		rt.exec("mkdir " + dir + "/uniprot");	
		System.out.println("Downloading ftp://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/idmapping/idmapping.dat.gz...");
		url = new URL("ftp://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/idmapping/idmapping.dat.gz");
		FTP.downloadFile(url, dir+"/uniprot/idmapping.dat.gz");
		System.out.println("Decompressing idmapping.dat.gz...");
		try {
			Process proc = rt.exec("gunzip -d " + dir + "/uniprot/idmapping.dat.gz");
			proc.waitFor();
		} catch (InterruptedException e) {
			System.out.println("Process interrupted. Terminating...");
			System.exit(0);
		}
		System.out.println("Done");
		System.out.println("Mapping RefSeq protein IDs onto UniProt ACs...");
		GenBank2UniProt1_0.parseFile(dir+"/uniprot/idmapping.dat"); 		
		System.out.println("Saved to PostgreSQL");
		
		//Get Swiss-Prot information from uniprot_sprot.dat, and insert into PostgreSQL
		System.out.println("Downloading ftp://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/complete/uniprot_sprot.dat.gz...");
		url = new URL("ftp://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/complete/uniprot_sprot.dat.gz");
		FTP.downloadFile(url, dir+"/uniprot/uniprot_sprot.dat.gz");
		System.out.println("Decompressing uniprot_sprot.dat.gz...");
		try {
			Process proc = rt.exec("gunzip -d " + dir + "/uniprot/uniprot_sprot.dat.gz");
			proc.waitFor();
		} catch (InterruptedException e) {
			System.out.println("Process interrupted. Terminating...");
			System.exit(0);
		}
		System.out.println("Done");
		System.out.println("Retrieving Swiss-Prot information from uniprot_sprot.dat in uniprot for UniProt ACs of interest...");		
		Protein.parseFile(dir+"/uniprot/uniprot_sprot.dat");
		System.out.println("Saved to PostgreSQL");
		
		//Retrieve protein names and amino acid sequences from a FASTA format file, and update Protein relation in PostgreSQL
		System.out.println("Downloading ftp://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/complete/uniprot_sprot.fasta.gz...");
		url = new URL("ftp://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/complete/uniprot_sprot.fasta.gz");
		FTP.downloadFile(url, dir+"/uniprot/uniprot_sprot.fasta.gz");
		System.out.println("Decompressing uniprot_sprot.fasta.gz...");
		try {
			Process proc = rt.exec("gunzip -d " + dir + "/uniprot/uniprot_sprot.fasta.gz");
			proc.waitFor();
		} catch (InterruptedException e) {
			System.out.println("Process interrupted. Terminating...");
			System.exit(0);
		}
		System.out.println("Done");
		System.out.println("Reading uniprot_sprot.fasta in uniprot to get amino acid sequences and protein names...");		 
		ReadFastaProtein.parseFile(dir+"/uniprot/uniprot_sprot.fasta");		
		System.out.println("Saved to PostgreSQL");
		
		//Map UniProt ACs onto GO IDs, and insert into PostgreSQL
		System.out.println("Mapping UniProt ACs onto GO IDs...");
		UniProt2GO.parseFile(dir+"/uniprot/uniprot_sprot.dat");
		System.out.println("Saved to PostgreSQL");
		
		
		//GO DATASET
		//Retrieve Gene Ontology terms for GO IDs of interest, and insert into PostgreSQL
		rt.exec("mkdir " + dir + "/gene_ontology");			
		System.out.println("Downloading http://purl.obolibrary.org/obo/go/go-basic.obo...");
		url = new URL("http://purl.obolibrary.org/obo/go/go-basic.obo");
		wgetFile(url,"go-basic.obo",dir+"/gene_ontology");	
		System.out.println("Done");
		System.out.println("Retrieving Gene Ontology terms from go-basic.obo in gene_ontology for GO IDs of interest...");  		
		GO.parseFile(dir+"/gene_ontology/go-basic.obo");
		System.out.println("Saved to PostgreSQL");
		
		
		//PFAM DATASET
		//Retrieve Pfam data and insert into corresponding relations in PostgreSQL
		if (retrPfam.equalsIgnoreCase("Y")) {
			rt.exec("mkdir " + dir + "/pfam");
			System.out.println("Downloading ftp://ftp.ebi.ac.uk/pub/databases/Pfam/current_release/Pfam-A.full.uniprot.gz...");
			url = new URL("ftp://ftp.ebi.ac.uk/pub/databases/Pfam/current_release/Pfam-A.full.uniprot.gz");
			FTP.downloadFile(url, dir+"/pfam/Pfam-A.full.uniprot.gz");
			System.out.println("Decompressing Pfam-A.full.uniprot.gz...");
			try {
				Process proc = rt.exec("gunzip -d " + dir + "/pfam/Pfam-A.full.uniprot.gz");
				proc.waitFor();
			} catch (InterruptedException e) {
				System.out.println("Process interrupted. Terminating...");
				System.exit(0);
			}
			System.out.println("Done");
			System.out.println("Mapping UniProt ACs onto Pfam ACs, and retrieving Pfam entries of interest from Pfam-A.full.uniprot in pfam...");
			Pfam.parseFile(dir+"/pfam/Pfam-A.full.uniprot");	 		
			System.out.println("Saved to PostgreSQL");
		}
		
		
		//PDB DATASET
		//Map UniProt ACs onto PDB IDs, chains and taxa IDs, and insert into PostgreSQL
		rt.exec("mkdir " + dir + "/pdb");			
		System.out.println("Downloading ftp://ftp.ebi.ac.uk/pub/databases/msd/sifts/flatfiles/tsv/pdb_chain_uniprot.tsv.gz and "
				+ "ftp://ftp.ebi.ac.uk/pub/databases/msd/sifts/flatfiles/tsv/pdb_chain_taxonomy.tsv.gz...");
		
		url = new URL("ftp://ftp.ebi.ac.uk/pub/databases/msd/sifts/flatfiles/tsv/pdb_chain_uniprot.tsv.gz");
		FTP.downloadFile(url, dir+"/pdb/pdb_chain_uniprot.tsv.gz");
		
		url = new URL("ftp://ftp.ebi.ac.uk/pub/databases/msd/sifts/flatfiles/tsv/pdb_chain_taxonomy.tsv.gz");
		FTP.downloadFile(url, dir+"/pdb/pdb_chain_taxonomy.tsv.gz");
		
		System.out.println("Decompressing pdb_chain_uniprot.tsv.gz and pdb_chain_taxonomy.tsv.gz...");
		try {
			Process proc = rt.exec("gunzip -d " + dir + "/pdb/pdb_chain_uniprot.tsv.gz");
			proc.waitFor();
			proc = rt.exec("gunzip -d " + dir + "/pdb/pdb_chain_taxonomy.tsv.gz");
			proc.waitFor();
		} catch (InterruptedException e) {
			System.out.println("Process interrupted. Terminating...");
			System.exit(0);
		}
		System.out.println("Done");
		System.out.println("Mapping UniProt ACs onto PDB IDs and chains...");
		UniProt2PDB.parseFile(dir+"/pdb/pdb_chain_uniprot.tsv");
		System.out.println("Saved to PostgreSQL");
		System.out.println("Getting taxonomy information from pdb_chain_taxonomy.tsv in pdb for PDB IDs of interest...");
		PDB_Chain_Taxa.parseFile(dir+"/pdb/pdb_chain_taxonomy.tsv");
		System.out.println("Saved");
		
		//Retrieve PDB information, including amino acid sequences for the chains, and insert into PostgreSQL
		System.out.println("Downloading http://ftp.wwpdb.org/pub/pdb/derived_data/index/entries.idx and "
				+ "http://ftp.wwpdb.org/pub/pdb/derived_data/pdb_seqres.txt.gz...");		
		
		url = new URL("http://ftp.wwpdb.org/pub/pdb/derived_data/index/entries.idx");
		wgetFile(url,"entries.idx",dir+"/pdb");
		
		url = new URL("http://ftp.wwpdb.org/pub/pdb/derived_data/pdb_seqres.txt.gz");
		wgetFile(url,"pdb_seqres.txt.gz",dir+"/pdb");
		
		System.out.println("Decompressing pdb_seqres.txt.gz...");
		try {
			Process proc = rt.exec("gunzip -d " + dir + "/pdb/pdb_seqres.txt.gz");
			proc.waitFor();
		} catch (InterruptedException e) {
			System.out.println("Process interrupted. Terminating...");
			System.exit(0);
		}
		System.out.println("Done");
		System.out.println("Retrieving PDB entries of interest from entries.idx in pdb...");
		PDB_Entry.parseFile(dir+"/pdb/entries.idx");
		System.out.println("Saved to PostgreSQL");
		System.out.println("Getting amino acid sequences from pdb_seqres.txt in pdb for PDB IDs of interest...");
		PDB_SeqRes.parseFile(dir+"/pdb/pdb_seqres.txt");
		System.out.println("Saved");
		
		input.close();

	}
	
	public static void printWelcomeMessage() {
		
		System.out.println("TAGOPSIN - TAxonomy, Gene, Ontology, Protein, Structure INtegrated\n"
				+ "version 1.1");
		System.out.println("E. Bundhoo et al. (2019), Journal, vv(ii): pp-pp, doi: xxx");
		System.out.println("Licensed under the GNU General Public License");
		
		System.out.println();
		System.out.println("This program will retrieve data from the following public databases:\n"
				+ "NCBI Taxonomy, NCBI Nucleotide, UniProtKB, Gene Ontology, "
				+ "Pfam, EBI SIFTS and RCSB PDB.\n");
		System.out.println("TAGOPSIN uses the NCBI E-utilities. Please see NCBI's Disclaimer and Copyright "
				+ "Notice at https://www.ncbi.nlm.nih.gov/About/disclaimer.html.\n");
	}
	
	public static String getPfamDataset() {	
		
		System.out.print("The Pfam data file is very large (at least 80 GB). High computer performance and good Internet connection are required.\n"	
				+ "Do you wish to get the Pfam dataset? Y/N: ");
		String retrPfam = input.next();
		
		while (!retrPfam.equalsIgnoreCase("N") && !retrPfam.equalsIgnoreCase("Y")) { 
			System.out.print("Invalid key! Please enter Y or N: ");
			retrPfam = input.next();
	    }
		
		return retrPfam;
	}
	
	public static void check_database_conn() throws IOException {
		
		Console console = System.console();
		
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
			//Download JDBC JAR file and establish connection to PostgreSQL
			System.out.println("Downloading https://jdbc.postgresql.org/download/postgresql-42.2.7.jar...");
			wgetFile(new URL("https://jdbc.postgresql.org/download/postgresql-42.2.7.jar"),"postgresql-42.2.7.jar",dir);	
			System.out.println("Done");
			
			System.out.println("Extracting files from postgresql-42.2.7.jar...");
			rt.exec("jar xf " + dir + "/postgresql-42.2.7.jar");
			System.out.println("Done");
			
			System.out.println("Connecting to PostgreSQL...");
			
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
				System.exit(0);
			}
		}
		
	}
	
	public static ConnectParam access_conn_param() {
		return cparam;
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
			System.exit(0);
		}
	}

}
