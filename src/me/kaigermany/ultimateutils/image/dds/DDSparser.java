package me.kaigermany.ultimateutils.image.dds;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class DDSparser {
	
	public static BufferedImage[] parse(InputStream is){
		/*
		@Deprecated
		public static BufferedImage parse(InputStream is){
			try{
				TextureImageFormatLoaderTGA loader = new TextureImageFormatLoaderTGA();
				BufferedImage imageToConvert = loader.loadTextureImage(is , true, false);
				return imageToConvert;
			}catch(Exception e){
				e.printStackTrace();
				return null;
			}
		}
		@Deprecated
		public static byte[] decompress(byte[] compressedData, int width, int height, int type){
			Squish.CompressionType t = Squish.CompressionType.DXT1;
			if(type == 2) t = Squish.CompressionType.DXT3;
			if(type == 4) t = Squish.CompressionType.DXT5;
			return DXTBufferDecompressor.squishDecompressToArray(compressedData, width, height, t);
		}
		*/
		try{
			DataInputStream dis = new DataInputStream(is);
			Header header = new Header();
			header.read(dis);
			System.out.println("size: " + header.width + " | " + header.height);
			
			final int DDSD_LINEARSIZE = 0x00080000; // dwLinearSize is valid
			final int DDPF_FOURCC = 0x00000004; // FourCC code is valid
			if ((header.pfFlags & DDPF_FOURCC) != 0 && (header.flags & DDSD_LINEARSIZE) == 0) {
				// Figure out how big the linear size should be
				int depth = header.backBufferCountOrDepth;
				if (depth == 0) {
					depth = 1;
				}
				//System.out.println(header.width + " | " + header.height);
				
				header.pitchOrLinearSize = computeCompressedBlockSize(header.width, header.height, depth, header.pfFourCC);
				header.flags |= DDSD_LINEARSIZE;
			}
			
			/*
			final int side = 0;
			int numLevels = getNumMipMaps(header);
			if (numLevels == 0) {
				numLevels = 1;
			}
			ImageInfo[] result = new ImageInfo[numLevels];
			for (int i = 0; i < numLevels; i++) {
				result[i] = getMipMap(header, side, i);
			}
			*/
			int[] midMapByteSizes;
			{
				int numLevels = getNumMipMaps(header);
				if (numLevels == 0) {
					numLevels = 1;
				}
				midMapByteSizes = new int[numLevels];
				for (int i = 0; i < numLevels; i++) {
					midMapByteSizes[i] = mipMapSizeInBytes(header, i);
				}
			}
			int cubepages = isCubemap(header) ? 6 : 1;
			byte[][] frames = new byte[cubepages * midMapByteSizes.length][];
			for(int i=0; i<frames.length; i++) {
				byte[] arr = new byte[midMapByteSizes[i / cubepages]];
				dis.readFully(arr);
				//for(int ii=1; ii<midMapByteSizes.length; ii++) dis.skip(midMapByteSizes[ii]);
				frames[i] = arr;
			}
			
			/*
			// Figure out how far to seek
			int seek = 0;//Header.writtenSize();
			if (isCubemap(header)) {
				seek += sideShiftInBytes(header, side);
			}
			for (int i = 0; i < map; i++) {
				seek += mipMapSizeInBytes(header, i);
			}
			buf.limit(seek + mipMapSizeInBytes(header, map));
			buf.position(seek);
			ByteBuffer next = buf.slice();
			buf.position(0);
			buf.limit(buf.capacity());
			*/
			//final int DDPF_FOURCC = 0x00000004; // FourCC code is valid
			//return new ImageInfo(next, Math.max(header.width >>>= map, 1), Math.max(header.height >>>= map, 1), (header.pfFlags & DDPF_FOURCC) != 0, header.pfFourCC);
			BufferedImage[] out = new BufferedImage[frames.length];
			for(int i=0; i<frames.length; i++) {
				int w = header.width >> (i / cubepages);
				int h = header.height >> (i / cubepages);
				
				System.out.println(w + " | " + h);
				//byte[] arr1 = new byte[header.width * header.height * 4];
				//arr1 = Squish.decompressImage(arr1, header.width, header.height, frames[i], PixelFormats.getSquishCompressionFormat(header.pfFourCC));
				BufferedImage img = new BufferedImage(w, h, 2);
				
				//ByteBuffer.wrap(arr1).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(
				//	((DataBufferInt)img.getRaster().getDataBuffer()).getData()
				//);
				if(header.pfFourCC == 0){
					try{
						int[] bufPtr = ((DataBufferInt)img.getRaster().getDataBuffer()).getData();
						ByteBuffer.wrap(frames[i]).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(bufPtr);
					}catch(Exception e){
						e.printStackTrace();
					}
				} else{
					decompressImage(((DataBufferInt)img.getRaster().getDataBuffer()).getData(), w, h, frames[i], getSquishCompressionFormat(header.pfFourCC));
				}
				
				out[i] = img;
			}
			return out;
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
	public static CompressionType getSquishCompressionFormat(final int pixelFormat) throws IOException {
		final int D3DFMT_DXT1 = 0x31545844;
		final int D3DFMT_DXT3 = 0x33545844;
		final int D3DFMT_DXT5 = 0x35545844;
		switch(pixelFormat) { 
		case D3DFMT_DXT1: 
			return CompressionType.DXT1;
		case D3DFMT_DXT3: 
			return CompressionType.DXT3;
		case D3DFMT_DXT5: 
			return CompressionType.DXT5;
		default:
			throw new IOException("UnsupportedDataTypeException: given pixel format not supported compression format (" + Integer.toHexString(pixelFormat) + ")");
		}
	}
	
	public static int getNumMipMaps(Header header) {
		final int DDSD_MIPMAPCOUNT = 0x00020000; // Mip map count is valid
		return (header.flags & DDSD_MIPMAPCOUNT) != 0 ? header.mipMapCountOrAux : 0;
	}
	
	private static int computeCompressedBlockSize(int width, int height, int depth, int compressionFormat) {
		final int D3DFMT_DXT1 =  0x31545844;
		int blockSize = ((width + 3)/4) * ((height + 3)/4) * ((depth + 3)/4);
		switch (compressionFormat) {
		case D3DFMT_DXT1:  blockSize *=  8; break;
		default:           blockSize *= 16; break;
		}
		return blockSize;
	}
	
	public static boolean isCubemap(Header header) {
		final int DDSCAPS_COMPLEX = 0x00000008; // Complex surface structure, such as a cube map
		final int DDSCAPS2_CUBEMAP = 0x00000200;
		return ((header.ddsCaps1 & DDSCAPS_COMPLEX) != 0) && ((header.ddsCaps2 & DDSCAPS2_CUBEMAP) != 0);
	}
	/*
	private static int sideShiftInBytes(Header header, int side) {
		final int DDSCAPS2_CUBEMAP_POSITIVEX = 0x00000400;
		final int DDSCAPS2_CUBEMAP_NEGATIVEX = 0x00000800;
		final int DDSCAPS2_CUBEMAP_POSITIVEY = 0x00001000;
		final int DDSCAPS2_CUBEMAP_NEGATIVEY = 0x00002000;
		final int DDSCAPS2_CUBEMAP_POSITIVEZ = 0x00004000;
		final int DDSCAPS2_CUBEMAP_NEGATIVEZ = 0x00008000;
		final int[] sides = {
				DDSCAPS2_CUBEMAP_POSITIVEX,
				DDSCAPS2_CUBEMAP_NEGATIVEX,
				DDSCAPS2_CUBEMAP_POSITIVEY,
				DDSCAPS2_CUBEMAP_NEGATIVEY,
				DDSCAPS2_CUBEMAP_POSITIVEZ,
				DDSCAPS2_CUBEMAP_NEGATIVEZ
		};

		int shift = 0;
		int sideSize = sideSizeInBytes(header);
		for (int i = 0; i < sides.length; i++) {
			int temp = sides[i];
			if ((temp & side) != 0) {
				return shift;
			}

			shift += sideSize;
		}

		throw new RuntimeException("Illegal side: " + side);
	}
	*/
	
	/*
	private static int sideSizeInBytes(Header header) {
		int numLevels = getNumMipMaps(header);
		if (numLevels == 0) {
			numLevels = 1;
		}

		int size = 0;
		for (int i = 0; i < numLevels; i++) {
			size += mipMapSizeInBytes(header, i);
		}

		return size;
	}
	*/
	
	public static int mipMapSizeInBytes(Header header, int map) {
		int width  = Math.max(header.width >>> map, 1);
		int height = Math.max(header.height >>> map, 1);
		final int DDPF_FOURCC = 0x00000004; // FourCC code is valid
		if ((header.pfFlags & DDPF_FOURCC) != 0) {
			final int D3DFMT_DXT1 =  0x31545844;
			int blockSize = (header.pfFourCC == D3DFMT_DXT1 ? 8 : 16);
			return ((width+3)/4)*((height+3)/4)*blockSize;
		} else {
			return width * height * (header.pfRGBBitCount / 8);
		}
	}
	
	private static int swapEndian(int a) {
		return (a >>> 24) | (((a >> 16) & 0xFF) << 8)  | (((a >> 8) & 0xFF) << 16)  | (a << 24);
	}
	
	public static class Header {
		int size;                 // size of the DDSURFACEDESC structure
		int flags;                // determines what fields are valid
		int height;               // height of surface to be created
		int width;                // width of input surface
		int pitchOrLinearSize;
		int backBufferCountOrDepth;
		int mipMapCountOrAux;     // number of mip-map levels requested (in this context)
		int alphaBitDepth;        // depth of alpha buffer requested
		int reserved1;            // reserved
		int surface;              // pointer to the associated surface memory
		// NOTE: following two entries are from DDCOLORKEY data structure
		// Are overlaid with color for empty cubemap faces (unused in this reader)
		int colorSpaceLowValue;
		int colorSpaceHighValue;
		int destBltColorSpaceLowValue;
		int destBltColorSpaceHighValue;
		int srcOverlayColorSpaceLowValue;
		int srcOverlayColorSpaceHighValue;
		int srcBltColorSpaceLowValue;
		int srcBltColorSpaceHighValue;
		// NOTE: following entries are from DDPIXELFORMAT data structure
		// Are overlaid with flexible vertex format description of vertex
		// buffers (unused in this reader)
		int pfSize;                 // size of DDPIXELFORMAT structure
		int pfFlags;                // pixel format flags
		int pfFourCC;               // (FOURCC code)
		// Following five entries have multiple interpretations, not just
		// RGBA (but that's all we support right now)
		int pfRGBBitCount;          // how many bits per pixel
		int pfRBitMask;             // mask for red bits
		int pfGBitMask;             // mask for green bits
		int pfBBitMask;             // mask for blue bits
		int pfABitMask;             // mask for alpha channel
		int ddsCaps1;               // Texture and mip-map flags
		int ddsCaps2;               // Advanced capabilities including cubemap support
		int ddsCapsReserved1;
		int ddsCapsReserved2;
		int textureStage;           // stage in multitexture cascade

		void read(DataInputStream dis) throws IOException {
			final int MAGIC = 0x20534444;
			int magic                     = swapEndian(dis.readInt());
			if (magic != MAGIC) {
				throw new IOException("Incorrect magic number 0x" +
						Integer.toHexString(magic) +
						" (expected " + MAGIC + ")");
			}

			size                          = swapEndian(dis.readInt());
			flags                         = swapEndian(dis.readInt());
			height                        = swapEndian(dis.readInt());
			width                         = swapEndian(dis.readInt());
			pitchOrLinearSize             = swapEndian(dis.readInt());
			backBufferCountOrDepth        = swapEndian(dis.readInt());
			mipMapCountOrAux              = swapEndian(dis.readInt());
			alphaBitDepth                 = swapEndian(dis.readInt());
			reserved1                     = swapEndian(dis.readInt());
			surface                       = swapEndian(dis.readInt());
			colorSpaceLowValue            = swapEndian(dis.readInt());
			colorSpaceHighValue           = swapEndian(dis.readInt());
			destBltColorSpaceLowValue     = swapEndian(dis.readInt());
			destBltColorSpaceHighValue    = swapEndian(dis.readInt());
			srcOverlayColorSpaceLowValue  = swapEndian(dis.readInt());
			srcOverlayColorSpaceHighValue = swapEndian(dis.readInt());
			srcBltColorSpaceLowValue      = swapEndian(dis.readInt());
			srcBltColorSpaceHighValue     = swapEndian(dis.readInt());
			pfSize                        = swapEndian(dis.readInt());
			pfFlags                       = swapEndian(dis.readInt());
			pfFourCC                      = swapEndian(dis.readInt());
			pfRGBBitCount                 = swapEndian(dis.readInt());
			pfRBitMask                    = swapEndian(dis.readInt());
			pfGBitMask                    = swapEndian(dis.readInt());
			pfBBitMask                    = swapEndian(dis.readInt());
			pfABitMask                    = swapEndian(dis.readInt());
			ddsCaps1                      = swapEndian(dis.readInt());
			ddsCaps2                      = swapEndian(dis.readInt());
			ddsCapsReserved1              = swapEndian(dis.readInt());
			ddsCapsReserved2              = swapEndian(dis.readInt());
			textureStage                  = swapEndian(dis.readInt());
		}

		// buf must be in little-endian byte order
		void write(DataOutputStream dos) throws IOException {
			final int MAGIC = 0x20534444;
			dos.writeInt(swapEndian(MAGIC));
			dos.writeInt(swapEndian(size));
			dos.writeInt(swapEndian(flags));
			dos.writeInt(swapEndian(height));
			dos.writeInt(swapEndian(width));
			dos.writeInt(swapEndian(pitchOrLinearSize));
			dos.writeInt(swapEndian(backBufferCountOrDepth));
			dos.writeInt(swapEndian(mipMapCountOrAux));
			dos.writeInt(swapEndian(alphaBitDepth));
			dos.writeInt(swapEndian(reserved1));
			dos.writeInt(swapEndian(surface));
			dos.writeInt(swapEndian(colorSpaceLowValue));
			dos.writeInt(swapEndian(colorSpaceHighValue));
			dos.writeInt(swapEndian(destBltColorSpaceLowValue));
			dos.writeInt(swapEndian(destBltColorSpaceHighValue));
			dos.writeInt(swapEndian(srcOverlayColorSpaceLowValue));
			dos.writeInt(swapEndian(srcOverlayColorSpaceHighValue));
			dos.writeInt(swapEndian(srcBltColorSpaceLowValue));
			dos.writeInt(swapEndian(srcBltColorSpaceHighValue));
			dos.writeInt(swapEndian(pfSize));
			dos.writeInt(swapEndian(pfFlags));
			dos.writeInt(swapEndian(pfFourCC));
			dos.writeInt(swapEndian(pfRGBBitCount));
			dos.writeInt(swapEndian(pfRBitMask));
			dos.writeInt(swapEndian(pfGBitMask));
			dos.writeInt(swapEndian(pfBBitMask));
			dos.writeInt(swapEndian(pfABitMask));
			dos.writeInt(swapEndian(ddsCaps1));
			dos.writeInt(swapEndian(ddsCaps2));
			dos.writeInt(swapEndian(ddsCapsReserved1));
			dos.writeInt(swapEndian(ddsCapsReserved2));
			dos.writeInt(swapEndian(textureStage));
		}
	}
	
	public static void decompressImage(int[] rgba, final int width, final int height, final byte[] blocks, final CompressionType type) {
		System.out.println("DDS type: " + type);
		//rgba = checkDecompressInput(rgba, width, height, blocks, type);

		final int[] targetRGBA = new int[16];

		// loop over blocks
		int sourceBlock = 0;
		for ( int y = 0; y < height; y += 4 ) {
			for ( int x = 0; x < width; x += 4 ) {
				// decompress the block
				decompress(targetRGBA, blocks, sourceBlock, type);

				// write the decompressed pixels to the correct image locations
				int sourcePixel = 0;
				for ( int py = 0; py < 4; ++py ) {
					for ( int px = 0; px < 4; ++px ) {
						// get the target location
						int sx = x + px;
						int sy = y + py;
						if ( sx < width && sy < height ) {
							rgba[(width * sy + sx)] = targetRGBA[sourcePixel];
						}
						sourcePixel++;
					}
				}

				// advance
				sourceBlock += type.blockSize;
			}
		}
	}
/*
	private static byte[] checkDecompressInput(byte[] rgba, final int width, final int height, final byte[] blocks, final CompressionType type) {
		final int storageSize = getStorageRequirements(width, height, type);

		if ( blocks == null || blocks.length < storageSize )
			throw new IllegalArgumentException("Invalid source image data specified.");

		if ( rgba == null || rgba.length < (width * height * 4) )
			rgba = new byte[(width * height * 4)];

		return rgba;
	}
	*/
	
	private static void decompress(final int[] rgba, final byte[] block, final int offset, final CompressionType type) {
		// decompress colour
		ColourBlock.decompressColour(rgba, block, offset + type.blockOffset, type == CompressionType.DXT1);

		// decompress alpha separately if necessary
		if ( type == CompressionType.DXT3 )
			CompressorAlpha.decompressAlphaDxt3(rgba, block, offset);
		else if ( type == CompressionType.DXT5 )
			CompressorAlpha.decompressAlphaDxt5(rgba, block, offset);
	}
	
	public static final class Vec {

		private float x;
		private float y;
		private float z;

		public Vec() {
		}

		public Vec(final float a) {
			this(a, a, a);
		}

		public Vec(final float a, final float b, final float c) {
			x = a;
			y = b;
			z = c;
		}

		public float x() { return x; }

		public float y() { return y; }

		public float z() { return z; }

		public Vec set(final float a) {
			this.x = a;
			this.y = a;
			this.z = a;

			return this;
		}

		public Vec set(final float x, final float y, final float z) {
			this.x = x;
			this.y = y;
			this.z = z;

			return this;
		}

		public Vec set(final Vec v) {
			this.x = v.x;
			this.y = v.y;
			this.z = v.z;

			return this;
		}

		public Vec add(final Vec v) {
			x += v.x;
			y += v.y;
			z += v.z;

			return this;
		}

		public Vec add(final float x, final float y, final float z) {
			this.x += x;
			this.y += y;
			this.z += z;

			return this;
		}

		public Vec sub(final Vec v) {
			x -= v.x;
			y -= v.y;
			z -= v.z;

			return this;
		}

		public Vec mul(final float s) {
			x *= s;
			y *= s;
			z *= s;

			return this;
		}

		public Vec div(final float s) {
			final float t = 1.0f / s;

			x *= t;
			y *= t;
			z *= t;

			return this;
		}

		public float dot(final Vec v) {
			return x * v.x + y * v.y + z * v.z;
		}

	}
	
	public static final class ColourBlock {

		private static final int[] remapped = new int[16];

		private static final int[] indices = new int[16];

		private static final int[] codes = new int[16];

		private ColourBlock() {}

		protected static final float GRID_X = 31.0f;
		protected static final float GRID_Y = 63.0f;
		protected static final float GRID_Z = 31.0f;
		

		private static int floatTo565(final Vec colour) {
			// get the components in the correct range
			final int r = Math.round(GRID_X * colour.x());
			final int g = Math.round(GRID_Y * colour.y());
			final int b = Math.round(GRID_Z * colour.z());

			// pack into a single value
			return (r << 11) | (g << 5) | b;
		}

		private static void writeColourBlock(final int a, final int b, final int[] indices, final byte[] block, final int offset) {
			// write the endpoints
			block[offset + 0] = (byte)(a & 0xff);
			block[offset + 1] = (byte)(a >> 8);
			block[offset + 2] = (byte)(b & 0xff);
			block[offset + 3] = (byte)(b >> 8);

			// write the indices
			for ( int i = 0; i < 4; ++i ) {
				final int index = 4 * i;
				block[offset + 4 + i] = (byte)(indices[index + 0] | (indices[index + 1] << 2) | (indices[index + 2] << 4) | (indices[index + 3] << 6));
			}
		}

		public static void writeColourBlock3(final Vec start, final Vec end, final int[] indices, final byte[] block, final int offset) {
			// get the packed values
			int a = floatTo565(start);
			int b = floatTo565(end);

			// remap the indices
			if ( a <= b ) {
				// use the indices directly
				System.arraycopy(indices, 0, remapped, 0, 16);
			} else {
				// swap a and b
				final int tmp = a;
				a = b;
				b = tmp;
				for ( int i = 0; i < 16; ++i ) {
					if ( indices[i] == 0 )
						remapped[i] = 1;
					else if ( indices[i] == 1 )
						remapped[i] = 0;
					else
						remapped[i] = indices[i];
				}
			}

			// write the block
			writeColourBlock(a, b, remapped, block, offset);
		}

		public static void writeColourBlock4(final Vec start, final Vec end, final int[] indices, final byte[] block, final int offset) {
			// get the packed values
			int a = floatTo565(start);
			int b = floatTo565(end);

			// remap the indices

			if ( a < b ) {
				// swap a and b
				final int tmp = a;
				a = b;
				b = tmp;
				for ( int i = 0; i < 16; ++i )
					remapped[i] = (indices[i] ^ 0x1) & 0x3;
			} else if ( a == b ) {
				// use index 0
				Arrays.fill(remapped, 0);
			} else {
				// use the indices directly
				System.arraycopy(indices, 0, remapped, 0, 16);
			}

			// write the block
			writeColourBlock(a, b, remapped, block, offset);
		}

		static void decompressColour(final int[] rgba, final byte[] block, final int offset, final boolean isDXT1) {
			//byte[] rgba2 = new byte[rgba.length * 4];
			// unpack the endpoints
			final int[] codes = ColourBlock.codes;

			final int a = unpack565(block, offset, codes, 0);
			final int b = unpack565(block, offset + 2, codes, 4);

			// generate the midpoints
			for ( int i = 0; i < 3; ++i ) {//foreach r,g,b:
				final int c = codes[i];
				final int d = codes[4 + i];

				if ( isDXT1 && a <= b ) {
					codes[8 + i] = (c + d) / 2;
					codes[12 + i] = 0;
				} else {
					codes[8 + i] = (2 * c + d) / 3;
					codes[12 + i] = (c + 2 * d) / 3;
				}
			}

			// fill in alpha for the intermediate values
			codes[8 + 3] = 255;
			codes[12 + 3] = (isDXT1 && a <= b) ? 0 : 255;

			// unpack the indices
			final int[] indices = ColourBlock.indices;

			for ( int i = 0; i < 4; ++i ) {
				final int index = 4 * i;
				final int packed = (block[offset + 4 + i] & 0xFF);

				indices[index + 0] = packed & 0x3;
				indices[index + 1] = (packed >> 2) & 0x3;
				indices[index + 2] = (packed >> 4) & 0x3;
				indices[index + 3] = (packed >> 6) & 0x3;
			}

			// store out the colours
			for ( int i = 0; i < 16; ++i ) {
				final int index = 4 * indices[i];
				//for ( int j = 0; j < 4; ++j )
				//	rgba2[4 * i + j] = (byte)codes[index + j];
				rgba[i] = codes[index] | (codes[index+1] << 8) | (codes[index+2] << 16) | (codes[index+3] << 24);
			}
			
			//ByteBuffer.wrap(rgba2).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(rgba);
		}

		private static int unpack565(final byte[] packed, final int pOffset, final int[] colour, final int cOffset) {
			// build the packed value
			int value = (packed[pOffset + 0] & 0xff) | ((packed[pOffset + 1] & 0xff) << 8);

			// get the components in the stored range
			int red = (value >> 11) & 0x1f;
			int green = (value >> 5) & 0x3f;
			int blue = value & 0x1f;

			// scale up to 8 bits
			colour[cOffset + 2] = (red << 3) | (red >> 2);
			colour[cOffset + 1] = (green << 2) | (green >> 4);
			colour[cOffset + 0] = (blue << 3) | (blue >> 2);
			colour[cOffset + 3] = 255;

			// return the value
			return value;
		}

	}
	
	public static final class CompressorAlpha {

		private static final int[] swapped = new int[16];

		private static final int[] codes5 = new int[8];
		private static final int[] codes7 = new int[8];

		private static final int[] indices5 = new int[16];
		private static final int[] indices7 = new int[16];

		private static final int[] codes = new int[8];
		private static final int[] indices = new int[16];

		private CompressorAlpha() {}

		static void compressAlphaDxt3(final byte[] rgba, final int mask, final byte[] block, final int offset) {
			// quantise and pack the alpha values pairwise
			for ( int i = 0; i < 8; ++i ) {
				// quantise down to 4 bits
				final float alpha1 = (rgba[8 * i + 3] & 0xFF) * (15.0f / 255.0f);
				final float alpha2 = (rgba[8 * i + 7] & 0xFF) * (15.0f / 255.0f);
				int quant1 = Math.round(alpha1);
				int quant2 = Math.round(alpha2);

				// set alpha to zero where masked
				final int bit1 = 1 << (2 * i);
				final int bit2 = 1 << (2 * i + 1);
				if ( (mask & bit1) == 0 )
					quant1 = 0;
				if ( (mask & bit2) == 0 )
					quant2 = 0;

				// pack into the byte
				block[offset + i] = (byte)(quant1 | (quant2 << 4));
			}
		}

		static void decompressAlphaDxt3(final int[] rgba, final byte[] block, final int offset) {
			/*
			byte[] rgba2 = new byte[rgba.length * 4];
			ByteBuffer bb = ByteBuffer.allocate(16*4);
			bb.order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().put(rgba);
			bb.rewind();
			bb.get(rgba2);
			*/
			// unpack the alpha values pairwise
			for ( int i = 0; i < 8; ++i ) {
				// quantise down to 4 bits
				final int quant = (block[offset + i] & 0xFF);

				// unpack the values
				int lo = quant & 0x0f;
				int hi = (quant >> 4) & 0x0f;

				// convert back up to bytes
				//rgba2[8 * i + 3] = (byte)(lo | (lo << 4));
				//rgba2[8 * i + 7] = (byte)(hi | (hi >> 4));
				rgba[i*2] = (rgba[i*2] & 0x00FFFFFF) | ((lo | (lo << 4)) << 24);
				rgba[i*2+1] = (rgba[i*2+1] & 0x00FFFFFF) | ((hi | (hi << 4)) << 24);
				
			}
			//ByteBuffer.wrap(rgba2).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(rgba);
		}

		private static int fitCodes(final byte[] rgba, final int mask, final int[] codes, final int[] indices) {
			// fit each alpha value to the codebook
			int err = 0;
			for ( int i = 0; i < 16; ++i ) {
				// check this pixel is valid
				final int bit = 1 << i;
				if ( (mask & bit) == 0 ) {
					// use the first code
					indices[i] = 0;
					continue;
				}

				// find the least error and corresponding index
				final int value = (rgba[4 * i + 3] & 0xFF);
				int least = Integer.MAX_VALUE;
				int index = 0;
				for ( int j = 0; j < 8; ++j ) {
					// get the squared error from this code
					int dist = value - codes[j];
					dist *= dist;

					// compare with the best so far
					if ( dist < least ) {
						least = dist;
						index = j;
					}
				}

				// save this index and accumulate the error
				indices[i] = index;
				err += least;
			}

			// return the total error
			return err;
		}

		private static void writeAlphaBlock(final int alpha0, final int alpha1, final int[] indices, final byte[] block, final int offset) {
			// write the first two bytes
			block[offset + 0] = (byte)alpha0;
			block[offset + 1] = (byte)alpha1;

			// pack the indices with 3 bits each
			int src = 0;
			int dest = 2;
			for ( int i = 0; i < 2; ++i ) {
				// pack 8 3-bit values
				int value = 0;
				for ( int j = 0; j < 8; ++j ) {
					final int index = indices[src++];
					value |= (index << 3 * j);
				}

				// store in 3 bytes
				for ( int j = 0; j < 3; ++j )
					block[offset + dest++] = (byte)((value >> 8 * j) & 0xff);
			}
		}

		private static void writeAlphaBlock5(final int alpha0, final int alpha1, final int[] indices, final byte[] block, final int offset) {
			// check the relative values of the endpoints
			final int[] swapped = CompressorAlpha.swapped;

			if ( alpha0 > alpha1 ) {
				// swap the indices
				for ( int i = 0; i < 16; ++i ) {
					int index = indices[i];
					if ( index == 0 )
						swapped[i] = 1;
					else if ( index == 1 )
						swapped[i] = 0;
					else if ( index <= 5 )
						swapped[i] = 7 - index;
					else
						swapped[i] = index;
				}

				// write the block
				writeAlphaBlock(alpha1, alpha0, swapped, block, offset);
			} else {
				// write the block
				writeAlphaBlock(alpha0, alpha1, indices, block, offset);
			}
		}

		private static void writeAlphaBlock7(final int alpha0, final int alpha1, final int[] indices, final byte[] block, final int offset) {
			// check the relative values of the endpoints
			final int[] swapped = CompressorAlpha.swapped;

			if ( alpha0 < alpha1 ) {
				// swap the indices
				for ( int i = 0; i < 16; ++i ) {
					int index = indices[i];
					if ( index == 0 )
						swapped[i] = 1;
					else if ( index == 1 )
						swapped[i] = 0;
					else
						swapped[i] = 9 - index;
				}

				// write the block
				writeAlphaBlock(alpha1, alpha0, swapped, block, offset);
			} else {
				// write the block
				writeAlphaBlock(alpha0, alpha1, indices, block, offset);
			}
		}

		static void compressAlphaDxt5(final byte[] rgba, final int mask, final byte[] block, final int offset) {
			// get the range for 5-alpha and 7-alpha interpolation
			int min5 = 255;
			int max5 = 0;
			int min7 = 255;
			int max7 = 0;
			for ( int i = 0; i < 16; ++i ) {
				// check this pixel is valid
				final int bit = 1 << i;
				if ( (mask & bit) == 0 )
					continue;

				// incorporate into the min/max
				final int value = (rgba[4 * i + 3] & 0xFF);
				if ( value < min7 )
					min7 = value;
				if ( value > max7 )
					max7 = value;
				if ( value != 0 && value < min5 )
					min5 = value;
				if ( value != 255 && value > max5 )
					max5 = value;
			}

			// handle the case that no valid range was found
			if ( min5 > max5 )
				min5 = max5;
			if ( min7 > max7 )
				min7 = max7;

			// fix the range to be the minimum in each case
			if ( max5 - min5 < 5 )
				max5 = Math.min(min5 + 5, 255);
			if ( max5 - min5 < 5 )
				min5 = Math.max(0, max5 - 5);

			if ( max7 - min7 < 7 )
				max7 = Math.min(min7 + 7, 255);
			if ( max7 - min7 < 7 )
				min7 = Math.max(0, max7 - 7);

			// set up the 5-alpha code book
			final int[] codes5 = CompressorAlpha.codes5;

			codes5[0] = min5;
			codes5[1] = max5;
			for ( int i = 1; i < 5; ++i )
				codes5[1 + i] = ((5 - i) * min5 + i * max5) / 5;
			codes5[6] = 0;
			codes5[7] = 255;

			// set up the 7-alpha code book
			final int[] codes7 = CompressorAlpha.codes7;

			codes7[0] = min7;
			codes7[1] = max7;
			for ( int i = 1; i < 7; ++i )
				codes7[1 + i] = ((7 - i) * min7 + i * max7) / 7;

			// fit the data to both code books
			int err5 = fitCodes(rgba, mask, codes5, indices5);
			int err7 = fitCodes(rgba, mask, codes7, indices7);

			// save the block with least error
			if ( err5 <= err7 )
				writeAlphaBlock5(min5, max5, indices5, block, offset);
			else
				writeAlphaBlock7(min7, max7, indices7, block, offset);
		}

		static void decompressAlphaDxt5(final int[] rgba, final byte[] block, final int offset) {
			/*
			byte[] rgba2 = new byte[rgba.length * 4];
			ByteBuffer bb = ByteBuffer.allocate(16*4);
			bb.order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().put(rgba);
			bb.rewind();
			bb.get(rgba2);
			*/
			// get the two alpha values
			final int alpha0 = (block[offset + 0] & 0xFF);
			final int alpha1 = (block[offset + 1] & 0xFF);

			// compare the values to build the codebook
			final int[] codes = CompressorAlpha.codes;

			codes[0] = alpha0;
			codes[1] = alpha1;
			if ( alpha0 <= alpha1 ) {
				// use 5-alpha codebook
				for ( int i = 1; i < 5; ++i )
					codes[1 + i] = ((5 - i) * alpha0 + i * alpha1) / 5;
				codes[6] = 0;
				codes[7] = 255;
			} else {
				// use 7-alpha codebook
				for ( int i = 1; i < 7; ++i )
					codes[1 + i] = ((7 - i) * alpha0 + i * alpha1) / 7;
			}

			// decode the indices
			final int[] indices = CompressorAlpha.indices;

			int src = 2;
			int dest = 0;
			for ( int i = 0; i < 2; ++i ) {
				// grab 3 bytes
				int value = 0;
				for ( int j = 0; j < 3; ++j ) {
					int b = (block[offset + src++] & 0xFF);
					value |= (b << 8 * j);
				}

				// unpack 8 3-bit values from it
				for ( int j = 0; j < 8; ++j ) {
					int index = (value >> 3 * j) & 0x7;
					indices[dest++] = index;
				}
			}

			// write out the indexed codebook values
			for ( int i = 0; i < 16; ++i ) {
				rgba[i] = (rgba[i] & 0x00FFFFFF) | ((codes[indices[i]] & 0xFF) << 24);
			}
			
			
			//ByteBuffer.wrap(rgba2).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(rgba);
		}

	}
	
	public static enum CompressionType {

		DXT1(8),
		DXT3(16),
		DXT5(16);

		public final int blockSize;
		public final int blockOffset;

		CompressionType(final int blockSize) {
			this.blockSize = blockSize;
			this.blockOffset = blockSize - 8;
		}
	}
}
