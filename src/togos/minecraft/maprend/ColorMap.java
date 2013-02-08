package togos.minecraft.maprend;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ColorMap
{
	public static final int INDEX_MASK = 0xFFFF;
	public static final int SIZE = INDEX_MASK+1;

	protected static int parseInt( String s ) {
		// Integer.parseInt pukes if the number is too big for a signed integer!
		// So use Long.parseLong and cast, instead.
		if( s.startsWith("0x") ) return (int)Long.parseLong(s.substring(2), 16);
		return (int)Long.parseLong(s);
	}
	
	public static int[] load( BufferedReader s ) throws IOException {
		int[] map = new int[SIZE];
		String line;
		while( (line = s.readLine()) != null ) {
			if( line.trim().isEmpty() ) continue;
			if( line.trim().startsWith("#") ) continue;
			
			String[] v = line.split("\t", 3);
			int color = parseInt(v[1]);
			if( "default".equals(v[0]) ) {
				for( int i=0; i<map.length; ++i ) map[i] = color;
			} else {
				int blockId = parseInt(v[0]);
				map[blockId&INDEX_MASK] = color;
			}
		}
		return map;
	}
	
	public static int[] getDefaultColorMap() {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(ColorMap.class.getResourceAsStream("block-colors.txt")));
			try {
				return load(br);
			} finally {
				br.close();
			}
		} catch( IOException e ) {
			throw new RuntimeException("Error loading built-in color map", e);
		}
	}
}
