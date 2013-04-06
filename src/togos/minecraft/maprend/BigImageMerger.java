/*
 * BigImageComposer
 *
 * @ Project : TMCMR 
 * @ File Name : BigImageComposer.java
 * @ Date : 06.04.2013
 *
 * @ Copyright (C) 2013 Klaus-Peter Hammerschmidt
 * 
 * This program is free software; 
 * you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; 
 * either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; 
 * if not, see <http://www.gnu.org/licenses/>.
 */
package togos.minecraft.maprend;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import togos.minecraft.maprend.RegionMap.Region;

/**
 * @author Klaus-Peter Hammerschmidt
 * 
 */
public class BigImageMerger
{

	private Dimension getDimension( RegionMap rm ) {
		return new Dimension( (rm.maxX-rm.minX+1)*512, (rm.maxZ-rm.minZ+1)*512 );
	}

	public void createBigImage( RegionMap rm, File outputDir, boolean debug ) {
		Dimension dim = getDimension( rm );
		BufferedImage bigImage = new BufferedImage( dim.width, dim.height, BufferedImage.TYPE_INT_ARGB );
		if( debug )
			System.out.println( "Dimension: "+dim.width+", "+dim.height );
		for( Region r : rm.regions ) {
			BufferedImage region = null;
			try {
				region = ImageIO.read( r.imageFile );
			} catch ( IOException e ) {
				System.err.println( "Could not load image "+r.imageFile.getName() );
				continue;
			}
			bigImage.createGraphics().drawImage( region, (r.rx-rm.minX)*512, (r.rz-rm.minZ)*512, null );
			if( debug )
				System.out.println( "Region "+r.rx+", "+r.rz+" drawn to "+(r.rx-rm.minX)*512+", "+(r.rz-rm.minZ)*512 );
		}
		try {
			ImageIO.write( bigImage, "png", new File( outputDir+"/big.png" ) );
		} catch ( IOException e ) {
			System.out.println( "Could not write big image to "+outputDir+"/big.png" );
		}
	}
}
