package me.kaigermany.ultimateutils;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;

import me.kaigermany.ultimateutils.classextensioninternals.FastHex;

public class StaticUtils {
	private StaticUtils(){}
	
	public static byte[] readAllBytes(InputStream is) throws IOException {
		byte[] buf = new byte[4096];
		int l;
		ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
		while((l = is.read(buf)) != -1){
			baos.write(buf, 0, l);
		}
		return baos.toByteArray();
	}
	
	public static BufferedImage loadImage(File file) throws IOException {
		return ImageIO.read(file);
	}
	
	public static BufferedImage loadImage(InputStream is) throws IOException {
		return ImageIO.read(is);
	}

	public static void saveImage(BufferedImage image, File file) throws IOException {
		String name = file.getName();
		int pointPos = name.lastIndexOf('.');
		if(pointPos == -1) throw new IOException("unable to identify file type from given filename");
		saveImage(image, name.substring(pointPos + 1), file);
	}

	public static void saveImage(BufferedImage image, String type, File file) throws IOException {
		ImageIO.write(image, type, file);
	}
	
	public static void saveImage(BufferedImage image, String type, OutputStream os) throws IOException {
		ImageIO.write(image, type, os);
	}
	
	public static void hexDump(byte[] in) {
		int offset = 0;
		FastHex h = new FastHex(){};
		while (true) {
			System.out.print(h.hexInt(offset) + " ");
			for (int i = 0; i < 16; i++) {
				int a = offset + i;
				if (a < in.length)
					System.out.print(h.hexByte(in[a]) + " ");
				else
					System.out.print("   ");
			}
			for (int i = 0; i < 16; i++) {
				int a = offset + i;
				if (a < in.length)
					System.out.print((char) in[a]);
				else
					System.out.print(" ");
			}
			System.out.println();
			offset += 16;
			if (offset >= in.length)
				break;
		}
	}
	
	public static Iterable<Map.Entry<ZipEntry, byte[]>> zipIterator(File file) throws IOException {
		final ZipFile zf = new ZipFile(file);
		final Enumeration<? extends ZipEntry> iterator = zf.entries();
		return new Iterable<Map.Entry<ZipEntry, byte[]>>(){
			@Override
			public Iterator<Map.Entry<ZipEntry, byte[]>> iterator() {
				return new Iterator<Map.Entry<ZipEntry, byte[]>>() {
					@Override
					public boolean hasNext() {
						boolean next = iterator.hasMoreElements();
						if(!next){
							try {
								zf.close();
							} catch (IOException e) {
								RuntimeException ex = new RuntimeException("unable to close ZipFile");
								ex.addSuppressed(e);
								throw ex;
							}
						}
						return next;
					}

					@Override
					public Entry<ZipEntry, byte[]> next() {
						final ZipEntry entry = iterator.nextElement();
						
						return new Map.Entry<ZipEntry, byte[]>(){

							@Override
							public ZipEntry getKey() {
								return entry;
							}

							@Override
							public byte[] getValue() {
								try {
									return readAllBytes(zf.getInputStream(entry));
								} catch (IOException e) {
									RuntimeException ex = new RuntimeException("unable to read ZipFile entry");
									ex.addSuppressed(e);
									throw ex;
								}
							}

							@Override
							public byte[] setValue(byte[] value) {
								return null;
							}
							
						};
					}
					
				};
			}
		};
	}
	
