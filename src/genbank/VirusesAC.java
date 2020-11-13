package genbank;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Map.Entry;

import util.DBqueries;
import util.Utility;

/**
 * This class parses the file viruses.txt found on NCBI FTP server, retrieves RefSeq 
 * genome ACs and builds EFetch URLs. It uses the NCBI E-utilities. Please see NCBI's 
 * Disclaimer and Copyright Notice at <https://www.ncbi.nlm.nih.gov/About/disclaimer.html>. 
 * 
 * @author 	Eshan Bundhoo, University of Mauritius
 * @since	2019-10-08
 * @version	1.0
 *
 */

public class VirusesAC {

	public static Set<String> parseFile(String dir, String filename, String outFile, String wgetLoc) 
			throws FileNotFoundException, ParseException {
		
		Formatter output = new Formatter(outFile);
		Set<String> set = new HashSet<String>();
		
		Scanner scanner = new Scanner(new FileInputStream(filename));
		
		int[] array = DBqueries.getTaxIDs();
		
		Map<Integer, HashSet<String>> mapGenAc = new HashMap<>();
		
		String genomeAc = "";

		//Start reading the file viruses.txt
		while (scanner.hasNextLine()) {
			
			String line = scanner.nextLine();
			
			if (line.startsWith("#"))
				continue;
			
			String[] tokens = line.split("\t");
			
			int taxid = Integer.parseInt(tokens[1]);
	
			if (Utility.isIDofInterest(array, taxid)) {
				
				String segments = tokens[9];
				
				//Get the RefSeq genome AC and store in a HashMap
				if (!segments.equals("-")) {
					if (segments.contains("; ")) {				//more than one genome AC for the same entry
						String[] acs = segments.split("; ");
						for (int i=0;i<acs.length;i++) {
							if (acs[i].contains(":")) {
								genomeAc = acs[i].split(":")[1].split("\\.")[0];
								if (genomeAc.startsWith("N"))
									EukaryotesAC.getACs(mapGenAc, taxid, genomeAc);
							}
						}	
					}
					else if (segments.contains(":")) {			//feature "Unknown" in viruses.txt
						genomeAc = segments.split(":")[1].split("\\.")[0];
						if (genomeAc.startsWith("N"))
							EukaryotesAC.getACs(mapGenAc, taxid, genomeAc);
					}
					else {
						genomeAc = segments.split("\\.")[0];
						if (genomeAc.startsWith("N"))
							EukaryotesAC.getACs(mapGenAc, taxid, genomeAc);
					}
				}
			}
				
		}
		scanner.close();
		
		//Build the EFetch URL and write script to output file
		String baseURL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=nuccore&rettype=gbwithparts&retmode=text&id=";
		String baseURLf = dir + "/efetch.fcgi?db=nuccore&rettype=gbwithparts&retmode=text&id=";
		String baseURLg = dir + "/genbank/genomes/";
		
		Set<Entry<Integer, HashSet<String>>> entrySet = mapGenAc.entrySet();
		Iterator<Entry<Integer, HashSet<String>>> it = entrySet.iterator();
		
		while (it.hasNext()) {
			
			Entry<Integer, HashSet<String>> entry = it.next();
			HashSet<String> setOfAcs = entry.getValue();
			
			for (String ac : setOfAcs) {
				output.format(wgetLoc+" -q \"%s\" -P %s\n", baseURL+ac, dir);
				output.format("mv \"%s\" \"%s\"\n", baseURLf+ac, baseURLg+ac+".txt");
				set.add(ac);
			}
		}
		output.close();
		
		return set;
	}

}
