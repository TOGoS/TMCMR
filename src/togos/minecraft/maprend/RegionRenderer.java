package togos.minecraft.maprend;

import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

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
	static class Timer {
		public long regionLoading;
		public long preRendering;
		public long postProcessing;
		public long imageSaving;
		public long total;
		
		public int regionCount;
		public int sectionCount;
		
		protected String formatTime( String name, long millis ) {
			return String.format("%20s: % 8d   % 8.2f   % 8.4f", name, millis, millis/(double)regionCount, millis/(double)sectionCount);
		}
	}
	
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
		this.air16Color = Color.overlay( 0, colorMap.getColor(0), 16 );
		this.debug = debug;
	}
	
	/**
	 * Extract a 4-bit integer from a byte in an array, where the first nybble
	 * in each byte (even nybble indexes) occupies the lower 4 bits and the second
	 * (odd nybble indexes) occupies the high bits.
	 * 
	 * @param arr the source array
	 * @param index the index (in nybbles) of the desired 4 bits
	 * @return the desired 4 bits as the lower bits of a byte
	 */
	protected static final byte nybble( byte[] arr, int index ) {
		return (byte)((index % 2 == 0 ? arr[index/2] : (arr[index/2]>>4))&0x0F);
	}
	
	/**
	 * @param levelTag
	 * @param maxSectionCount
	 * @param sectionBlockIds block IDs for non-empty sections will be written to sectionBlockIds[sectionIndex][blockIndex]
	 * @param sectionBlockData block data for non-empty sections will be written to sectionBlockData[sectionIndex][blockIndex]
	 * @param sectionsUsed sectionsUsed[sectionIndex] will be set to true for non-empty sections
	 */
	protected static void loadChunkData( CompoundTag levelTag, int maxSectionCount, short[][] sectionBlockIds, byte[][] sectionBlockData, boolean[] sectionsUsed ) {
		for( int i=0; i<maxSectionCount; ++i ) {
			sectionsUsed[i] = false;
		}
		
		for( Tag t : ((ListTag)levelTag.getValue().get("Sections")).getValue() ) {
			CompoundTag sectionInfo = (CompoundTag)t;
			int sectionIndex = ((ByteTag)sectionInfo.getValue().get("Y")).getValue().intValue();
			byte[]  blockIdsLow = ((ByteArrayTag)sectionInfo.getValue().get("Blocks")).getValue();
			byte[]  blockData   = ((ByteArrayTag)sectionInfo.getValue().get("Data")).getValue();
			short[] destSectionBlockIds = sectionBlockIds[sectionIndex];
			byte[]  destSectionData = sectionBlockData[sectionIndex];
			sectionsUsed[sectionIndex] = true;
			for( int y=0; y<16; ++y ) {
				for( int z=0; z<16; ++z ) {
					for( int x=0; x<16; ++x ) {
						int index = y*256+z*16+x;
						short blockType = (short) (blockIdsLow[index]&0xFF);
						// TODO: Add in value from 'Add' << 8
						
						destSectionBlockIds[index] = blockType;
						destSectionData[index] = nybble( blockData, index );
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
	
	Timer timer = new Timer();
	protected long startTime;
	protected void resetInterval() { startTime = System.currentTimeMillis(); }
	protected long getInterval() { return System.currentTimeMillis() - startTime; }
	
	/**
	 * Load color and height data from a region.
	 * @param rf
	 * @param colors color data will be written here
	 * @param heights height data (height of top of topmost non-transparent block) will be written here
	 */
	protected void preRender( RegionFile rf, int[] colors, short[] heights ) {
		int maxSectionCount = 16;
		short[][] sectionBlockIds = new short[maxSectionCount][16*16*16];
		byte[][] sectionBlockData = new byte[maxSectionCount][16*16*16];
		boolean[] usedSections = new boolean[maxSectionCount]; 
		
		for( int cz=0; cz<32; ++cz ) {
			for( int cx=0; cx<32; ++cx ) {				
				resetInterval();
				DataInputStream cis = rf.getChunkDataInputStream(cx,cz);
				if( cis == null ) continue;
				NBTInputStream nis = null;
				try {
					nis = new NBTInputStream(cis);
					CompoundTag rootTag = (CompoundTag)nis.readTag();
					CompoundTag levelTag = (CompoundTag)rootTag.getValue().get("Level");
					loadChunkData( levelTag, maxSectionCount, sectionBlockIds, sectionBlockData, usedSections );
					timer.regionLoading += getInterval();
					
					for( int s=0; s<maxSectionCount; ++s ) {
						if( usedSections[s] ) {
							++timer.sectionCount;
						}
					}
					
					resetInterval();
					for( int z=0; z<16; ++z ) {
						for( int x=0; x<16; ++x ) {
							int pixelColor = 0;
							short pixelHeight = 0;
							
							for( int s=0; s<maxSectionCount; ++s ) {
								if( usedSections[s] ) {
									short[] blockIds  = sectionBlockIds[s];
									byte[]  blockData = sectionBlockData[s];
									
									for( int idx=z*16+x, y=0, absY=s*16; y<16; ++y, idx+=256, ++absY ) {
										final short blockId    =  blockIds[idx];
										final byte  blockDatum = blockData[idx];
										final int blockColor = colorMap.getColor( blockId&0xFFFF, blockDatum );
										pixelColor = Color.overlay( pixelColor, blockColor );
										if( Color.alpha(blockColor) >= shadeOpacityCutoff  ) {
											pixelHeight = (short)absY;
										}
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
					timer.preRendering += getInterval();
				} catch( IOException e ) {
					System.err.println("Error reading chunk from "+rf.getFile()+" at "+cx+","+cz);
					e.printStackTrace(System.err);
				} finally {
					if( nis != null ) {
						try {
							nis.close();
						} catch( IOException e ) {
							System.err.println("Failed to close NBTInputStream!");
							e.printStackTrace(System.err);
						}
					}
				}
			}
		}
	}
	
	public BufferedImage render( RegionFile rf ) {
		resetInterval();
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
		timer.postProcessing += getInterval();
		
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
		long startTime = System.currentTimeMillis();
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
				resetInterval();
				ImageIO.write(bi, "png", imageFile);
				timer.imageSaving += getInterval();
			} catch( IOException e ) {
				System.err.println("Error writing PNG to "+imageFile);
				e.printStackTrace();
			}
			++timer.regionCount;
		}
		timer.total += System.currentTimeMillis() - startTime;
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
	
	protected static boolean singleDirectoryGiven( List<File> files ) {
		return files.size() == 1 && files.get(0).isDirectory();
	}
	
	public static void main( String[] args ) throws Exception {
		String outputDirname = null;
		boolean force = false;
		boolean debug = false;
		Boolean createTileHtml = null;
		Boolean createImageTree = null;
		String colorMapFile = null;
		
		ArrayList<File> regionFiles = new ArrayList<File>(); 
		
		for( int i=0; i<args.length; ++i ) {
			if( args[i].charAt(0) != '-' ) {
				regionFiles.add(new File(args[i]));
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
		
		if( regionFiles.size() == 0 ) {
			System.err.println("No regions or directories specified.");
			System.err.println(USAGE);
			System.exit(1);
		}
		if( outputDirname == null ) {
			System.err.println("Output directory unspecified.");
			System.err.println(USAGE);
			System.exit(1);
		}
		
		final ColorMap colorMap = colorMapFile == null ?
			ColorMap.loadDefault() :
			ColorMap.load( new File(colorMapFile) );
		
		if( createTileHtml == null && singleDirectoryGiven(regionFiles) ) createTileHtml = Boolean.FALSE;
		
		if( createTileHtml == null ) createTileHtml = Boolean.TRUE;
		if( createImageTree == null ) createImageTree = Boolean.FALSE;
		
		RegionMap rm = RegionMap.load( regionFiles );
		RegionRenderer rr = new RegionRenderer( colorMap, debug );
		
		rr.renderAll( rm, outputDirname, force );
		if( debug ) {
			final Timer tim = rr.timer;
			System.err.println("Rendered "+tim.regionCount+" regions, "+tim.sectionCount+" sections in "+(tim.total)+"ms");
			System.err.println("The following times lines indicate milliseconds total, per region, and per section");
			System.err.println(tim.formatTime("Loading",        tim.regionLoading ));
			System.err.println(tim.formatTime("Pre-rendering",  tim.preRendering  ));
			System.err.println(tim.formatTime("Post-processing",tim.postProcessing));
			System.err.println(tim.formatTime("Image saving",   tim.imageSaving   ));
			System.err.println(tim.formatTime("Total",          tim.total         ));
		}
		
		if( createTileHtml.booleanValue() ) rr.createTileHtml( rm, outputDirname );
		if( createImageTree.booleanValue() ) rr.createImageTree( rm );
	}
}
