package com.minecrafttas.discombobulator.tasks;

import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FilenameUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import com.minecrafttas.discombobulator.Discombobulator;
import com.minecrafttas.discombobulator.processor.FilePreprocessor;
import com.minecrafttas.discombobulator.utils.BetterFileWalker;
import com.minecrafttas.discombobulator.utils.SocketLock;

/**
 * This task preprocesses the base source code into all versions.
 * 
 * @author Pancake, Scribble
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

		Path baseSourceDir = baseProjectDir.resolve("src");
		if (!Files.exists(baseSourceDir))
			throw new RuntimeException("Base source folder not found");

		BetterFileWalker.walk(baseSourceDir, path -> {
			System.out.println("Preprocessing " + path);
			Path inFile = baseSourceDir.resolve(path);
			String extension = FilenameUtils.getExtension(path.getFileName().toString());

			try {
				Discombobulator.fileProcessor.preprocessVersions(inFile, versionsConfig, extension, baseSourceDir);
			} catch (MalformedInputException e) {
				Discombobulator.printError(String.format("Can't process file, probably not a text file...\n Maybe add ignoredFileFormats = [\"*.%s\"] to the build.gradle?", extension), path.getFileName().toString());
				e.printStackTrace();
				return;
			} catch (Exception e) {
				Discombobulator.printError(e.getMessage(), path.getFileName().toString());
				e.printStackTrace();
				return;
			}
		});

		// Delete all excess files in version folders
		for (Entry<String, Path> versionPair : versionsConfig.entrySet()) {
			String version = versionPair.getKey();
			Path versionProjectDir = versionPair.getValue();
			Path versionSourceDir = versionProjectDir.resolve("src");
			FilePreprocessor.deleteExcessFiles(baseSourceDir, versionSourceDir, version);
		}

		// Unlock port
		lock.unlock();
	}
}
