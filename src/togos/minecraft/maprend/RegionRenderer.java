package togos.minecraft.maprend;

import org.jnbt.*;
import togos.minecraft.maprend.BiomeMap.Biome;
import togos.minecraft.maprend.BlockMap.Block;
import togos.minecraft.maprend.RegionMap.Region;
import togos.minecraft.maprend.io.ContentStore;
import togos.minecraft.maprend.io.RegionFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RegionRenderer
{

  private RegionRendererCommand rendererCommand;

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
	
	public final Set<Integer> defaultedBlockIds = new HashSet<Integer>();
	public final Set<Integer> defaultedBlockIdDataValues = new HashSet<Integer>();
	public final Set<Integer> defaultedBiomeIds = new HashSet<Integer>();
	public final BlockMap blockMap;
	public final BiomeMap biomeMap;
	public final int air16Color; // Color of 16 air blocks stacked
	/**
	 * Alpha below which blocks are considered transparent for purposes of shading
	 * (i.e. blocks with alpha < this will not be shaded, but blocks below them will be)
	 */
	private int shadeOpacityCutoff = 0x20; 
	
	public RegionRenderer( BlockMap blockMap, BiomeMap biomeMap, RegionRendererCommand cmd ) {
    rendererCommand = cmd;
		assert blockMap != null;
		assert biomeMap != null;
		
		this.blockMap = blockMap;
		this.biomeMap = biomeMap;
		this.air16Color = Color.overlay( 0, getColor(0, 0, 0), 16 );
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
	protected static void loadChunkData( CompoundTag levelTag, int maxSectionCount, short[][] sectionBlockIds, byte[][] sectionBlockData, boolean[] sectionsUsed, byte[] biomeIds ) {
		for( int i=0; i<maxSectionCount; ++i ) {
			sectionsUsed[i] = false;
		}
		
		Tag biomesTag = levelTag.getValue().get( "Biomes" );
		if (biomesTag != null) {
			System.arraycopy( ((ByteArrayTag)biomesTag).getValue(), 0, biomeIds, 0, 16*16 );
		} else {
			for(int i = 0; i< 16*16; i++) {
				biomeIds[i] = -1;
			}
		}
		
		for( Tag t : ((ListTag)levelTag.getValue().get("Sections")).getValue() ) {
			CompoundTag sectionInfo = (CompoundTag)t;
			int sectionIndex = ((ByteTag)sectionInfo.getValue().get("Y")).getValue().intValue();
			byte[]  blockIdsLow = ((ByteArrayTag)sectionInfo.getValue().get("Blocks")).getValue();
			byte[]  blockData   = ((ByteArrayTag)sectionInfo.getValue().get("Data")).getValue();
			Tag addTag = sectionInfo.getValue().get("Add");
			byte[] blockAdd = null;
			if (addTag != null) {
				blockAdd = ((ByteArrayTag)addTag).getValue();
			}
			short[] destSectionBlockIds = sectionBlockIds[sectionIndex];
			byte[]  destSectionData = sectionBlockData[sectionIndex];
			sectionsUsed[sectionIndex] = true;
			for( int y=0; y<16; ++y ) {
				for( int z=0; z<16; ++z ) {
					for( int x=0; x<16; ++x ) {
						int index = y*256+z*16+x;
						short blockType = (short) (blockIdsLow[index]&0xFF);
						if (blockAdd != null) {
							blockType |= nybble(blockAdd, index)<<8;
						}
						destSectionBlockIds[index] = blockType;
						destSectionData[index] = nybble( blockData, index );
					}
				}
			}
		}
	}
	
	//// Color look-up ////
	
	protected void defaultedBlockColor( int blockId ) {
		defaultedBlockIds.add(blockId);
	}
	protected void defaultedSubBlockColor( int blockId, int blockDatum ) {
		defaultedBlockIdDataValues.add(blockId | blockDatum << 16);
	}
	protected void defaultedBiomeColor( int biomeId ) {
		defaultedBiomeIds.add(biomeId);
	}
	
	protected int getColor( int blockId, int blockDatum, int biomeId ) {
		assert blockId >= 0 && blockId < blockMap.blocks.length;
		assert blockDatum >= 0;
		
		int blockColor;
		int biomeInfluence;
		
		Block bc = blockMap.blocks[blockId];
		if( bc.hasSubColors.length > blockDatum && bc.hasSubColors[blockDatum] ) {
			blockColor = bc.subColors[blockDatum];
			biomeInfluence = bc.subColorInfluences[blockDatum];
		} else {
			if( blockDatum != 0 ) {
				defaultedSubBlockColor(blockId, blockDatum);
			}
			blockColor = bc.baseColor;
			biomeInfluence = bc.baseInfluence;
		}
		if( bc.isDefault ) {
			defaultedBlockColor(blockId);
		}
		
		Biome biome = biomeMap.getBiome(biomeId);
		int biomeColor = biome.getMultiplier( biomeInfluence );
		if( biome.isDefault ) defaultedBiomeColor(biomeId);
		
		return Color.multiplySolid( blockColor, biomeColor );
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
		byte[] biomeIds = new byte[16*16];
		
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
					loadChunkData( levelTag, maxSectionCount, sectionBlockIds, sectionBlockData, usedSections, biomeIds );
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
							boolean diamond = false;
							boolean air = false;
							boolean buriedAir = false;
							boolean torch = false;
							int biomeId = biomeIds[z*16+x]&0xFF;
							
							for( int s=0; s<maxSectionCount; ++s ) {
								if( usedSections[s] ) {
									short[] blockIds  = sectionBlockIds[s];
									byte[]  blockData = sectionBlockData[s];
									
									for( int idx=z*16+x, y=0, absY=s*16; y<16; ++y, idx+=256, ++absY ) {
										final short blockId    =  blockIds[idx];
										final byte  blockDatum = blockData[idx];
										if (rendererCommand.showDiamonds && ((blockId&0xFFFF) == 56)) // diamond
											diamond = true;
										if (air && ((blockId&0xFFFF) == 1)) // stone over air
											buriedAir = true;
										if (rendererCommand.showAir && ((blockId&0xFFFF) == 0)) // air
										  air = true;
										if (rendererCommand.showTorches && ((blockId&0xFFFF) == 50)) { // torch
											torch = true;
										}
										int blockColor = getColor( blockId&0xFFFF, blockDatum, biomeId );
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
							colors[dIdx] = buriedAir ? 0xFFFFFFFF : pixelColor;
							colors[dIdx] = torch ? 0xFFFFFF00 : colors[dIdx];
							colors[dIdx] = diamond ? 0xFF00FFFF : colors[dIdx];
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
		
	public void renderAll(RegionMap rm) throws IOException {
		File outputDir = rendererCommand.outputDir;
		long startTime = System.currentTimeMillis();
		
		if( !outputDir.exists() ) outputDir.mkdirs();
		
		if( rm.regions.size() == 0 ) {
			System.err.println("Warning: no regions found!");
		}
		
		for( Region r : rm.regions ) {
			if( r == null ) continue;

      debugMessage("Region " + pad(r.rx, 4) + ", " + pad(r.rz, 4) + "...");

      String imageFilename = "tile."+r.rx+"."+r.rz+".png";
			File imageFile = r.imageFile = new File( outputDir+"/"+imageFilename );
			
			if( imageFile.exists() ) {
				if( !rendererCommand.forceReRender && imageFile.lastModified() > r.regionFile.lastModified() ) {
					debugMessage("image already up-to-date\n");
					continue;
				}
				imageFile.delete();
			}
			debugMessage("generating " + imageFilename + "...\n");
			
			RegionFile rf = new RegionFile( r.regionFile );
			BufferedImage bi;
			try {
			   bi = render( rf );
			} finally {
			   rf.close();
			}
      
      if (rendererCommand.overlayGrid)
        new RegionGrid(r.rx, r.rz, 100).overlayGridOnImage(bi);
			
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

  private void debugMessage(String debugMessage) {
    if( rendererCommand.debug ) {
      System.err.print(debugMessage);
    }
  }

  /**
	 * Create a "tiles.html" file containing a table with
	 * all region images (tile.<x>.<z>.png) that exist in outDir
	 * within the given bounds (inclusive)
	 */
	public void createTileHtml( int minX, int minZ, int maxX, int maxZ, File outputDir ) {
		debugMessage("Writing HTML tiles...\n");
		try {
			Writer w = new OutputStreamWriter(new FileOutputStream(new File(outputDir+"/tiles.html")));
			w.write("<html><body style=\"background:black\"><table border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\n");
			
			for( int z=minZ; z<=maxZ; ++z ) {
				w.write("<tr>");
				for( int x=minX; x<=maxX; ++x ) {
					w.write("<td>");
					String imageFilename = "tile."+x+"."+z+".png";
					File imageFile = new File( outputDir+"/"+imageFilename );
					if( imageFile.exists() ) {
						w.write("<img src=\""+imageFilename+"\"/>");
					}
					w.write("</td>");
				}
				w.write("</tr>\n");
			}
			
			w.write("</table>\n<span style=\"color: cornflowerblue;font-size: smaller;font-family: monospace;display: inline-block;\">");
			w.write("Page rendered at "+ new Date().toString());
			w.write("</span>\n</body></html>");
			w.close();
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}
	
	public void createImageTree( RegionMap rm ) {
		debugMessage("Composing image tree...\n");
		ImageTreeComposer itc = new ImageTreeComposer(new ContentStore());
		System.out.println( itc.compose( rm ) );
	}
	
	public void createBigImage( RegionMap rm, File outputDir) {
		debugMessage("Creating big image...\n");
		BigImageMerger bic = new BigImageMerger();
		bic.createBigImage( rm, outputDir, rendererCommand.debug );
	}

  protected static final boolean booleanValue( Boolean b, boolean defalt ) {
		return b == null ? defalt : b.booleanValue();
	}
	
	protected static boolean singleDirectoryGiven( List<File> files ) {
		return files.size() == 1 && files.get(0).isDirectory();
	}

  public static void main( String[] args ) throws Exception {
		System.exit( RegionRendererCommand.fromArguments( args ).run() );
	}

}
