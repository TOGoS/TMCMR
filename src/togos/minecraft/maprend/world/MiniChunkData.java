package togos.minecraft.maprend.world;

import java.util.ArrayList;
import java.util.List;

public class MiniChunkData
{
	/**
	 * X,Y,Z coordinates (in blocks a.k.a. world units a.k.a. meters)
	 * of the bottom northeast corner of the chunk within the world.
	 */
	public final long posX, posY, posZ;
	public final int width, height, depth;
	
	public byte[] blockData;
	public byte[] blockExtraBits;
	public List tileEntityData = new ArrayList();
	
	public MiniChunkData( long px, long py, long pz, int width, int height, int depth ) {
		this.posX = px;
		this.posY = py;
		this.posZ = pz;
		this.width = width;
		this.height = height;
		this.depth = depth;
		this.blockData = new byte[height*depth*width];
		this.blockExtraBits = new byte[(height*depth*width+1)/2];
	}
	
	/*
	 * Return the 
	 * X,Y,Z coordinates (in blocks a.k.a. world units a.k.a. meters)
	 * of the bottom northeast corner of the chunk within the world.
	 */
	public long getChunkPositionX() { return posX; }
	public long getChunkPositionY() { return posY; }
	public long getChunkPositionZ() { return posZ; }
	
	public int getChunkWidth() {  return width;  }
	public int getChunkHeight() { return height; }
	public int getChunkDepth() {  return depth;  }
	
	protected int blockIndex( int x, int y, int z ) {
		return y + z*height + x*depth*height;
	}
	
	protected void putNybble( byte[] data, int index, int value ) {
		int byteIndex = index>>1;
		byte oldValue = data[byteIndex];
		if( (index & 0x1) == 0 ) {
			data[ byteIndex ] = (byte)((oldValue & 0xF0) | (value & 0x0F));
		} else {
			data[ byteIndex ] = (byte)((oldValue & 0x0F) | ((value<<4) & 0xF0));
		}
	}
	
	protected byte getNybble( byte[] data, int index ) {
		int byteIndex = index>>1;
		if( (index & 0x1) == 0 ) {
			return (byte)((data[ byteIndex ] >> 4) & 0x0F);
		} else {
			return (byte)((data[ byteIndex ] >> 0) & 0x0F);
		}
	}
	
	//// Block ////
	
	public byte getBlock( int x, int y, int z ) {
		return blockData[ blockIndex(x,y,z) ];
	}
	
	public void setBlockNumber( int x, int y, int z, byte blockNum ) {
		blockData[ blockIndex(x,y,z) ] = blockNum;
	}
	
	public byte getBlockExtraBits( int x, int y, int z ) {
		return getNybble( blockExtraBits, blockIndex(x,y,z) );
	}
	
	public void setBlockExtraBits( int x, int y, int z, byte value ) {
		putNybble( blockExtraBits, blockIndex(x,y,z), value );
	}
	
	public void setBlock( int x, int y, int z, byte blockNum, byte extraBits ) {
		setBlockNumber( x, y, z, blockNum );
		setBlockExtraBits( x, y, z, extraBits );
	}
	
	public void setBlock( int x, int y, int z, byte blockNum ) {
		setBlock( x, y, z, blockNum, (byte)0 );
	}
}
