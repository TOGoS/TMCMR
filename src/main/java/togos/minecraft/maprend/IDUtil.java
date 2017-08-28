package togos.minecraft.maprend;

public class IDUtil
{
	public static int parseInt( String s ) {
		// Integer.parseInt pukes if the number is too big for a signed integer!
		// So use Long.parseLong and cast, instead.
		if( s.startsWith("0x") ) return (int)Long.parseLong(s.substring(2), 16);
		return (int)Long.parseLong(s);
	}
	
	public static String blockIdString( int blockId ) {
		if( blockId > 0xFFFF ) {
			return String.format("0x%04X:0x%01X", blockId & 0xFFFF, blockId >> 16);
		} else {
			return String.format("0x%04X", blockId);
		}
	}
}
