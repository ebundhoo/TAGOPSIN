package genbank;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This class represents a RefSeq entry (under "Replicons" header) in the file 
 * prokaryotes.txt found on NCBI FTP server.
 * 
 * @author 	Anisah W. Ghoorah, University of Mauritius
 * @since	2019-02-19
 * @version	1.0
 * 
 */

public class RefSeqEntry {
	
	//Attributes
	private String ac;
	private Date date;
	
	//Constructor
	public RefSeqEntry(String ac, String date) throws ParseException {
		this.ac = ac;
		this.date = new SimpleDateFormat("yyyy/MM/dd").parse(date);		//"Modify Date" in prokaryotes.txt
	}
	
	//Getters and Setters
	public String getAc() {
		return ac;
	}
	
	public void setAc(String ac) {
		this.ac = ac;
	}
	
	public Date getDate() {
		return date;
	}
	
	public void setDate(Date date) {
		this.date = date;
	}
	
}
