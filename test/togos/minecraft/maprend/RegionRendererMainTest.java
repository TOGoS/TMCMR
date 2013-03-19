package togos.minecraft.maprend;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.*;
import static junit.framework.Assert.assertEquals;

public class RegionRendererMainTest {
  private RegionRenderer.Main main;

  @Before
  public void setUp() throws Exception {
    main = new RegionRenderer.Main();
  }

  @Test
  public void defaultArguments() throws Exception {
    assertNull(main.outputDir);
    assertNull(main.createImageTree);
    assertNull(main.colorMapFile);
    assertNull(main.createTileHtml);
    assertFalse(main.force);
    assertFalse(main.debug);
    assertEquals(0, main.regionFiles.size());
  }

  private String[] toArgs(String argString) {
    return argString.split(" ");
  }

  private void extractAndAssertValidArgs(String argString) {
    String badArg = main.extractArguments(toArgs(argString));
    assertNull(badArg);
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
    extractAndAssertValidArgs("in -o out -f -debug -create-tile-html -create-image-tree");
    assertTrue(main.force);
    assertTrue(main.debug);
    assertTrue(main.createTileHtml);
    assertTrue(main.createImageTree);
  }

  @Test
  public void colorMapArgument() throws Exception {
    extractAndAssertValidArgs("in -o out -color-map cm");
    assertEquals("cm", main.colorMapFile.getName());
  }

  @Test
  public void badArgument() throws Exception {
    String badArg = main.extractArguments(toArgs("-bad"));
    assertEquals("Unrecognised argument: -bad", badArg);
  }

  @Test
  public void noRegionFiles() throws Exception {
    String badArg = main.extractArguments(toArgs("-o out"));
    assertEquals("No regions or directories specified.", badArg);
  }

  @Test
  public void noOutputFile() throws Exception {
    String badArg = main.extractArguments(toArgs("in"));
    assertEquals("Output directory unspecified.", badArg);
  }
}

