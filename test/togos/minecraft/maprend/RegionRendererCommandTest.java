package togos.minecraft.maprend;

import org.junit.Test;

import java.io.File;

import static junit.framework.Assert.*;

public class RegionRendererCommandTest {
  private RegionRendererCommand main;

  @Test
  public void defaultArguments() throws Exception {
    main = RegionRendererCommand.fromArguments();
    assertNull(main.outputDir);
    assertNull(main.createImageTree);
    assertNull(main.colorMapFile);
    assertNull(main.biomeMapFile);
    assertNull(main.createTileHtml);
    assertFalse(main.forceReRender);
    assertFalse(main.debug);
    assertFalse(main.createBigImage);
    assertFalse(main.printHelpAndExit);
    assertEquals(0, main.regionFiles.size());
    assertEquals(BoundingRect.INFINITE, main.regionLimitRect);
    assertFalse(main.overlayGrid);
    assertFalse(main.showDiamonds);
  }

  private static String[] toArgs(String argString) {
    return argString.split(" ");
  }

  private void extractAndAssertValidArgs(String argString) {
    main = RegionRendererCommand.fromArguments(toArgs(argString));
    assertNull(main.errorMessage);
  }

  @Test
  public void standardArguments() throws Exception {
    extractAndAssertValidArgs("-o out in");
    assertEquals("out", main.outputDir.getName());
    assertEquals(1, main.regionFiles.size());
    assertEquals("in", main.regionFiles.get(0).getName());
  }

  @Test
  public void moreThanOneInputArgument() throws Exception {
    extractAndAssertValidArgs("in1 in2 -o out");
    assertEquals(2, main.regionFiles.size());
    assertEquals("in1", main.regionFiles.get(0).getName());
    assertEquals("in2", main.regionFiles.get(1).getName());
  }

  @Test
  public void flagArguments() throws Exception {
    extractAndAssertValidArgs("in -o out -f " +
      "-debug -create-tile-html -create-image-tree " +
      "-create-big-image -h -grid -D");
    assertTrue(main.forceReRender);
    assertTrue(main.debug);
    assertTrue(main.createTileHtml);
    assertTrue(main.createImageTree);
    assertTrue(main.createBigImage);
    assertTrue(main.printHelpAndExit);
    assertTrue(main.overlayGrid);
    assertTrue(main.showDiamonds);
  }

  @Test
  public void colorMapArgument() throws Exception {
    extractAndAssertValidArgs("in -o out -color-map cm -biome-map bm");
    assertEquals("cm", main.colorMapFile.getName());
    assertEquals("bm", main.biomeMapFile.getName());
  }

  @Test
  public void regionLimitRectangle() throws Exception {
    extractAndAssertValidArgs("in -o out -region-limit-rect 1 2 3 4");
    assertEquals(new BoundingRect(1, 2, 3, 4), main.regionLimitRect);
  }

  @Test
  public void badArgument() throws Exception {
    RegionRendererCommand cmd = RegionRendererCommand.fromArguments(toArgs("-bad"));
    assertEquals("Unrecognised argument: -bad", cmd.errorMessage);
  }

  @Test
  public void noRegionFiles() throws Exception {
    RegionRendererCommand cmd = RegionRendererCommand.fromArguments(toArgs("-o out"));
    assertEquals("No regions or directories specified.", cmd.errorMessage);
  }

  @Test
  public void noOutputFile() throws Exception {
    RegionRendererCommand cmd = RegionRendererCommand.fromArguments(toArgs("in"));
    assertEquals("Output directory unspecified.", cmd.errorMessage);
  }
  
  @Test
  public void createTileDefaultFromDir() throws Exception {
	File tempDir = new File("temp");
	if( !tempDir.exists() ) tempDir.mkdirs();
    RegionRendererCommand cmd = RegionRendererCommand.fromArguments(toArgs("temp"));
    assertEquals(true, cmd.shouldCreateTileHtml() );
  }
  
  @Test
  public void createTileDefaultFromRegion() throws Exception {
    File tempDir = new File("temp");
    if( !tempDir.exists() ) tempDir.mkdirs();
    RegionRendererCommand cmd = RegionRendererCommand.fromArguments(toArgs("temp/r.0.0.mca"));
    assertEquals(false, cmd.shouldCreateTileHtml() );
  }
}
