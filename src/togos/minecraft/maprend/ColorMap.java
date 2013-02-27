package togos.minecraft.maprend;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

public final class ColorMap
{
	public static final int INDEX_MASK = 0xFFFF;
	public static final int SIZE = INDEX_MASK+1;
	
	public final BlockColors[] colors;
	public ColorMap( BlockColors[] colors ) {
		this.colors = colors;
	}
	
	protected static int parseInt( String s ) {
		// Integer.parseInt pukes if the number is too big for a signed integer!
		// So use Long.parseLong and cast, instead.
		if( s.startsWith("0x") ) return (int)Long.parseLong(s.substring(2), 16);
		return (int)Long.parseLong(s);
	}
	
	public static ColorMap load( BufferedReader s ) throws IOException {
		BlockColors[] colors = new BlockColors[SIZE];
		for( int i=0; i<SIZE; ++i ) {
			colors[i] = new BlockColors(0);
		}
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
				int blockData = v2.length == 2 ? parseInt( v2[1] ) : -1;
				if( blockData < 0 ) {
					colors[blockId&INDEX_MASK].setBaseColor( color );
				} else {
					colors[blockId&INDEX_MASK].setSubColor( blockData, color );
				}
			}
		}
		return new ColorMap(colors);
	}
	
	public static ColorMap load( File f ) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(f));
		try {
			return load(br);
		} finally {
			br.close();
		}
	}
	
	public static ColorMap loadDefault() {
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
	
	public final int getColor(int block, int data) {
		return colors[block].getColor(data);
	}
	
	public final int getColor(int block) {
		return getColor(block, -1);
	}
	
	/**
	 * Holds the default color for a block and, optionally,
	 * a color for specific 'block data' values.
	 */
	public static class BlockColors {
		public static final int SUB_COLOR_COUNT = 0x10;
		
		private int baseColor;

		private int[] subColors;
		private boolean[] hasSubColors;

		private BlockColors(int baseColor) {
			this.baseColor = baseColor;
		}
		
		/**
		 * Returns the color specified for this block for the given blockData value,
		 * or the block's base color if no color has been specified specifically for
		 * that blockData value.  
		 */
		private final int getColor( int blockData ) {
			return (subColors==null || blockData < 0 || blockData >= subColors.length) ?
				baseColor :
				hasSubColors[blockData] ? subColors[blockData] : baseColor;
		}
		
		private void setSubColor( int blockData, int color ) {
			if( blockData < 0 || blockData >= SUB_COLOR_COUNT ) {
				throw new RuntimeException("Block data value out of bounds: "+blockData);
			}
			if( subColors == null ) {
				hasSubColors = new boolean[SUB_COLOR_COUNT];
				subColors = new int[SUB_COLOR_COUNT];
			}
			hasSubColors[blockData] = true;
			subColors[blockData] = color;
		}
		
		private void setBaseColor( int color ) {
			baseColor = color;
		}
	}
}
