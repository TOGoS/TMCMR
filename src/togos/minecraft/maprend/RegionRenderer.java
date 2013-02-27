package togos.minecraft.maprend;

import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.imageio.ImageIO;

import org.jnbt.ByteArrayTag;
import org.jnbt.ByteTag;
import org.jnbt.CompoundTag;
import org.jnbt.ListTag;
import org.jnbt.NBTInputStream;
import org.jnbt.Tag;

import togos.minecraft.maprend.RegionMap.Region;
import togos.minecraft.maprend.io.ContentStore;
import togos.minecraft.maprend.io.RegionFile;

public class RegionRenderer
{
	public static final short BASE_HEIGHT = 64;
	
	public final boolean debug;
	public final ColorMap colorMap;
	public final int air16Color; // Color of 16 air blocks stacked
	/**
	 * Alpha below which blocks are considered transparent for purposes of shading
	 * (i.e. blocks with alpha < this will not be shaded, but blocks below them will be)
	 */
	private int shadeOpacityCutoff = 0x20; 
	
	public RegionRenderer( ColorMap colorMap, boolean debug ) {
		if( colorMap == null ) throw new RuntimeException("colorMap cannot be null");
		this.colorMap = colorMap;
		this.air16Color = Color.overlay( 0, colorMap.getColor( 0 ), 16 );
		this.debug = debug;
	}
	
	/**
	 * @param arr the source array
	 * @param index the index of the desired 4 bits
	 * @return the desired 4 bits as the lower bits of a byte
	 */
	protected static byte nibble4( byte[] arr, int index ) {
		return (byte) (index%2==0 ? arr[index/2]&0x0F : (arr[index/2]>>4)&0x0F);
	}
	
	/**
	 * @param levelTag
	 * @param maxSectionCount
	 * @param sectionBlocks block ids for non-empty sections will be written to sectionBlocks[sectionIndex][blockIndex]
	 * @param sectionsUsed sectionsUsed[sectionIndex] will be set to true for non-empty sections
	 */
	protected static void loadChunkData( CompoundTag levelTag, int maxSectionCount, short[][] sectionBlocks, byte[][] sectionData, boolean[] sectionsUsed ) {
		for( int i=0; i<maxSectionCount; ++i ) {
			sectionsUsed[i] = false;
		}
		
		for( Tag t : ((ListTag)levelTag.getValue().get("Sections")).getValue() ) {
			CompoundTag sectionInfo = (CompoundTag)t;
			int sectionIndex = ((ByteTag)sectionInfo.getValue().get("Y")).getValue().intValue();
			byte[] blockIdsLow = ((ByteArrayTag)sectionInfo.getValue().get("Blocks")).getValue();
			short[] destSectionBlocks = sectionBlocks[sectionIndex];
			byte[] blockData = ((ByteArrayTag) sectionInfo.getValue().get("Data")).getValue();
			byte[] destSectionData = sectionData[sectionIndex];
			sectionsUsed[sectionIndex] = true;
			for( int y=0; y<16; ++y ) {
				for( int z=0; z<16; ++z ) {
					for( int x=0; x<16; ++x ) {
						int index = y*256+z*16+x;
						short blockType = (short) (blockIdsLow[index]&0xFF);
						// TODO: Add in value from 'Add' << 8

						destSectionBlocks[index] = blockType;
						destSectionData[index] = nibble4( blockData, index );
					}
				}
			}
		}
	}
		
	//// Handy color-manipulation functions ////
	

	protected static void demultiplyAlpha( int[] color ) {
		for( int i=color.length-1; i>=0; --i ) color[i] = Color.demultiplyAlpha(color[i]);
	}
	
	protected void shade( short[] height, int[] color ) {
		int width=512, depth=512;

		int idx = 0;
		for( int z=0; z<depth; ++z ) {
			for( int x=0; x<width; ++x, ++idx ) {
				float dyx, dyz;
				
				if( color[idx] == 0 ) continue;
				
				if(      x == 0       ) dyx = height[idx+1]-height[idx];
				else if( x == width-1 ) dyx = height[idx]-height[idx-1];
				else dyx = (height[idx+1]-height[idx-1]) * 2;

				if(      z == 0       ) dyz = height[idx+width]-height[idx];
				else if( z == depth-1 ) dyz = height[idx]-height[idx-width];
				else dyz = (height[idx+width]-height[idx-width]) * 2;
				
				float shade = dyx+dyz;
				if( shade >  10 ) shade =  10;
				if( shade < -10 ) shade = -10;
				
				shade += (height[idx] - BASE_HEIGHT) / 7.0;
				
				color[idx] = Color.shade( color[idx], (int)(shade*8) );
			}
		}
	}
	
