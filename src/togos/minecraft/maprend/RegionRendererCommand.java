package togos.minecraft.maprend;

import java.io.File;
import java.io.IOException;
import java.util.*;

class RegionRendererCommand {
  File outputDir = null;
  boolean forceReRender = false;
  boolean debug = false;
  boolean printHelpAndExit = false;
  File colorMapFile = null;
  File biomeMapFile = null;
  ArrayList<File> regionFiles = new ArrayList<File>();
  Boolean createTileHtml = null;
  Boolean createImageTree = null;
  boolean createBigImage = false;
  BoundingRect regionLimitRect = BoundingRect.INFINITE;
  public boolean overlayGrid = false;
  public boolean showDiamonds = false;

  private int argIndex;
  private String[] args;
  String errorMessage = null;
  private boolean argsOk;

  public RegionRendererCommand(String[] args) {
    this.args = args;
  }

  public static RegionRendererCommand fromArguments(String... args) {
    RegionRendererCommand m = new RegionRendererCommand(args);
    if (m.parseArguments())
      m.errorMessage = validateSettings(m);

    return m;
  }

  private boolean parseArguments() {
    for (argIndex = 0; argIndex < args.length; ++argIndex) {
      if (args[argIndex].charAt(0) == '-') {
        if (!parseArg()) {
          errorMessage = "Unrecognised argument: " + args[argIndex];
          return false;
        }
      } else
        regionFiles.add(new File(args[argIndex]));
    }
    return true;
  }

  private boolean parseArg() {
    return doArg("-o", () -> outputDir = new File(args[++argIndex]))
      || doArg("-f", () -> forceReRender = true)
      || doArg("-debug", () -> debug = true)
      || doArg("-grid", () -> overlayGrid = true)
      || doArg("-D", () -> showDiamonds = true)
      || doArg("-create-tile-html", () -> createTileHtml = true)
      || doArg("-create-image-tree", () -> createImageTree = true)
      || doArg("-create-big-image", () -> createBigImage = true)
      || doArg("-color-map", () -> colorMapFile = new File(args[++argIndex]))
      || doArg("-biome-map", () -> biomeMapFile = new File(args[++argIndex]))
      || doArg("-h", () -> printHelpAndExit = true)
      || doArg("-?", () -> printHelpAndExit = true)
      || doArg("--help", () -> printHelpAndExit = true)
      || doArg("-help", () -> printHelpAndExit = true)
      || doArg("-region-limit-rect", () -> setRegionLimitRect());
  }

  private void setRegionLimitRect() {
    int minX = Integer.parseInt(args[++argIndex]);
    int minY = Integer.parseInt(args[++argIndex]);
    int maxX = Integer.parseInt(args[++argIndex]);
    int maxY = Integer.parseInt(args[++argIndex]);
    regionLimitRect = new BoundingRect(minX, minY, maxX, maxY);
  }

  private boolean doArg(String arg, Runnable r) {
    if (argIs(arg)) {
      r.run();
      return true;
    }
    return false;
  }

  private boolean argIs(String arg) {
    return arg.equals(args[argIndex]);
  }

  private static String validateSettings(RegionRendererCommand m) {
    if (m.regionFiles.size() == 0)
      return "No regions or directories specified.";
    else if (m.outputDir == null)
      return "Output directory unspecified.";
    else
      return null;
  }

  static boolean getDefault(Boolean b, boolean defaultValue) {
    return b != null ? b.booleanValue() : defaultValue;
  }

  public boolean shouldCreateTileHtml() {
    return getDefault(this.createTileHtml, RegionRenderer.singleDirectoryGiven(regionFiles));
  }

  public boolean shouldCreateImageTree() {
    return getDefault(this.createImageTree, false);
  }

  public int run() throws IOException {
    int status = 0;
    if (errorMessage != null) {
      System.err.println("Error: " + errorMessage + "\n" + USAGE);
      status = 1;
    } else if (printHelpAndExit)
      System.out.println(USAGE);
    else
      renderRegions();
    return status;
  }

  private void renderRegions() throws IOException {
    BlockMap colorMap = makeColorMap();
    BiomeMap biomeMap = makeBiomeMap();
    RegionMap rm = makeRegionMap();
    RegionRenderer rr = new RegionRenderer(colorMap, biomeMap, this);
    rr.renderAll(rm);
    generateSummaries(rm, rr);
  }

  private BlockMap makeColorMap() throws IOException {
    return colorMapFile == null ? BlockMap.loadDefault() :
      BlockMap.load(colorMapFile);
  }

  private BiomeMap makeBiomeMap() throws IOException {
    return biomeMapFile == null ? BiomeMap.loadDefault() :
      BiomeMap.load(biomeMapFile);
  }

  private RegionMap makeRegionMap() {
    return RegionMap.load(regionFiles, regionLimitRect);
  }

  private void generateSummaries(RegionMap rm, RegionRenderer rr) {
    printDebugMessages(rr);
    if (shouldCreateTileHtml())
      rr.createTileHtml(rm.minX, rm.minZ, rm.maxX, rm.maxZ, outputDir);
    if (shouldCreateImageTree())
      rr.createImageTree(rm);
    if (createBigImage)
      rr.createBigImage(rm, outputDir);
  }

