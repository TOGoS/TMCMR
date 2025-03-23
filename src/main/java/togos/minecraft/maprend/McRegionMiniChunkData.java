package togos.minecraft.maprend;

import java.util.ArrayList;
import java.util.List;

public class McRegionMiniChunkData
{
	/**
	 * X,Y,Z coordinates (in blocks a.k.a. world units a.k.a. meters)
	 * of the bottom northeast corner of the chunk within the world.
	 */
	public final long posX, posY, posZ;
	public final int width, height, depth;

	public byte[] blocks;
	public byte[] block_data;
	public List tileEntityData = new ArrayList();

	public McRegionMiniChunkData( long px, long py, long pz, int width, int height, int depth ) {
		this.posX = px;
		this.posY = py;
		this.posZ = pz;
		this.width = width;
		this.height = height;
		this.depth = depth;
		this.blocks = new byte[height*depth*width];
		this.block_data = new byte[(height*depth*width+1)/2];
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
		return blocks[ blockIndex(x,y,z) ];
	}

	public void setBlockNumber( int x, int y, int z, byte blockNum ) {
		blocks[ blockIndex(x,y,z) ] = blockNum;
	}

	public byte getBlockExtraBits( int x, int y, int z ) {
		return getNybble( block_data, blockIndex(x,y,z) );
	}

	public void setBlockExtraBits( int x, int y, int z, byte value ) {
		putNybble( block_data, blockIndex(x,y,z), value );
	}

	public void setBlock( int x, int y, int z, byte blockNum, byte extraBits ) {
		setBlockNumber( x, y, z, blockNum );
		setBlockExtraBits( x, y, z, extraBits );
	}

	public void setBlock( int x, int y, int z, byte blockNum ) {
		setBlock( x, y, z, blockNum, (byte)0 );
	}
}
