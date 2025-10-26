package me.kaigermany.ultimateutils.image.dds;

public enum DDSCompressionType {
	DXT1(8),
	DXT3(16),
	DXT5(16);

	public final int blockSize;
	public final int blockOffset;

	DDSCompressionType(final int blockSize) {
		this.blockSize = blockSize;
		this.blockOffset = blockSize - 8;
	}
}
