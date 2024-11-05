package com.minecrafttas.discombobulator.tasks;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
	private Map<String, File> buildDirs = new HashMap<>();

	@TaskAction
	public void collectBuilds() {
		File collectDir = getBuildDir(getProject());
		collectDir.mkdirs();
		for (Entry<String, File> entry : buildDirs.entrySet()) {
			File buildDir = entry.getValue();
			for (File artifact : buildDir.listFiles()) {
				artifact.renameTo(new File(collectDir, artifact.getName()));
			}
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
			this.buildDirs.put(project.getName(), new File(getBuildDir(project), "libs"));
		}
		this.setDependsOn(compileTasks);
	}

	/**
	 * @param project The project to use
	 * @return The build directory from the project
	 */
	private File getBuildDir(Project project) {
		return project.getLayout().getBuildDirectory().getAsFile().get();
	}
}
