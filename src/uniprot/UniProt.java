package uniprot;
import java.util.Set;
import java.util.HashSet;

/**
 * This class represents the generic UniProt entry.
 * 
 * @author 	Anisah W. Ghoorah, University of Mauritius
 * @since	2019-02-19
 * @version	1.0
 *
 */

public class UniProt {
	
	//Attributes
	private String id;
	private int length;
	private String ac;
	private String name;
	private String function;
	private String sc_location;
	private Set<String> goid;		//DR   GO;
	
	//Constructor
	public UniProt() {
		 id = null;
		 length = 0;
		 ac = null;
		 name = null;
		 function = null;
		 sc_location = null;
		 goid = new HashSet<>();
		
	}

	//Getters and Setters
	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public String getAc() {
		return ac;
	}

	public void setAc(String ac) {
		this.ac = ac;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFunction() {
		return function;
	}

	public void setFunction(String function) {
		this.function = function;
	}

	public String getScLocation() {
		return sc_location;
	}

	public void setScLocation(String sc_location) {
		this.sc_location = sc_location;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public Set<String> getGoId() {
		return goid;
	}

	public void addGoId(String goid) {
		this.goid.add(goid);
	}

}
