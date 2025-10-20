package me.kaigermany.ultimateutils.image;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import me.kaigermany.ultimateutils.data.SortedWriter;
import me.kaigermany.ultimateutils.sync.thread.CoProcessor;

public class FFmpegVideoWriter {
	private final File outputFile;
	private final String codec;
	private final String pixelFormat;
	private final int framerate;
	private final CoProcessor cp;
	private final SortedWriter<byte[]> writer;
	private final String ffmpegPath;

	private Process ffmpegProcess;
	private OutputStream ffmpegIn;
	private boolean started = false;
	private int width;
	private int height;
	private volatile long currentFrameId = 0;

	public FFmpegVideoWriter(File ffmpeg, File outputFile, String codec, String pixelFormat, int framerate) {
		this.outputFile = outputFile;
		this.codec = codec;
		this.pixelFormat = pixelFormat;
		this.framerate = framerate;
		this.cp = CoProcessor.getInstance();
		this.writer = new SortedWriter<>((id, rgbBytes)->{
			try {
				synchronized (this) {
					ffmpegIn.write(rgbBytes);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		this.ffmpegPath = ffmpeg.getAbsolutePath();
	}

	public void write(final BufferedImage image) throws IOException {
		final long id;
		synchronized (this) {
			if (!started) {
				start(image.getWidth(), image.getHeight());
			}

			if (image.getWidth() != width || image.getHeight() != height) {
				throw new IllegalArgumentException("Frame size mismatch.");
			}
			id = currentFrameId;
			currentFrameId++;
			
			cp.putJob(()->{
				byte[] rgb = toRGB24(image);
				writer.write(id, rgb);
			});
		}
	}
	
	private void start(int width, int height) throws IOException {
		this.width = width;
		this.height = height;

		ProcessBuilder pb = new ProcessBuilder(ffmpegPath, "-f", "rawvideo", "-pixel_format", "rgb24", "-video_size",
				width + "x" + height, "-framerate", String.valueOf(framerate), "-i", "-", "-c:v", codec, "-pix_fmt",
				pixelFormat, outputFile.getAbsolutePath());

		pb.redirectError(ProcessBuilder.Redirect.INHERIT);//print debug output into console.

		ffmpegProcess = pb.start();
		//ffmpegIn = new BufferedOutputStream(ffmpegProcess.getOutputStream());
		ffmpegIn = ffmpegProcess.getOutputStream();
		started = true;
	}

	private static byte[] toRGB24(BufferedImage image) {
	    int w = image.getWidth();
	    int h = image.getHeight();
	    int[] pixels = new int[w * h];
	    image.getRGB(0, 0, w, h, pixels, 0, w);

	    byte[] rgb = new byte[w * h * 3];

	    for (int i = 0, j = 0; i < pixels.length; i++) {
	        int pixel = pixels[i];
	        rgb[j++] = (byte) ((pixel >> 16) & 0xFF); // R
	        rgb[j++] = (byte) ((pixel >> 8) & 0xFF);  // G
	        rgb[j++] = (byte) (pixel & 0xFF);         // B
	    }

	    return rgb;
	}

	public void close() throws IOException, InterruptedException {
		cp.awaitAllJobs();
		synchronized (this) {
			if (!started){
				return;
			}
	
			ffmpegIn.close();
			int exitCode = ffmpegProcess.waitFor();
			if (exitCode != 0) {
				throw new IOException("FFmpeg exited with code " + exitCode);
			}
		}
	}
}