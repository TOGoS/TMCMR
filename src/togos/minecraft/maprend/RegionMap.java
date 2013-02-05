package togos.minecraft.maprend;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegionMap
{
	static class Region {
		public int rx, rz;
		public String regionFile;
	}
	
	public List regions = new ArrayList();
	public int minX=Integer.MAX_VALUE, minZ=Integer.MAX_VALUE, maxX=Integer.MIN_VALUE, maxZ=Integer.MIN_VALUE;
	
	public Region[] xzMap() {
		int width = maxX-minX+1;
		int depth = maxZ-minZ+1;
		Region[] m = new Region[width*depth];
		for( Iterator i=regions.iterator(); i.hasNext(); ) {
			Region r = (Region)i.next();
			m[ (r.rx-minX) + (r.rz-minZ)*width ] = r;
		}
		return m;
	}
	
	public void addRegion( Region r ) {
		regions.add(r);
		if( r.rx < minX ) minX = r.rx;
		if( r.rz < minZ ) minZ = r.rz;
		if( r.rx > maxX ) maxX = r.rx;
		if( r.rz > maxZ ) maxZ = r.rz;
	}
		
	static final Pattern rfpat = Pattern.compile("^r\\.(-?\\d+)\\.(-?\\d+)\\.mca$");
	
	public static RegionMap load( File dir ) {
		RegionMap rm = new RegionMap();
		
		File[] files = dir.listFiles();
		for( int i=0; i<files.length; ++i ) {
			Matcher m = rfpat.matcher(files[i].getName());
			if( m.matches() ) {
				Region r = new Region();
				r.rx = Integer.parseInt(m.group(1));
				r.rz = Integer.parseInt(m.group(2));
				r.regionFile = files[i].getPath();
				rm.addRegion( r );
			}
		}
		
		return rm;
	}
	
	protected static String pad( String x, int width ) {
		if( x.length() > width ) return x.substring( x.length()-width );
		while( x.length() < width ) x = " "+x;
		return x;
	}
	
	protected static String fmt( int i ) {
		return pad( String.valueOf(i), 4 );
	}
	
	public static void dump( RegionMap rm, PrintStream ps ) {
		Region[] xzMap = rm.xzMap();
		int width = rm.maxX-rm.minX+1;
		
		ps.println("Region");
		if( rm.minX == Integer.MAX_VALUE ) {
			ps.println("No files found!");
			return;
		}
		ps.println("X: "+rm.minX+" to "+rm.maxX+", Z: "+rm.minZ+" to "+rm.maxZ);
		
		ps.print("      ");
		for( int x=rm.minX; x<=rm.maxX; ++x ) {
			ps.print( fmt(x) );
		}
		ps.println();
		ps.println();
		for( int z=rm.minZ; z<=rm.maxZ; ++z ) {
			ps.print( fmt(z) );
			ps.print( "  " );
			for( int i=0; i<1; ++i ) {
				if( i > 0 ) ps.print("      ");
				for( int x=rm.minX; x<=rm.maxX; ++x ) {
					if( xzMap[(x-rm.minX)+(z-rm.minZ)*width] != null ) {
						ps.print("[RG]");
					} else {
						ps.print("    ");
					}
				}
				ps.println();
			}
		}
	}
	
	public static void main( String[] args ) {
		String regionDir = args[0];
		dump( load(new File(regionDir)), System.out );
	}
}
