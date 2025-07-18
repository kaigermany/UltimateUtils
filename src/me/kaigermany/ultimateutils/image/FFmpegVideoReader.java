package me.kaigermany.ultimateutils.image;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.BiConsumer;

import javax.imageio.ImageIO;

import me.kaigermany.ultimateutils.sync.thread.ThreadLock;

public class FFmpegVideoReader {
	public volatile boolean finished = false;
	private final BiConsumer<BufferedImage, Integer> frameListener;
	private final ThreadLock lock = new ThreadLock();
	private final Process process;
	private final InputStream stream;

	public FFmpegVideoReader(File ffmpeg, File videoFile, BiConsumer<BufferedImage, Integer> frameListener) throws IOException {
		this.frameListener = frameListener;
		
		ProcessBuilder videoProcess = new ProcessBuilder(ffmpeg.getAbsolutePath(), "-loglevel", "quiet", "-hwaccel", "auto", "-i",
				videoFile.getAbsolutePath(), "-pix_fmt", "rgb24", "-c:v", "bmp", "-f", "rawvideo", "pipe:1");
		//videoProcess.redirectErrorStream();
		process = videoProcess.start();
		stream = new BufferedInputStream(process.getInputStream(), 32 * 1024);
		
		lock.lock();
		new Thread(this::runLoop, "FFmpegVideoReader").start();
	}

	public void await() {
		lock.enterBlock();
	}
	
	public boolean isDone(){
		return finished;
	}

	private void runLoop() {
		int count = 0;
		ImageIO.setUseCache(false);
		
		while (true) {
			BufferedImage img;
			try {
				img = ImageIO.read(stream);
			} catch (Exception e) {
				e.printStackTrace();
				break;
			}
			if (img == null) break;
			frameListener.accept(img, count);
			count++;
		}
		
		frameListener.accept(null, -1); // Signal that we are done
		finished = true;
		lock.unlock();
	}
}