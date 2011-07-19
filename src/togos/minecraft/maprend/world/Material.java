package togos.minecraft.maprend.world;

public class Material {
	public final int color;
	public final byte blockType;
	public final byte blockExtraBits;
	public final String name;
	public final String icon;
	
	public Material( byte blockType, byte extraBits, int color, String icon, String name ) {
		this.blockType = blockType;
		this.blockExtraBits = extraBits;
		this.name = name;
		this.color = color;
		this.icon = icon;
	}
}