  private void printDebugMessages(RegionRenderer rr) {
    if (debug) {
      printTimings(rr);
      printUnmappedBlockIds(rr);
      printUnmappedBiomes(rr);
      System.err.println();
    }
  }

  private void printUnmappedBlockIds(RegionRenderer rr) {
    printUnmappedBlocks(rr);
    printUnmappedBlockDataPairs(rr);
  }

  private void printUnmappedBlocks(RegionRenderer rr) {
    List<Integer> defaultedBlockIds = new ArrayList<>(rr.defaultedBlockIds);
    Collections.sort(defaultedBlockIds);
    if (defaultedBlockIds.size() > 0) {
      System.err.println("The following block IDs were not explicitly mapped to colors:");
      int z = 0;
      for (int blockId : defaultedBlockIds) {
        System.err.print(z == 0 ? "  " : z % 10 == 0 ? ",\n  " : ", ");
        System.err.print(IDUtil.blockIdString(blockId));
        ++z;
      }
      System.err.println();
    } else {
      System.err.println("All block IDs encountered were accounted for in the block color map.");
    }
    System.err.println();
  }

  private void printUnmappedBlockDataPairs(RegionRenderer rr) {
    List<Integer> defaultedBlockIdDataValues = new ArrayList<>(rr.defaultedBlockIdDataValues);
    Collections.sort(defaultedBlockIdDataValues, new BlockIdComparator());
    if (defaultedBlockIdDataValues.size() > 0) {
      System.err.println("The following block ID + data value pairs were not explicitly mapped to colors");
      System.err.println("(this is not necessarily a problem, as the base IDs were mapped to a color):");
      int z = 0;
      for (int blockId : defaultedBlockIdDataValues) {
        System.err.print(z == 0 ? "  " : z % 10 == 0 ? ",\n  " : ", ");
        System.err.print(IDUtil.blockIdString(blockId));
        ++z;
      }
      System.err.println();
    } else {
      System.err.println("All block ID + data value pairs encountered were accounted for in the block color map.");
    }
    System.err.println();
  }

  private void printUnmappedBiomes(RegionRenderer rr) {
    if (rr.defaultedBiomeIds.size() > 0) {
      System.err.println("The following biome IDs were not explicitly mapped to colors:");
      int z = 0;
      for (int biomeId : rr.defaultedBiomeIds) {
        System.err.print(z == 0 ? "  " : z % 10 == 0 ? ",\n  " : ", ");
        System.err.print(String.format("0x%02X", biomeId));
        ++z;
      }
      System.err.println();
    } else {
      System.err.println("All biome IDs encountered were accounted for in the biome color map.");
    }
  }

  private void printTimings(RegionRenderer rr) {
    final RegionRenderer.Timer tim = rr.timer;
    System.err.println("Rendered " + tim.regionCount + " regions, " + tim.sectionCount + " sections in " + (tim.total) + "ms");
    System.err.println("The following times lines indicate milliseconds total, per region, and per section");
    System.err.println(tim.formatTime("Loading", tim.regionLoading));
    System.err.println(tim.formatTime("Pre-rendering", tim.preRendering));
    System.err.println(tim.formatTime("Post-processing", tim.postProcessing));
    System.err.println(tim.formatTime("Image saving", tim.imageSaving));
    System.err.println(tim.formatTime("Total", tim.total));
    System.err.println();
  }

  public static final String USAGE =
    "Usage: TMCMR [options] -o <output-dir> <input-files>\n" +
      "  -h, -? ; print usage instructions and exit\n" +
      "  -f     ; force re-render even when images are newer than regions\n" +
      "  -grid  ; Overlay a 100x100 grid on the map.\n" +
      "  -debug ; be chatty\n" +
      "  -D     ; show diamonds\n" +
      "  -color-map <file>  ; load a custom color map from the specified file\n" +
      "  -biome-map <file>  ; load a custom biome color map from the specified file\n" +
      "  -create-tile-html  ; generate tiles.html in the output directory\n" +
      "  -create-image-tree ; generate a PicGrid-compatible image tree\n" +
      "  -create-big-image  ; merges all rendered images into a single file\n" +
      "  -region-limit-rect <x0> <y0> <x1> <y1> ; limit which regions are rendered\n" +
      "                     ; to those between the given region coordinates, e.g.\n" +
      "                     ; 0 0 2 2 to render the 4 regions southeast of the origin.\n" +
      "\n" +
      "Input files may be 'region/' directories or individual '.mca' files.\n" +
      "\n" +
      "tiles.html will always be generated if a single directory is given as input.\n" +
      "\n" +
      "Compound image tree blobs will be written to ~/.ccouch/data/tmcmr/\n" +
      "Compound images can then be rendered with PicGrid.";

  private static class BlockIdComparator implements Comparator<Integer> {
    public int compare(Integer o1, Integer o2) {
      o1 = repack(o1);
      o2 = repack(o2);
      return o1.compareTo(o2);
    }

    private Integer repack(Integer i) {
      i = (i>>16 & 0xF) | ((i&0xffff)<<4);
      return i;
    }
  }
}
