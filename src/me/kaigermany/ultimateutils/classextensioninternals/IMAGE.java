package me.kaigermany.ultimateutils.classextensioninternals;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.imageio.ImageIO;

public interface IMAGE {
	default BufferedImage loadImage(File file) throws IOException {
		return ImageIO.read(file);
	}
	default BufferedImage loadImage(InputStream is) throws IOException {
		return ImageIO.read(is);
	}
	default void saveImage(BufferedImage image, File file) throws IOException {
		String name = file.getName();
		int pointPos = name.lastIndexOf('.');
		if(pointPos == -1) throw new IOException("unable to identify file type from given filename");
		saveImage(image, name.substring(pointPos + 1), file);
	}
	default void saveImage(BufferedImage image, String type, File file) throws IOException {
		ImageIO.write(image, type, file);
	}
	default void saveImage(BufferedImage image, String type, OutputStream os) throws IOException {
		ImageIO.write(image, type, os);
	}
}
