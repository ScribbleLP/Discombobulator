package com.minecrafttas.discombobulator.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.minecrafttas.discombobulator.processor.LinePreprocessor;
import com.minecrafttas.discombobulator.utils.Pair;

class TestOrdering extends TestBase {

	private List<String> allVersions = Arrays.asList("1.20.0", "1.19.3", "1.19.2", "1.19.0", "1.18.2", "1.18.1", "1.17.1", "1.16.5", "1.16.1", "infinity", "1.15.2", "1.14.4");

	private LinePreprocessor processor = new LinePreprocessor(allVersions, null);

	/**
	 * TargetVersion: 1.18.1 Expected: 1.18.1
	 * 
	 * @throws Exception
	 */
	@Test
	void testTargetVersionBeingExact() throws Exception {
		String folder = "TestOrdering/simple";
		String actualName = "Actual.java";
		String expectedName = "Expected1.18.1.txt";
		String targetVersion = "1.18.1";

		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);

		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), getExtension(actualName));

		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());

		assertEquals(expected, actual);

	}

	/**
	 * TargetVersion: 1.18.1 Expected: 1.18.1
	 * 
	 * @throws Exception
	 */
	@Test
	void testTargetVersionNestedBeingExact() throws Exception {
		String folder = "TestOrdering/nested";
		String actualName = "Actual.java";
		String expectedName = "Expected1.18.1.txt";
		String targetVersion = "1.18.1";

		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);

		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), getExtension(actualName));

		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());

		assertEquals(expected, actual);

	}

	/**
	 * TargetVersion: 1.16.5 Expected: A lot
	 * 
	 * @throws Exception
	 */
	@Test
	void testMultipleNestingBlocksInOneBlock() throws Exception {
		String folder = "TestOrdering/verynested";
		String actualName = "Actual.java";
		String expectedName = "Expected1.16.5.txt";
		String targetVersion = "1.16.5";

		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);

		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), getExtension(actualName));

		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());

		assertEquals(expected, actual);

	}
}
