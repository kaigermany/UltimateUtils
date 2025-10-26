package me.kaigermany.ultimateutils.image.dds;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import me.kaigermany.ultimateutils.image.dds.DDSLookupTables;
import me.kaigermany.ultimateutils.image.dds.DDSparser.ColourBlock;

// heavily based on: https://github.com/Dahie/DDS-Utils/tree/master

public class DDSwriter {

	public static void writeDDS(BufferedImage img, OutputStream os, DDSCompressionType format,	boolean generateMipMaps) throws IOException {
		write(os, img, format, generateMipMaps);
	}
	
	public static void writeDDS(BufferedImage img, OutputStream os) throws IOException {
		write(os, img, DDSCompressionType.DXT5, false);
	}
	/*
	public static boolean isPowerOfTwo(int a){
		return ((a - 1) & a) == 0;//TODO TEST!!!!!
	}
	*/
	public static int calculateMaxNumberOfMipMaps(final int width, final int height) {
		return ((int) Math.floor(Math.log(Math.max(width, height)) / Math.log(2.0)))+1; // plus original
	}
	
	public static boolean isPowerOfTwo(final int value) {
		double p = Math.floor(Math.log(value) / Math.log(2.0));
		double n = Math.pow(2.0, p);
	    return (n==value);
	}
	
	private static boolean hasCubicDimensions(BufferedImage topmost) {
		return !isPowerOfTwo(topmost.getWidth()) && !isPowerOfTwo(topmost.getHeight());
	}

	private static final int D3DFMT_R8G8B8    			= 20;
	private static final int D3DFMT_A8R8G8B8  			= 21;
	private static final int D3DFMT_X8R8G8B8  			= 22;
	private static final int D3DFMT_DXT1 = 0x31545844;
	private static final int D3DFMT_DXT3 = 0x33545844;
	private static final int D3DFMT_DXT5 = 0x35545844;
	// Selected bits in DDS capabilities flags
	private static final int DDSCAPS_TEXTURE      = 0x00001000; // Can be used as a texture
	private static final int DDSCAPS_MIPMAP       = 0x00400000; // Is one level of a mip-map
	private static final int DDSCAPS_COMPLEX      = 0x00000008; // Complex surface structure, such as a cube map

	
	private static final int DDSD_CAPS            = 0x00000001; // Capacities are valid
	private static final int DDSD_HEIGHT          = 0x00000002; // Height is valid
	private static final int DDSD_WIDTH           = 0x00000004; // Width is valid
	private static final int DDSD_PITCH           = 0x00000008; // Pitch is valid
	private static final int DDSD_BACKBUFFERCOUNT = 0x00000020; // Back buffer count is valid
	private static final int DDSD_ZBUFFERBITDEPTH = 0x00000040; // Z-buffer bit depth is valid (shouldn't be used in DDSURFACEDESC2)
	private static final int DDSD_ALPHABITDEPTH   = 0x00000080; // Alpha bit depth is valid
	private static final int DDSD_LPSURFACE       = 0x00000800; // lpSurface is valid
	private static final int DDSD_PIXELFORMAT     = 0x00001000; // ddpfPixelFormat is valid
	private static final int DDSD_MIPMAPCOUNT     = 0x00020000; // Mip map count is valid
	private static final int DDSD_LINEARSIZE      = 0x00080000; // dwLinearSize is valid
	private static final int DDSD_DEPTH           = 0x00800000; // dwDepth is valid

	private static final int DDPF_ALPHAPIXELS     = 0x00000001; // Alpha channel is present
	private static final int DDPF_ALPHA           = 0x00000002; // Only contains alpha information
	private static final int DDPF_FOURCC          = 0x00000004; // FourCC code is valid
	private static final int DDPF_PALETTEINDEXED4 = 0x00000008; // Surface is 4-bit color indexed
	private static final int DDPF_PALETTEINDEXEDTO8=0x00000010; // Surface is indexed into a palette which stores indices
	// into the destination surface's 8-bit palette
	private static final int DDPF_RGB             = 0x00000040; // RGB data is present


	
	
	
	public static void write(OutputStream os, 
			BufferedImage sourceImage, 
			DDSCompressionType format,
			boolean generateMipMaps) throws IOException {
		
		int width = sourceImage.getWidth();
		int height = sourceImage.getHeight();
		
		//convert RGB to RGBA image
		sourceImage = convert(sourceImage);
		
		
		byte[][] mipmapBuffer = getDXTCompressedBuffer(generateMipmaps(sourceImage, generateMipMaps), format);
		write(format, width, height, mipmapBuffer, os);
	}

	private static byte[][] getDXTCompressedBuffer(ArrayList<BufferedImage> mipmaps, DDSCompressionType compressionType) {
		byte[][] mipmapBuffer = new byte[mipmaps.size()][];
		for (int j = 0; j < mipmaps.size(); j++) {
			BufferedImage image = mipmaps.get(j);
			byte[] byteData = convertBIintoARGBArray(image);
			mipmapBuffer[j] = compressImage(byteData, image.getWidth(), image.getHeight(), compressionType);
		}
		return mipmapBuffer;
	}
	
