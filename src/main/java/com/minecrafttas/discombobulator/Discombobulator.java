package com.minecrafttas.discombobulator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleVersionIdentifier;

import com.minecrafttas.discombobulator.extensions.PreprocessingConfiguration;
import com.minecrafttas.discombobulator.tasks.TaskCollectBuilds;
import com.minecrafttas.discombobulator.tasks.TaskPreprocessBase;
import com.minecrafttas.discombobulator.tasks.TaskPreprocessWatch;
import com.minecrafttas.discombobulator.utils.Pair;

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

		TaskPreprocessWatch watchTask = project.getTasks().register("preprocessWatch", TaskPreprocessWatch.class).get();
		watchTask.setGroup("discombobulator");
		watchTask.setDescription("Starts a watch session. Preprocesses files into other versions on file change.");

		TaskCollectBuilds collectBuilds = project.getTasks().register("collectBuilds", TaskCollectBuilds.class).get();
		collectBuilds.setGroup("discombobulator");
		collectBuilds.setDescription("Builds, then collects all versions in root/build");
		List<Task> compileTasks = new ArrayList<>();
		for (Project subProject : project.getSubprojects()) {
			compileTasks.add(subProject.getTasksByName("remapJar", false).iterator().next());
		}
		collectBuilds.updateCompileTasks(compileTasks);

		project.afterEvaluate(_project -> {
			boolean inverted = config.getInverted().getOrElse(false);
			PORT_LOCK = config.getPort().getOrElse(8762);
			processor = new Processor(getVersion(), config.getPatterns().get(), inverted);

			// Yes this is yoinked from the gradle forums to get the disco version. Is there
			// a better method? Probably. Do I care? Currently, no.
			final Configuration classpath = _project.getBuildscript().getConfigurations().getByName("classpath");
			final String version = classpath.getResolvedConfiguration().getResolvedArtifacts().stream()
					.map(artifact -> artifact.getModuleVersion().getId())
					.filter(id -> "com.minecrafttas".equalsIgnoreCase(id.getGroup())
							&& "discombobulator".equalsIgnoreCase(id.getName()))
					.findAny().map(ModuleVersionIdentifier::getVersion).orElseThrow(() -> new IllegalStateException(
							"Discombobulator plugin has been deployed with wrong coordinates: expected group to be 'com.minecrafttas' and name to be 'Discombobulator'"));
			discoVersion = version;
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
				+ getCenterText("You should try our sister preprocessor, Dicombobulator!") + "\n"
				+ "		Created by Pancake and Scribble\n" + getCenterText(discoVersion) + "\n\n";

	}

	public static List<Pair<String, String>> getVersionPairs() {
		List<Pair<String, String>> out = new ArrayList<>();
		List<String> verPre = config.getVersions().get();
		Pattern regex = Pattern.compile("([\\w\\.]+)(:\\s*(.+))?");
		for (String ver : verPre) {
			Matcher matcher = regex.matcher(ver);
			if (matcher.find()) {
				out.add(Pair.of(matcher.group(1), matcher.group(3)));
			}
		}
		return out;
	}

	public static List<String> getVersion() {
		List<String> versions = new ArrayList<>();
		Pattern regex = Pattern.compile("([\\w\\.]+)(:\\s*(.+))?");
		// Initialize Processor
		List<String> mapsnpaths = config.getVersions().get();
		for (String ver : mapsnpaths) {
			Matcher matcher = regex.matcher(ver);
			if (matcher.find()) {
				versions.add(matcher.group(1));
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
}
