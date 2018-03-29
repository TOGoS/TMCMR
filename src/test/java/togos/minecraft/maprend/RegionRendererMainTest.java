package togos.minecraft.maprend;

import java.io.File;
import junit.framework.TestCase;
import togos.minecraft.maprend.RegionRenderer.RegionRendererCommand;

public class RegionRendererMainTest extends TestCase {
	private RegionRenderer.RegionRendererCommand main;

	public void testDefaultArguments() throws Exception {
		main = RegionRendererCommand.fromArguments();
		assertNull(main.outputDir);
		assertNull(main.createImageTree);
		assertNull(main.colorMapFile);
		assertNull(main.createTileHtml);
		assertFalse(main.forceReRender);
		assertFalse(main.debug);
		assertEquals(0, main.regionFiles.size());
	}

	private static String[] toArgs(String argString) {
		return argString.split(" ");
	}

	private void extractAndAssertValidArgs(String argString) {
		main = RegionRendererCommand.fromArguments(toArgs(argString));
		assertNull(main.errorMessage);
	}

	public void testStandardArguments() throws Exception {
		extractAndAssertValidArgs("-o out in");
		assertEquals("out", main.outputDir.getName());
		assertEquals(1, main.regionFiles.size());
		assertEquals("in", main.regionFiles.get(0).getName());
	}

	public void testMoreThanOneInputArgument() throws Exception {
		extractAndAssertValidArgs("in1 in2 -o out");
		assertEquals(2, main.regionFiles.size());
		assertEquals("in1", main.regionFiles.get(0).getName());
		assertEquals("in2", main.regionFiles.get(1).getName());
	}

	public void testFlagArguments() throws Exception {
		extractAndAssertValidArgs("in -o out -f -debug -create-tile-html -create-image-tree");
		assertTrue(main.forceReRender);
		assertTrue(main.debug);
		assertTrue(main.createTileHtml);
		assertTrue(main.createImageTree);
	}

	public void testColorMapArgument() throws Exception {
		extractAndAssertValidArgs("in -o out -color-map cm");
		assertEquals("cm", main.colorMapFile.getName());
	}

	public void testBadArgument() throws Exception {
		RegionRendererCommand cmd = RegionRendererCommand
				.fromArguments(toArgs("-bad"));
		assertEquals("Unrecognised argument: -bad", cmd.errorMessage);
	}

	public void testNoRegionFiles() throws Exception {
		RegionRendererCommand cmd = RegionRendererCommand
				.fromArguments(toArgs("-o out"));
		assertEquals("No regions or directories specified.", cmd.errorMessage);
	}

	public void testNoOutputFile() throws Exception {
		RegionRendererCommand cmd = RegionRendererCommand
				.fromArguments(toArgs("in"));
		assertEquals("Output directory unspecified.", cmd.errorMessage);
	}

	public void testCreateTileDefaultFromDir() throws Exception {
		File tempDir = new File("temp");
		if (!tempDir.exists())
			tempDir.mkdirs();
		RegionRendererCommand cmd = RegionRendererCommand
				.fromArguments(toArgs("temp"));
		assertEquals(true, cmd.shouldCreateTileHtml());
	}

	public void testCreateTileDefaultFromRegion() throws Exception {
		File tempDir = new File("temp");
		if (!tempDir.exists())
			tempDir.mkdirs();
		RegionRendererCommand cmd = RegionRendererCommand
				.fromArguments(toArgs("temp/r.0.0.mca"));
		assertEquals(false, cmd.shouldCreateTileHtml());
	}
	
	public void testNoMinMaxHeights() throws Exception {
		extractAndAssertValidArgs("-o out in");
		assertEquals(Integer.MIN_VALUE, main.minHeight);
		assertEquals(Integer.MAX_VALUE, main.maxHeight);
	}

	public void testMinMaxHeights() throws Exception {
		extractAndAssertValidArgs("-min-height 37 -max-height 64 -o out in");
		assertEquals(37, main.minHeight);
		assertEquals(64, main.maxHeight);
	}
}