	private static BufferedImage convert(final BufferedImage srcImage) {
		if(srcImage.getType() == BufferedImage.TYPE_4BYTE_ABGR) return srcImage;
		BufferedImage img = new BufferedImage(srcImage.getWidth(), srcImage.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g2d = img.createGraphics();
		g2d.drawImage(srcImage, 0, 0, null);
		g2d.dispose();
		return img;
	}
	
	public static void write(DDSCompressionType format, final int width, final int height, final byte[][] mipmapData,
			OutputStream os) throws IOException {
		final int mipmapCount = mipmapData.length;

		int topmostMipmapSize = computeCompressedBlockSize(width, height, 1, format);

		// Allocate and initialize a Header
		Header header = new Header();
		header.size = Header.size();
		header.flags = DDSD_CAPS | DDSD_HEIGHT | DDSD_WIDTH | DDSD_PIXELFORMAT;
		if (mipmapCount > 1) {
			header.flags |= DDSD_MIPMAPCOUNT;
			header.mipMapCountOrAux = mipmapCount;
		}
		header.width = width;
		header.height = height;
		
		int d3dFormat;
		switch (format) {
			default:
			case DXT1: d3dFormat = D3DFMT_DXT1; break;
			case DXT3: d3dFormat = D3DFMT_DXT3; break;
			case DXT5: d3dFormat = D3DFMT_DXT5; break;
		}

		header.flags |= DDSD_LINEARSIZE;
		header.pfFlags |= DDPF_FOURCC;
		header.pfFourCC = d3dFormat;
		header.pitchOrLinearSize = topmostMipmapSize;
		header.pfSize = Header.pfSize();
		// Not sure whether we can get away with leaving the rest of the
		// header blank

		header.ddsCaps1 = DDSCAPS_TEXTURE | DDSCAPS_MIPMAP | DDSCAPS_COMPLEX;

		os.write(header.write());
		for (int i = 0; i < mipmapCount; i++) {
			os.write(mipmapData[i]);
		}
		os.close();
	}

	private static int computeCompressedBlockSize(final int width, final int height, final int depth,
			DDSCompressionType format) {
		int blockSize = ((width + 3) / 4) * ((height + 3) / 4) * ((depth + 3) / 4);
		if (format == DDSCompressionType.DXT1) {
			blockSize *= 8;
		} else {// DXT3, DXT5
			blockSize *= 16;
		}
		return blockSize;
	}
	
	public static class Header {
		private static final int MAGIC = 0x20534444;
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

		byte[] write(){
			byte[] headerBytes = new byte[Header.writtenSize()];
			ByteBuffer hdr = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN);
			write(hdr);
			return headerBytes;
		}
		
		// buf must be in little-endian byte order
		void write(final ByteBuffer buf) {
			buf.putInt(MAGIC);
			buf.putInt(size);
			buf.putInt(flags);
			buf.putInt(height);
			buf.putInt(width);
			buf.putInt(pitchOrLinearSize);
			buf.putInt(backBufferCountOrDepth);
			buf.putInt(mipMapCountOrAux);
			buf.putInt(alphaBitDepth);
			buf.putInt(reserved1);
			buf.putInt(surface);
			buf.putInt(colorSpaceLowValue);
			buf.putInt(colorSpaceHighValue);
			buf.putInt(destBltColorSpaceLowValue);
			buf.putInt(destBltColorSpaceHighValue);
			buf.putInt(srcOverlayColorSpaceLowValue);
			buf.putInt(srcOverlayColorSpaceHighValue);
			buf.putInt(srcBltColorSpaceLowValue);
			buf.putInt(srcBltColorSpaceHighValue);
			buf.putInt(pfSize);
			buf.putInt(pfFlags);
			buf.putInt(pfFourCC);
			buf.putInt(pfRGBBitCount);
			buf.putInt(pfRBitMask);
			buf.putInt(pfGBitMask);
			buf.putInt(pfBBitMask);
			buf.putInt(pfABitMask);
			buf.putInt(ddsCaps1);
			buf.putInt(ddsCaps2);
			buf.putInt(ddsCapsReserved1);
			buf.putInt(ddsCapsReserved2);
			buf.putInt(textureStage);
		}

		private static int size() {
			return 124;
		}

		private static int pfSize() {
			return 32;
		}

		private static int writtenSize() {
			return 128;
		}
	}
	
	private static ArrayList<BufferedImage> generateMipmaps(BufferedImage image, boolean generateMipMaps){
		ArrayList<BufferedImage> mipmaps = new ArrayList<BufferedImage>(10);
		mipmaps.add(image);
		if(!generateMipMaps) return mipmaps;
		/* disabled square check.
		if (hasCubicDimensions(image)) {
			throw new IllegalArgumentException("NonCubic Dimensions!");
		}
		*/
		// dimensions of first map
		int mipmapWidth = image.getWidth();
		int mipmapHeight = image.getHeight();
		while(true){
			mipmapWidth = Math.max(mipmapWidth >> 1, 1);
			mipmapHeight = Math.max(mipmapHeight >> 1, 1);
			image = rescaleBI(image, mipmapWidth, mipmapHeight);
			mipmaps.add(image);
			if(mipmapWidth == 1 && mipmapWidth == 1) return mipmaps;
		}
	}


	public static byte[] convertBIintoARGBArray(final BufferedImage bi) {
		DataBuffer dataBuffer = bi.getRaster().getDataBuffer();
		final int componentCount = bi.getColorModel().getNumComponents();

		int width = bi.getWidth();
		int height = bi.getHeight();
		int bufferedImageType = bi.getType();
		
		int length = height * width * 4;
		byte[] argb = new byte[length];
	
		int r, g, b, a;
		int count = 0;
	//	if() TODO FIXME, what is the other supported?
	//		throw new UnsupportedDataTypeException("BufferedImages types TYPE_4BYTE_ABGR supported")
		if(length != dataBuffer.getSize())
			throw new IllegalStateException("Databuffer has not the expected length: " + dataBuffer.getSize()+ " instead of " + length);
		
		for (int i = 0; i < dataBuffer.getSize(); i=i+componentCount) {
			// databuffer has unsigned integers, they must be converted to signed byte 
			// original order from BufferedImage
	//		
			if(componentCount > 3) {
				// 32bit image
				if (bufferedImageType != BufferedImage.TYPE_4BYTE_ABGR) {
					/* working with png+alpha */
					a =  (dataBuffer.getElem(i) );
					r =  (dataBuffer.getElem(i+1));
					g =  (dataBuffer.getElem(i+2));
					b =  (dataBuffer.getElem(i+3));
				} else {
					/* not working with png+alpha */
					b =  (dataBuffer.getElem(i) );
					g =  (dataBuffer.getElem(i+1));
					r =  (dataBuffer.getElem(i+2));
					a =  (dataBuffer.getElem(i+3));
				}
					
				argb[i] =   (byte) (a & 0xFF);
				argb[i+1] = (byte) (r & 0xFF);
				argb[i+2] = (byte) (g & 0xFF);
				argb[i+3] = (byte) (b & 0xFF);
			} 
			else 
			{ //24bit image
				
				b =  (dataBuffer.getElem(count));
				count++;
				g =  (dataBuffer.getElem(count));
				count++;
				r =  (dataBuffer.getElem(count));
				count++;
	
				argb[i] = (byte) (255);
				argb[i+1] = (byte) (r & 0xFF);
				argb[i+2] = (byte) (g & 0xFF);
				argb[i+3] = (byte) (b & 0xFF);
			}
		}
		// aim should be ARGB order
		return argb;
	}


	

