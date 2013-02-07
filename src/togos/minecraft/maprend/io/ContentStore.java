package togos.minecraft.maprend.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.bitpedia.util.Base32;

public class ContentStore
{
	protected MessageDigest getDigestor() {
        try {
	        return MessageDigest.getInstance("SHA-1");
        } catch( NoSuchAlgorithmException e ) {
        	throw new RuntimeException(e);
        }
	}
	
	public String rootStoreDir = System.getProperty("user.home") + "/.ccouch/data/tmcmr";
	
	protected File storeFileForUrn( String urn ) {
		if( urn.startsWith("urn:sha1:") ) {
			String base32 = urn.substring(9);
			return new File(rootStoreDir + "/" + base32.substring(0,2) + "/" + base32);
		} else {
			throw new RuntimeException("Don't know where to store "+urn);
		}
	}
	
	protected static void mkParentDirs( File f ) {
		File p = f.getParentFile();
		if( p != null && !p.exists() ) p.mkdirs();
	}
	
	protected static File tempDest( File dest ) {
		return new File( dest.getParent()+"/."+dest.getName()+".temp"+System.currentTimeMillis());
	}
	
	protected static void copy( File src, File dest ) throws IOException {
		File tempDest = tempDest(dest);
		
		FileInputStream fis = new FileInputStream(src);
		FileOutputStream fos = new FileOutputStream(tempDest);
		byte[] buffer = new byte[65536];
		int r;
		while( (r = fis.read(buffer)) > 0 ) {
			fos.write( buffer, 0, r );
		}
		fis.close();
		fos.close();
		
		tempDest.setLastModified(src.lastModified());
		tempDest.setReadOnly();
		tempDest.renameTo(dest);
	}
	
	protected static void write( byte[] data, File dest ) throws IOException {
		File tempDest = tempDest(dest);
		FileOutputStream fos = new FileOutputStream(tempDest);
		fos.write( data );
		fos.close();
		
		tempDest.setReadOnly();
		tempDest.renameTo(dest);
	}
	
	public String store( File f ) throws IOException {
		MessageDigest md = getDigestor();
		FileInputStream fis = new FileInputStream(f);
		byte[] buffer = new byte[65536];
		int r;
		while( (r = fis.read(buffer)) > 0 ) {
	        md.update(buffer, 0, r);			
		}
		fis.close();
		
		String urn = "urn:sha1:"+Base32.encode(md.digest());
		File dest = storeFileForUrn(urn);
		if( !dest.exists() ) {
			mkParentDirs(dest);
			copy(f, dest);
		}
		return urn;
	}
	
	public String store( byte[] data ) throws IOException {
		MessageDigest md = getDigestor();
        md.update(data);
        
        String urn = "urn:sha1:"+Base32.encode(md.digest());
		File dest = storeFileForUrn(urn);
		if( !dest.exists() ) {
			mkParentDirs(dest);
			write(data, dest);
		}
		return urn;
	}
	
	public String store( String content ) throws IOException {
		if( !content.endsWith("\n") ) content += "\n";
		byte[] data;
		try {
	        data = content.getBytes("UTF-8");
        } catch( UnsupportedEncodingException e ) {
        	throw new RuntimeException(e);
        }
		return store(data);
	}
}
