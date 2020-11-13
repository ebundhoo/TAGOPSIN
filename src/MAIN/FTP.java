package main;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * This class downloads files from FTP servers.
 *  
 * @author 	Eshan Bundhoo, University of Mauritius
 * @since	2019-02-19
 * @version	1.0
 *
 */

public class FTP {
	
	private static final int BUFFER_SIZE = 536870912;

	public static void downloadFile(URL url, String pathToFile) throws IOException {
		
		try {
			URLConnection conn = url.openConnection();
			InputStream istream = conn.getInputStream();
			
			FileOutputStream ostream = new FileOutputStream(pathToFile);
			
			byte[] buffer = new byte[BUFFER_SIZE];
			int bytesRead;
			while ((bytesRead = istream.read(buffer)) > 0)
				ostream.write(buffer,0,bytesRead);
				
			ostream.close();
			istream.close();
		} catch (Exception ex) {
			System.out.println("Download failed. Please check Internet connection and try again.");	
			System.exit(0);
        }

	}

}
