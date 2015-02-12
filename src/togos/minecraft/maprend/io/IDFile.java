package togos.minecraft.maprend.io;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

import org.bitpedia.util.Base32;

public class IDFile
{
	public static void main(String[] args) {
		if( args.length != 1 ) {
			System.err.println("Error: Expected usage: IDFile <file>");
			System.exit(1);
		}
		
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			FileInputStream fis = new FileInputStream(new File(args[0]));
			byte[] buffer = new byte[65536];
			int r;
			while( (r = fis.read(buffer)) > 0 ) {
		        md.update(buffer, 0, r);			
			}
			fis.close();
			
			System.out.println("urn:sha1:"+Base32.encode(md.digest()));
		} catch( Exception e ) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
