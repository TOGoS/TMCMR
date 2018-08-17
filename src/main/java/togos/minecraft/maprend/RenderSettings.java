package togos.minecraft.maprend;

import java.io.File;

public class RenderSettings {

	public boolean	debug						= false;
	public File		colorMapFile				= null;
	public File		biomeMapFile				= null;
	public int		minHeight					= Integer.MIN_VALUE;
	public int		maxHeight					= Integer.MAX_VALUE;

	/**
	 * Above = brighter, below = darker TODO affect grass color in a more native way
	 */
	public int		shadingReferenceAltitude	= 64;
	/** Maximum brightness difference through shading */
	public int		altitudeShadingFactor		= 50;

	public int		minAltitudeShading			= -20;
	public int		maxAltitudeShading			= +20;

	public String	mapTitle					= "Regions";
	public int[]	mapScales					= { 1 };

	public RenderSettings() {
	}

	public RenderSettings(
			File colorMapFile, File biomeMapFile, boolean debug, int minHeight, int maxHeight,
			int shadingRefAlt, int minAltShading, int maxAltShading, int altShadingFactor,
			String mapTitle, int[] mapScales) {

		this.colorMapFile = colorMapFile;
		this.biomeMapFile = biomeMapFile;
		this.debug = debug;

		this.minHeight = minHeight;
		this.maxHeight = maxHeight;
		this.shadingReferenceAltitude = shadingRefAlt;
		this.minAltitudeShading = minAltShading;
		this.maxAltitudeShading = maxAltShading;
		this.altitudeShadingFactor = altShadingFactor;

		this.mapTitle = mapTitle;
		this.mapScales = mapScales;
	}
}