	private static int calcBlockSize(final int width, final int height, final DDSCompressionType type) {
		return ((width + 3) / 4) * ((height + 3) / 4) * type.blockSize;
	}


	private static byte[] compressImage(final byte[] rgba, final int width, final int height, final DDSCompressionType type) {
		int storageRequirements = calcBlockSize(width, height, type);
		byte[] compressedBytes = new byte[storageRequirements];
		final boolean weightAlpha = false;

		final byte[] sourceRGBA = new byte[16 * 4];

		// loop over blocks
		int targetBlock = 0;
		for (int y = 0; y < height; y += 4) {
			for (int x = 0; x < width; x += 4) {
				// build the 4x4 block of pixels
				int targetPixel = 0;
				int enabledPixelMask = 0;
				for (int py = 0; py < 4; ++py) {
					final int sy = y + py;
					for (int px = 0; px < 4; ++px) {
						// get the source pixel in the image
						final int sx = x + px;

						// enable if we're in the image
						if (sx < width && sy < height) {
							// copy the rgba value
							int sourcePixel = 4 * (width * sy + sx);
							for (int i = 0; i < 4; ++i) {
								// int b = (byte) (rgba[sourcePixel++] & 0xFF);
								// System.out.println(b);
								sourceRGBA[targetPixel++] = (byte) rgba[sourcePixel++];
							}

							// enable this pixel
							enabledPixelMask |= (1 << (4 * py + px));
						} else {
							// skip this pixel as its outside the image
							targetPixel += 4;
						}
					}
				}

				// compress it into the output
				compressBlock(sourceRGBA, enabledPixelMask, compressedBytes, targetBlock, type, weightAlpha);

				// advance
				targetBlock += type.blockSize;
			}
		}

		return compressedBytes;
	}

	private static void compressBlock(final byte[] rgba, final int enabledPixelMask, final byte[] compressedBytes, final int offset,
			final DDSCompressionType type, final boolean weightAlpha) {
		// get the block locations
		ColourSet colours = new ColourSet();
		final int colourBlock = offset + type.blockOffset;
		final int alphaBlock = offset;

		// create the minimal point set
		colours.init(rgba, enabledPixelMask, type, weightAlpha);

		// check the compression type and compress colour
		if (colours.getCount() == 1) {// always do a single colour fit
			CompressorSingleColour(colours, type, compressedBytes, colourBlock);
		} else {
			CompressCluster(colours, type, compressedBytes, colourBlock);
		}

		// compress alpha separately if necessary
		if (type == DDSCompressionType.DXT3)
			compressAlphaDxt3(rgba, enabledPixelMask, compressedBytes, alphaBlock);
		else if (type == DDSCompressionType.DXT5)
			compressAlphaDxt5(rgba, enabledPixelMask, compressedBytes, alphaBlock);
	}




	public static BufferedImage rescaleBI(final BufferedImage originalImage, final int newWidth, final int newHeight) {
		Image rescaledImage = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
		if (rescaledImage instanceof BufferedImage){
			return (BufferedImage) rescaledImage;
		}
		
		BufferedImage bi = new BufferedImage(rescaledImage.getWidth(null), rescaledImage.getHeight(null), BufferedImage.TYPE_4BYTE_ABGR);
	    Graphics g = bi.createGraphics();
	    g.drawImage(rescaledImage, 0, 0, null);
	    g.dispose();
		return bi;
	}


	public static class ColourSet {
		private int count;
	
		private final DDSVec[] points = new DDSVec[16];
		private final float[] weights = new float[16];
		private final int[] remap = new int[16];
	
		private boolean transparent;
	
		ColourSet() {
			for ( int i = 0; i < points.length; i++ )
				points[i] = new DDSVec();
		}
	
		void init(final byte[] rgba, final int enabledPixelMask, final DDSCompressionType type, final boolean weightAlpha) {
			// check the compression mode for dxt1
			final boolean isDXT1 = type == DDSCompressionType.DXT1;
	
			count = 0;
			transparent = false;
	
			// create the minimal set
			for ( int i = 0; i < 16; ++i ) {
				// check this pixel is enabled
				final int bit = 1 << i;
				if ( (enabledPixelMask & bit) == 0 ) {
					remap[i] = -1;
					continue;
				}
	
				// check for transparent pixels when using dxt1
				if ( isDXT1 && (rgba[4 * i + 3] & 0xFF) < 128 ) {
					remap[i] = -1;
					transparent = true;
					continue;
				}
	
				// loop over previous points for a match
				for ( int j = 0; ; ++j ) {
					// allocate a new point
					if ( j == i ) {
						// normalise coordinates to [0,1]
						final float r = (rgba[4 * i] & 0xFF) / 255.0f;
						final float g = (rgba[4 * i + 1] & 0xFF) / 255.0f;
						final float b = (rgba[4 * i + 2] & 0xFF) / 255.0f;
	
						// add the point
						points[count].set(r, g, b);
						// ensure there is always non-zero weight even for zero alpha
						weights[count] = (weightAlpha ? ((rgba[4 * i + 3] & 0xFF) + 1) / 256.0f : 1.0f);
						remap[i] = count++; // advance
						break;
					}
	
					// check for a match
					final int oldbit = 1 << j;
					final boolean match = ((enabledPixelMask & oldbit) != 0)
										  && (rgba[4 * i] == rgba[4 * j])
										  && (rgba[4 * i + 1] == rgba[4 * j + 1])
										  && (rgba[4 * i + 2] == rgba[4 * j + 2])
										  && (rgba[4 * j + 3] != 0 || !isDXT1);
	
					if ( match ) {
						// get the index of the match
						final int index = remap[j];
	
						// ensure there is always non-zero weight even for zero alpha
						// map to this point and increase the weight
						weights[index] += (weightAlpha ? ((rgba[4 * i + 3] & 0xFF) + 1) / 256.0f : 1.0f);
						remap[i] = index;
						break;
					}
				}
			}
		}
	
		int getCount() { return count; }
	
