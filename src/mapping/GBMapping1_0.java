package mapping;

/**
 * This class represents the mapping of a RefSeq protein ID onto its corresponding 
 * UniProt AC for a given RefSeq genome AC (as per the file idmapping.dat found on 
 * UniProt FTP server).
 * 
 * @author 	Eshan Bundhoo, University of Mauritius
 * @since	2019-02-19
 * @version	1.0
 *
 */

public class GBMapping1_0 {
	
	private String refseq;
	private String genomeAc;
	private String uniprotAc;
	
	public GBMapping1_0() {
		refseq = null;
		genomeAc = null;
		uniprotAc = null;
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
