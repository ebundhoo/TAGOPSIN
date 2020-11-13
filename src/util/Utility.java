package util;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * This class contains general utility functions used by TAGOPSIN.
 * 
 * @author 	Anisah W. Ghoorah, University of Mauritius
 * @since	2019-02-19
 * @version	1.0
 *
 */

public class Utility {
	
	public static boolean isIDofInterest(int[] array, int target) {
		
		if (Arrays.binarySearch(array, target)>=0)
			return true;
		
		return false;
		
	}
	
	public static boolean isIDofInterest(String[] array, String target) {
		
		if (Arrays.binarySearch(array, target)>=0)
			return true;
		
		return false;
		
	}
	
	public static List<String> getFilesInDirectory(String filename) throws IOException {
		
		List<String> list = new LinkedList<>();
		
	    Path path = Paths.get(filename);
	    
	    DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path);
	    
	    for (Path p : directoryStream)
	    		list.add(p.toAbsolutePath().toString());
	    
		return list;
	}

}
