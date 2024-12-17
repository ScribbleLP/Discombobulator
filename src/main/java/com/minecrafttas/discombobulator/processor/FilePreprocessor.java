package com.minecrafttas.discombobulator.processor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.filefilter.WildcardFileFilter;

import com.minecrafttas.discombobulator.Discombobulator;
import com.minecrafttas.discombobulator.utils.BetterFileWalker;
import com.minecrafttas.discombobulator.utils.LineFeedHelper;
import com.minecrafttas.discombobulator.utils.SafeFileOperations;
import com.minecrafttas.discombobulator.utils.Triple;

public class FilePreprocessor {

	private final LinePreprocessor processor;
	private final WildcardFileFilter fileFilter;

	public FilePreprocessor(LinePreprocessor processor, WildcardFileFilter fileFilter) {
		this.processor = processor;
		this.fileFilter = fileFilter;
	}

	public void preprocessFile(Path inFile, Path outFile, String version, String extension) throws Exception {

//		System.out.println(inFile);
//		System.out.println(outFile + "\n");

		/*
		 *  Check if file was just preprocessed.
		 *  This is important when using the file watcher
		 *  
		 *  Example:
		 *  1.12
		 *  1.14
		 *  
		 *  If we edit 1.12, the file watcher triggers
		 *  and edits the same file in 1.14.
		 *  
		 *  But this would also trigger the file watcher for 1.14,
		 *  creating an endless loop of preprocessing back and forth.
		 *  
		 *  So we lock the file just before writing.
		 */
		if (Discombobulator.pathLock.isLocked(inFile)) {
			return;
		}

		if (fileFilter != null && fileFilter.accept(inFile.toFile())) {
			System.out.println(String.format("Ignoring %s", inFile.getFileName().toString()));
			Files.copy(inFile, outFile, StandardCopyOption.REPLACE_EXISTING);
			return;
		}

		List<String> linesToProcess = Files.readAllLines(inFile);

		preprocessLines(linesToProcess, outFile, version, extension);
	}

	public Triple<List<String>, Path, Path> /*<- TODO Change to it's own class*/ preprocessVersions(Path inFile, Map<String, Path> versions, String extension, Path currentDir) throws Exception {

//		System.out.println(inFile);

		boolean ignored = fileFilter != null && fileFilter.accept(inFile.toFile());

		Path relativeInFile = currentDir.relativize(inFile);

		List<String> linesToProcess = null;
		if (!ignored)
			linesToProcess = Files.readAllLines(inFile);
		else
			System.out.println(String.format("Ignoring %s", inFile.getFileName().toString()));

		Triple<List<String>, Path, Path> out = null;

		// Iterate through all versions
		for (Entry<String, Path> versionPair : versions.entrySet()) {
			String versionName = versionPair.getKey();
			Path targetProject = versionPair.getValue();
			Path targetSubSourceDir = targetProject.resolve("src");
			Path outFile = targetSubSourceDir.resolve(relativeInFile);

			if (ignored) {
				Files.copy(inFile, outFile, StandardCopyOption.REPLACE_EXISTING);
				continue;
			}

			// Preprocess the lines
			List<String> outLines = processor.preprocess(versionName, linesToProcess, extension);

			// If the version equals the original version, then skip it
			if (targetSubSourceDir.equals(currentDir)) {
				out = Triple.of(outLines, inFile, outFile);
				continue;
			}

			preprocessLines(outLines, outFile, versionName, extension);
		}

		return out;
	}

	/**
	 * Preprocesses and writes the inLines to a file
	 * @param inLines The lines to preprocess
	 * @param outFile The file to write to
	 * @param version The version to preprocess to
	 * @param extension The file extension
	 * @return The preprocessed lines
	 * @throws Exception
	 */
	public List<String> preprocessLines(List<String> inLines, Path outFile, String version, String extension) throws Exception {
		List<String> lines = processor.preprocess(version, inLines, extension);

		// Lock the file
		Discombobulator.pathLock.scheduleAndLock(outFile);

		// Write file and update last modified date
		Files.createDirectories(outFile.getParent());
		Files.write(outFile, String.join(LineFeedHelper.newLine(), lines).getBytes());

		return lines;
	}

	/**
	 * Compares 2 source directories and deletes all files in otherSourceDir that are not in baseSourceDir
	 * @param baseSourceDir The source dir to compare
	 * @param otherSourceDir The source dir to delete from
	 * @param version The version to check, used in logging
	 */
	public static void deleteExcessFiles(Path baseSourceDir, Path otherSourceDir, String version) {
		BetterFileWalker.walk(otherSourceDir, relativePath -> {
			// Verify if file exists in base source dir
			Path baseFile = baseSourceDir.resolve(relativePath);
			if (!Files.exists(baseFile)) {
				System.out.println(String.format("Deleting %s in version %s", relativePath.getFileName().toString(), version));
				SafeFileOperations.delete(baseFile);
			}
		});
	}

	public LinePreprocessor getLineProcessor() {
		return processor;
	}

	public WildcardFileFilter getFileFilter() {
		return fileFilter;
	}
}
