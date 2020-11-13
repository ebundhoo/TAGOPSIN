package genbank;
import java.util.Stack;

/**
 * This class represents the genome of a given organism (as per GenBank annotation).
 * 
 * @author 	Anisah W. Ghoorah, University of Mauritius
 * @since	2019-02-19
 * @version	1.0
 *
 */

public class Genome {
	
	private String gen_ac;
	private String organism;
	private String genome_seq;
	private Stack<CDS> stackCDS;

	public Genome() {
		gen_ac = "";
		organism = "";
		genome_seq = "";
		stackCDS = new Stack<>();
	}
	
	public String getGenAc() {
		return gen_ac;
	}

	public void setGenAc(String gen_ac) {
		this.gen_ac = gen_ac;
	}

	public String getOrganism() {
		return organism;
	}
	
	public void setOrganism(String organism) {
		this.organism = organism;
	}
	
	public String getGenomeSeq() {
		return genome_seq;
	}
	
	public void setGenomeSeq(String genome_seq) {
		this.genome_seq = genome_seq;
	}

	public Stack<CDS> getStackCDS() {
		return stackCDS;
	}

	public void addCDS(CDS e) {
		this.stackCDS.push(e);
	}
	
}


