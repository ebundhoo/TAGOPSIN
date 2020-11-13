package pfam;

/**
 * This class represents the generic UniProt domain.
 * 
 * @author 	Anisah W. Ghoorah, University of Mauritius
 * @since	2019-02-19
 * @version	1.0
 *
 */

public class UniProtDomain{
	
	//Attributes
	private String uniprotac;	//UniProt AC
	private int start;			//start position on the amino acid sequence
	private int stop;			//end position on the amino acid sequence
	
	//Constructor
	public UniProtDomain(String uniprotac, int start, int stop) {
		this.uniprotac = uniprotac;
		this.start = start;
		this.stop = stop;
	}

	//Getters
	public String getUniProtAc() {
		return uniprotac;
	}

	public int getStart() {
		return start;
	}

	public int getStop() {
		return stop;
	}
	
}