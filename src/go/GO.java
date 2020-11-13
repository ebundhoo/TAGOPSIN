package go;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import util.ConnectDB;

/**
 * This class parses a file in the OBO format, extracts data relating to individual GO terms 
 * and inserts them into PostgreSQL. The data include exhaustively the GO ID(s), parent GO ID(s), 
 * name, definition and namespace.
 * 
 * @author 	Eshan Bundhoo and Anisah W. Ghoorah, University of Mauritius
 * @since	2019-02-19
 * @version	1.0
 *
 */

public class GO {

	public static void parseFile(String filename) throws FileNotFoundException {
		
		Scanner scanner = new Scanner(new FileInputStream(filename));
		GOterm ont = null;
		
		Map<String, GOterm> map = new HashMap<>();
		
		//Start reading the file
		while (scanner.hasNextLine()) {
			
			String line = scanner.nextLine();
			
			if ( line.startsWith("[Term]") ) {
				ont = new GOterm();	
			}
			if (ont!=null && line.startsWith("id: ")) {
				String id = extract_id(line);
				ont.setId(id);
			}
			if (ont!=null && line.startsWith("alt_id: ")) {
				String alt_id = extract_id(line);
				ont.addAlt_id(alt_id);
			}
			if (ont!=null && line.startsWith("name:")) {
				String name = extract_name(line);   
				ont.setName(name);
			}
			if (ont!=null && line.startsWith("namespace")) {
				String namespace = extract_name(line);
				ont.setNamespace(namespace);
			}
			if (ont!=null && line.startsWith("def: ")) {
				String definition = extract_def(line);    
				ont.setDefinition(definition);
			}
			if (ont!=null && line.startsWith("is_a: ")) {	
				String is_a = extract_id(line);
				ont.addIs_a(is_a);
			}
			if (ont!=null && line.equals("")) {
				map.put(ont.getId(), ont);
				ont = null;
			}

		}
		scanner.close();
		
		Set<String> goids = map.keySet();
		
		//Insert data into table "go_parent"
		for (String id : goids) {
				
			for (String is_a : map.get(id).getIs_a())
			
				insert_go_parent(id,is_a);
				
			if (!map.get(id).getAlt_id().isEmpty()) {
			
				for (String alt_id : map.get(id).getAlt_id()) {
						
					for (String is_a : map.get(id).getIs_a())
						
						insert_go_parent(alt_id,is_a);
				}
			}
			
		}
		
		//Insert data into table "go"
		for (String id : goids) {
			
			insert_go(id, map.get(id).getName(), map.get(id).getDefinition(), map.get(id).getNamespace());
			
			if (!map.get(id).getAlt_id().isEmpty()) {
				
				for (String alt_id : map.get(id).getAlt_id()) {
						
					insert_go(alt_id, map.get(id).getName(), map.get(id).getDefinition(), map.get(id).getNamespace());
				}
			}
		}

	}
	
	public static String extract_id(String line) {
		
		return (line.split("\\s+"))[1];
	}
	
	public static String extract_name(String line) {
		
		return (line.split(": "))[1];
	}
	
	public static String extract_def(String line) {
		
		String[] tokens = line.split(": \"");		
		String[] words = tokens[1].split(" ");
		String def = words[0];
		String defFinal = "";
		
		for (int i=1;i<words.length;i++) {
			
			if (words[i].startsWith("["))
				break;
			def = def + " " + words[i];	
			defFinal = def.replaceAll("\"", "");
		}
		return defFinal;
	}
	
	public static void insert_go(String id,String name,String def,String namespace) {
		
		String SQL = "INSERT INTO go VALUES(?,?,?,?)";
		
		try (Connection conn = ConnectDB.connect();
				PreparedStatement pstmt = conn.prepareStatement(SQL)) {
			pstmt.setString(1, id);
			pstmt.setString(2, name);
			pstmt.setString(3, def);
			pstmt.setString(4, namespace);
			
			pstmt.executeUpdate();
			conn.close();

		} catch (SQLException ex) {
			System.out.println(ex.getMessage());
		}
		
	}
	
	public static void insert_go_parent(String id,String id_parent) {
		
		String SQL = "INSERT INTO go_parent VALUES(?,?)";
		
		try (Connection conn = ConnectDB.connect();
				PreparedStatement pstmt = conn.prepareStatement(SQL)) {
			pstmt.setString(1, id);
			pstmt.setString(2, id_parent);
			
			pstmt.executeUpdate();
			conn.close();

		} catch (SQLException ex) {
			System.out.println(ex.getMessage());
		}
		
	}

}
