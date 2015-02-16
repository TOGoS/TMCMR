package togos.minecraft.maprend;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import org.jnbt.ByteArrayTag;
import org.jnbt.ByteTag;
import org.jnbt.CompoundTag;
import org.jnbt.ListTag;
import org.jnbt.NBTInputStream;
import org.jnbt.Tag;

import togos.minecraft.maprend.BiomeMap.Biome;
import togos.minecraft.maprend.BlockMap.Block;
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
	
	public final Set<Integer> defaultedBlockIds = new HashSet<Integer>();
	public final Set<Integer> defaultedBlockIdDataValues = new HashSet<Integer>();
	public final Set<Integer> defaultedBiomeIds = new HashSet<Integer>();
	public final boolean debug;
	public final BlockMap blockMap;
	public final BiomeMap biomeMap;
	public final int air16Color; // Color of 16 air blocks stacked
	public final int minHeight;
	public final int maxHeight;
	public final int shadingReferenceAltitude;
	public final int altitudeShadingFactor;
	public final int minAltitudeShading;
	public final int maxAltitudeShading;
	/**
	 * Alpha below which blocks are considered transparent for purposes of shading
	 * (i.e. blocks with alpha < this will not be shaded, but blocks below them will be)
	 */
	private int shadeOpacityCutoff = 0x20; 
	
	public RegionRenderer( BlockMap blockMap, BiomeMap biomeMap, boolean debug, int minHeight, int maxHeight, int shadingRefAlt, int minAltShading, int maxAltShading, int altShadingFactor) {
		assert blockMap != null;
		assert biomeMap != null;
		
		this.blockMap = blockMap;
		this.biomeMap = biomeMap;
		this.air16Color = Color.overlay( 0, getColor(0, 0, 0), 16 );
		this.debug = debug;
		
		this.minHeight = minHeight;
		this.maxHeight = maxHeight;
		this.shadingReferenceAltitude = shadingRefAlt;
		this.minAltitudeShading = minAltShading;
		this.maxAltitudeShading = maxAltShading;
		this.altitudeShadingFactor = altShadingFactor;
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
				
				int altShade = altitudeShadingFactor * (height[idx] - shadingReferenceAltitude) / 255;
				if( altShade < minAltitudeShading ) altShade = minAltitudeShading;
				if( altShade > maxAltitudeShading ) altShade = maxAltitudeShading;
				
				shade += altShade;
				
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
							int biomeId = biomeIds[z*16+x]&0xFF;
							
							for( int s=0; s<maxSectionCount; ++s ) {
								int absY=s*16;
								
								if( absY    >= maxHeight ) continue;
								if( absY+16 <= minHeight ) continue;
								
								if( usedSections[s] ) {
									short[] blockIds  = sectionBlockIds[s];
									byte[]  blockData = sectionBlockData[s];
									
									for( int idx=z*16+x, y=0; y<16; ++y, idx+=256, ++absY ) {
										if( absY < minHeight || absY >= maxHeight ) continue;
										
										final short blockId    =  blockIds[idx];
										final byte  blockDatum = blockData[idx];
										int blockColor = getColor( blockId&0xFFFF, blockDatum, biomeId );
										pixelColor = Color.overlay( pixelColor, blockColor );
										if( Color.alpha(blockColor) >= shadeOpacityCutoff  ) {
											pixelHeight = (short)absY;
										}
									}
								} else {
									if( minHeight <= absY && maxHeight >= absY+16 ) {
										// Optimize the 16-blocks-of-air case:
										pixelColor = Color.overlay( pixelColor, air16Color );
									} else {
										 // TODO: mix
									}
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
	
	final String mapTitle = "Regions"; // TODO: make configurable!
	final int[] renderScales = {1,4,16};
	
	public void renderAll( RegionMap rm, File outputDir, boolean force ) throws IOException {
		long startTime = System.currentTimeMillis();
		
		if( !outputDir.exists() ) outputDir.mkdirs();
		
		if( rm.regions.size() == 0 ) {
			System.err.println("Warning: no regions found!");
		}
		
		for( Region r : rm.regions ) {
			if( r == null ) continue;
			
			if( debug ) System.err.print("Region "+pad(r.rx, 4)+", "+pad(r.rz, 4)+"...");
			
			String imageFilename = "tile."+r.rx+"."+r.rz+".png";
			File fullSizeImageFile = r.imageFile = new File( outputDir, imageFilename );
			
			boolean fullSizeNeedsReRender = false;
			if( fullSizeImageFile.exists() ) {
				if( force || !fullSizeImageFile.exists() || r.regionFile.lastModified() < fullSizeImageFile.lastModified() ) {
					fullSizeNeedsReRender = true;
				} else {
					if( debug ) System.err.println("image already up-to-date");
				}
			}
			
			boolean anyScalesNeedReRender = false;
			for( int scale : renderScales ) {
				File f = new File( outputDir, "tile."+r.rx+"."+r.rz+".1-"+scale+".png" );
				if( force || !f.exists() || f.lastModified() < r.regionFile.lastModified() ) {
					anyScalesNeedReRender = true;
				}
			}
			
			BufferedImage fullSize;
			if( fullSizeNeedsReRender ) {
				fullSizeImageFile.delete();
				if( debug ) System.err.println("generating "+imageFilename+"...");
				
				RegionFile rf = new RegionFile( r.regionFile );
				try {
					fullSize = render( rf );
				} finally {
					rf.close();
				}
				
				try {
					resetInterval();
					ImageIO.write(fullSize, "png", fullSizeImageFile);
					timer.imageSaving += getInterval();
				} catch( IOException e ) {
					System.err.println("Error writing PNG to "+fullSizeImageFile);
					e.printStackTrace();
				}
				++timer.regionCount;
			} else if( anyScalesNeedReRender ) {
				fullSize = ImageIO.read(fullSizeImageFile);
			} else {
				return;
			}
			
			for( int scale : renderScales ) {
				if( scale == 1 ) continue; // Already wrote!
				int size = 512 / scale;
				BufferedImage rescaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
				Graphics2D g = rescaled.createGraphics();
				g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				g.drawImage(fullSize, 0, 0, size, size, 0, 0, 512, 512, null);
				g.dispose();
				File f = new File( outputDir, "tile."+r.rx+"."+r.rz+".1-"+scale+".png" );
				ImageIO.write(rescaled, "png", f);
			}
		}
		timer.total += System.currentTimeMillis() - startTime;
	}
	
	/**
	 * Create a "tiles.html" file containing a table with
	 * all region images (tile.<x>.<z>.png) that exist in outDir
	 * within the given bounds (inclusive)
	 */
	public void createTileHtml( int minX, int minZ, int maxX, int maxZ, File outputDir ) {
		if( debug ) System.err.println("Writing HTML tiles...");
		for( int scale : renderScales ) {
			int regionSize = 512 / scale;
			
			try {
				File cssFile = new File(outputDir, "tiles.css");
				if( !cssFile.exists() ) {
					InputStream cssInputStream = getClass().getResourceAsStream("tiles.css");
					byte[] buffer = new byte[1024*1024];
					try {
						FileOutputStream cssOutputStream = new FileOutputStream(cssFile);
						try {
							int r;
							while( (r = cssInputStream.read(buffer)) > 0 ) {
								cssOutputStream.write(buffer, 0, r);
							}
						} finally {
							cssOutputStream.close();
						}
					} finally {
						cssInputStream.close();
					}
				}
				
				Writer w = new OutputStreamWriter(new FileOutputStream(new File(
					outputDir,
					scale == 1 ? "tiles.html" : "tiles.1-"+scale+".html"
				)));
				try {
					w.write("<html><head>\n");
					w.write("<title>"+mapTitle+" - 1:"+scale+"</title>\n");
					w.write("<link rel=\"stylesheet\" type=\"text/css\" href=\"tiles.css\"/><body>\n");
					w.write("<div style=\"height: "+(maxZ-minZ+1)*regionSize+"px\">");
					
					for( int z=minZ; z<=maxZ; ++z ) {
						for( int x=minX; x<=maxX; ++x ) {
							String fullSizeImageFilename = "tile."+x+"."+z+".png";
							File imageFile = new File( outputDir+"/"+fullSizeImageFilename );
							String scaledImageFilename = scale == 1 ?
								fullSizeImageFilename :
								"tile."+x+"."+z+".1-"+scale+".png";
							if( imageFile.exists() ) {
								int top = (z-minZ) * regionSize, left = (x-minX) * regionSize;
								String title = "Region "+x+", "+z;
								String style =
									"width: "+regionSize+"px; height: "+regionSize+"px; "+
									"position: absolute; top: "+top+"; left: "+left+"; "+
									"background-image: url("+scaledImageFilename+")";
								w.write("<a "+
									"class=\"tile\" "+
									"style=\""+style+"\" "+
									"title=\""+title+"\" "+
									"href=\""+fullSizeImageFilename+"\">&nbsp;</a>");
							}
						}
					}
					
					w.write("</div>\n");
					if( renderScales.length > 1 ) {
						w.write("<div class=\"scales-nav\">");
						w.write("<p>Scales:</p>");
						w.write("<ul>");
						for( int otherScale : renderScales ) {
							if( otherScale == scale ) {
								w.write("<li>1:"+scale+"</li>");
							} else {
								String otherFilename = otherScale == 1 ? "tiles.html" : "tiles.1-"+otherScale+".html";
								w.write("<li><a href=\""+otherFilename+"\">1:"+otherScale+"</a></li>");
							}
						}
						w.write("</ul>");
						w.write("</div>");
					}
					w.write("<p class=\"notes\">");
					w.write("Page rendered at "+ new Date().toString());
					w.write("</p>\n");
					w.write("</body></html>");
				} finally {
					w.close();
				}
			} catch( IOException e ) {
				throw new RuntimeException(e);
			}
		}
	}
	
	public void createImageTree( RegionMap rm ) {
		if( debug ) System.err.println("Composing image tree...");
		ImageTreeComposer itc = new ImageTreeComposer(new ContentStore());
		System.out.println( itc.compose( rm ) );
	}
	
	public void createBigImage( RegionMap rm, File outputDir) {
		if( debug ) System.err.println( "Creating big image..." );
		BigImageMerger bic = new BigImageMerger();
		bic.createBigImage( rm, outputDir, debug );
	}
	
	public static final String USAGE =
		"Usage: TMCMR [options] -o <output-dir> <input-files>\n" +
		"  -h, -? ; print usage instructions and exit\n" +
		"  -f     ; force re-render even when images are newer than regions\n" +
		"  -debug ; be chatty\n" +
		"  -color-map <file>  ; load a custom color map from the specified file\n" +
		"  -biome-map <file>  ; load a custom biome color map from the specified file\n" +
		"  -create-tile-html  ; generate tiles.html in the output directory\n" +
		"  -create-image-tree ; generate a PicGrid-compatible image tree\n" +
		"  -create-big-image  ; merges all rendered images into a single file\n" +
		"  -min-height <y>    ; only draw blocks above this height\n" +
		"  -max-height <y>    ; only draw blocks below this height\n" +
		"  -region-limit-rect <x0> <y0> <x1> <y1> ; limit which regions are rendered\n" +
		"                     ; to those between the given region coordinates, e.g.\n" +
		"                     ; 0 0 2 2 to render the 4 regions southeast of the origin.\n" +
		"  -altitude-shading-factor <f>    ; how much altitude affects shading [36]\n" +
		"  -shading-reference-altitude <y> ; reference altitude for shading [64]\n" +
		"  -min-altitude-shading <x>       ; lowest altitude shading modifier [-20]\n" +
		"  -max-altitude-shading <x>       ; highest altitude shading modifier [20]\n" +
		"\n" +
		"Input files may be 'region/' directories or individual '.mca' files.\n" +
		"\n" +
		"tiles.html will always be generated if a single directory is given as input.\n" +
		"\n" +
		"Compound image tree blobs will be written to ~/.ccouch/data/tmcmr/\n" +
		"Compound images can then be rendered with PicGrid.";
	
	protected static final boolean booleanValue( Boolean b, boolean defalt ) {
		return b == null ? defalt : b.booleanValue();
	}
	
	protected static boolean singleDirectoryGiven( List<File> files ) {
		return files.size() == 1 && files.get(0).isDirectory();
	}
	
	//// Command-line processing ////
	
	static class RegionRendererCommand
	{
		public static RegionRendererCommand fromArguments( String...args ) {
			RegionRendererCommand m = new RegionRendererCommand();
			for( int i = 0; i < args.length; ++i ) {
				if( args[i].charAt(0) != '-' ) {
					m.regionFiles.add(new File(args[i]));
				} else if( "-o".equals(args[i]) ) {
					m.outputDir = new File(args[++i]);
				} else if( "-f".equals(args[i]) ) {
					m.forceReRender = true;
				} else if( "-debug".equals(args[i]) ) {
					m.debug = true;
				} else if( "-min-height".equals(args[i]) ) {
					m.minHeight = Integer.parseInt(args[++i]);
				} else if( "-max-height".equals(args[i]) ) {
					m.maxHeight = Integer.parseInt(args[++i]);
				} else if( "-create-tile-html".equals(args[i]) ) {
					m.createTileHtml = Boolean.TRUE;
				} else if( "-create-image-tree".equals(args[i]) ) {
					m.createImageTree = Boolean.TRUE;
				} else if( "-region-limit-rect".equals(args[i] ) ) {
					int minX = Integer.parseInt(args[++i]);
					int minY = Integer.parseInt(args[++i]);
					int maxX = Integer.parseInt(args[++i]);
					int maxY = Integer.parseInt(args[++i]);
					m.regionLimitRect = new BoundingRect( minX, minY, maxX, maxY );
				} else if( "-create-big-image".equals(args[i]) ) {
					m.createBigImage = true;
				} else if( "-color-map".equals(args[i]) ) {
					m.colorMapFile = new File(args[++i]);
				} else if( "-biome-map".equals(args[i]) ) {
					m.biomeMapFile = new File(args[++i]);
				} else if( "-altitude-shading-factor".equals(args[i]) ) {
					m.altitudeShadingFactor = Integer.parseInt(args[++i]);
				} else if( "-shading-reference-altitude".equals(args[i]) ) {
					m.shadingReferenceAltitude = Integer.parseInt(args[++i]);
				} else if( "-min-altitude-shading".equals(args[i]) ) {
					m.minAltitudeShading = Integer.parseInt(args[++i]);
				} else if( "-max-altitude-shading".equals(args[i]) ) {
					m.maxAltitudeShading = Integer.parseInt(args[++i]);
				} else if( "-h".equals(args[i]) || "-?".equals(args[i]) || "--help".equals(args[i]) || "-help".equals(args[i]) ) {
					m.printHelpAndExit = true;
				} else {
					m.errorMessage = "Unrecognised argument: " + args[i];
					return m;
				}
			}
			m.errorMessage = validateSettings(m);
			return m;
		}
		
		private static String validateSettings( RegionRendererCommand m ) {
			if( m.regionFiles.size() == 0 )
				return "No regions or directories specified.";
			else if( m.outputDir == null )
				return "Output directory unspecified.";
			else
				return null;
		}
		
		File outputDir = null;
		boolean forceReRender = false;
		boolean debug = false;
		boolean printHelpAndExit = false;
		File colorMapFile = null;
		File biomeMapFile = null;
		ArrayList<File> regionFiles = new ArrayList<File>();
		Boolean createTileHtml = null;
		Boolean createImageTree = null;
		boolean createBigImage = false;
		BoundingRect regionLimitRect = BoundingRect.INFINITE;
		int minHeight = Integer.MIN_VALUE;
		int maxHeight = Integer.MAX_VALUE;
		int shadingReferenceAltitude = 64;
		int minAltitudeShading = -20;
		int maxAltitudeShading = +20;
		int altitudeShadingFactor = 36;
		
		String errorMessage = null;
		
		static boolean getDefault( Boolean b, boolean defaultValue ) {
			return b != null ? b.booleanValue() : defaultValue; 
		}
		
		public boolean shouldCreateTileHtml() {
			return getDefault(this.createTileHtml, singleDirectoryGiven(regionFiles));
		}
		
		public boolean shouldCreateImageTree() {
			return getDefault(this.createImageTree, false);
		}
		
		public int run() throws IOException {
			if( errorMessage != null ) {
				System.err.println( "Error: "+errorMessage );
				System.err.println( USAGE );
				return 1;
			}
			if( printHelpAndExit ) {
				System.out.println( USAGE );
				return 0;
			}
			
			final BlockMap colorMap = colorMapFile == null ? BlockMap.loadDefault() : 
				BlockMap.load(colorMapFile);
			        
			final BiomeMap biomeMap = biomeMapFile == null ? BiomeMap.loadDefault() :
				BiomeMap.load( biomeMapFile );
			
			RegionMap rm = RegionMap.load(regionFiles, regionLimitRect);
			RegionRenderer rr = new RegionRenderer(
				colorMap, biomeMap, debug, minHeight, maxHeight,
				shadingReferenceAltitude, minAltitudeShading, maxAltitudeShading, altitudeShadingFactor
			);
			
			rr.renderAll(rm, outputDir, forceReRender);
			if( debug ) {
				final Timer tim = rr.timer;
				System.err.println("Rendered " + tim.regionCount + " regions, " + tim.sectionCount + " sections in " + (tim.total) + "ms");
				System.err.println("The following times lines indicate milliseconds total, per region, and per section");
				System.err.println(tim.formatTime("Loading",         tim.regionLoading));
				System.err.println(tim.formatTime("Pre-rendering",   tim.preRendering));
				System.err.println(tim.formatTime("Post-processing", tim.postProcessing));
				System.err.println(tim.formatTime("Image saving",    tim.imageSaving));
				System.err.println(tim.formatTime("Total",           tim.total));
				System.err.println();
				
				if( rr.defaultedBlockIds.size() > 0 ) {
					System.err.println("The following block IDs were not explicitly mapped to colors:");
					int z=0;
					for( int blockId : rr.defaultedBlockIds ) {
						System.err.print(z == 0 ? "  " : z % 10 == 0 ? ",\n  " : ", ");
						System.err.print(IDUtil.blockIdString(blockId));
						++z;
					}
					System.err.println();
				} else {
					System.err.println("All block IDs encountered were accounted for in the block color map.");
				}
				System.err.println();
				
				if( rr.defaultedBlockIdDataValues.size() > 0 ) {
					System.err.println("The following block ID + data value pairs were not explicitly mapped to colors");
					System.err.println("(this is not necessarily a problem, as the base IDs were mapped to a color):");
					int z=0;
					for( int blockId : rr.defaultedBlockIdDataValues ) {
						System.err.print(z == 0 ? "  " : z % 10 == 0 ? ",\n  " : ", ");
						System.err.print(IDUtil.blockIdString(blockId));
						++z;
					}
					System.err.println();
				} else {
					System.err.println("All block ID + data value pairs encountered were accounted for in the block color map.");
				}
				System.err.println();
				
				if( rr.defaultedBiomeIds.size() > 0 ) {
					System.err.println("The following biome IDs were not explicitly mapped to colors:");
					int z = 0;
					for( int biomeId : rr.defaultedBiomeIds ) {
						System.err.print(z == 0 ? "  " : z % 10 == 0 ? ",\n  " : ", ");
						System.err.print(String.format("0x%02X", biomeId));
						++z;
					}
					System.err.println();
				} else {
					System.err.println("All biome IDs encountered were accounted for in the biome color map.");
				}
				System.err.println();
			}
			
			if( shouldCreateTileHtml()  ) rr.createTileHtml(rm.minX, rm.minZ, rm.maxX, rm.maxZ, outputDir);
			if( shouldCreateImageTree() ) rr.createImageTree(rm);
			if( createBigImage ) rr.createBigImage(rm, outputDir);
			
			return 0;
		}
	}
	
	public static void main( String[] args ) throws Exception {
		System.exit( RegionRendererCommand.fromArguments( args ).run() );
	}
}
