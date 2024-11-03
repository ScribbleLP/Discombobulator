package com.minecrafttas.discombobulator.tasks;

import java.io.File;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import com.minecrafttas.discombobulator.Discombobulator;
import com.minecrafttas.discombobulator.PathLock;
import com.minecrafttas.discombobulator.utils.FileWatcher;
import com.minecrafttas.discombobulator.utils.Pair;
import com.minecrafttas.discombobulator.utils.SafeFileOperations;
import com.minecrafttas.discombobulator.utils.SocketLock;
import com.minecrafttas.discombobulator.utils.Triple;

/**
 * This task preprocesses the source code on file change
 * 
 * @author Pancake
 */
public class TaskPreprocessWatch extends DefaultTask {

	private List<FileWatcherThread> threads = new ArrayList<>();

	private Triple<List<String>, Path, Path> currentFileUpdater = null;

	private boolean msgSeen = false;

	@TaskAction
	public void preprocessWatch() {
		System.out.println(Discombobulator.getSplash());
		// Lock port
		var lock = new SocketLock(Discombobulator.PORT_LOCK);
		lock.tryLock();

		// Prepare list of physical version folders
		// Left=Version, Right=Path to gradle folder
		List<Pair<String, String>> versionsConfig = Discombobulator.getVersionPairs();

		List<Pair<String, Path>> versions = new ArrayList<>();

		for (Pair<String, String> versionConf : versionsConfig) {
			String path = versionConf.right();
			if (path == null) {
				path = versionConf.left();
			}
			File rootDir = new File(this.getProject().getProjectDir() + File.separator + path);
			if (new File(rootDir, "build.gradle").exists()) {
				String ver = versionConf.left();
				versions.add(Pair.of(ver, new File(rootDir, "src").toPath().toAbsolutePath()));
			}
		}

		for (Pair<String, Path> version : versions)
			this.watch(version.right(), versions);

		// Wait for user input and cancel the task

		Scanner sc = new Scanner(System.in);
		System.out.println("Press ENTER to stop the file watcher");
		String in;
		try {
			while (!(in = sc.nextLine()).isBlank()) {
				if (!in.isBlank()) {
					if (currentFileUpdater == null) {
						System.out.println("No recent file exists...\n");
						continue;
					}
					Path outFile = currentFileUpdater.right();
					Path inFile = currentFileUpdater.middle();
					List<String> outLines = currentFileUpdater.left();

					Discombobulator.pathLock.scheduleAndLock(outFile);
					Files.createDirectories(outFile.getParent());
					SafeFileOperations.write(outFile, outLines, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
					Files.setLastModifiedTime(outFile, Files.getLastModifiedTime(inFile));
					currentFileUpdater = null;

					System.out.println(String.format("Processed the recently edited file %s\n", outFile.getFileName()));
				}
			}
		} catch (IOException e1) {
		}
		sc.close();
		for (FileWatcherThread thread : this.threads)
			thread.close();
		lock.unlock();
	}

	/**
	 * Watches and preprocesses a source folder
	 * 
	 * @param file     Source folder
	 * @param versions Map of versions
	 */
	private void watch(Path file, List<Pair<String, Path>> versions) {
		String version = file.getParent().getFileName().toString();
		FileWatcher watcher = null;
		try {
			watcher = constructFileWatcher(file, versions, version);
		} catch (IOException e) {
			e.printStackTrace();
		}
		threads.add(new FileWatcherThread(watcher, version));
	}

	private FileWatcher constructFileWatcher(Path file, List<Pair<String, Path>> versions, String version)
			throws IOException {
		return new FileWatcher(file) {

			@Override
			protected void onNewFile(Path path) {

			}

			@Override
			protected void onModifyFile(Path path) {
				// Get the filename that is getting prerprocessed
				String filename = path.getFileName().toString();

				PathLock schedule = Discombobulator.pathLock;
				if (schedule.isLocked(path))
					return;

				// Get path relative to the root dir
				Path relativeFile = file.relativize(path);
				try {
					// Modify this file in other versions too

					// Read the original file
					List<String> inLines = Files.readAllLines(path);

					// Iterate through all versions
					for (Pair<String, Path> subVersion : versions) {
						// If the version equals the original version, then skip it

						// Preprocess the lines
						String[] split = path.getFileName().toString().split("\\.");
						List<String> outLines = Discombobulator.processor.preprocess(subVersion.left(), inLines,
								filename, split[split.length - 1]);

						// Write file
						Path outFile = subVersion.right().resolve(relativeFile);

						if (subVersion.right().equals(file)) {
							currentFileUpdater = Triple.of(outLines, path, outFile);
							continue;
						}

						schedule.scheduleAndLock(outFile);
						Files.createDirectories(outFile.getParent());
						SafeFileOperations.write(outFile, outLines, StandardOpenOption.CREATE,
								StandardOpenOption.WRITE);
						Files.setLastModifiedTime(outFile, Files.getLastModifiedTime(path));
					}
					// Modify this file in base project
					String[] split = path.getFileName().toString().split("\\.");
					List<String> lines = Discombobulator.processor.preprocess(null, inLines, filename,
							split[split.length - 1]);
					Path outFile = new File(TaskPreprocessWatch.this.getProject().getProjectDir(), "src").toPath()
							.toAbsolutePath().resolve(relativeFile);
					Files.createDirectories(outFile.getParent());
					SafeFileOperations.write(outFile, lines, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
					Files.setLastModifiedTime(outFile, Files.getLastModifiedTime(path));
					System.out.println(String.format("Processed %s in %s", path.getFileName(), version));

					if (msgSeen == false) {
						System.out.println("Type 1 to also preprocess this file\n");
						msgSeen = true;
					}
				} catch (IOException e) {
					e.printStackTrace();
				} catch (Exception e) {
					System.err.println(e.getMessage());
					return;
				}
			}

			@Override
			protected void onDeleteFile(Path path) {
				var relativeFile = file.relativize(path);
				// Delete this file in other versions too
				// Iterate through all versions
				for (Pair<String, Path> subVersion : versions) {
					if (subVersion.right().equals(file))
						continue;
					SafeFileOperations.delete(subVersion.right().resolve(relativeFile).toFile());
				}
				// Delete this file in base project
				SafeFileOperations.delete(new File(TaskPreprocessWatch.this.getProject().getProjectDir(), "src")
						.toPath().toAbsolutePath().resolve(relativeFile).toFile());
			}
		};
	}

	/**
	 * Custom closable FileWatcher Thread
	 * <p>
	 * Previously the threads kept running in the background, even after the main
	 * thread closed. With this, we can close the threads for good.
	 * 
	 * @author Scribble
	 *
	 */
	private class FileWatcherThread extends Thread {

		private FileWatcher watcher;

		public FileWatcherThread(FileWatcher watcher, String version) {
			super("FileWatcher-" + version);
			System.out.println("Started watching " + version);
			this.watcher = watcher;
			this.setDaemon(true);
			this.start();
		}

		@Override
		public void run() {
			try {
				watcher.watch();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				System.out.println("Interrupting " + this.getName());
				if (watcher != null)
					watcher.close();
				e.printStackTrace();
			} catch (ClosedWatchServiceException e) {
				System.out.println("Shutting down " + this.getName());
			}
		}

		public void close() {
			if (watcher != null)
				watcher.close();
		}
	}
}
