package togos.minecraft.maprend.renderer;

import com.flowpowered.nbt.ByteArrayTag;
import com.flowpowered.nbt.ByteTag;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.ListTag;
import com.flowpowered.nbt.Tag;

public class ChunkLoader {

	public static void loadChunkData(CompoundTag levelTag) {
		Tag<?> versionTag = levelTag.getValue().get("DataVersion");
		if (versionTag == null) {
			// Pre 1.9
			throw new RuntimeException("Worlds pre 1.9 are not supported. Please \"optimize\" this world in Minecraft");
		} else {
			int version = (Integer) versionTag.getValue();
			switch (version) {
				// 1.9.X
				case 100:
				case 169:
				case 175:
				case 176:
				case 183:
				case 184:

				// 1.10.X
				case 501:
				case 510:
				case 511:
				case 512:

				// 1.11.X
				case 800:
				case 819:
				case 921:
				case 922:

				// 1.12.X
				case 1022:
				case 1139:
				case 1242:
				case 1343:
					loadChunkDataLegacy();
					break;

				// 1.13.X
				case 1444:
				case 1519:
					loadChunkData();
					break;
			}
		}
	}

	public static void loadChunkDataLegacy() {

	}

	public static void loadChunkData() {

	}

	/**
	 * @param levelTag
	 * @param maxSectionCount
	 * @param sectionBlockIds block IDs for non-empty sections will be written to sectionBlockIds[sectionIndex][blockIndex]
	 * @param sectionBlockData block data for non-empty sections will be written to sectionBlockData[sectionIndex][blockIndex]
	 * @param sectionsUsed sectionsUsed[sectionIndex] will be set to true for non-empty sections
	 */
	public static void loadChunkData(CompoundTag levelTag, int maxSectionCount, short[][] sectionBlockIds, byte[][] sectionBlockData, boolean[] sectionsUsed, byte[] biomeIds) {
		for (int i = 0; i < maxSectionCount; ++i) {
			sectionsUsed[i] = false;
		}

		Tag<?> biomesTag = levelTag.getValue().get("Biomes");
		if (biomesTag != null) {
			System.arraycopy(((ByteArrayTag) biomesTag).getValue(), 0, biomeIds, 0, 16 * 16);
		} else {
			for (int i = 0; i < 16 * 16; i++) {
				biomeIds[i] = -1;
			}
		}

		for (Tag<?> t : ((ListTag<?>) levelTag.getValue().get("Sections")).getValue()) {
			CompoundTag sectionInfo = (CompoundTag) t;
			int sectionIndex = ((ByteTag) sectionInfo.getValue().get("Y")).getValue().intValue();
			byte[] blockIdsLow = ((ByteArrayTag) sectionInfo.getValue().get("Blocks")).getValue();
			byte[] blockData = ((ByteArrayTag) sectionInfo.getValue().get("Data")).getValue();
			Tag<?> addTag = sectionInfo.getValue().get("Add");
			byte[] blockAdd = null;
			if (addTag != null) {
				blockAdd = ((ByteArrayTag) addTag).getValue();
			}
			short[] destSectionBlockIds = sectionBlockIds[sectionIndex];
			byte[] destSectionData = sectionBlockData[sectionIndex];
			sectionsUsed[sectionIndex] = true;
			for (int y = 0; y < 16; ++y) {
				for (int z = 0; z < 16; ++z) {
					for (int x = 0; x < 16; ++x) {
						int index = y * 256 + z * 16 + x;
						short blockType = (short) (blockIdsLow[index] & 0xFF);
						if (blockAdd != null) {
							blockType |= nybble(blockAdd, index) << 8;
						}
						destSectionBlockIds[index] = blockType;
						destSectionData[index] = nybble(blockData, index);
					}
				}
			}
		}
	}

	/**
	 * Extract a 4-bit integer from a byte in an array, where the first nybble in each byte (even nybble indexes) occupies the lower 4 bits and the second (odd
	 * nybble indexes) occupies the high bits.
	 *
	 * @param arr the source array
	 * @param index the index (in nybbles) of the desired 4 bits
	 * @return the desired 4 bits as the lower bits of a byte
	 */
	protected static final byte nybble(byte[] arr, int index) {
		return (byte) ((index % 2 == 0 ? arr[index / 2] : (arr[index / 2] >> 4)) & 0x0F);
	}
}
