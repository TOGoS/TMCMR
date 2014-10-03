package togos.minecraft.maprend;

public class BoundingRect
{
	public static final BoundingRect INFINITE = new BoundingRect( Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE );
	
	/**
	 * These should generally be considered coordinates of bounding
	 * points between items rather than indexes of items themselves.
	 * e.g. 0,0,1,1 would contain one item, not four.
	 */
	public final int minX, minY, maxX, maxY;
	
	public BoundingRect( int minX, int minY, int maxX, int maxY ) {
		this.minX = minX;
		this.minY = minY;
		this.maxX = maxX;
		this.maxY = maxY;
	}

  public boolean equals(Object obj) {
    if (obj instanceof BoundingRect) {
      BoundingRect r = (BoundingRect) obj;
      return r.minX == minX && r.minY == minY && r.maxX == maxX && r.maxY == maxY;
    }
    return false;
  }

  public String toString() {
    return String.format("[min:(%d,%d) max:(%d,%d)]",minX, minY, maxX, maxY);
  }
}
