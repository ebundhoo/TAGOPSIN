package pfam;
import java.util.LinkedList;
import java.util.List;

/**
 * This class represents the generic Pfam entry.
 * 
 * @author 	Eshan Bundhoo and Anisah W. Ghoorah, University of Mauritius
 * @since	2019-02-19
 * @version	1.0
 *
 */

public class PfamEntry {
	
	//Attributes
	private String id;
	private String ac;
	private String description;
	private List<UniProtDomain> listUniProt; 
	
	//Constructor
	public PfamEntry() {
		id="";
		ac="";
		description="";
		listUniProt = new LinkedList<>();
	}
	
	//Getters and Setters
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getAc() {
		return ac;
	}
	
	public void setAc(String ac) {
		this.ac = ac;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public void addUniProtDomain(UniProtDomain e) {
		listUniProt.add(e);
	}
	
	public List<UniProtDomain> getListUniProt() {
		return listUniProt;
	}
	
}
