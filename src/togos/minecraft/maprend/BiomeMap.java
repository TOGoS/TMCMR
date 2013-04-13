package togos.minecraft.maprend;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

public final class BiomeMap
{
	public static final int INDEX_MASK = 0xFF;
	public static final int SIZE = INDEX_MASK + 1;
	
	private final Biome defaultBiome;
	private final Biome[] biomes;
	public BiomeMap(Biome[] biomes, Biome defaultBiome) {
		this.biomes = biomes;
		this.defaultBiome = defaultBiome;
	}
	
	protected static int parseInt( String s ) {
		// Integer.parseInt pukes if the number is too big for a signed integer!
		// So use Long.parseLong and cast, instead.
		if( s.startsWith("0x") ) return (int)Long.parseLong(s.substring(2), 16);
		return (int)Long.parseLong(s);
	}
	
	
	protected static BiomeMap load(BufferedReader in, String filename) throws IOException {
		Biome[] biomes = new Biome[SIZE];
		Biome defaultBiome = null;
		int lineNum = 0;
		String line;
		while( (line = in.readLine()) != null ) {
			++lineNum;
			if( line.trim().isEmpty() ) continue;
			if( line.trim().startsWith("#") ) continue;
			
			String[] v = line.split("\t", 5);
			if( v.length < 4 ) {
				System.err.println("Invalid biome map line at "+filename+":"+lineNum+": "+line);
				continue;
			}
			int grassColor = parseInt(v[1]);
			int foliageColor = parseInt(v[2]);
			int waterColor = parseInt(v[3]);
			if( "default".equals(v[0]) ) {
				defaultBiome = new Biome(grassColor, foliageColor, waterColor);
			} else {
				int blockId = parseInt( v[0] );
				biomes[blockId] = new Biome( grassColor, foliageColor, waterColor );
			}
		}
		if (defaultBiome == null) {
			defaultBiome = new Biome( 0xFFFF00FF, 0xFFFF00FF, 0xFFFF00FF );
		}
		return new BiomeMap(biomes, defaultBiome);
	}
	
	protected static BiomeMap loadDefault() {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(BiomeMap.class.getResourceAsStream("biome-colors.txt")));
			try {
				return load(br, "(default biome colors)");
			} finally {
				br.close();
			}
		} catch( IOException e ) {
			throw new RuntimeException("Error loading built-in biome map", e);
		}
	}
	
	public static BiomeMap load( File f ) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(f));
		try {
			return load(br, f.getPath());
		} finally {
			br.close();
		}
	}
	
	private Biome getBiome(int biomeId){
		if (biomes[biomeId&INDEX_MASK] != null) {
			return biomes[biomeId&INDEX_MASK];
		}
		return defaultBiome;
	}
	
	public int getGrassColor(int biomeId){
		return getBiome( biomeId ).grassColor;
	}
	
	public int getFoliageColor(int biomeId){
		return getBiome( biomeId ).foliageColor;
	}
	
	public int getWaterColor(int biomeId){
		return getBiome( biomeId ).waterColor;
	}

	public static final class Biome{
		final int grassColor;
		final int foliageColor;
		final int waterColor;
		public Biome(int grassColor, int foliageColor, int waterColor) {
			this.grassColor = grassColor;
			this.foliageColor = foliageColor;
			this.waterColor = waterColor;
		}
	}
}
