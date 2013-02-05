package togos.minecraft.maprend.io;

import org.jnbt.Tag;

public class IntArrayTag extends Tag
{
	int[] value;
	
	public IntArrayTag( String name, int[] value ) {
		super(name);
		this.value = value;
	}
	
	public int[] getInts() {
		return value;
	}
	
	public Object getValue() {
		return value;
	}
}
