package com.minecrafttas.discombobulator.tasks;

import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import com.minecrafttas.discombobulator.Discombobulator;
import com.minecrafttas.discombobulator.utils.BetterFileWalker;
import com.minecrafttas.discombobulator.utils.SafeFileOperations;
import com.minecrafttas.discombobulator.utils.SocketLock;

/**
 * This task preprocesses the base source code into all versions.
 * 
 * @author Pancake
 */
public class TaskPreprocessBase extends DefaultTask {

	@TaskAction
	public void preprocessBase() {
		// Lock port
		SocketLock lock = new SocketLock(Discombobulator.PORT_LOCK);
		lock.tryLock();

		System.out.println(Discombobulator.getSplash());

		// Prepare list of physical version folders
		Path baseProjectDir = this.getProject().getProjectDir().toPath();
		Map<String, Path> versionsConfig;
		try {
			versionsConfig = Discombobulator.getVersionPairs(baseProjectDir);
		} catch (Exception e) {
			if (e.getMessage() != null && !e.getMessage().isEmpty()) {
				Discombobulator.printError(e.getMessage());
			} else {
				e.printStackTrace();
			}
			return;
		}

		System.out.println("Preprocessing base source...\n");

		List<String> ignored = Discombobulator.ignored;

		FileFilter fileFilter = WildcardFileFilter.builder().setWildcards(ignored).get();
		if (!ignored.isEmpty())
			System.out.println(String.format("Ignoring %s\n\n", ignored));

		Path baseSourceDir = baseProjectDir.resolve("src");
		if (!Files.exists(baseSourceDir))
			throw new RuntimeException("Base source folder not found");

		BetterFileWalker.walk(baseSourceDir, path -> {
			System.out.println("Preprocessing " + path);
			try {
				for (Entry<String, Path> versionPairs : versionsConfig.entrySet()) {
					// Find input and output file
					Path inFile = baseSourceDir.resolve(path);
					Path subSourceDir = versionPairs.getValue().resolve("src");
					Path outFile = subSourceDir.resolve(path);
					String version = versionPairs.getKey();

//					System.out.println(inFile);
//					System.out.println(outFile+"\n");

					if (fileFilter.accept(inFile.toFile())) {
						System.out.println(String.format("Ignoring %s", inFile.getFileName().toString()));
						Files.copy(inFile, outFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
						continue;
					}

					// Preprocess file
					String extension = FilenameUtils.getExtension(path.getFileName().toString());

					List<String> linesToProcess;
					try {
						linesToProcess = Files.readAllLines(inFile);
					} catch (MalformedInputException e) {
						Discombobulator.printError(String.format("Can't process the specified file, probably not a text file: %s\n Maybe add ignoredFileFormats = [\"*.%s\"] to the build.gradle?", inFile.getFileName(), extension));
						continue;
					}

					List<String> lines = Discombobulator.processor.preprocess(version, linesToProcess, path.getFileName().toString(), extension);

					// Write file and update last modified date
					Files.createDirectories(outFile.getParent());
					SafeFileOperations.write(outFile, lines, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
					Files.setLastModifiedTime(outFile, Files.getLastModifiedTime(inFile));
				}
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Could not write to filesystem.", e);
			} catch (Exception e) {
				Discombobulator.printError(e.getMessage());
				return;
			}
		});

		// Delete all excess file in version folders
		for (Entry<String, Path> versionPairs : versionsConfig.entrySet()) {
			String version = versionPairs.getKey();

			BetterFileWalker.walk(baseSourceDir, path -> {
				// Verify if file exists in base source dir
				Path originalFile = baseSourceDir.resolve(path);
				if (!Files.exists(originalFile)) {
					System.out.println("Deleting " + originalFile + " in " + version);
					SafeFileOperations.delete(originalFile);
				}
			});
		}

		// Unlock port
		lock.unlock();
	}
}