		DDSVec[] getPoints() { return points; }
	
		float[] getWeights() { return weights; }
	
		boolean isTransparent() { return transparent; }
	
		void remapIndices(final int[] source, final int[] target) {
			for ( int i = 0; i < 16; ++i ) {
				int j = remap[i];
				if ( j == -1 )
					target[i] = 3;
				else
					target[i] = source[j];
			}
		}
	
	}







	private static final int MAX_ITERATIONS = 8;

	private static final float TWO_THIRDS = 2.0f / 3.0f;
	private static final float ONE_THIRD = 1.0f / 3.0f;
	private static final float HALF = 0.5f;

	public static void CompressCluster(final ColourSet colours, final DDSCompressionType type, final byte[] block, final int offset) {
		// initialise the best error
		float[] globalBestErrorPtr = {Float.MAX_VALUE};

		// initialise the metric

		// get the covariance matrix
		final Matrix covariance2 = Matrix.computeWeightedCovariance(colours);

		// compute the principle component
		DDSVec principle = Matrix.computePrincipleComponent(covariance2);
		
		final DDSVec xxSum = new DDSVec();
		
		if (type == DDSCompressionType.DXT1) {
			compress3(block, offset, colours, principle, globalBestErrorPtr, xxSum);
			if (!colours.isTransparent()) {
				compress4(block, offset, colours, principle, globalBestErrorPtr, xxSum);
			}
		} else {
			compress4(block, offset, colours, principle, globalBestErrorPtr, xxSum);
		}
	}

	public static void compress3(final byte[] block, final int offset, ColourSet colours, DDSVec principle, float[] globalBestErrorPtr, DDSVec xxSum) {
		final int count = colours.getCount();

		final DDSVec bestStart = new DDSVec(0.0f);
		final DDSVec bestEnd = new DDSVec(0.0f);
		float bestError = globalBestErrorPtr[0];

		final DDSVec a = new DDSVec();
		final DDSVec b = new DDSVec();

		float[] weighted = new float[16 * 3];
		final float[] weights = new float[16];
		final int[] orders = new int[16 * MAX_ITERATIONS];
		// prepare an ordering using the principle axis
		constructOrdering(principle, 0, colours, weights, weighted, orders, xxSum);

		// loop over iterations
		final int[] indices = new int[16];
		final int[] bestIndices = new int[16];

		final float[] alpha = new float[16];
		final float[] beta = new float[16];
		
		// check all possible clusters and iterate on the total order
		int bestIteration = 0;
		for ( int iteration = 0; ; ) {
			// first cluster [0,i) is at the start
			for ( int m = 0; m < count; ++m ) {
				indices[m] = 0;
				alpha[m] = weights[m];
				beta[m] = 0;
			}
			for ( int i = count; i >= 0; --i ) {
				// second cluster [i,j) is half along
				for ( int m = i; m < count; ++m ) {
					indices[m] = 2;
					alpha[m] = beta[m] = HALF * weights[m];
				}
				
				if ( solveLeastSquares(a, b, count, alpha, beta, weighted, xxSum) > bestError ) {
					continue;
				}
				
				for ( int j = count; j >= i; --j ) {
					// last cluster [j,k) is at the end
					if ( j < count ) {
						indices[j] = 1;
						alpha[j] = 0;
						beta[j] = weights[j];
					}

					// solve a least squares problem to place the endpoints
					final float error = solveLeastSquares(a, b, count, alpha, beta, weighted, xxSum);

					// keep the solution if it wins
					if ( error < bestError ) {
						bestStart.set(a);
						bestEnd.set(b);
						System.arraycopy(indices, 0, bestIndices, 0, 16);
						bestError = error;
						bestIteration = iteration;
					}
				}
			}

			// stop if we didn't improve in this iteration
			if ( bestIteration != iteration )
				break;

			// advance if possible
			if ( ++iteration == MAX_ITERATIONS )
				break;

			// stop if a new iteration is an ordering that has already been tried
			if ( !constructOrdering(a.set(bestEnd).sub(bestStart), iteration, colours, weights, weighted, orders, xxSum) )
				break;
		}

		// save the block if necessary
		if ( bestError < globalBestErrorPtr[0] ) {
			//final int[] orders = CompressorCluster.orders;
			final int[] unordered = new int[16];

			// remap the indices
			final int order = 16 * bestIteration;

			for ( int i = 0; i < count; ++i ){
				unordered[orders[order + i]] = bestIndices[i];
			}
			colours.remapIndices(unordered, bestIndices);

			// save the block
			ColourBlock.writeColourBlock3(bestStart, bestEnd, bestIndices, block, offset);

			// save the error
			globalBestErrorPtr[0] = bestError;
		}
	}

