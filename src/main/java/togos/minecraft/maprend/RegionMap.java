package togos.minecraft.maprend;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegionMap
{
	static class Region {
		public int rx, rz;
		public File regionFile;
		public File imageFile;
	}
	
	public ArrayList<Region> regions = new ArrayList<Region>();
	public int minX=Integer.MAX_VALUE, minZ=Integer.MAX_VALUE, maxX=Integer.MIN_VALUE, maxZ=Integer.MIN_VALUE;
	
	public Region regionAt( int rx, int rz ) {
		for( Region r : regions ) {
			if( r.rx == rx && r.rz == rz ) return r;
		}
		return null;
	}
	
	public void addRegion( Region r ) {
		regions.add(r);
		if( r.rx < minX ) minX = r.rx;
		if( r.rz < minZ ) minZ = r.rz;
		if( r.rx >= maxX ) maxX = r.rx+1;
		if( r.rz >= maxZ ) maxZ = r.rz+1;
	}

	private void removeOldRegionFiles() {
		int i = regions.size() - 1;
		while( i >= 0 ) {
			String path = regions.get(i).regionFile.getPath();
			int len = path.length();
			if( len > 4 && path.regionMatches(len - 4, ".mcr", 0, 4) ) {
				regions.remove(i);
				if(i < regions.size()) continue;
			}
			i--;
		}
	}

	static final Pattern rfpat = Pattern.compile("^r\\.(-?[0-9]+)\\.(-?[0-9]+)\\.(mc[ar])$");
	static final Pattern arfpat = Pattern.compile("^r\\.(-?[0-9]+)\\.(-?[0-9]+)\\.mca$");

	protected void add( File dir, BoundingRect limit ) {
		Matcher m;
		if( dir.isDirectory() ) { 
			boolean anvil_only = false;
			File[] files = dir.listFiles();
			for( int i=0; i<files.length; ++i ) {
				m = (anvil_only ? arfpat : rfpat).matcher(files[i].getName());
				if( m.matches() ) {
					if( !anvil_only && m.group(3).equals("mca") ) {
						anvil_only = true;
						removeOldRegionFiles();
					}
					add( files[i], limit );
				}
			}
		} else if( (m = rfpat.matcher(dir.getName())).matches() ) {
			if( !dir.exists() ) {
				System.err.println("Warning: region file '"+dir+"' doesn't exist!");
				return;
			}
			Region r = new Region();
			r.rx = Integer.parseInt(m.group(1));
			r.rz = Integer.parseInt(m.group(2));
			r.regionFile = dir;
			if( r.rx >= limit.minX && r.rx < limit.maxX && r.rz >= limit.minY && r.rz < limit.maxY ) {
				addRegion( r );
			}
		} else {
			throw new RuntimeException(dir+" does not seem to be a directory or a region file");
		}
	}
	
	//// Le statique ////
	
	public static RegionMap load( File file, BoundingRect limit ) {
		RegionMap rm = new RegionMap();
		rm.add(file, limit);
		return rm;
	}
	
	public static RegionMap load( List<File> files, BoundingRect limit ) {
		RegionMap rm = new RegionMap();
		for( File f : files ) rm.add(f, limit);
		return rm;
	}
}
