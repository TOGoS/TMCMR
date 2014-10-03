package togos.minecraft.maprend;

import java.awt.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

class RegionGrid {
  private int rx;
  private int rz;
  private int gridSize;
  private int REGION_DIMENSION = 512;
  private final int CROSS_SIZE = 3;

  public RegionGrid(int rx, int rz, int gridSize) {
    this.rx = rx;
    this.rz = rz;
    this.gridSize = gridSize;
  }

  void overlayGridOnImage(BufferedImage bi) {
    Set<Point> crosses = getCrosses();
    Graphics g = bi.getGraphics();
    markCrosses(g, crosses);
  }

  void markCrosses(Graphics g, Set<Point> crosses) {
    for (Point cross : crosses)
      markCross(g, cross);
  }

  private void markCross(Graphics g, Point cross) {
    int x = cross.x;
    int y = cross.y;
    g.setColor(Color.RED);
    g.drawLine(x - CROSS_SIZE, y, x + CROSS_SIZE, y);
    g.setColor(Color.BLACK);
    g.drawLine(x, y - CROSS_SIZE, x, y + CROSS_SIZE);
  }

  Set<Point> getCrosses() {
    HashSet<Point> crosses = new HashSet<Point>();
    for (int x = toRegionOffset(rx); x < REGION_DIMENSION; x += gridSize)
      for (int z = toRegionOffset(rz); z < REGION_DIMENSION; z += gridSize)
        addCross(crosses, x, z);
    return crosses;
  }

  private int toRegionOffset(int regionCoordinate) {
    int mapCoordinate = regionCoordinate * REGION_DIMENSION;
    int remainder = mapCoordinate % gridSize;
    int overshooot = remainder < 0 ? remainder + gridSize : remainder;
    int offset = gridSize - overshooot;
    return offset % gridSize;
  }

  private boolean addCross(HashSet<Point> crosses, int x, int z) {
    return crosses.add(new Point(x, z));
  }

  public int getRegionDimension() {
    return REGION_DIMENSION;
  }

  public void setRegionDimension(int regionDimension) {
    this.REGION_DIMENSION = regionDimension;
  }
}
