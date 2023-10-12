package me.kaigermany.ultimateutils.classextensioninternals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public interface IO {
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
	default byte[] loadBytes(File file) throws IOException {
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
	default void saveBytes(File file, byte[] data) throws IOException {
		File dir = file.getParentFile();
		if(!dir.exists()) dir.mkdirs();
		FileOutputStream fos = new FileOutputStream(file);
		fos.write(data);
		fos.close();
	}
	
	static int copyStreamToArray(InputStream is, byte[] out) throws IOException {
		int off = 0;
		int len;
		while(out.length - off > 0 && (len = is.read(out, off, out.length - off)) != -1){
			off += len;
		}
		return off;
	}
}
