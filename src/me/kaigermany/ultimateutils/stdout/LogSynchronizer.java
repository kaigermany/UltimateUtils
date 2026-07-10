package me.kaigermany.ultimateutils.stdout;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class LogSynchronizer {
	private static final Object sharedLock = new Object();
	private static PrintStream newStdOut, newStdErr, oldStrOut, oldStdErr;
	
	public static void inject(){
		oldStrOut = System.out;
		oldStdErr = System.err;
		System.setOut(newStdOut = createChannel(oldStrOut));
		System.setErr(newStdErr = createChannel(oldStdErr));
	}
	
	public static void release(boolean force){
		if(!force){
			if(System.out != newStdOut) throw new IllegalArgumentException("System.out was manipulated! release() could destroy your printing-architecture!");
			if(System.err != newStdErr) throw new IllegalArgumentException("System.err was manipulated! release() could destroy your printing-architecture!");
		}
		System.setOut(oldStrOut);
		System.setErr(oldStdErr);
		newStdOut = newStdErr = oldStrOut = oldStdErr = null;
	}
	
	private static PrintStream createChannel(PrintStream stream) {
		return new PrintStream(new SynchronizedPrintChannel(stream));
	}
	
	private static class SynchronizedPrintChannel extends OutputStream {
		private final PrintStream output;
		
		public SynchronizedPrintChannel(PrintStream output) {
			this.output = output;
		}

		@Override
		public void write(int chr) throws IOException {
			byte[] buf = new byte[1];
			buf[0] = (byte)chr;
			write(buf, 0, 1);
		}
		
		@Override
		public void write(byte[] b) throws IOException {
			write(b);
		}
		
		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			synchronized (sharedLock) {
				synchronized (output) {
					output.write(b, off, len);
				}
			}
		}
	}
	
}
