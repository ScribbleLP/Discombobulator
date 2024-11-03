package com.minecrafttas.discombobulator.tasks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import com.minecrafttas.discombobulator.Discombobulator;
import com.minecrafttas.discombobulator.utils.BetterFileWalker;
import com.minecrafttas.discombobulator.utils.Pair;
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
		List<Pair<String, String>> versionsConfig = Discombobulator.getVersionPairs();

		List<Pair<String, String>> versions = new ArrayList<>();

		for (Pair<String, String> versionConf : versionsConfig) {
			String path = versionConf.right();
			if (path == null) {
				path = versionConf.left();
			}
			if (new File(this.getProject().getProjectDir(), path + File.separator + "build.gradle").exists()) {
				versions.add(Pair.of(versionConf.left(), path));
			}
		}

		System.out.println("Preprocessing base source...");

		File baseSourceDir = new File(this.getProject().getProjectDir(), "src");
		if (!baseSourceDir.exists())
			throw new RuntimeException("Base source folder not found");

		BetterFileWalker.walk(baseSourceDir.toPath(), path -> {
			System.out.println("Preprocessing " + path.getFileName());
			try {
				for (Pair<String, String> version : versions) {
					// Find input and output file
					Path inFile = baseSourceDir.toPath().resolve(path);
					Path outFile = new File(baseSourceDir.getParent(), version.right() + File.separatorChar + "src")
							.toPath().resolve(path);

//					System.out.println(inFile);
//					System.out.println(outFile+"\n");

					// Preprocess file
					String[] split = path.getFileName().toString().split("\\.");
					List<String> lines = Discombobulator.processor.preprocess(version.left(),
							Files.readAllLines(inFile), version.left(), split[split.length - 1]);

					// Write file and update last modified date
					Files.createDirectories(outFile.getParent());
					SafeFileOperations.write(outFile, lines, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
					Files.setLastModifiedTime(outFile, Files.getLastModifiedTime(inFile));
				}
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Could not write to filesystem.", e);
			} catch (Exception e) {
				System.err.println(e.getMessage());
				return;
			}
		});

		// Delete all excess file in version folders
		for (Pair<String, String> version : versions) {
			Path subSourceDir = new File(baseSourceDir.getParent(), version.right() + File.separatorChar + "src")
					.toPath();
			BetterFileWalker.walk(subSourceDir, path -> {
				// Verify if file exists in base source dir
				Path originalFile = baseSourceDir.toPath().resolve(path);
				if (!Files.exists(originalFile)) {
					System.out.println("Deleting " + path.getFileName() + " in " + version.left());
					SafeFileOperations.delete(subSourceDir.resolve(path).toFile());
				}
			});
		}

		// Unlock port
		lock.unlock();
	}
}
