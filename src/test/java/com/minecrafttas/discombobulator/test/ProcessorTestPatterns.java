package com.minecrafttas.discombobulator.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.minecrafttas.discombobulator.processor.LinePreprocessor;
import com.minecrafttas.discombobulator.utils.Pair;

class ProcessorTestPatterns extends TestBase {

	Map<String, Map<String, String>> patterns = Map.of("GetLevel", Map.of("1.14.4", "level", "def", "world"), "GetMinecraft", Map.of("1.14.4", "Minecraft.getInstance()", "def", "Minecraft.getMinecraft()"));

	private List<String> allVersions = Arrays.asList("1.15.2", "1.14.4", "1.13.2", "1.12.2", "1.11.2");

	private LinePreprocessor processor = new LinePreprocessor(allVersions, patterns);

	/**
	 * TargetVersion: 1.14.4
	 * Expected: 1.14.4
	 * @throws Exception
	 */
	@Test
	void testPattern1() throws Exception {
		String folder = "TestPattern";
		String actualName = "Actual.java";
		String expectedName = "Expected1.14.4.txt";
		String targetVersion = "1.14.4";

		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);

		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), getExtension(actualName));

		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());

		assertEquals(expected, actual);
	}

	/**
	 * TargetVersion: 1.15.2
	 * Expected: 1.14.4
	 * @throws Exception
	 */
	@Test
	void testPattern2() throws Exception {
		String folder = "TestPattern";
		String actualName = "Actual.java";
		String expectedName = "Expected1.14.4.txt";
		String targetVersion = "1.15.2";

		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);

		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), getExtension(actualName));

		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());

		assertEquals(expected, actual);
	}

	/**
	 * TargetVersion: 1.12.2
	 * Expected: 1.12.2
	 * @throws Exception
	 */
	@Test
	void testPattern3() throws Exception {
		String folder = "TestPattern";
		String actualName = "Actual.java";
		String expectedName = "Expected1.12.2.txt";
		String targetVersion = "1.12.2";

		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);

		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), getExtension(actualName));

		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());

		assertEquals(expected, actual);
	}

	/**
	 * TargetVersion: 1.11.2
	 * Expected: 1.12.2
	 * @throws Exception
	 */
	@Test
	void testPattern4() throws Exception {
		String folder = "TestPattern";
		String actualName = "Actual.java";
		String expectedName = "Expected1.12.2.txt";
		String targetVersion = "1.11.2";

		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);

		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), getExtension(actualName));

		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());

		assertEquals(expected, actual);
	}

	/**
	 * TargetVersion: 1.14.2
	 * Expected: Fail
	 * @throws Exception
	 */
	@Test
	void testNonExistingPattern() throws Exception {
		String folder = "TestPatternFail";
		String actualName = "Actual3.java";
		String expectedName = null;
		String targetVersion = "1.14.2";

		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);

		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			processor.preprocess(targetVersion, lines.left(), getExtension(actualName));
		});

		assertEquals("The specified pattern  GetMinecraft , GetLevel in line 		Minecraft.getInstance(); // @ GetMinecraft , GetLevel; was not found for any version", exception.getMessage());
	}

}
