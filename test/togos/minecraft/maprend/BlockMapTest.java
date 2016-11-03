package togos.minecraft.maprend;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.StringReader;

import static org.junit.Assert.*;

public class BlockMapTest {

  private TestBlockLoader loader;

  @Before
  public void setUp() throws Exception {
    loader = new TestBlockLoader();
  }

  @Test
  public void blockMapLoad_loadsLines() throws Exception {
    String testLines =
      "\n" +
        "#Comment\n" +
        "default  0xEEEEEEEE\n" +
        "0x0003:6  0xFEED   biome_grass";
    BufferedReader lines = new BufferedReader(new StringReader(testLines));
    BlockMap loadedMap = BlockMap.load(lines, "test");
    BlockMap.Block unmappedBlock = loadedMap.blocks[0x100];
    BlockMap.Block mappedBlock = loadedMap.blocks[0x0001];
    BlockMap.Block subColorBlock = loadedMap.blocks[0x0002];
    BlockMap.Block grassSubColorBlock = loadedMap.blocks[0x0003];

    assertTrue(unmappedBlock.isDefault);
    assertEquals(0xEEEEEEEE, unmappedBlock.baseColor);

    assertFalse(grassSubColorBlock.isDefault);
    assertTrue(grassSubColorBlock.hasSubColors[6]);
    assertEquals(0xFEED, grassSubColorBlock.subColors[6]);
    assertEquals(BlockMap.INF_GRASS, grassSubColorBlock.subColorInfluences[6]);
  }

  private void assertBlockAttributes(String[] tokens, int id, int data, int color, int influence) {
    loader.parseBlockColorLine(tokens);
    assertEquals(id, loader.blockId);
    assertEquals(data, loader.blockData);
    assertEquals(color, loader.color);
    assertEquals(influence, loader.influence);
  }

  @Test
  public void idAndColor() throws Exception {
    String[] tokens = {"1", "2"};
    assertBlockAttributes(tokens, 1, -1, 2, BlockMap.INF_NONE);
  }

  @Test
  public void idTypeAndColor() throws Exception {
    String[] tokens = {"10:9", "8"};
    assertBlockAttributes(tokens, 10, 9, 8, BlockMap.INF_NONE);
  }

  @Test
  public void idTypeColorAndGrass() throws Exception {
    String[] tokens = {"20:19", "18", "biome_grass"};
    assertBlockAttributes(tokens, 20, 19, 18, BlockMap.INF_GRASS);
  }

  @Test
  public void idTypeColorAndFoliage() throws Exception {
    String[] tokens = {"20:19", "18", "biome_foliage"};
    assertBlockAttributes(tokens, 20, 19, 18, BlockMap.INF_FOLIAGE);
  }

  @Test
  public void idTypeColorAndWater() throws Exception {
    String[] tokens = {"20:19", "18", "biome_water"};
    assertBlockAttributes(tokens, 20, 19, 18, BlockMap.INF_WATER);
  }

}

class TestBlockLoader extends BlockMap.BlockMapLoader {
  int blockId;
  int blockData;
  int color;
  int influence;

  protected void setBlockMap(int blockId, int blockData, int color, int influence) {
    this.blockId = blockId;
    this.blockData = blockData;
    this.color = color;
    this.influence = influence;
  }
}