/*
 * ColorExtractor
 *
 * @ Project : TMCMR 
 * @ File Name : ColorExtractor.java
 * @ Date : 28.03.2013
 *
 * @ Copyright (C) 2013 Klaus-Peter Hammerschmidt
 * 
 * This program is free software; 
 * you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; 
 * either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; 
 * if not, see <http://www.gnu.org/licenses/>.
 */
package togos.minecraft.maprend.texture;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.imageio.ImageIO;

import togos.minecraft.maprend.Color;

/**
 * @author Klaus-Peter Hammerschmidt
 * 
 */
public final class ColorExtractor
{

	private static final int DEFAULT_COLOR = 0xFFFF00FF;
	private static final int ID_MASK = 0xFFFF;
	private static final int DATA_MASK = 0xF;
	private static final int ALPHA_CUTOFF = 0x20;

	private static int parseInt( String s ) {
		// Integer.parseInt pukes if the number is too big for a signed integer!
		// So use Long.parseLong and cast, instead.
		if( s.startsWith( "0x" ) )
			return (int) Long.parseLong( s.substring( 2 ), 16 );
		return (int) Long.parseLong( s );
	}

	private static int getAverageColor( String filename ) throws IOException {
		BufferedImage image = ImageIO.read( new File( getPath( filename ) ) );
		int a = 0, r = 0, g = 0, b = 0;
		int nPix = image.getWidth()*image.getHeight();
		int aPix = 0;
		for( int y = image.getHeight()-1; y>=0; --y ) {
			for( int x = image.getWidth()-1; x>=0; --x ) {
				int color = image.getRGB( x, y );
				int alpha = Color.component( color, 24 );
				a += alpha;
				if( alpha>ALPHA_CUTOFF ) {
					aPix++;
					r += Color.component( color, 16 );
					g += Color.component( color, 8 );
					b += Color.component( color, 0 );
				}
			}
		}
		return Color.color( a/nPix, r/aPix, g/aPix, b/aPix );
	}

	private static int getColorRecursive( String[] args, int index, ColorInfo res ) throws ColorDescriptionException {
		if( args.length<=index ) {
			throw new IllegalArgumentException();
		}

		if( args[index].equals( "average" ) ) {
			if( args.length<=index+1 ) {
				throw new ColorDescriptionException( "Mode average expects an additional argument" );
			}
			try {
				res.color = getAverageColor( args[index+1] );
				return index+2;
			} catch ( FileNotFoundException e ) {
				throw new ColorDescriptionException( "Could not find image"+args[index+1] );
			} catch ( IOException e ) {
				throw new ColorDescriptionException( "Could not load image"+args[index+1] );
			}
		} else if( args[index].equals( "fixed" ) ) {
			if( args.length<=index+1 ) {
				throw new ColorDescriptionException( "Mode fixed expects an additional argument" );
			}
			res.color = parseInt( args[index+1] );
			return index+2;
		} else if( args[index].equals( "multiply" ) ) {
			if( args.length<=index+2 ) {
				throw new ColorDescriptionException( "Mode multiply expects additional arguments" );
			}
			ColorInfo infoA = new ColorInfo();
			int nextIndex = getColorRecursive( args, index+1, infoA );
			ColorInfo infoB = new ColorInfo();
			nextIndex = getColorRecursive( args, nextIndex, infoB );
			res.color = Color.multiplySolid( infoA.color, infoB.color );
			res.biomeColor = infoA.biomeColor;
			return nextIndex;
		}

		throw new ColorDescriptionException( "Unrecognized mode "+args[index] );
	}

	private static ColorInfo getColorFromArgs( String[] args ) throws ColorDescriptionException {
		ColorInfo res = new ColorInfo();
		getColorRecursive( args, 2, res );
		return res;
	}

	private static String composeLine( int id, int data, int color, String name ) {
		StringBuilder res = new StringBuilder();

		res.append( String.format( "0x%04X", id&ID_MASK ) );
		if( data>=0 ) {
			res.append( String.format( ":0x%01X", data&DATA_MASK ) );
		}
		res.append( '\t' );
		res.append( String.format( "0x%08X", color ) );
		res.append( "\t# " );
		res.append( name );
		res.append( '\n' );

		return res.toString();
	}

	private static final String getPath( String filename ) {
		return "data/textures/blocks/"+filename;
	}

	public static void main( String[] args ) {
		if( args.length!=2 ) {
			System.err.println( "Usage: input output" );
			return;
		}

		File infile = new File( args[0] );
		if( !infile.exists() ) {
			System.err.println( "Input file \""+args[0]+"\"not found!" );
			return;
		}

		BufferedReader in = null;
		BufferedWriter out = null;
		try {
			in = new BufferedReader( new FileReader( infile ) );

			out = new BufferedWriter( new FileWriter( new File( args[1] ) ) );
			out.write( "# This file defines colors for blocks!\n" +
					"# You can use your own color map using the -color-map <file> argument.\n" +
					"#\n" +
					"# The format is block ID, optionally colon and metadata, tab, color, optionally followed by another tab, a pound, and a comment.\n" +
					"# Tabs are important; don't use spaces or commas!\n" +
					"#\n" +
					"# Empty lines and lines starting with # are ignored, too.\n" +
					"#\n" +
					"# 'default' must appear before other block ID -> color mappings\n" +
					"# Any block with an ID not specifically mapped to a color after the default\n" +
					"# mapping will be colored with the default color.\n\n");

			out.write( String.format( "default\t0x%08X\n", DEFAULT_COLOR ) );

			String line;
			int lineNumber = 0;
			while ( (line = in.readLine())!=null ) {
				lineNumber++;
				line = line.trim();
				if( line.isEmpty() )
					continue;
				if( line.startsWith( "#" ) )
					continue;

				String[] v = line.split( "\t" );
				if( v.length<2 ) {
					System.err.println( "Illegal line "+lineNumber+": "+line );
					continue;
				}
				if( v.length<3||v[2].startsWith( "#" ) ) {
					System.out.println( "Ignoring line "+lineNumber+": "+line );
					continue;
				}

				String[] v2 = v[0].split( ":" );
				int id = 0, data = -1;
				try {
					id = parseInt( v2[0] );
					data = v2.length==2 ? parseInt( v2[1] ) : -1;
				} catch ( NumberFormatException e ) {
					System.err.println( "Error in line "+lineNumber+": "+line+" - "+e.getMessage() );
					continue;
				}
				ColorInfo color = null;
				try {
					color = getColorFromArgs( v );
				} catch ( ColorDescriptionException e ) {
					System.out.println( "Error in line "+lineNumber+": "+e.getMessage() );
					continue;
				}

				out.write( composeLine( id, data, color.color, v[1] ) );

			}

		} catch ( IOException e ) {
			e.printStackTrace();
		} finally {
			if( in!=null ) {
				try {
					in.close();
				} catch ( IOException e ) {
				}
			}
			if( out!=null ) {
				try {
					out.close();
				} catch ( IOException e ) {
				}
			}
		}

	}

	private static class ColorInfo
	{

		// not used yet
		// static final int BIOME_COLOR_NONE = 0;
		// static final int BIOME_COLOR_GRASS = 1;
		// static final int BIOME_COLOR_FOLIAGE = 2;
		// static final int BIOME_COLOR_WATER = 3;

		int color;
		int biomeColor;
	}

	private static class ColorDescriptionException extends Exception
	{
		private static final long serialVersionUID = 1L;

		ColorDescriptionException(String message) {
			super( message );
		}

	}
}
