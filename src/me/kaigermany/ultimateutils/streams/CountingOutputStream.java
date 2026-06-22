package me.kaigermany.ultimateutils.streams;

import java.io.IOException;
import java.io.OutputStream;

public class CountingOutputStream extends OutputStream {
	public static interface CountUpdateListener {
		void onUpdate(long numBytes);
	}
	
	private final OutputStream os;
	private final CountUpdateListener listener;
	
	private long count;
	
	public CountingOutputStream(OutputStream os, CountUpdateListener listener){
		this.os = os;
		this.listener = listener;
	}
	
	public long getNumBytesWritten(){
		return count;
	}

	@Override
	public void write(int b) throws IOException {
		os.write(b);
		count++;
		if(listener != null) listener.onUpdate(count);
	}

	@Override
	public void write(byte[] buf) throws IOException {
		os.write(buf);
		count += buf.length;
		if(listener != null) listener.onUpdate(count);
	}

	@Override
	public void write(byte[] buf, int off, int len) throws IOException {
		os.write(buf, off, len);
		count += len;
		if(listener != null) listener.onUpdate(count);
	}
	
	@Override
	public void flush() throws IOException {
		os.flush();
	}
	
	@Override
	public void close() throws IOException {
		os.close();
	}
}