	public static void compress4(final byte[] block, final int offset, ColourSet colours, DDSVec principle, float[] globalBestErrorPtr, DDSVec xxSum) {
		final int count = colours.getCount();//2..16

		final DDSVec bestStart = new DDSVec(0.0f);
		final DDSVec bestEnd = new DDSVec(0.0f);
		float bestError = globalBestErrorPtr[0];

		final DDSVec start = new DDSVec();
		final DDSVec end = new DDSVec();
		
		float[] weighted = new float[16 * 3];
		final float[] weights = new float[16];
		final int[] orders = new int[16 * MAX_ITERATIONS];
		// prepare an ordering using the principle axis
		constructOrdering(principle, 0, colours, weights, weighted, orders, xxSum);

		// check all possible clusters and iterate on the total order
		final int[] indices = new int[16];
		final int[] bestIndices = new int[16];

		final float[] alpha = new float[16];
		final float[] beta = new float[16];
		
		int bestIteration = 0;
		// loop over iterations
		for ( int iteration = 0; ; ) {
			// first cluster [0,i) is at the start
			for ( int m = 0; m < count; ++m ) {//clear
				indices[m] = 0;
				alpha[m] = weights[m];
				beta[m] = 0;
			}
			
			for ( int i = count; i >= 0; --i ) {
				// second cluster [i,j) is one third along
				for ( int m = i; m < count; ++m ) {
					indices[m] = 2;
					alpha[m] = TWO_THIRDS * weights[m];
					beta[m] = ONE_THIRD * weights[m];
				}
				
				if ( solveLeastSquares(start, end, count, alpha, beta, weighted, xxSum) > bestError ) {
					continue;
				}
				
				for ( int j = count; j >= i; --j ) {
					// third cluster [j,k) is two thirds along
					for ( int m = j; m < count; ++m ) {
						indices[m] = 3;
						alpha[m] = ONE_THIRD * weights[m];
						beta[m] = TWO_THIRDS * weights[m];
					}
					
					for ( int k = count; k >= j; --k ) {
						// last cluster [k,n) is at the end
						if ( k < count ) {
							indices[k] = 1;
							alpha[k] = 0;
							beta[k] = weights[k];
						}

						// solve a least squares problem to place the endpoints
						final float error = solveLeastSquares(start, end, count, alpha, beta, weighted, xxSum);

						// keep the solution if it wins
						if ( error < bestError ) {
							bestStart.set(start);
							bestEnd.set(end);
							System.arraycopy(indices, 0, bestIndices, 0, 16);
							bestError = error;
							bestIteration = iteration;
						}
					}
				}
			}

			// stop if we didn't improve in this iteration
			if ( bestIteration != iteration ) break;

			// advance if possible
			++iteration;
			if ( iteration == MAX_ITERATIONS ) break;

			// stop if a new iteration is an ordering that has already been tried
			if ( !constructOrdering(start.set(bestEnd).sub(bestStart), iteration, colours, weights, weighted, orders, xxSum) ) break;
		}
		
		//System.out.println("#iteration = " + bestIteration);

		// save the block if necessary
		if ( bestError < globalBestErrorPtr[0] ) {
			//final int[] orders = CompressorCluster.orders;
			//final int[] unordered = CompressorCluster.unordered;
			final int[] unordered = new int[16];
			// remap the indices
			final int order = 16 * bestIteration;
			for ( int i = 0; i < count; ++i )
				unordered[orders[order + i]] = bestIndices[i];
			colours.remapIndices(unordered, bestIndices);

			// save the block
			ColourBlock.writeColourBlock4(bestStart, bestEnd, bestIndices, block, offset);

			// save the error
			globalBestErrorPtr[0] = bestError;
		}
	}

	private static boolean constructOrdering(final DDSVec axis, final int iteration, ColourSet colours, float[] weights, float[] weighted, int[] orders, DDSVec xxSum) {
		final float[] dps = new float[16];
		// cache some values
		final int count = colours.getCount();
		final DDSVec[] values = colours.getPoints();

		// build the list of dot products
		final int order = 16 * iteration;
		for ( int i = 0; i < count; ++i ) {
			dps[i] = values[i].dot(axis);
			orders[order + i] = i;
		}

		// stable sort using them
		for ( int i = 0; i < count; ++i ) {
			for ( int j = i; j > 0 && dps[j] < dps[j - 1]; --j ) {
				final float tmpF = dps[j];
				dps[j] = dps[j - 1];
				dps[j - 1] = tmpF;

				final int tmpI = orders[order + j];
				orders[order + j] = orders[order + j - 1];
				orders[order + j - 1] = tmpI;
			}
		}

		// check this ordering is unique
		for ( int it = 0; it < iteration; ++it ) {
			final int prev = 16 * it;
			boolean same = true;
			for ( int i = 0; i < count; ++i ) {
				if ( orders[order + i] != orders[prev + i] ) {
					same = false;
					break;
				}
			}
			if ( same )
				return false;
		}

		// copy the ordering and weight all the points
		final DDSVec[] points = colours.getPoints();
		final float[] cWeights = colours.getWeights();
		xxSum.set(0.0f);

		for ( int i = 0, j = 0; i < count; ++i, j += 3 ) {
			final int p = orders[order + i];

			final float weight = cWeights[p];
			final DDSVec point = points[p];

			weights[i] = weight;

			final float wX = weight * point.x();
			final float wY = weight * point.y();
			final float wZ = weight * point.z();

			xxSum.add(wX * wX, wY * wY, wZ * wZ);

			weighted[j + 0] = wX;
			weighted[j + 1] = wY;
			weighted[j + 2] = wZ;
		}
		return true;
	}

	private static float solveLeastSquares(final DDSVec start, final DDSVec end, int count,
			float[] alpha, float[] beta, float[] weighted, DDSVec xxSum) {
		float alpha2_sum = 0.0f;
		float beta2_sum = 0.0f;
		float alphabeta_sum = 0.0f;

		float alphax_sumX = 0f;
		float alphax_sumY = 0f;
		float alphax_sumZ = 0f;

		float betax_sumX = 0f;
		float betax_sumY = 0f;
		float betax_sumZ = 0f;

		// accumulate all the quantities we need
		for ( int i = 0, j = 0; i < count; ++i, j += 3 ) {
			final float a = alpha[i];
			final float b = beta[i];

			alpha2_sum += a * a;
			beta2_sum += b * b;
			alphabeta_sum += a * b;

			alphax_sumX += weighted[j + 0] * a;
			alphax_sumY += weighted[j + 1] * a;
			alphax_sumZ += weighted[j + 2] * a;

			betax_sumX += weighted[j + 0] * b;
			betax_sumY += weighted[j + 1] * b;
			betax_sumZ += weighted[j + 2] * b;
		}

		float aX, aY, aZ;
		float bX, bY, bZ;

		// zero where non-determinate
		if ( beta2_sum == 0.0f ) {
			final float rcp = 1.0f / alpha2_sum;

			aX = alphax_sumX * rcp;
			aY = alphax_sumY * rcp;
			aZ = alphax_sumZ * rcp;
			bX = bY = bZ = 0.0f;
		} else if ( alpha2_sum == 0.0f ) {
			final float rcp = 1.0f / beta2_sum;

			aX = aY = aZ = 0.0f;
			bX = betax_sumX * rcp;
			bY = betax_sumY * rcp;
			bZ = betax_sumZ * rcp;
		} else {
			float rcp;// Detect Infinity:
			if((rcp = (alpha2_sum * beta2_sum - alphabeta_sum * alphabeta_sum)) == 0) return Float.MAX_VALUE;
			rcp = 1.0f / rcp;
			
			aX = (alphax_sumX * beta2_sum - betax_sumX * alphabeta_sum) * rcp;
			aY = (alphax_sumY * beta2_sum - betax_sumY * alphabeta_sum) * rcp;
			aZ = (alphax_sumZ * beta2_sum - betax_sumZ * alphabeta_sum) * rcp;

			bX = (betax_sumX * alpha2_sum - alphax_sumX * alphabeta_sum) * rcp;
			bY = (betax_sumY * alpha2_sum - alphax_sumY * alphabeta_sum) * rcp;
			bZ = (betax_sumZ * alpha2_sum - alphax_sumZ * alphabeta_sum) * rcp;
		}

		// clamp the output to [0, 1]
		// clamp to the grid
		aX = clampXZ(aX);
		aY = clampY(aY);
		aZ = clampXZ(aZ);

		start.set(aX, aY, aZ);
		
		bX = clampXZ(bX);
		bY = clampY(bY);
		bZ = clampXZ(bZ);
		
		end.set(bX, bY, bZ);

		// compute the error
		final float eX = aX * aX * alpha2_sum + bX * bX * beta2_sum + xxSum.x() + 2.0f * (aX * bX * alphabeta_sum - aX * alphax_sumX - bX * betax_sumX);
		final float eY = aY * aY * alpha2_sum + bY * bY * beta2_sum + xxSum.y() + 2.0f * (aY * bY * alphabeta_sum - aY * alphax_sumY - bY * betax_sumY);
		final float eZ = aZ * aZ * alpha2_sum + bZ * bZ * beta2_sum + xxSum.z() + 2.0f * (aZ * bZ * alphabeta_sum - aZ * alphax_sumZ - bZ * betax_sumZ);

		// apply the metric to the error term
		return CompressionMetric.PERCEPTUAL.dot(eX, eY, eZ);
	}
	
