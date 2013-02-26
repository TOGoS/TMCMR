package togos.minecraft.maprend;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ColorMap
{
	public static final int INDEX_MASK = 0xFFFF;
	public static final int SIZE = INDEX_MASK+1;
	
	private BlockColors[] colors;

	protected int parseInt( String s ) {
		// Integer.parseInt pukes if the number is too big for a signed integer!
		// So use Long.parseLong and cast, instead.
		if( s.startsWith("0x") ) return (int)Long.parseLong(s.substring(2), 16);
		return (int)Long.parseLong(s);
	}
	
	
	
	public void load( BufferedReader s ) throws IOException {
		colors = new BlockColors[SIZE];
		String line;
		while( (line = s.readLine()) != null ) {
			if( line.trim().isEmpty() ) continue;
			if( line.trim().startsWith("#") ) continue;
			
			String[] v = line.split("\t", 3);
			int color = parseInt(v[1]);
			if( "default".equals(v[0]) ) {
				for( int i=0; i<colors.length; ++i ) colors[i] = new BlockColors( color );
			} else {
				String[] v2 = v[0].split( ":", 2 );
				int blockId = parseInt( v2[0] );
				int blockData = -1;
				if( v2.length>1 ) {
					blockData = parseInt( v2[1] );
				}
				if( blockData<0 ) {
					colors[blockId&INDEX_MASK].setBaseColor( color );
				} else {
					colors[blockId&INDEX_MASK].setSubColor( blockData, color );
				}
			}
		}
	}
	
	public void load( File f ) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(f));
		try {
			load(br);
		} finally {
			br.close();
		}
	}
	
	public void loadDefault() {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(ColorMap.class.getResourceAsStream("block-colors.txt")));
			try {
				load(br);
			} finally {
				br.close();
			}
		} catch( IOException e ) {
			throw new RuntimeException("Error loading built-in color map", e);
		}
	}
	
    public int getColor(int block, int data) {
        return colors[block].getColor(data);
    }
    
    public int getColor(int block) {
        return getColor(block, -1);
    }
	
	private class BlockColors
	{

		public static final int SUB_COLORS = 16;

		private int baseColor;

		private int[] subColors;
		private boolean[] hasSubColors;

		private BlockColors(int baseColor) {
			this.baseColor = baseColor;
		}

		private int getColor( int metaData ) {
			if( subColors==null||metaData<0||metaData>=subColors.length ) {
				return baseColor;
			}
			return hasSubColors[metaData] ? subColors[metaData] : baseColor;
		}

		private void setSubColor( int metaData, int color ) {
			if( subColors==null ) {
				hasSubColors = new boolean[SUB_COLORS];
				subColors = new int[SUB_COLORS];
			}
			hasSubColors[metaData] = true;
			subColors[metaData] = color;
		}

		private void setBaseColor( int color ) {
			baseColor = color;
		}
	}
}
