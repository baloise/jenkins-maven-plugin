package com.baloise.maven.jenkins;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.codehaus.plexus.util.FileUtils;

public class FUtil {

	public static void unzip(InputStream in, File dest) throws IOException {
		try {
			ZipInputStream zin = new ZipInputStream(in);
			ZipEntry ze = null;
			byte[] buffer = new byte[8 * 1024];
			while ((ze = zin.getNextEntry()) != null) {
				if (!ze.isDirectory()) {
					String name = ze.getName();
					File outputFile = new File(dest, name);
					File outputDir = outputFile.getParentFile();
					if (!(outputDir.exists() && outputDir.isDirectory())
							&& !outputDir.mkdirs())
						throw new IOException("Can not create directories for "
								+ outputDir.getAbsolutePath());
					outputFile.createNewFile();
					FileOutputStream fout = new FileOutputStream(outputFile);
					int len = 0;
					while ((len = zin.read(buffer)) > 0) {
						fout.write(buffer, 0, len);
					}
					fout.close();
				}
				zin.closeEntry();
			}
			zin.close();
		} catch (IOException e) {
			throw new IOException("Can not unzip to " + dest.getAbsolutePath());
		}
	}

	public static void copyDirectory(File sourceDirectory, File destinationDirectory)
			throws IOException {
		if (!sourceDirectory.exists()) {
			return;
		}
	
		List<File> files = FileUtils.getFiles(sourceDirectory, "**", null );
	
		for (File file : files) {
			copyFileToDirectory(file, sourceDirectory, destinationDirectory);
		}
	}

	public static void copyFileToDirectory(final File source, final File sourceDirectory , final File destinationDirectory) throws IOException {
		if (destinationDirectory.exists() && !destinationDirectory.isDirectory()) {
			throw new IllegalArgumentException("Destination is not a directory");
		}
		String relativeName = source.toString().substring((int) sourceDirectory.toString().length()+1);
		FileUtils.copyFile(source, new File(destinationDirectory, relativeName));
	}
}