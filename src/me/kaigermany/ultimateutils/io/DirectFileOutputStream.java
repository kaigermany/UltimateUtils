package me.kaigermany.ultimateutils.io;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

public class DirectFileOutputStream extends OutputStream {
	private RandomAccessFile raf;
	private volatile long len;
	
	public DirectFileOutputStream(File out) throws IOException {
		//if(out.exists()) out.delete();
		if(!out.exists()) out.createNewFile();
		raf = new RandomAccessFile(out, "rwd");
		raf.setLength(0);
	}
	
	@Override
	public void write(int b) throws IOException {
		raf.seek(len);
		raf.write(b);
		len++;
	}
	
	@Override
	public void write(byte[] buf) throws IOException {
		raf.seek(len);
		raf.write(buf);
		len += buf.length;
	}
	
	@Override
	public void write(byte[] buf, int offset, int length) throws IOException {
		raf.seek(len);
		raf.write(buf, offset, length);
		len += length;
	}
	
	@Override
	public void flush() throws IOException {}
	
	@Override
	public void close() throws IOException {
		if(raf != null){
			raf.close();
			raf = null;
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}
}
