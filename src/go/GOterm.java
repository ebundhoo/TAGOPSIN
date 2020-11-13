package go;
import java.util.LinkedList;
import java.util.List;

/**
 * This class represents the generic Gene Ontology term (denoted by "[Term]") in an OBO file.
 * 
 * @author 	Eshan Bundhoo and Anisah W. Ghoorah, University of Mauritius
 * @since	2019-02-19
 * @version	1.0
 *
 */

public class GOterm {

	//Attributes
	private String id;
	private String name;
	private String namespace;
	private String definition;
	private List<String> alt_id;
	private List<String> is_a;
	
	//Constructor
	public GOterm() {
		 id="";
		 name="";
		 namespace="";
		 definition="";
		 alt_id=new LinkedList<>();
		 is_a=new LinkedList<>();	
	}
	
	//Getters and Setters
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}
	
	public String getDefinition() {
		return definition;
	}

	public void setDefinition(String definition) {
		this.definition = definition;
	}
	
	public List<String> getAlt_id() {
		return alt_id;
	}
	
	public void addAlt_id(String alt_id) {
		this.alt_id.add(alt_id);
	}

	public List<String> getIs_a() {
		return is_a;
	}

	public void addIs_a(String is_a) {
		this.is_a.add(is_a);
	}

}

