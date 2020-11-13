package mapping;

/**
 * This class represents the mapping of a RefSeq protein ID onto its corresponding UniProt AC 
 * for a given RefSeq genome AC (prokaryotes, viruses) or a given locus name (eukaryotes).
 * 
 * @author 	Eshan Bundhoo, University of Mauritius
 * @since	2020-06-18
 * @version	1.1
 *
 */

public class GBMapping {
	
	private String locusName;
	private String refseq;
	private String genomeAc;
	private String uniprotAc;
	
	public GBMapping() {
		locusName = null;
		refseq = null;
		genomeAc = null;
		uniprotAc = null;
	}

	public String getLocusName() {
		return locusName;
	}

	public void setLocusName(String locusName) {
		this.locusName = locusName;
	}

	public String getRefseq() {
		return refseq;
	}

	public void setRefseq(String refseq) {
		this.refseq = refseq;
	}

	public String getGenomeAc() {
		return genomeAc;
	}

	public void setGenomeAc(String genomeAc) {
		this.genomeAc = genomeAc;
	}

	public String getUniprotAc() {
		return uniprotAc;
	}

	public void setUniprotAc(String uniprotAc) {
		this.uniprotAc = uniprotAc;
	}
	

}
