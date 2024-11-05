package com.minecrafttas.discombobulator.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.minecrafttas.discombobulator.Processor;
import com.minecrafttas.discombobulator.utils.Pair;

class TestVersionNestingInverted extends TestBase {

	private List<String> allVersions = Arrays.asList("1.12.2", "1.12", "1.11.2", "1.8.9", "1.7.10");

	private Processor processor = new Processor(allVersions, null, true);

	/**
	 * TargetVersion: 1.12 Expected: 1.12 only
	 * 
	 * @throws Exception
	 */
	@Test
	void testTripleNesting() throws Exception {
		String folder = "TestNesting/triplenestinginverted";
		String actualName = "Actual.java";
		String expectedName = "Expected1.12.txt";
		String targetVersion = "1.12";

		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);

		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), actualName, getExtension(actualName));

		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());

		assertEquals(expected, actual);
	}

	/**
	 * TargetVersion: 1.11.2 Expected: 1.12, 1.11.2
	 * 
	 * @throws Exception
	 */
	@Test
	void testTripleNesting2() throws Exception {
		String folder = "TestNesting/triplenestinginverted";
		String actualName = "Actual.java";
		String expectedName = "Expected1.11.2.txt";
		String targetVersion = "1.11.2";

		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);

		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), actualName, getExtension(actualName));

		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());

		assertEquals(expected, actual);
	}

	/**
	 * TargetVersion: 1.8.9 Expected: 1.12, 1.11.2, 1.8.9
	 * 
	 * @throws Exception
	 */
	@Test
	void testTripleNesting3() throws Exception {
		String folder = "TestNesting/triplenestinginverted";
		String actualName = "Actual.java";
		String expectedName = "Expected1.8.9.txt";
		String targetVersion = "1.8.9";

		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);

		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), actualName, getExtension(actualName));

		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());

		assertEquals(expected, actual);
	}

	/**
	 * TargetVersion: 1.7.10 Expected: 1.12, 1.11.2, 1.7.10
	 * 
	 * @throws Exception
	 */
	@Test
	void testTripleNesting4() throws Exception {
		String folder = "TestNesting/triplenestinginverted";
		String actualName = "Actual.java";
		String expectedName = "Expected1.7.10.txt";
		String targetVersion = "1.7.10";

		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);

		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), actualName, getExtension(actualName));

		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());

		assertEquals(expected, actual);
	}

	/**
	 * TargetVersion: Null Expected: 1.12.2
	 * 
	 * @throws Exception
	 */
	@Test
	void testNoTargetVersionInverted() throws Exception {
		String folder = "TestNesting/triplenestinginverted";
		String actualName = "Actual.java";
		String expectedName = "Expected1.12.2.txt";
		String targetVersion = null;

		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);

		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), actualName, getExtension(actualName));

		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());

		assertEquals(expected, actual);
	}

	// =================================== Errors

	/**
	 * Nesting version lower than parent fail
	 * 
	 * @throws Exception
	 */
	@Test
	void testOneBelowParentInNesting() throws Exception {
		String folder = "TestNesting/errorsinverted";
		String actualName = "Actual2.java";
		String expectedName = null;
		String targetVersion = "1.12.2";

		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);

		Exception exception = assertThrows(Exception.class, () -> {
			processor.preprocess(targetVersion, lines.left(), actualName, getExtension(actualName));
		});

		assertEquals("The version in the nesting block is greater than in the parent block. Nested: 1.11.2, Parent: 1.8.9, Line: 10, File: Actual2.java", exception.getMessage());
	}

	/**
	 * Test additional end in nesting
	 * 
	 * @throws Exception
	 */
	@Test
	void testOneEndTooMuch() throws Exception {
		String folder = "TestNesting/errorsinverted";
		String actualName = "Actual3.java";
		String expectedName = null;
		String targetVersion = "1.12.2";

		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);

		Exception exception = assertThrows(Exception.class, () -> {
			processor.preprocess(targetVersion, lines.left(), actualName, getExtension(actualName));
		});

		assertEquals("Unexpected 'end' in nested block found in line 14 in Actual3.java", exception.getMessage());
	}

	/**
	 * Test missing end in nesting
	 * 
	 * @throws Exception
	 */
	@Test
	void testOneEndMissing() throws Exception {
		String folder = "TestNesting/errorsinverted";
		String actualName = "Actual4.java";
		String expectedName = null;
		String targetVersion = "1.12.2";

		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);

		Exception exception = assertThrows(Exception.class, () -> {
			processor.preprocess(targetVersion, lines.left(), actualName, getExtension(actualName));
		});

		assertEquals("Missing an end for nesting before line 13 in Actual4.java", exception.getMessage());
	}
}
