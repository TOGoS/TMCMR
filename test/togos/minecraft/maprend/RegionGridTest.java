package togos.minecraft.maprend;

import org.junit.Test;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


public class RegionGridTest {

  private Set<Point> crosses;

  Set<Point> set(int... coords) {
    Set<Point> pointSet = new HashSet<Point>();
    for (int i = 0; i < coords.length; i += 2)
      pointSet.add(new Point(coords[i], coords[i + 1]));
    return pointSet;
  }

  private void makeCrossesForRegion(int rx, int rz, int regionDimension, int gridSize) {
    RegionGrid grid = new RegionGrid(rx, rz, gridSize);
    grid.setRegionDimension(regionDimension);
    crosses = grid.getCrosses();
  }

  private void assertCrosses(int... coords) {
    assertThat(crosses, is(set(coords)));
  }

  @Test
  public void degenerateOriginRegion() throws Exception {
    makeCrossesForRegion(0, 0, 1, 1);
    assertCrosses(0, 0);
  }

  @Test
  public void originDimension2GridSize1() throws Exception {
    makeCrossesForRegion(0, 0, 2, 1);
    assertCrosses(0, 0, 0, 1, 1, 0, 1, 1);
  }

  @Test
  public void originDimension2GridSize2() throws Exception {
    makeCrossesForRegion(0, 0, 2, 2);
    assertCrosses(0, 0);
  }

  @Test
  public void OriginDimension3GridSize2() throws Exception {
    makeCrossesForRegion(0, 0, 3, 2);
    assertCrosses(0, 0, 0, 2, 2, 0, 2, 2);
  }

  @Test
  public void OneZeroDimension3GridSize2() throws Exception {
    //  012 012
    //0 x.x .x.
    //1 ... ...
    //2 x.x .x.
    makeCrossesForRegion(1, 0, 3, 2);
    assertCrosses(1, 0, 1, 2);
  }

  @Test
  public void ZeroOneDimension3GridSize2() throws Exception {
    //  012
    //0 x.x
    //1 ...
    //2 x.x
    //
    //0 ...
    //1 x.x
    //2 ...
    makeCrossesForRegion(0, 1, 3, 2);
    assertCrosses(0, 1, 2, 1);
  }

  @Test
  public void OneOneDimension3GridSize2() throws Exception {
    //  012
    //0 x.x
    //1 ...
    //2 x.x
    //      012
    //0 ... ...
    //1 x.x .x.
    //2 ... ...
    makeCrossesForRegion(1, 1, 3, 2);
    assertCrosses(1,1);
  }

  @Test
  public void M1M1Dimension3GridSize2() throws Exception {
    //012   012
    //... 0 ...
    //.x. 1 x.x
    //... 2 ...
    //----+----
    //.x. 0 x.x .x.
    //... 1 ... ...
    //.x. 2 x.x .x.
    makeCrossesForRegion(-1, -1, 3, 2);
    assertCrosses(1,1);
  }

  @Test
  public void M1M1Dimension5GridSize4() throws Exception {
    // 01234   01234
    // ..... 0 .....
    // .x... 1 x...x
    // ..... 2 .....
    // ..... 3 .....
    // ..... 4 .....
    //-------+------
    // .x... 0 x...x
    // ..... 1 .....
    // ..... 2 .....
    // ..... 3 .....
    // .x... 4 x...x
    makeCrossesForRegion(-1, -1, 5, 4);
    assertCrosses(1,1);
    makeCrossesForRegion(0,-1,5,4);
    assertCrosses(0,1,4,1);
    makeCrossesForRegion(-2,-2,5,4);
    assertCrosses(2,2);
    makeCrossesForRegion(1,1,5,4);
    assertCrosses(3,3);
  }

  @Test
  public void modulus() throws Exception {
    assertEquals(-1, -3 % 2);
    assertEquals(-1, -5 % 2);
    assertEquals(-3, -10 % 7);
  }

}
