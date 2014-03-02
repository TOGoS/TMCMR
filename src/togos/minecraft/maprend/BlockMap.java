package togos.minecraft.maprend;

import java.io.*;

import static togos.minecraft.maprend.IDUtil.parseInt;

public final class BlockMap {
  public static final int INDEX_MASK = 0xFFFF;
  public static final int SIZE = INDEX_MASK + 1;
  public static final int INF_NONE = 0;
  public static final int INF_GRASS = 1;
  public static final int INF_FOLIAGE = 2;
  public static final int INF_WATER = 3;
  public final Block[] blocks;

  public BlockMap(Block[] blocks) {
    assert blocks != null;
    assert blocks.length == SIZE;

    this.blocks = blocks;
  }

  public static BlockMap load(File f) throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(f));
    try {
      return load(br, f.getPath());
    } finally {
      br.close();
    }
  }

  public static BlockMap load(BufferedReader s, String filename) throws IOException {
    return new BlockMapLoader().load(s, filename);
  }

  public static BlockMap loadDefault() {
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(BlockMap.class.getResourceAsStream("block-colors.txt")));
      try {
        return load(br, "(default block colors)");
      } finally {
        br.close();
      }
    } catch (IOException e) {
      throw new RuntimeException("Error loading built-in color map", e);
    }
  }

  /**
   * Holds the default color for a block and, optionally,
   * a color for specific 'block data' values.
   */
  public static class Block {
    public static final int SUB_COLOR_COUNT = 0x10;
    protected static final int[] EMPTY_INT_ARRAY = new int[0];
    protected static final boolean[] EMPTY_BOOLEAN_ARRAY = new boolean[0];
    public int baseColor;
    public int baseInfluence;
    public boolean isDefault;
    public int[] subColors = EMPTY_INT_ARRAY;
    public int[] subColorInfluences = EMPTY_INT_ARRAY;
    public boolean[] hasSubColors = EMPTY_BOOLEAN_ARRAY;

    private Block(int baseColor, int baseInfluence, boolean isDefault) {
      this.baseColor = baseColor;
      this.baseInfluence = baseInfluence;
      this.isDefault = isDefault;
    }

    private void setSubColor(int blockData, int color, int influence) {
      if (blockData < 0 || blockData >= SUB_COLOR_COUNT) {
        throw new RuntimeException("Block data value out of bounds: " + blockData);
      }
      if (subColors.length == 0) {
        hasSubColors = new boolean[SUB_COLOR_COUNT];
        subColors = new int[SUB_COLOR_COUNT];
        subColorInfluences = new int[SUB_COLOR_COUNT];
      }
      hasSubColors[blockData] = true;
      subColors[blockData] = color;
      subColorInfluences[blockData] = influence;
    }

    private void setBaseColor(int color, int influence, boolean isDefault) {
      this.baseColor = color;
      this.baseInfluence = influence;
      this.isDefault = isDefault;
    }
  }

  protected static class BlockMapLoader {
    private static Block[] blocks;

    public BlockMap load(BufferedReader s, String filename) throws IOException {
      clearMap();

      String line;
      for (int lineNum = 1; (line = s.readLine()) != null; ++lineNum)
        if (parseBlockMapLine(line) == false)
          System.err.println("Invalid color map line at " + filename + ":" + lineNum + ": " + line);

      return new BlockMap(blocks);
    }

    private Block[] clearMap() {
      blocks = new Block[SIZE];
      for (int i = 0; i < SIZE; ++i) {
        blocks[i] = new Block(0, 0, true);
      }
      return blocks;
    }

    private boolean parseBlockMapLine(String line) {
      boolean validLine = false;
      try {
        String[] mapTokens = line.trim().split("\\s+", 4);
        switch (identifyLineType(mapTokens)) {
          case EMPTY:
          case COMMENT:
            validLine = true;
            break;
          case ERROR:
            validLine = false;
            break;
          case DEFAULT:
            setMapDefaultColor(parseInt(mapTokens[1]));
            validLine = true;
            break;
          case COLOR_LINE:
            parseBlockColorLine(mapTokens);
            validLine = true;
            break;
        }
      } catch (Exception e) {
        validLine = false;
      }
      return validLine;
    }

    ;

    private LineType identifyLineType(String[] mapTokens) {
      if (mapTokens.length == 0)
        return LineType.EMPTY;
      else if (mapTokens[0].startsWith("#"))
        return LineType.COMMENT;
      else if (mapTokens.length < 2)
        return LineType.ERROR;
      else if (mapTokens[0].equalsIgnoreCase("default"))
        return LineType.DEFAULT;
      else
        return LineType.COLOR_LINE;
    }

    protected void parseBlockColorLine(String[] mapTokens) {
      int color = parseInt(mapTokens[1]);
      String[] idTokens = mapTokens[0].split(":", 2);
      int blockId = parseInt(idTokens[0]);
      int blockData = idTokens.length == 2 ? parseInt(idTokens[1]) : -1;
      int influence = determineInfluence(mapTokens);

      setBlockMap(blockId, blockData, color, influence);
    }

    private int determineInfluence(String[] mapTokens) {
      if (mapTokens.length > 2)
        return selectSpecifiedInfluence(mapTokens[2]);
      return INF_NONE;
    }

    private int selectSpecifiedInfluence(String influenceToken) {
      if (influenceToken.equals("biome_grass")) {
        return INF_GRASS;
      } else if (influenceToken.equals("biome_foliage")) {
        return INF_FOLIAGE;
      } else if (influenceToken.equals("biome_water")) {
        return INF_WATER;
      }
      return INF_NONE;
    }

    protected void setBlockMap(int blockId, int blockData, int color, int influence) {
      int blockIndex = blockId & INDEX_MASK;
      if (blockData < 0) {
        blocks[blockIndex].setBaseColor(color, influence, false);
      } else {
        blocks[blockIndex].setSubColor(blockData, color, influence);
        blocks[blockIndex].isDefault = false;
      }
    }

    private void setMapDefaultColor(int defaultColor) {
      for (int i = 0; i < blocks.length; ++i)
        blocks[i].setBaseColor(defaultColor, 0, true);
    }

    private enum LineType {EMPTY, COMMENT, ERROR, DEFAULT, COLOR_LINE}
  }
}
