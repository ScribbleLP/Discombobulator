package com.minecrafttas.discombobulator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleVersionIdentifier;

import com.minecrafttas.discombobulator.extensions.PreprocessingConfiguration;
import com.minecrafttas.discombobulator.tasks.TaskCollectBuilds;
import com.minecrafttas.discombobulator.tasks.TaskPreprocessBase;
import com.minecrafttas.discombobulator.tasks.TaskPreprocessVersion;
import com.minecrafttas.discombobulator.tasks.TaskPreprocessWatch2;
import com.minecrafttas.discombobulator.utils.PathLock;

/**
 * Gradle plugin main class
 * 
 * @author Pancake
 */
public class Discombobulator implements Plugin<Project> {

	public static int PORT_LOCK = 8762;

	public static PreprocessingConfiguration config;

	public static Processor processor;

	public static PathLock pathLock;

	private static String discoVersion;

	public static List<String> ignored;

	/**
	 * Apply the gradle plugin to the project
	 */
	@Override
	public void apply(Project project) {
		// Make buildscript extension for preprocessor
		config = project.getExtensions().create("discombobulator", PreprocessingConfiguration.class);
		// Create schedule
		pathLock = new PathLock();

		// Register tasks
		TaskPreprocessBase baseTask = project.getTasks().register("preprocessBase", TaskPreprocessBase.class).get();
		baseTask.setGroup("discombobulator");
		baseTask.setDescription("Split base source into seperate version folders");

		TaskPreprocessWatch2 watchTask = project.getTasks().register("preprocessWatch", TaskPreprocessWatch2.class).get();
		watchTask.setGroup("discombobulator");
		watchTask.setDescription("Starts a watch session. Preprocesses files into other versions on file change.");

		TaskCollectBuilds collectBuilds = project.getTasks().register("collectBuilds", TaskCollectBuilds.class).get();
		collectBuilds.setGroup("discombobulator");
		collectBuilds.setDescription("Builds, then collects all versions in root/build");

		List<Task> compileTasks = new ArrayList<>();
		for (Project subProject : project.getSubprojects()) {
			compileTasks.add(subProject.getTasksByName("remapJar", false).iterator().next());

			TaskPreprocessVersion versionTask = subProject.getTasks().register("preprocessVersion", TaskPreprocessVersion.class).get();
			versionTask.setGroup("discombobulator");
			versionTask.setDescription("Preprocesses this version back to the base folder and to versions other than this one");
		}
		collectBuilds.updateCompileTasks(compileTasks);

		project.afterEvaluate(_project -> {
			boolean inverted = config.getInverted().getOrElse(false);
			PORT_LOCK = config.getPort().getOrElse(8762);

			Map<String, Path> versionPairs = null;
			Path projectDir = _project.getProjectDir().toPath();
			try {
				versionPairs = getVersionPairs(projectDir);
			} catch (Exception e) {
				if (e.getMessage() != null && !e.getMessage().isEmpty()) {
					printError(e.getMessage());
				} else {
					e.printStackTrace();
				}
				return;
			}
			List<String> versionStrings = new ArrayList<>(versionPairs.keySet());
			processor = new Processor(versionStrings, config.getPatterns().get(), inverted);

			// Yes this is yoinked from the gradle forums to get the disco version. Is there
			// a better method? Probably. Do I care? Currently, no.
			final Configuration classpath = _project.getBuildscript().getConfigurations().getByName("classpath");
			final String version = classpath.getResolvedConfiguration().getResolvedArtifacts().stream().map(artifact -> artifact.getModuleVersion().getId()).filter(id -> "com.minecrafttas".equalsIgnoreCase(id.getGroup())
					&& "discombobulator".equalsIgnoreCase(id.getName())).findAny().map(ModuleVersionIdentifier::getVersion).orElseThrow(() -> new IllegalStateException("Discombobulator plugin has been deployed with wrong coordinates: expected group to be 'com.minecrafttas' and name to be 'Discombobulator'"));
			discoVersion = version;

			ignored = config.getIgnoredFileFormats().getOrElse(new ArrayList<>());
		});

	}

	public static String getSplash() {
		return "\n" + " (                                                                 \n"
				+ " )\\ )                         )         )      (         )         \n"
				+ "(()/( (               )    ( /(      ( /(   (  )\\   ) ( /(    (    \n"
				+ " /(_)))\\ (   (  (    (     )\\())  (  )\\()) ))\\((_| /( )\\())(  )(   \n"
				+ "(_))_((_))\\  )\\ )\\   )\\  '((_)\\   )\\((_)\\ /((_)_ )(_)|_))/ )\\(()\\  \n"
				+ " |   \\(_|(_)((_|(_)_((_)) | |(_) ((_) |(_|_))(| ((_)_| |_ ((_)((_) \n"
				+ " | |) | (_-< _/ _ \\ '  \\()| '_ \\/ _ \\ '_ \\ || | / _` |  _/ _ \\ '_| \n"
				+ " |___/|_/__|__\\___/_|_|_| |_.__/\\___/_.__/\\_,_|_\\__,_|\\__\\___/_|   \n"
				+ "                                                                   \n" + "\n"
				+ getCenterText("Less jank!") + "\n"
				+ "		Created by Pancake and Scribble\n" + getCenterText(discoVersion) + "\n\n";

	}

	public static LinkedHashMap<String, Path> getVersionPairs(Path baseProjectDir) throws Exception {
		Map<String, String> versionsConfig = config.getVersions().get();
		LinkedHashMap<String, Path> versions = new LinkedHashMap<>();

		for (Entry<String, String> versionConf : versionsConfig.entrySet()) {
			String version = versionConf.getKey();
			String pathString = versionConf.getValue();
			if (pathString == null || pathString.isEmpty()) {
				pathString = version;
			}
			Path subProjectDir = baseProjectDir.resolve(pathString);

			if (Files.exists(subProjectDir.resolve("build.gradle"))) {
				versions.putLast(versionConf.getKey(), subProjectDir);
			} else {
				throw new Exception("Could not find build.gradle in " + subProjectDir.toString());
			}
		}
		return versions;
	}

	private static String getCenterText(String text) {
		int length = text.length();
		int total = 31;
		if (length % 2 == 0) {
			total = 32;
		}
		return String.format("%s%s", " ".repeat(total - length / 2), text);
	}

	public static void printError(String line) {
		System.err.println("\033[0;31m" + line + "\033[0m");
	}
	
	public static void printError(String line, String filename) {
		printError(String.format("[%s] %s", filename, line));
	}
}
