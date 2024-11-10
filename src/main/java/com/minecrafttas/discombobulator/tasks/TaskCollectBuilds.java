package com.minecrafttas.discombobulator.tasks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskAction;

/**
 * This task builds and collects all version
 * 
 * @author Pancake
 */
public class TaskCollectBuilds extends DefaultTask {

	/**
	 * List of all build dirs
	 */
	private Map<String, Path> buildDirs = new HashMap<>();

	@TaskAction
	public void collectBuilds() {
		Path collectDir = getBuildDir(getProject());

		try {
			if (!Files.exists(collectDir))
				Files.createDirectory(collectDir);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		for (Entry<String, Path> entry : buildDirs.entrySet()) {
			Path buildDir = entry.getValue();
			Stream<Path> stream;

			try {
				stream = Files.list(buildDir);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}

			stream.forEach(path -> {
				Path targetFile = collectDir.resolve(path.getFileName());

				try {
					Files.move(path, targetFile, StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
			});

			stream.close();
		}
	}

	/**
	 * Updates this task with all compile tasks
	 * 
	 * @param compileTasks List of compile tasks
	 */
	public void updateCompileTasks(List<Task> compileTasks) {
		for (Task task : compileTasks) {
			Project project = task.getProject();
			this.buildDirs.put(project.getName(), getBuildDir(project).resolve("libs"));
		}
		this.setDependsOn(compileTasks);
	}

	/**
	 * @param project The project to use
	 * @return The build directory from the project
	 */
	private Path getBuildDir(Project project) {
		return project.getLayout().getBuildDirectory().get().getAsFile().toPath();
	}
}
