package com.minecrafttas.discombobulator.tasks;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import com.minecrafttas.discombobulator.Discombobulator;
import com.minecrafttas.discombobulator.PathLock;
import com.minecrafttas.discombobulator.utils.FileWatcher;
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
	/**
	 * <p>The source dir in the base project that is used for version control<br>
	 * <code>rootdir/src</code>
	 */
	private Path baseSourceDir;

	private boolean msgSeen = false;

	private WildcardFileFilter fileFilter;

	@TaskAction
	public void preprocessWatch() {
		System.out.println(Discombobulator.getSplash());
		// Lock port
		var lock = new SocketLock(Discombobulator.PORT_LOCK);
		lock.tryLock();

		// Prepare list of physical version folders
		Path baseProjectDir = this.getProject().getProjectDir().toPath();
		baseSourceDir = baseProjectDir.resolve("src");

		List<String> ignored = Discombobulator.ignored;
		fileFilter = WildcardFileFilter.builder().setWildcards(ignored).get();
		if (!ignored.isEmpty())
			System.out.println(String.format("Ignoring %s\n\n", ignored));

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

		for (Entry<String, Path> versionPair : versionsConfig.entrySet()) {
			Path subSourceDir = versionPair.getValue().resolve("src");
			this.watch(subSourceDir, versionsConfig);
		}

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
	 * @param subSourceDir Source folder of the sub project
	 * @param versionSet Map of versions
	 */
	private void watch(Path subSourceDir, Map<String, Path> versionSet) {
		String version = subSourceDir.getParent().getFileName().toString();
		FileWatcher watcher = null;
		try {
			watcher = constructFileWatcher(subSourceDir, versionSet, version);
		} catch (IOException e) {
			e.printStackTrace();
		}
		threads.add(new FileWatcherThread(watcher, version));
	}

	private FileWatcher constructFileWatcher(Path subSourceDir, Map<String, Path> versions, String version) throws IOException {
		return new FileWatcher(subSourceDir) {

			@Override
			protected void onNewFile(Path path) {

			}

			@Override
			protected void onModifyFile(Path path) {
				// Get the filename that is getting preprocessed
				String filename = path.getFileName().toString();

				PathLock schedule = Discombobulator.pathLock;
				if (schedule.isLocked(path))
					return;

				// Get path relative to the root dir
				Path inFile = subSourceDir.relativize(path);

				boolean ignore = false;
				if (fileFilter.accept(inFile.toFile())) {
					System.out.println(String.format("Ignoring %s", inFile.getFileName().toString()));
					ignore = true;
				}

				try {
					// Modify this file in other versions too

					// Read the original file
					String extension = FilenameUtils.getExtension(path.getFileName().toString());
					List<String> linesToProcess = new ArrayList<>();
					try {
						if (!ignore)
							linesToProcess = Files.readAllLines(path);
					} catch (MalformedInputException e) {
						Discombobulator.printError(String.format("Can't process the specified file, probably not a text file: %s\n Maybe add ignoredFileFormats = [\"*.%s\"] to the build.gradle?", path.getFileName(), extension));
						return;
					}

					// Iterate through all versions
					for (Entry<String, Path> versionPair : versions.entrySet()) {
						String versionName = versionPair.getKey();
						Path targetProject = versionPair.getValue();
						Path targetSubSourceDir = targetProject.resolve("src");

						// Write file
						Path outFile = targetSubSourceDir.resolve(inFile);

						if (ignore) {
							Files.copy(inFile, outFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
							continue;
						}

						// Preprocess the lines
						List<String> outLines = Discombobulator.processor.preprocess(versionName, linesToProcess, filename, extension);

						// If the version equals the original version, then skip it
						if (targetSubSourceDir.equals(subSourceDir)) {
							currentFileUpdater = Triple.of(outLines, path, outFile);
							continue;
						}

						schedule.scheduleAndLock(outFile);
						Files.createDirectories(outFile.getParent());
						SafeFileOperations.write(outFile, outLines, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
						Files.setLastModifiedTime(outFile, Files.getLastModifiedTime(path));
					}

					// Modify this file in base project

					Path outFile = baseSourceDir.resolve(inFile);

					if (ignore) {
						Files.copy(inFile, outFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
						return;
					}

					List<String> lines = Discombobulator.processor.preprocess(null, linesToProcess, filename, extension);

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
					Discombobulator.printError(e.getMessage());
					return;
				}
			}

			@Override
			protected void onDeleteFile(Path path) {
				Path relativeFile = subSourceDir.relativize(path);
				// Delete this file in other versions too
				// Iterate through all versions
				for (Entry<String, Path> versionPair : versions.entrySet()) {
					Path targetProject = versionPair.getValue();

					if (targetProject.equals(subSourceDir))
						continue;

					SafeFileOperations.delete(targetProject.resolve(relativeFile));
				}
				// Delete this file in base project
				SafeFileOperations.delete(baseSourceDir.resolve(relativeFile));
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
//				e.printStackTrace();
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
