package genbank;

/**
 * This class represents the nucleotide sequence of a given protein-coding gene 
 * on the genome.
 * 
 * @author 	Anisah W. Ghoorah, University of Mauritius
 * @since	2019-02-19
 * @version	1.0
 *
 */

public class NtSeq {

	//Attributes
	private int start;					//start position on the genome
	private int stop;					//end position on the genome
	private String seq;					//nucleotide sequence
	
	//Constructor
	public NtSeq(int start, int stop) {
		this.start = start;
		this.stop = stop;
		this.seq = null;
	}
	
	//Getters and Setters
	public String getSeq() {
		return seq;
	}

	public void setSeq(String seq) {
		this.seq = seq;
	}

	public int getStart() {
		return start;
	}
	
	public void setStart(int start) {
		this.start = start;
	}
	
	public int getStop() {
		return stop;
	}
	
	public void setStop(int stop) {
		this.stop = stop;
	}
	
}
