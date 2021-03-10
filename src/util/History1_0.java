package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Formatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

/**
 * This class contains functions to store and retrieve TAGOPSIN's usage history
 * 
 * @author 	Eshan Bundhoo and Anisah W. Ghoorah, University of Mauritius
 * @since	2020-07-24
 * @version	1.0
 *
 */

public class History1_0 {
	
	private static File file;
	private static List<String> dirList;
	private static List<String> dataFiles = new LinkedList<>();
	
	public static boolean historyExists() {
		
		file = new File ("/var/tmp/tagopsin.log");
		  
		if (file.exists())
		    return true;
		     
		return false;
	}
	
	//function to write log file in /var/tmp
	public static void writeLogFile(String dir) throws FileNotFoundException {
		
		Formatter output = new Formatter("/var/tmp/tagopsin.log");
		output.format("%s\n%s\n\n%s", "TAGOPSIN version 1.2", new SimpleDateFormat("yyyy/MM/dd, HH:mm:ss").format(new java.util.Date()), 
				"dirPath = "+dir);
		output.close();
	}
	
	//function to read log file found in /var/tmp
	public static String readLogFile() throws FileNotFoundException {
		
		String filename = "/var/tmp/tagopsin.log", dir = "";
		Scanner scanner = new Scanner(new FileInputStream(filename));
		
		while (scanner.hasNextLine()) {
			
			String line = scanner.nextLine();
			
			if (line.startsWith("dirPath")) {
				String tokens[] = line.split(" = ");
				dir = tokens[1];
			}
		}
		return dir;
	}
	
	//functions to check for presence of standard data files
	public static boolean checkStandardFiles(String dir) throws IOException {
		
		System.out.println("Checking for presence of standard data files in \"" + dir + "\"...");
		
		if (checkTaxonomyFile(dir) && checkUniProtFiles(dir) && checkGOfile(dir) && checkPfamFile(dir) && checkPDBfiles(dir))
			return true;
		
		return false;
	}
	
	public static boolean checkTaxonomyFile(String dir) throws IOException {
		
		dirList = Utility.getFilesInDirectory(dir);
		
		if (!dirList.contains(dir+"/taxdump")) {
			System.out.println("taxdump directory not found");
			return false;
		}
		else {
			List<String> taxaFiles = Utility.getFilesInDirectory(dir+"/taxdump");
			
			for (String file : taxaFiles) {
				String fName = Paths.get(file).getFileName().toString();
				if (fName.equals("names.dmp")) {
					System.out.println("names.dmp is present in \"" + dir + "/taxdump\"");
					return true;
				}
			}
			return false;
		}
		
	}
	
	public static boolean checkUniProtFiles(String dir) throws IOException {
		
		dirList = Utility.getFilesInDirectory(dir);
		
		if (!dirList.contains(dir+"/uniprot")) {
			System.out.println("uniprot directory not found");
			return false;
		}
		else {
			List<String> uniprotFiles = Utility.getFilesInDirectory(dir+"/uniprot");
			
			for (String file : uniprotFiles) {
				String fName = Paths.get(file).getFileName().toString();
				dataFiles.add(fName);
			}
			
			if (dataFiles.contains("uniprot_sprot.dat") && dataFiles.contains("uniprot_sprot.fasta")) {
				System.out.println("uniprot_sprot.dat and uniprot_sprot.fasta are present in \"" + dir + "/uniprot\"");
				return true;
			}
			return false;
		}
		
	}
	
	public static boolean checkGOfile(String dir) throws IOException {
		
		dirList = Utility.getFilesInDirectory(dir);
		
		if (!dirList.contains(dir+"/gene_ontology")) {
			System.out.println("gene_ontology directory not found");
			return false;
		}
		else {
			List<String> goFiles = Utility.getFilesInDirectory(dir+"/gene_ontology");
			
			for (String file : goFiles) {
				String fName = Paths.get(file).getFileName().toString();
				if (fName.equals("go-basic.obo")) {
					System.out.println("go-basic.obo is present in \"" + dir + "/gene_ontology\"");
					return true;
				}
			}
			return false;
		}
		
	}
	
	public static boolean checkPfamFile(String dir) throws IOException {
		
		dirList = Utility.getFilesInDirectory(dir);
		
		if (!dirList.contains(dir+"/pfam")) {
			System.out.println("pfam directory not found");
			return false;
		}
		else {
			List<String> pfamFiles = Utility.getFilesInDirectory(dir+"/pfam");
			
			for (String file : pfamFiles) {
				String fName = Paths.get(file).getFileName().toString();
				if (fName.equals("Pfam-A.full.uniprot")) {
					System.out.println("Pfam-A.full.uniprot is present in \"" + dir + "/pfam\"");
					return true;
				}
			}
			return false;
		}
	}
	
	public static boolean checkPDBfiles(String dir) throws IOException {
		
		dirList = Utility.getFilesInDirectory(dir);
		
		if (!dirList.contains(dir+"/pdb")) {
			System.out.println("pdb directory not found");
			return false;
		}
		else {
			List<String> pdbFiles = Utility.getFilesInDirectory(dir+"/pdb");
			
			for (String file : pdbFiles) {
				String fName = Paths.get(file).getFileName().toString();
				dataFiles.add(fName);
			}
			
			if (dataFiles.contains("pdb_chain_uniprot.tsv") && dataFiles.contains("pdb_chain_taxonomy.tsv") && dataFiles.contains("entries.idx")
					&& dataFiles.contains("pdb_seqres.txt")) {
				System.out.println("All PDB files are present in \"" + dir + "/pdb\"");
				return true;
			}
			return false;
		}
		
	}
	
	//function to check for presence of any file in the file system
	public static boolean dataFileIsPresent(String filename) {
		
		file = new File (filename);
		  
		if (file.exists())
		    return true;
		     
		return false;
	}

}
