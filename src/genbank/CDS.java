package genbank;
import java.util.LinkedList;
import java.util.List;

/**
 * This class represents the generic coding sequence feature in the GenBank flat file.
 * 
 * @author 	Anisah W. Ghoorah, University of Mauritius
 * @since	2020-06-18
 * @version	1.1
 *
 */

public class CDS {
	
	//Feature "CDS" in the GenBank flat file
	private String gene;
	private String locus_tag;
	private List<NtSeq> listNtSeq;
	private String product;
	private String protein_id;
	private String uniprot_ac;   		//db_xref=GOA  optional
	private String prot_seq;			//translation
	private String type;				//whether "complement", "join" etc.
	
	//Constructor
	public CDS() {	
		gene = null;
		locus_tag = null;
		listNtSeq = new LinkedList<>();
		product = null;
		protein_id = null;
		uniprot_ac = null;
		prot_seq = null;
		type = null;
	}
	
	//Getters and Setters
	public List<NtSeq> getListNtSeq() {
		return listNtSeq;
	}

	public void addNtSeq(NtSeq e) {
		this.listNtSeq.add(e);
	}
	
	public String getGene() {
		return gene;
	}

	public void setGene(String gene) {
		this.gene = gene;
	}

	public String getLocusTag() {
		return locus_tag;
	}
	
	public void setLocusTag(String locus_tag) {
		this.locus_tag = locus_tag;
	}

	public String getProduct() {
		return product;
	}

	public void setProduct(String product) {
		this.product = product;
	}

	public String getProteinID() {
		return protein_id;
	}
	
	public void setProteinID(String protein_id) {
		this.protein_id = protein_id;
	}
	
	public String getUniProtAC() {
		return uniprot_ac;
	}
	
	public void setUniProtAC(String uniprot_ac) {
		this.uniprot_ac = uniprot_ac;
	}
	
	public String getProtSeq() {
		return prot_seq;
	}
	
	public void setProtSeq(String prot_seq) {
		this.prot_seq = prot_seq;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
}