	//// Rendering ////
	
	/**
	 * @param rf
	 * @param colors color data will be written here
	 * @param heights height data (height of top of topmost non-transparent block) will be written here
	 */
	protected void preRender( RegionFile rf, int[] colors, short[] heights ) {
		int maxSectionCount = 16;
		short[][] sectionBlocks = new short[maxSectionCount][16*16*16];
		byte[][] sectionData = new byte[maxSectionCount][16*16*16];
		boolean[] usedSections = new boolean[maxSectionCount]; 
		
		for( int cz=0; cz<32; ++cz ) {
			for( int cx=0; cx<32; ++cx ) {				
				DataInputStream cis = rf.getChunkDataInputStream(cx,cz);
				if( cis == null ) continue;
				NBTInputStream nis = null;
				try {
					nis = new NBTInputStream(cis);
					CompoundTag rootTag = (CompoundTag)nis.readTag();
					CompoundTag levelTag = (CompoundTag)rootTag.getValue().get("Level");
					loadChunkData( levelTag, maxSectionCount, sectionBlocks, sectionData, usedSections );
					
					for( int z=0; z<16; ++z ) {
						for( int x=0; x<16; ++x ) {
							int pixelColor = 0;
							short pixelHeight = 0;
							
							for( int s=0; s<maxSectionCount; ++s ) {
								if( usedSections[s] ) {
									short[] blocks = sectionBlocks[s];
									byte[] data = sectionData[s];
									
									for( int y=0; y<16; ++y ) {
										final short absY = (short)(s*16+y+1);
										// TODO: height-based shading?
										
										final short blockId = blocks[y*256+z*16+x];
										final int blockColor = colorMap.getColor( blockId&0xFFFF, data[y*256+z*16+x] );
										pixelColor = Color.overlay( pixelColor, blockColor );
										
										if( Color.alpha(blockColor) >= shadeOpacityCutoff  ) pixelHeight = absY;
									}
								} else {
									pixelColor = Color.overlay( pixelColor, air16Color );
								}
							}

							final int dIdx = 512*(cz*16+z)+16*cx+x; 
							colors[dIdx] = pixelColor;
							heights[dIdx] = pixelHeight;
						}
					}
					
				} catch( IOException e ) {
					System.err.println("Error reading chunk from "+rf.getFile()+" at "+cx+","+cz);
					e.printStackTrace();
				} finally {
					if( nis!=null ) {
						try {
							nis.close();
						} catch ( IOException e ) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
	
	public BufferedImage render( RegionFile rf ) {
		int width=512, depth=512;
		
		int[] surfaceColor  = new int[width*depth];
		short[] surfaceHeight = new short[width*depth];
		
		preRender( rf, surfaceColor, surfaceHeight );
		demultiplyAlpha( surfaceColor );
		shade( surfaceHeight, surfaceColor );
		
		BufferedImage bi = new BufferedImage( width, depth, BufferedImage.TYPE_INT_ARGB );
		
		for( int z=0; z<depth; ++z ) {
			bi.setRGB( 0, z, width, 1, surfaceColor, width*z, width );
		}

		return bi;
	}
	
	protected static String pad( String v, int targetLength ) {
		while( v.length() < targetLength ) v = " "+v;
		return v;
	}
	
	protected static String pad( int v, int targetLength ) {
		return pad( ""+v, targetLength );
	}
		
	public void renderAll( RegionMap rm, String outputDirname, boolean force ) {
		Region[] regions = rm.xzMap();
		
		File outputDir = new File(outputDirname);
		if( !outputDir.exists() ) outputDir.mkdirs();
		
		if( regions.length == 0 ) {
			System.err.println("Warning: no regions found!");
		}
		
		for( int i=0; i<regions.length; ++i ) {
			Region r = regions[i];
			if( r == null ) continue;
			
			if( debug ) System.err.print("Region "+pad(r.rx, 4)+", "+pad(r.rz, 4)+"...");
			
			String imageFilename = "tile."+r.rx+"."+r.rz+".png";
			File imageFile = r.imageFile = new File( outputDirname+"/"+imageFilename );
			
			if( imageFile.exists() ) {
				if( !force && imageFile.lastModified() > r.regionFile.lastModified() ) {
					if( debug ) System.err.println("image already up-to-date");
					continue;
				}
				imageFile.delete();
			}
			if( debug ) System.err.println("generating "+imageFilename+"...");
			
			BufferedImage bi = render( new RegionFile( r.regionFile ) );
			
			try {
		        ImageIO.write(bi, "png", imageFile);
	        } catch( IOException e ) {
	        	System.err.println("Error writing PNG to "+imageFile);
		        e.printStackTrace();
	        }
		}
	}
	
	public void createTileHtml( RegionMap rm, String outputDirname ) {
		if( debug ) System.err.println("Writing HTML tiles...");
		try {
			Writer w = new OutputStreamWriter(new FileOutputStream(new File(outputDirname+"/tiles.html")));
			w.write("<html><body style=\"background:black\"><table border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\n");
			
			for( int z=rm.minZ; z<=rm.maxZ; ++z ) {
				w.write("<tr>");
				for( int x=rm.minX; x<=rm.maxX; ++x ) {
					w.write("<td>");
					String imageFilename = "tile."+x+"."+z+".png";
					File imageFile = new File( outputDirname+"/"+imageFilename );
					if( imageFile.exists() ) {
						w.write("<img src=\""+imageFilename+"\"/>");
					}
					w.write("</td>");
				}
				w.write("</tr>\n");
			}
			
			w.write("</table></body></html>");
			w.close();
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}
	
	public void createImageTree( RegionMap rm ) {
		if( debug ) System.err.println("Composing image tree...");
		ImageTreeComposer itc = new ImageTreeComposer(new ContentStore());
		System.out.println( itc.compose( rm ) );
	}
	
	public static final String USAGE =
		"Usage: TMCMR <region-dir> -o <output-dir> [-f]\n" +
		"  -f     ; force re-render even when images are newer than regions\n" +
		"  -debug ; be chatty\n" +
		"  -color-map <file>  ; load a custom color map from the specified file\n" +
		"  -create-image-tree ; generate a PicGrid-compatible image tree\n" +
		"\n" +
		"Compound image tree blobs will be written to ~/.ccouch/data/tmcmr/\n" +
		"Compound images can then be rendered with PicGrid.";
	
	protected static final boolean booleanValue( Boolean b, boolean defalt ) {
		return b == null ? defalt : b.booleanValue();
	}
	
	public static void main( String[] args ) throws Exception {
		String regionDirname = null;
		String outputDirname = null;
		boolean force = false;
		boolean debug = false;
		Boolean createTileHtml = null;
		Boolean createImageTree = null;
		String colorMapFile = null;
		
		for( int i=0; i<args.length; ++i ) {
			if( args[i].charAt(0) != '-' ) {
				if( regionDirname == null ) regionDirname = args[i];
				else if( outputDirname == null ) outputDirname = args[i];
				else {
					System.err.println("Unrecognised argument: "+args[i]);
				}
			} else if( "-o".equals(args[i]) ) {
				outputDirname = args[++i];
			} else if( "-f".equals(args[i]) ) {
				force = true;
			} else if( "-debug".equals(args[i]) ) {
				debug = true;
			} else if( "-create-tile-html".equals(args[i]) ) {
				createTileHtml = Boolean.TRUE;
			} else if( "-create-image-tree".equals(args[i]) ) {
				createImageTree = Boolean.TRUE;
			} else if( "-color-map".equals(args[i]) ) {
				colorMapFile = args[++i];
			} else {
				System.err.println("Unrecognised argument: "+args[i]);
				System.err.println(USAGE);
				System.exit(1);
			}
		}
		
		if( regionDirname == null ) {
			System.err.println("Region directory unspecified.");
			System.err.println(USAGE);
			System.exit(1);
		}
		if( outputDirname == null ) {
			System.err.println("Output directory unspecified.");
			System.err.println(USAGE);
			System.exit(1);
		}
		
		ColorMap colorMap = new ColorMap();
		if( colorMapFile==null ) {
			colorMap.loadDefault();
		} else {
			colorMap.load( new File( colorMapFile ) );
		}

		File regionDir = new File(regionDirname);
		if( createTileHtml == null && !regionDir.isDirectory() ) createTileHtml = Boolean.FALSE;
		
		if( createTileHtml == null ) createTileHtml = Boolean.TRUE;
		if( createImageTree == null ) createImageTree = Boolean.FALSE;
		
		RegionMap rm = RegionMap.load( regionDir );
		RegionRenderer rr = new RegionRenderer( colorMap, debug );
		rr.renderAll( rm, outputDirname, force );
		if( createTileHtml.booleanValue() ) rr.createTileHtml( rm, outputDirname );
		if( createImageTree.booleanValue() ) rr.createImageTree( rm );
	}
}