	public static Iterable<Map.Entry<ZipEntry, byte[]>> zipIterator(InputStream is) throws IOException {
		final ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is, 1 << 20));
		return new Iterable<Map.Entry<ZipEntry, byte[]>>(){
			@Override
			public Iterator<Map.Entry<ZipEntry, byte[]>> iterator() {
				return new Iterator<Map.Entry<ZipEntry, byte[]>>() {
					ZipEntry entry = getNextEntry();
					
					private ZipEntry getNextEntry(){
						try{
							return zis.getNextEntry();
						} catch (IOException e) {
							RuntimeException ex = new RuntimeException("unable to read ZipFile entry");
							ex.addSuppressed(e);
							throw ex;
						}
					}
					
					@Override
					public boolean hasNext() {
						if(entry == null) entry = getNextEntry();
						boolean next = entry != null;
						if(!next){
							try {
								zis.close();
							} catch (IOException e) {
								RuntimeException ex = new RuntimeException("unable to close ZipFile");
								ex.addSuppressed(e);
								throw ex;
							}
						}
						return next;
					}

					@Override
					public Entry<ZipEntry, byte[]> next() {
						if(entry == null) entry = getNextEntry();
						final ZipEntry entry = this.entry;
						this.entry = null;
						
						final byte[] fileBytes;
						
						try {
							fileBytes = readAllBytes(zis);
						} catch (IOException e) {
							RuntimeException ex = new RuntimeException("unable to read ZipFile entry");
							ex.addSuppressed(e);
							throw ex;
						}
						
						return new Map.Entry<ZipEntry, byte[]>(){

							@Override
							public ZipEntry getKey() {
								return entry;
							}

							@Override
							public byte[] getValue() {
								return fileBytes;
							}

							@Override
							public byte[] setValue(byte[] value) {
								return null;
							}
							
						};
					}
					
				};
			}
		};
	}
	
	/**
	 * Loads a File from disk.
	 * the size is limited to Java's array size limit of 2 GB.
	 * If missing, FileInputStream throws FileNotFoundException.
	 * If there are some other Exceptions, the method will throw them.
	 * if finally all bytes are read successfully the array will be returned.
	 * @param file File to load
	 * @return byte[] file contents
	 * @throws IOException
	 */
	public static byte[] loadBytes(File file) throws IOException {
		FileInputStream fis = new FileInputStream(file);
		byte[] data;
		
		{
			long l = file.length();
			if((l & ~0x7FFFFFFFL) != 0) {//check if more than 31 bits in use.
				fis.close();
				throw new IOException("The file is too big! loadBytes() can only handle files up to 2GB");
			}
			data = new byte[(int)l];
		}//drop long l.
		
		int off = copyStreamToArray(fis, data);
		fis.close();
		if(off != data.length) throw new IOException("File read was incomplete, the stream was closed too early");
		return data;
	}
	
	/**
	 * Store a new File on disk.
	 * If directory is missing, it will be created.
	 * If file already exists it gets overwritten.
	 * @param file File to save
	 * @param data raw bytes to save
	 * @throws IOException
	 */
	public static void saveBytes(File file, byte[] data) throws IOException {
		File dir = file.getParentFile();
		if(!dir.exists()) dir.mkdirs();
		FileOutputStream fos = new FileOutputStream(file);
		fos.write(data);
		fos.close();
	}
	
	public static int copyStreamToArray(InputStream is, byte[] out) throws IOException {
		int off = 0;
		int len;
		while(out.length - off > 0 && (len = is.read(out, off, out.length - off)) != -1){
			off += len;
		}
		return off;
	}
	
	public static String toHumanReadableFileSize(long bytes){
		if(bytes < 1024){
			return bytes + " B";
		}
		String[] sizeUnits = new String[] {"B", "KB", "MB", "GB", "TB", "PB", "EB"};

        int steps = 0;
        long beforeComma = bytes;
        long afterComma = bytes * 1000;
        
        while(beforeComma / 1000 > 0) {
            beforeComma /= 1024;
            afterComma /= 1024;
            
            steps++;
        }
        
        afterComma = (afterComma - beforeComma * 1000);
        
        try{
	        char[] a = new char[7];
	        int wp = 0;
	        a[4] = (char)(beforeComma / 100);
	        a[5] = (char)((beforeComma / 10) % 10);
	        a[6] = (char)(beforeComma % 10);
	        int rp = 4;
	        if(a[rp] == 0) {
	        	rp++;
	            if(a[rp] == 0) rp++;
	        }
	        for(int i=rp; i<7; i++){
	        	a[wp++] = (char)('0' + a[i]);
	        }
	        rp -= 4;
	        rp = 3 - rp;
	        if(rp != 3){
	        	a[wp++] = ',';
		        for(int i=rp; i<3; i++){
		        	a[wp++] = (char)('0' + (char)(afterComma / 100) % 10);
		        	afterComma *= 10;
		        }
	        }
        	a[wp++] = ' ';
	        String text = sizeUnits[steps];
	        for(int i=0; i<text.length(); i++) {
	        	a[wp++] = text.charAt(i);
	        }
	        return new String(a, 0, wp);
        }catch(Exception e){
        	e.printStackTrace();
        	return "";
        }
	}
	
	public static void deleteDirectory(File dir){
		for(File f : dir.listFiles()){
			if(f.isDirectory()){
				deleteDirectory(f);
			} else {
				f.delete();
			}
		}
	}
}
