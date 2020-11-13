package util;

import java.io.IOException;
import java.net.URL;

import main.FTP;
import main.MainProgram;

/**
 * This class contains all the functions used by the main program (MainProgram.java) to download 
 * standard data files. 
 * 
 * @author 	Eshan Bundhoo, University of Mauritius
 * @since	2020-10-15
 * @version	1.0
 *
 */

public class StdFiles {
	
	private static Runtime rt = Runtime.getRuntime();
	private static URL url;
	
	public static void downloadTaxonomy(String dir) throws IOException {
		
		//Download taxdump.tar.gz from NCBI Taxonomy FTP and decompress
		rt.exec("mkdir " + dir + "/taxdump");
		System.out.println("Downloading ftp://ftp.ncbi.nlm.nih.gov/pub/taxonomy/taxdump.tar.gz...");
		url = new URL("ftp://ftp.ncbi.nlm.nih.gov/pub/taxonomy/taxdump.tar.gz");
		FTP.downloadFile(url, dir+"/taxdump/taxdump.tar.gz");
		System.out.println("Decompressing taxdump.tar.gz...");
		rt.exec("tar -xf " + dir + "/taxdump/taxdump.tar.gz -C " + dir + "/taxdump");		
		System.out.println("Done");
		
	}
	
	public static void downloadUniProt(String dir) throws IOException {
		
		//Download uniprot_sprot.dat.gz from UniProt FTP and decompress
		rt.exec("mkdir " + dir + "/uniprot");
		System.out.println("Downloading "
				+ "ftp://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/complete/uniprot_sprot.dat.gz...");
		url = new URL("ftp://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/complete/uniprot_sprot.dat.gz");
		FTP.downloadFile(url, dir+"/uniprot/uniprot_sprot.dat.gz");
		System.out.println("Decompressing uniprot_sprot.dat.gz...");
		try {
			Process proc = rt.exec("gunzip -d " + dir + "/uniprot/uniprot_sprot.dat.gz");
			proc.waitFor();
		} catch (InterruptedException e) {
			System.out.println("Process interrupted. Terminating now...");
			System.exit(1);
		}
		System.out.println("Done");
		
		//Download uniprot_sprot.fasta.gz from UniProt FTP and decompress
		System.out.println("Downloading "
				+ "ftp://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/complete/uniprot_sprot.fasta.gz...");
		url = new URL("ftp://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/complete/uniprot_sprot.fasta.gz");
		FTP.downloadFile(url, dir+"/uniprot/uniprot_sprot.fasta.gz");
		System.out.println("Decompressing uniprot_sprot.fasta.gz...");
		try {
			Process proc = rt.exec("gunzip -d " + dir + "/uniprot/uniprot_sprot.fasta.gz");
			proc.waitFor();
		} catch (InterruptedException e) {
			System.out.println("Process interrupted. Terminating now...");
			System.exit(1);
		}
		System.out.println("Done");
		
	}
	
	public static void downloadGO(String dir) throws IOException {
		
		//Download go-basic.obo from PURL OBO library
		rt.exec("mkdir " + dir + "/gene_ontology");			
		System.out.println("Downloading http://purl.obolibrary.org/obo/go/go-basic.obo...");
		url = new URL("http://purl.obolibrary.org/obo/go/go-basic.obo");
		MainProgram.wgetFile(url,"go-basic.obo",dir+"/gene_ontology");	
		System.out.println("Done");
		
	}
	
	public static void downloadPfam(String dir) throws IOException {
		
		//Download Pfam-A.full.uniprot.gz from Pfam FTP and decompress
		rt.exec("mkdir " + dir + "/pfam");
		System.out.println("Downloading ftp://ftp.ebi.ac.uk/pub/databases/Pfam/current_release/Pfam-A.full.uniprot.gz...");
		url = new URL("ftp://ftp.ebi.ac.uk/pub/databases/Pfam/current_release/Pfam-A.full.uniprot.gz");
		FTP.downloadFile(url, dir+"/pfam/Pfam-A.full.uniprot.gz");
		System.out.println("Decompressing Pfam-A.full.uniprot.gz...");
		try {
			Process proc = rt.exec("gunzip -d " + dir + "/pfam/Pfam-A.full.uniprot.gz");
			proc.waitFor();
		} catch (InterruptedException e) {
			System.out.println("Process interrupted. Terminating now...");
			System.exit(1);
		}
		System.out.println("Done");
		
	}
	
	public static void downloadPDB(String dir) throws IOException {
		
		//Download pdb_chain_uniprot.tsv.gz and pdb_chain_taxonomy.tsv.gz from SIFTS FTP and decompress
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
			System.out.println("Process interrupted. Terminating now...");
			System.exit(1);
		}
		System.out.println("Done");
		
		//Download entries.idx and pdb_seqres.txt.gz from wwPDB HTTP/FTP server and decompress
		System.out.println("Downloading http://ftp.wwpdb.org/pub/pdb/derived_data/index/entries.idx and "
				+ "http://ftp.wwpdb.org/pub/pdb/derived_data/pdb_seqres.txt.gz...");		
		
		url = new URL("http://ftp.wwpdb.org/pub/pdb/derived_data/index/entries.idx");
		MainProgram.wgetFile(url,"entries.idx",dir+"/pdb");
		
		url = new URL("http://ftp.wwpdb.org/pub/pdb/derived_data/pdb_seqres.txt.gz");
		MainProgram.wgetFile(url,"pdb_seqres.txt.gz",dir+"/pdb");
		
		System.out.println("Decompressing pdb_seqres.txt.gz...");
		try {
			Process proc = rt.exec("gunzip -d " + dir + "/pdb/pdb_seqres.txt.gz");
			proc.waitFor();
		} catch (InterruptedException e) {
			System.out.println("Process interrupted. Terminating now...");
			System.exit(1);
		}
		System.out.println("Done");
	}

}
