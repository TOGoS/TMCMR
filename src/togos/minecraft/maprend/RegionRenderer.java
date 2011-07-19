package togos.minecraft.maprend;

import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.imageio.ImageIO;

import org.jnbt.CompoundTag;

import togos.minecraft.maprend.RegionMap.Region;
import togos.minecraft.maprend.io.BetterNBTInputStream;
import togos.minecraft.maprend.io.RegionFile;
import togos.minecraft.maprend.world.ChunkData;
import togos.minecraft.maprend.world.Material;
import togos.minecraft.maprend.world.Materials;

public class RegionRenderer
{
	protected void getChunkSurfaceData( ChunkData cd, byte[] type, byte[] height, int dx, int dy, int dwidth ) {
		byte[] blockData = cd.blockData;
		
		for( int z=0; z<16; ++z ) {
			for( int x=0; x<16; ++x ) {
				for( int y=127; y>=0; --y ) {
					int blockIdx = y + z*128 + x*16*128;
					
					byte blockType = blockData[blockIdx];
					if( blockType == 0 ) continue;
					
					int didx = (dy+z)*dwidth+dx+x; 
					type[didx] = blockType;
					height[didx] = (byte)y;
					break;
				}
			}
		}
	}
	
	protected void getRegionSurfaceData( String filename, RegionFile rf, byte[] type, byte[] height ) {
		for( int cz=0; cz<32; ++cz ) {
			for( int cx=0; cx<32; ++cx ) {
				DataInputStream cis = rf.getChunkDataInputStream(cx,cz);
				if( cis == null ) continue;
				
				try {
					BetterNBTInputStream nis = new BetterNBTInputStream(cis);
					CompoundTag rootTag = (CompoundTag)nis.readTag();
					CompoundTag levelTag = (CompoundTag)rootTag.getValue().get("Level");
					ChunkData cd = ChunkData.fromTag( levelTag );
					getChunkSurfaceData( cd, type, height, cx*16, cz*16, 512 );
				} catch( IOException e ) {
					System.err.println("Error reading chunk from "+filename+" at "+cx+","+cz);
					e.printStackTrace();
				}
			}
		}
	}
	
	//// Handy color-manipulation functions ////
	
	protected static final int clampByte( int component ) {
		if( component < 0 ) return 0;
		if( component > 255 ) return 255;
		return component;
	}
	
	protected static final int color( int r, int g, int b ) {
		return 0xFF000000 |
			(clampByte(r) << 16) |
			(clampByte(g) <<  8) |
			(clampByte(b) <<  0);
	}
	
	protected static final int component( int color, int shift ) {
		return (color >> shift) & 0xFF;
	}
	
	protected static final int shade( int color, int amt ) {
		return color(
			component( color, 16 ) + amt,
			component( color,  8 ) + amt,
			component( color,  0 ) + amt
		);
	}
	
	
	protected void shade( byte[] height, int[] color ) {
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
				
				shade += (height[idx] - 64) / 7.0;
				
				color[idx] = shade( color[idx], (int)(shade*8) );
			}
		}
	}
	
	public BufferedImage render( String filename, RegionFile rf ) {
		int width=512, depth=512;
		
		byte[] surfaceType   = new byte[width*depth];
		byte[] surfaceHeight = new byte[width*depth];
		int[ ] surfaceColor  = new  int[width*depth];
		getRegionSurfaceData( filename, rf, surfaceType, surfaceHeight );
		
		Material[] materials = Materials.byBlockType;
		
		int i = 0;
		for( int z=0; z<depth; ++z ) {
			for( int x=0; x<width; ++x, ++i ) {
				surfaceColor[i] = materials[surfaceType[i]].color;
			}
		}
		
		shade( surfaceHeight, surfaceColor );
		
		BufferedImage bi = new BufferedImage( width, depth, BufferedImage.TYPE_INT_ARGB );
		
		for( int z=0; z<depth; ++z ) {
			bi.setRGB( 0, z, width, 1, surfaceColor, width*z, width );
		}

		return bi;
	}
	
	public void renderAll( RegionMap rm, String outputDirname, boolean force ) {
		Region[] regions = rm.xzMap();
		
		File outputDir = new File(outputDirname);
		if( !outputDir.exists() ) outputDir.mkdirs();
		
		for( int i=0; i<regions.length; ++i ) {
			Region r = regions[i];
			if( r == null ) continue;
			
			String imageFilename = "tile."+r.rx+"."+r.rz+".png";
			File regionFile = new File( r.regionFile );
			File imageFile = new File( outputDirname+"/"+imageFilename );
			
			if( imageFile.exists() ) {
				if( !force && imageFile.lastModified() > regionFile.lastModified() ) {
					//System.err.println(imageFilename+" already up-to-date");
					continue;
				}
				imageFile.delete();
			}
			//System.err.println("Generating "+imageFilename+"...");
			
			BufferedImage bi = render( r.regionFile, new RegionFile( regionFile ) );
			
			try {
		        ImageIO.write(bi, "png", imageFile);
	        } catch( IOException e ) {
	        	System.err.println("Error writing PNG to "+imageFile);
		        e.printStackTrace();
	        }
		}
		
		//System.err.println("Writing index...");
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
	
	public static final String USAGE =
		"Usage: TMCMR <region-dir> -o <output-dir> [-f]\n" +
		"  -f  ; force re-render even when images are newer than regions";
	
	public static void main( String[] args ) {
		String regionDirname = null;
		String outputDirname = null;
		boolean force = false;
		
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
		
		RegionMap rm = RegionMap.load( new File(regionDirname) );
		RegionRenderer rr = new RegionRenderer();
		rr.renderAll( rm, outputDirname, force );
	}
}
