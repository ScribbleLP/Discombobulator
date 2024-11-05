package com.minecrafttas.discombobulator.extensions;

import java.util.Map;

import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;

/**
 * Configuration for preprocessing
 * @author Pancake
 */
public abstract class PreprocessingConfiguration {

	/**
	 * All versions for the project
	 * <pre>
	 * versions = [
	 * 	"1.14.4",
	 * 	"1.12.2",
	 * 	"1.8.9: AlternateFolderName1.8.9"
	 * ]
	 * </pre>
	 * @return Ordered list of versions
	 */
	public abstract ListProperty<String> getVersions();

	/**
	 * All patterns for the project.
	 * <pre>
	 * patterns = [
	 * 	GetMinecraft: [
	 * 		"1.14.4": "Minecraft.getMinecraft().getPlayer()",
	 *		"1.12.2": "Minecraft.getMinecraft().player",
	 *		"def": "Minecraft.getMinecraft().thePlayer"
	 * 	]
	 * ]
	 * </pre>
	 * Anything annotated by @key will be adjusted according to the version where "1.14.4" would refer to any version above or equal to 1.14.4<br>
	 * {@code Minecraft.getMinecraft().getPlayer(); // @GetMinecraft} would be adjusted to {@code Minecraft.getMinecraft().thePlayer} in versions below 1.12.2
	 * @return Map of patterns
	 */
	public abstract MapProperty<String, Map<String, String>> getPatterns();

	/**
	 * If the default version should be inverted
	 * @return True if inverted
	 */
	public abstract Property<Boolean> getInverted();

	/**
	 * The port for the port lock
	 * @return The port number
	 */
	public abstract Property<Integer> getPort();
}