	private static float clampXZ(final float v) {
		if ( v <= 0.0f ) return 0.0f;
		else if ( v >= 1.0f ) return 1.0f;
		return (int)(31 * v + 0.5f) / 31F;
	}
	
	private static float clampY(final float v) {
		if ( v <= 0.0f ) return 0.0f;
		else if ( v >= 1.0f ) return 1.0f;
		return (int)(63 * v + 0.5f) / 63F;
	}
	

	public static enum CompressionMetric {
		PERCEPTUAL(0.2126f, 0.7152f, 0.0722f), UNIFORM(1.0f, 1.0f, 1.0f);

		public final float r;

		public final float g;

		public final float b;

		CompressionMetric(final float r, final float g, final float b) {
			this.r = r;
			this.g = g;
			this.b = b;
		}

		public float dot(final float x, final float y, final float z) {
			return r * x + g * y + b * z;
		}
	}



	private static final class Matrix {
		private static final float FLT_EPSILON = 0.00001f;

		private float[] values = new float[6];

		Matrix() {}

		static Matrix computeWeightedCovariance(final ColourSet m_colours) {
			final int count = m_colours.getCount();
			final DDSVec[] points = m_colours.getPoints();
			final float[] weights = m_colours.getWeights();

			final DDSVec centroid = new DDSVec();
			final DDSVec a = new DDSVec();
			final DDSVec b = new DDSVec();

			// compute the centroid
			float total = 0.0f;
			for (int i = 0; i < count; ++i) {
				total += weights[i];
				centroid.add(a.set(points[i]).mul(weights[i]));
			}
			centroid.div(total);

			// accumulate the covariance matrix
			Matrix covariance = new Matrix();

			final float[] values = covariance.values;

			for (int i = 0; i < count; ++i) {
				a.set(points[i]).sub(centroid);
				b.set(a).mul(weights[i]);

				values[0] += a.x() * b.x();
				values[1] += a.x() * b.y();
				values[2] += a.x() * b.z();
				values[3] += a.y() * b.y();
				values[4] += a.y() * b.z();
				values[5] += a.z() * b.z();
			}
			return covariance;
		}

		private static DDSVec getMultiplicity1Evector(final Matrix matrix, final float evalue) {
			final float[] values = matrix.values;

			// compute M
			final float[] m = new float[6];
			m[0] = values[0] - evalue;
			m[1] = values[1];
			m[2] = values[2];
			m[3] = values[3] - evalue;
			m[4] = values[4];
			m[5] = values[5] - evalue;

			// compute U
			final float[] u = new float[6];
			u[0] = m[3] * m[5] - m[4] * m[4];
			u[1] = m[2] * m[4] - m[1] * m[5];
			u[2] = m[1] * m[4] - m[2] * m[3];
			u[3] = m[0] * m[5] - m[2] * m[2];
			u[4] = m[1] * m[2] - m[4] * m[0];
			u[5] = m[0] * m[3] - m[1] * m[1];

			// find the largest component
			float mc = Math.abs(u[0]);
			int mi = 0;
			for (int i = 1; i < 6; ++i) {
				final float c = Math.abs(u[i]);
				if (c > mc) {
					mc = c;
					mi = i;
				}
			}

			// pick the column with this component
			switch (mi) {
			case 0:
				return new DDSVec(u[0], u[1], u[2]);
			case 1:
			case 3:
				return new DDSVec(u[1], u[3], u[4]);
			default:
				return new DDSVec(u[2], u[4], u[5]);
			}
		}

		private static DDSVec getMultiplicity2Evector(final Matrix matrix, final float evalue) {
			final float[] values = matrix.values;

			// compute M
			final float[] m = new float[6];
			m[0] = values[0] - evalue;
			m[1] = values[1];
			m[2] = values[2];
			m[3] = values[3] - evalue;
			m[4] = values[4];
			m[5] = values[5] - evalue;

			// find the largest component
			float mc = Math.abs(m[0]);
			int mi = 0;
			for (int i = 1; i < 6; ++i) {
				final float c = Math.abs(m[i]);
				if (c > mc) {
					mc = c;
					mi = i;
				}
			}

			// pick the first eigenvector based on this index
			switch (mi) {
			case 0:
			case 1:
				return new DDSVec(-m[1], m[0], 0.0f);
			case 2:
				return new DDSVec(m[2], 0.0f, -m[0]);
			case 3:
			case 4:
				return new DDSVec(0.0f, -m[4], m[3]);
			default:
				return new DDSVec(0.0f, -m[5], m[4]);
			}
		}

