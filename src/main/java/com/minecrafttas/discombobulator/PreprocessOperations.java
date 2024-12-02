package com.minecrafttas.discombobulator;

import java.io.FileFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.minecrafttas.discombobulator.utils.BetterFileWalker;
import com.minecrafttas.discombobulator.utils.SafeFileOperations;
import com.minecrafttas.discombobulator.utils.Triple;

public class PreprocessOperations {

	public static void preprocessFile(Path inFile, Path outFile, String version, FileFilter fileFilter, String extension) throws Exception {

//		System.out.println(inFile);
//		System.out.println(outFile+"\n");

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

		if (fileFilter.accept(inFile.toFile())) {
			System.out.println(String.format("Ignoring %s", inFile.getFileName().toString()));
			Files.copy(inFile, outFile, StandardCopyOption.REPLACE_EXISTING);
			return;
		}

		List<String> linesToProcess = Files.readAllLines(inFile);

		preprocessLines(linesToProcess, outFile, version, fileFilter, extension);
	}

	public static Triple<List<String>, Path, Path> /*<- TODO Change to it's own class*/ preprocessVersions(Path inFile, Map<String, Path> versions, FileFilter fileFilter, String extension, Path currentDir) throws Exception {

		List<String> linesToProcess = Files.readAllLines(inFile);

		Triple<List<String>, Path, Path> out = null;

		// Iterate through all versions
		for (Entry<String, Path> versionPair : versions.entrySet()) {
			String versionName = versionPair.getKey();
			Path targetProject = versionPair.getValue();
			Path targetSubSourceDir = targetProject.resolve("src");
			Path outFile = targetSubSourceDir.resolve(inFile);

			if (fileFilter.accept(inFile.toFile())) {
				System.out.println(String.format("Ignoring %s", inFile.getFileName().toString()));
				Files.copy(inFile, outFile, StandardCopyOption.REPLACE_EXISTING);
				continue;
			}

			// Preprocess the lines
			List<String> outLines = Discombobulator.processor.preprocess(versionName, linesToProcess, extension);

			// If the version equals the original version, then skip it
			if (targetSubSourceDir.equals(currentDir)) {
				out = Triple.of(outLines, inFile, outFile);
				continue;
			}

			preprocessLines(outLines, outFile, versionName, fileFilter, extension);
		}

		return out;
	}

	/**
	 * Preprocesses and writes the inLines to a file
	 * @param inLines The lines to preprocess
	 * @param outFile The file to write to
	 * @param version The version to preprocess to
	 * @param fileFilter The file filter to ignore
	 * @param extension The file extension
	 * @return The preprocessed lines
	 * @throws Exception
	 */
	public static List<String> preprocessLines(List<String> inLines, Path outFile, String version, FileFilter fileFilter, String extension) throws Exception {
		List<String> lines = Discombobulator.processor.preprocess(version, inLines, extension);

		// Lock the file
		Discombobulator.pathLock.scheduleAndLock(outFile);

		// Write file and update last modified date
		Files.createDirectories(outFile.getParent());
		SafeFileOperations.write(outFile, lines, StandardOpenOption.CREATE, StandardOpenOption.WRITE);

		return lines;
	}

	/**
	 * Compares 2 source directories and deletes all files in otherSourceDir that are not in baseSourceDir
	 * @param baseSourceDir The source dir to compare
	 * @param otherSourceDir The source dir to delete from
	 * @param version The version to check, used in logging
	 */
	public static void deleteExcessFiles(Path baseSourceDir, Path otherSourceDir, String version) {
		BetterFileWalker.walk(otherSourceDir, path -> {
			Path relativePath = otherSourceDir.relativize(path);
			// Verify if file exists in base source dir
			Path baseFile = baseSourceDir.resolve(relativePath);
			if (!Files.exists(baseFile)) {
				System.out.println(String.format("Deleting %s in version %s", path.getFileName().toString(), version));
				SafeFileOperations.delete(baseFile);
			}
		});
	}
}
