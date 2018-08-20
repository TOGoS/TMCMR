package togos.minecraft.maprend;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.imageio.ImageIO;
import org.junit.Test;
import com.flowpowered.nbt.regionfile.RegionFile;
import togos.minecraft.maprend.renderer.RegionRenderer;
import togos.minecraft.maprend.renderer.RenderSettings;
import togos.minecraft.maprend.standalone.PostProcessing;

public class RegionRendererTest {

	@Test
	public void simpleTest1() throws IOException, URISyntaxException, InterruptedException {
		if (true) {
			RegionRenderer renderer = new RegionRenderer(new RenderSettings());
			try (DirectoryStream<Path> dir = Files.newDirectoryStream(Paths.get(URI.create(getClass().getResource("/TestWorld1/region/").toString())))) {
				for (Path p : dir) {
					int[] colors = renderer.render(new RegionFile(p));
					BufferedImage image = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
					image.setRGB(0, 0, 512, 512, colors, 0, 512);
					String name = p.getFileName().toString();
					name = name.replace("r.", "tile.").replace(".mca", ".png");
					ImageIO.write(image, "png", Files.newOutputStream(Paths.get("./output").resolve(name)));
				}
			}
		}
		PostProcessing.createTileHtml(-50, -50, 50, 50, new File("output"), new RenderSettings());
		// PostProcessing.createTileHtml(-10, -10, 10, 10, new File("output"), new RenderSettings());
	}
}