		static DDSVec computePrincipleComponent(final Matrix matrix) {
			final float[] m = matrix.values;

			// compute the cubic coefficients
			final float c0 = m[0] * m[3] * m[5] + 2.0f * m[1] * m[2] * m[4] - m[0] * m[4] * m[4] - m[3] * m[2] * m[2]
					- m[5] * m[1] * m[1];
			final float c1 = m[0] * m[3] + m[0] * m[5] + m[3] * m[5] - m[1] * m[1] - m[2] * m[2] - m[4] * m[4];
			final float c2 = m[0] + m[3] + m[5];

			// compute the quadratic coefficients
			final float a = c1 - (1.0f / 3.0f) * c2 * c2;
			final float b = (-2.0f / 27.0f) * c2 * c2 * c2 + (1.0f / 3.0f) * c1 * c2 - c0;

			// compute the root count check
			final float Q = 0.25f * b * b + (1.0f / 27.0f) * a * a * a;

			// test the multiplicity
			if (FLT_EPSILON < Q) {
				// only one root, which implies we have a multiple of the
				// identity
				return new DDSVec(1.0f);
			} else if (Q < -FLT_EPSILON) {
				// three distinct roots
				final float theta = (float) Math.atan2(Math.sqrt(-Q), -0.5f * b);
				final float rho = (float) Math.sqrt(0.25f * b * b - Q);

				final float rt = (float) Math.pow(rho, 1.0f / 3.0f);
				final float ct = (float) Math.cos(theta / 3.0f);
				final float st = (float) Math.sin(theta / 3.0f);

				float l1 = (1.0f / 3.0f) * c2 + 2.0f * rt * ct;
				final float l2 = (1.0f / 3.0f) * c2 - rt * (ct + (float) Math.sqrt(3.0f) * st);
				final float l3 = (1.0f / 3.0f) * c2 - rt * (ct - (float) Math.sqrt(3.0f) * st);

				// pick the larger
				if (Math.abs(l2) > Math.abs(l1))
					l1 = l2;
				if (Math.abs(l3) > Math.abs(l1))
					l1 = l3;

				// get the eigenvector
				return getMultiplicity1Evector(matrix, l1);
			} else { // if( -FLT_EPSILON <= Q && Q <= FLT_EPSILON )
				// two roots
				final float rt;
				if (b < 0.0f)
					rt = (float) -Math.pow(-0.5f * b, 1.0f / 3.0f);
				else
					rt = (float) Math.pow(0.5f * b, 1.0f / 3.0f);

				final float l1 = (1.0f / 3.0f) * c2 + rt; // repeated
				final float l2 = (1.0f / 3.0f) * c2 - 2.0f * rt;

				// get the eigenvector
				if (Math.abs(l1) > Math.abs(l2))
					return getMultiplicity2Evector(matrix, l1);
				else
					return getMultiplicity1Evector(matrix, l2);
			}
		}

	}

	public static void CompressorSingleColour(final ColourSet colours, final DDSCompressionType type, final byte[] block, final int offset) {

		// grab the single colour
		final DDSVec colour_ = colours.getPoints()[0];
		int[] colour = {
				Math.round(255.0f * colour_.x()),
				Math.round(255.0f * colour_.y()),
				Math.round(255.0f * colour_.z())
		};

		// initialise the best error
		int[] globalBestErrorPtr = {Integer.MAX_VALUE};
		
		final int[] indices = new int[16];
		final DDSVec start = new DDSVec();
		final DDSVec end = new DDSVec();
		final int[][] sources = new int[3][];
		
		if (type == DDSCompressionType.DXT1) {
			compress3Single(block, offset, indices, start, end, colours, colour, sources, globalBestErrorPtr);
			if (!colours.isTransparent()) {
				compress4Single(block, offset, indices, start, end, colours, colour, sources, globalBestErrorPtr);
			}
		} else {
			compress4Single(block, offset, indices, start, end, colours, colour, sources, globalBestErrorPtr);
		}
	}

	static void compress3Single(final byte[] block, final int offset, int[] indices, DDSVec start, DDSVec end, ColourSet colours, int[] colour, int[][] sources, int[] globalBestErrorPtr) {
		// find the best end-points and index
		int[] index = new int[1];
		final int error = computeEndPointsSingle(3, DDSLookupTables.lookupsTable3, start, end, colour, sources, index, globalBestErrorPtr);
		// build the block if we win
		if (error < globalBestErrorPtr[0]) {
			// remap the indices
			colours.remapIndices(index, indices);

			// save the block
			ColourBlock.writeColourBlock3(start, end, indices, block, offset);

			// save the error
			globalBestErrorPtr[0] = error;
		}
	}

	static void compress4Single(final byte[] block, final int offset, int[] indices, DDSVec start, DDSVec end, ColourSet colours, int[] colour, int[][] sources, int[] globalBestErrorPtr) {
		// find the best end-points and index
		int[] index = new int[1];
		final int error = computeEndPointsSingle(4, DDSLookupTables.lookupsTable4, start, end, colour, sources, index, globalBestErrorPtr);

		// build the block if we win
		if (error < globalBestErrorPtr[0]) {
			// remap the indices
			colours.remapIndices(index, indices);

			// save the block
			ColourBlock.writeColourBlock4(start, end, indices, block, offset);

			// save the error
			globalBestErrorPtr[0] = error;
		}
	}

	private static int computeEndPointsSingle(final int count, final int[][][][] lookups, DDSVec start, DDSVec end, int[] colour, int[][] sources, int[] index2, int[] globalBestErrorPtr) {
		final float GRID_X = 31.0f;
		final float GRID_Y = 63.0f;
		final float GRID_Z = 31.0f;

		final float GRID_X_RCP = 1.0f / GRID_X;
		final float GRID_Y_RCP = 1.0f / GRID_Y;
		final float GRID_Z_RCP = 1.0f / GRID_Z;
		
		int bestError = globalBestErrorPtr[0];

		// check each index combination
		for (int index = 0; index < count; ++index) {
			// check the error for this codebook index
			int error = 0;
			for (int channel = 0; channel < 3; ++channel) {
				// grab the lookup table and index for this channel
				final int[][][] lookup = lookups[channel];
				final int target = colour[channel];

				// store a pointer to the source for this channel
				sources[channel] = lookup[target][index];

				// accumulate the error
				final int diff = sources[channel][2];
				error += diff * diff;
			}

			// keep it if the error is lower
			if (error < bestError) {
				start.set(sources[0][0] * GRID_X_RCP, sources[1][0] * GRID_Y_RCP, sources[2][0] * GRID_Z_RCP);

				end.set(sources[0][1] * GRID_X_RCP, sources[1][1] * GRID_Y_RCP, sources[2][1] * GRID_Z_RCP);

				index2[0] = index;
				bestError = error;
			}
		}

		return bestError;
	}




