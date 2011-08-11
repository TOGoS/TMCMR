package togos.minecraft.maprend.world;

import java.util.Map;

import org.jnbt.ByteArrayTag;
import org.jnbt.CompoundTag;
import org.jnbt.IntTag;

public class ChunkData extends MiniChunkData
{
	public ChunkData( long px, long py, long pz, int w, int h, int d ) {
		super(px,py,pz,w,h,d);
	}
	
	/*
	public final int height = 128; // Y/+up/-down
	public final int depth  =  16; // Z/+west/-east
	public final int width  =  16; // X/+south/-north
	
	            (N, -x)
	               |
	               |
	               |
	               |
	(W, +z)--------0---------(E, -z) 
	               |
	               |
	               |
   	               |
	            (S, +x)
	*/
	
	public byte[] skyLightData   = new byte[(height*depth*width+1)/2];
	public byte[] blockLightData = new byte[(height*depth*width+1)/2];
	public byte[] lightHeightData = new byte[depth*width];
	public boolean terrainPopulated = false;
	
	//// Sky light ////
	
	public void setSkyLight( int x, int y, int z, int value ) {
		putNybble(skyLightData, blockIndex(x,y,z), value);
	}
	
	//// Light height ////
	
	public void setLightHeight( int x, int z, int height ) {
		lightHeightData[z*width+x] = (byte)(height);
	}
	
	public static ChunkData fromTag( CompoundTag t ) {
		Map m = t.getValue();
		IntTag xPos = (IntTag)m.get( "xPos" );
		IntTag zPos = (IntTag)m.get( "zPos" );
		
		ChunkData cd = new ChunkData(
			16*xPos.getValue().intValue(), 0, 16*zPos.getValue().intValue(),
			16, 128, 16
		);
		
		cd.blockData = ((ByteArrayTag)m.get("Blocks")).getValue();
		cd.blockExtraBits = ((ByteArrayTag)m.get("Data")).getValue();
		cd.skyLightData = ((ByteArrayTag)m.get("SkyLight")).getValue();
		cd.blockLightData = ((ByteArrayTag)m.get("BlockLight")).getValue();
		cd.lightHeightData = ((ByteArrayTag)m.get("HeightMap")).getValue();
		// TODO: this part
		//cd.tileEntityData = ((CompoundTag)m.get("TileEntities")).getValue();
		return cd;
	}
}
