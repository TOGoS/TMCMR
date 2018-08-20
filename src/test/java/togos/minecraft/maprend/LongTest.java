package togos.minecraft.maprend;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.util.Random;
import org.junit.Test;
import togos.minecraft.maprend.renderer.RegionRenderer;

public class LongTest {

	@Test
	public void test() {
		for (int BITS = 4; BITS <= 12; BITS++) {
			BITS = 5;
			System.out.println(BITS);
			Random random = new Random(1234);

			// Fill a byte buffer with random data
			ByteBuffer buffer = ByteBuffer.wrap(new byte[BITS * 64 * 8]);
			buffer.order(ByteOrder.BIG_ENDIAN);
			random.nextBytes(buffer.array());

			// Convert it to a long buffer
			LongBuffer data = buffer.asLongBuffer();
			data.rewind();
			long[] longData = new long[BITS * 64];
			data.get(longData);

			// Control data: Convert all bytes to a binary string and zero-pad them, append them to a large bit string
			// Slicing the bit string into equal length substrings will give the control data
			StringBuffer number = new StringBuffer();
			for (byte b : buffer.array())
				number.append(Integer.toBinaryString(0x80000000 | ((b) & 0xff)).substring(24));
			System.out.println(number);
			for (int i = 0; i < BITS * 64; i++) {
				System.out.println(i);
				RegionRenderer.printLong(longData[0]);
				RegionRenderer.printLong(longData[1]);
				// RegionRenderer.printLong(Long.parseLong(number.substring(i * BITS, i * BITS + BITS)));
				// System.out.println(((i * BITS) ^ 63) + " " + ((i * BITS + BITS) ^ 63));
				// RegionRenderer.printLong(Long.parseLong(number.substring((i * BITS + BITS) ^ 63, (i * BITS) ^ 63)));
				RegionRenderer.printLong(RegionRenderer.extractFromLongNOTWORKING(longData, i, BITS));

				if (i > 15)
					System.exit(0);
				// Compare conversion with control data
				// assertEquals(Long.parseLong(number.substring(i * BITS, i * BITS + BITS), 2), RegionRenderer.extractFromLong(longData, i, BITS));
			}
		}
	}
}