	private static void compressAlphaDxt3(final byte[] rgba, final int mask, final byte[] block, final int offset) {
		// quantise and pack the alpha values pairwise
		for (int i = 0; i < 8; ++i) {
			// quantise down to 4 bits
			final float alpha1 = (rgba[8 * i + 3] & 0xFF) * (15.0f / 255.0f);
			final float alpha2 = (rgba[8 * i + 7] & 0xFF) * (15.0f / 255.0f);
			int quant1 = Math.round(alpha1);
			int quant2 = Math.round(alpha2);

			// set alpha to zero where masked
			final int bit1 = 1 << (2 * i);
			final int bit2 = 1 << (2 * i + 1);
			if ((mask & bit1) == 0)
				quant1 = 0;
			if ((mask & bit2) == 0)
				quant2 = 0;

			// pack into the byte
			block[offset + i] = (byte) (quant1 | (quant2 << 4));
		}
	}

	private static int fitCodes(final byte[] rgba, final int mask, final int[] codes, final int[] indices) {
		// fit each alpha value to the codebook
		int err = 0;
		for (int i = 0; i < 16; ++i) {
			// check this pixel is valid
			final int bit = 1 << i;
			if ((mask & bit) == 0) {
				// use the first code
				indices[i] = 0;
				continue;
			}

			// find the least error and corresponding index
			final int value = (rgba[4 * i + 3] & 0xFF);
			int least = Integer.MAX_VALUE;
			int index = 0;
			for (int j = 0; j < 8; ++j) {
				// get the squared error from this code
				int dist = value - codes[j];
				dist *= dist;

				// compare with the best so far
				if (dist < least) {
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

	private static void writeAlphaBlock(final int alpha0, final int alpha1, final int[] indices, final byte[] block,
			final int offset) {
		// write the first two bytes
		block[offset + 0] = (byte) alpha0;
		block[offset + 1] = (byte) alpha1;

		// pack the indices with 3 bits each
		int src = 0;
		int dest = 2;
		for (int i = 0; i < 2; ++i) {
			// pack 8 3-bit values
			int value = 0;
			for (int j = 0; j < 8; ++j) {
				final int index = indices[src++];
				value |= (index << 3 * j);
			}

			// store in 3 bytes
			for (int j = 0; j < 3; ++j)
				block[offset + dest++] = (byte) ((value >> 8 * j) & 0xff);
		}
	}

	private static void writeAlphaBlock5(final int alpha0, final int alpha1, final int[] indices,
			final byte[] block, final int offset) {
		// check the relative values of the endpoints
		final int[] swapped = new int[16];

		if (alpha0 > alpha1) {
			// swap the indices
			for (int i = 0; i < 16; ++i) {
				int index = indices[i];
				if (index == 0)
					swapped[i] = 1;
				else if (index == 1)
					swapped[i] = 0;
				else if (index <= 5)
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

	private static void writeAlphaBlock7(final int alpha0, final int alpha1, final int[] indices,
			final byte[] block, final int offset) {
		// check the relative values of the endpoints
		final int[] swapped = new int[16];

		if (alpha0 < alpha1) {
			// swap the indices
			for (int i = 0; i < 16; ++i) {
				int index = indices[i];
				if (index == 0)
					swapped[i] = 1;
				else if (index == 1)
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

	private static void compressAlphaDxt5(final byte[] rgba, final int mask, final byte[] block, final int offset) {
		// get the range for 5-alpha and 7-alpha interpolation
		int min5 = 255;
		int max5 = 0;
		int min7 = 255;
		int max7 = 0;
		for (int i = 0; i < 16; ++i) {
			// check this pixel is valid
			final int bit = 1 << i;
			if ((mask & bit) == 0)
				continue;

			// incorporate into the min/max
			final int value = (rgba[4 * i + 3] & 0xFF);
			if (value < min7)
				min7 = value;
			if (value > max7)
				max7 = value;
			if (value != 0 && value < min5)
				min5 = value;
			if (value != 255 && value > max5)
				max5 = value;
		}

		// handle the case that no valid range was found
		if (min5 > max5)
			min5 = max5;
		if (min7 > max7)
			min7 = max7;

		// fix the range to be the minimum in each case
		if (max5 - min5 < 5)
			max5 = Math.min(min5 + 5, 255);
		if (max5 - min5 < 5)
			min5 = Math.max(0, max5 - 5);

		if (max7 - min7 < 7)
			max7 = Math.min(min7 + 7, 255);
		if (max7 - min7 < 7)
			min7 = Math.max(0, max7 - 7);

		// set up the 5-alpha code book
		final int[] codes5 = new int[8];

		codes5[0] = min5;
		codes5[1] = max5;
		for (int i = 1; i < 5; ++i)
			codes5[1 + i] = ((5 - i) * min5 + i * max5) / 5;
		codes5[6] = 0;
		codes5[7] = 255;

		// set up the 7-alpha code book
		final int[] codes7 = new int[8];

		codes7[0] = min7;
		codes7[1] = max7;
		for (int i = 1; i < 7; ++i)
			codes7[1 + i] = ((7 - i) * min7 + i * max7) / 7;
		
		int[] indices5 = new int[16];
		int[] indices7 = new int[16];
		
		// fit the data to both code books
		int err5 = fitCodes(rgba, mask, codes5, indices5);
		int err7 = fitCodes(rgba, mask, codes7, indices7);

		// save the block with least error
		if (err5 <= err7)
			writeAlphaBlock5(min5, max5, indices5, block, offset);
		else
			writeAlphaBlock7(min7, max7, indices7, block, offset);
	}

	
}
