package genbank;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import util.DBqueries;
import util.Utility;

/**
 * This class parses the file prokaryotes.txt found on NCBI FTP server, retrieves RefSeq 
 * genome ACs and builds EFetch URLs. It uses the NCBI E-utilities. Please see NCBI's 
 * Disclaimer and Copyright Notice at <https://www.ncbi.nlm.nih.gov/About/disclaimer.html>. 
 * 
 * @author 	Eshan Bundhoo, University of Mauritius
 * @since	2019-02-19
 * @version	1.0
 *
 */

public class ProkaryotesAC {
	
	public static Set<String> parseFile(String dir, String filename, String outFile, String wgetLoc) throws FileNotFoundException, ParseException {
		
		Formatter output = new Formatter(outFile);
		Set<String> set = new HashSet<String>();
		
		Scanner scanner = new Scanner(new FileInputStream(filename));
		
		int[] array = DBqueries.getTaxIDs();
		
		Map<String,RefSeqEntry> mapGenAc = new HashMap<>();
		Map<String,RefSeqEntry> mapPlAc = new HashMap<>();
		
		String genomeAc = "";
		String plasmidAc = "";

		//Read the file prokaryotes.txt
		while (scanner.hasNextLine()) {
			
			String line = scanner.nextLine();
			
			if (line.startsWith("#"))
				continue;
			
			String[] tokens = line.split("\t");
			
			int taxid = Integer.parseInt(tokens[1]);
	
			if (Utility.isIDofInterest(array, taxid)) {
				
				String replicons = tokens[8];
				String modifDate = tokens[14]; 
				String strain = tokens[22];
				
				//Get the RefSeq genome AC and store in a HashMap
				if (!replicons.equals("-")) {
					if (replicons.startsWith("chromosome")) {
						genomeAc = replicons.split(":")[1].split("\\.")[0];
						if (genomeAc.startsWith("N"))
							getLatestAc(mapGenAc, taxid, strain, genomeAc, modifDate);
						
						if (replicons.contains("; ")) {
							String[] acs = replicons.split("; ");
							for (int i=1;i<acs.length;i++) {
								if (acs[i].startsWith("plasmid")) {
									plasmidAc = acs[i].split(":")[1].split("\\.")[0];
									if (plasmidAc.startsWith("N"))
										getLatestAc(mapPlAc, taxid, strain, plasmidAc, modifDate);
								}
								else if (acs[i].startsWith("chromosome")) {			
									genomeAc = acs[i].split(":")[1].split("\\.")[0];
									if (genomeAc.startsWith("N"))
										getLatestAc(mapGenAc, taxid, strain, genomeAc, modifDate);
								}
							}
						}
					}
					else if (replicons.startsWith("plasmid")) {
						String[] p_acs = replicons.split(":");
						for (int i=1;i<p_acs.length;i++) {
							plasmidAc = p_acs[i].split("\\.")[0];
							if (plasmidAc.startsWith("N"))
								getLatestAc(mapPlAc, taxid, strain, plasmidAc, modifDate);
						}
					}
				}
			}
				
		}
		scanner.close();
		
		//Build the EFetch URL and write script to output file
		String baseURL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=nuccore&rettype=gbwithparts&retmode=text&id=";
		String baseURLf = dir + "/efetch.fcgi?db=nuccore&rettype=gbwithparts&retmode=text&id="; 	
		String baseURLg = dir + "/genbank/genomes/";	
		
		Collection<RefSeqEntry> collGenAc = mapGenAc.values();
		//Collection<RefSeqEntry> collPlAc = mapPlAc.values();
		
		for (RefSeqEntry entry : collGenAc) {
			output.format(wgetLoc+" -q \"%s\" -P %s\n", baseURL+entry.getAc(), dir);
			output.format("mv \"%s\" \"%s\"\n", baseURLf+entry.getAc(), baseURLg+entry.getAc()+".txt");
			set.add(entry.getAc());
		}
		output.close();
		
		return set;
	}
	
	public static void getLatestAc (Map<String,RefSeqEntry> map, int taxid, String strain, String ac, String date) throws ParseException {
		
		Date dateModif = new SimpleDateFormat("yyyy/MM/dd").parse(date);		//"Modify Date" in prokaryotes.txt
		String key = taxid+"|"+strain;
		
		if (!map.containsKey(key))
			map.put(key, new RefSeqEntry(ac, date));
		else {
			RefSeqEntry entry = map.get(key);
			if (entry.getDate().compareTo(dateModif) < 0)
				map.put(key, new RefSeqEntry(ac, date));
		}
			
	}

}
