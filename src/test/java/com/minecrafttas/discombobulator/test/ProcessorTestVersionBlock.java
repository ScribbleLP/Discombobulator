package com.minecrafttas.discombobulator.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.minecrafttas.discombobulator.Processor;
import com.minecrafttas.discombobulator.utils.Pair;

class ProcessorTestVersionBlock extends TestBase {

	private List<String> allVersions = Arrays.asList("1.20.0", "1.19.3", "1.19.2", "1.19.0", "1.18.2", "1.18.1", "1.17.1", "1.16.5", "1.16.1", "infinity", "1.15.2", "1.14.4");

	private Processor processor = new Processor(allVersions, null);

	/**
	 * TargetVersion: 1.18.1 Expected: 1.18.1
	 * 
	 * @throws Exception
	 */
	@Test
	void testTargetVersionBeingExact() throws Exception {
		String folder = "TestVersion";
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
	 * TargetVersion: 1.16.5 Expected: 1.16.1
	 * 
	 * @throws Exception
	 */
	@Test
	void testTargetVersionBeingAbove() throws Exception {
		String folder = "TestVersion";
		String actualName = "Actual.java";
		String expectedName = "Expected1.16.1.txt";
		String targetVersion = "1.16.5";

		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);

		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), getExtension(actualName));

		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());

		assertEquals(expected, actual);
	}

	/**
	 * TargetVersion: infinity Expected: 1.14.4
	 * 
	 * @throws Exception
	 */
	@Test
	void testTargetVersionBeingAboveDefault() throws Exception {
		String folder = "TestVersion";
		String actualName = "Actual.java";
		String expectedName = "Expected1.14.1.txt";
		String targetVersion = "infinity";

		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);

		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), getExtension(actualName));

		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());

		assertEquals(expected, actual);
	}

	/**
	 * TargetVersion: 1.14.4 Expected: 1.14.4
	 * 
	 * @throws Exception
	 */
	@Test
	void testTargetVersionBeingDefault() throws Exception {
		String folder = "TestVersion";
		String actualName = "Actual.java";
		String expectedName = "Expected1.14.1.txt";
		String targetVersion = "1.14.4";

		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);

		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), getExtension(actualName));

		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());

		assertEquals(expected, actual);
	}

	/**
	 * TargetVersion: 1.21 Expected: Fail
	 * 
	 * @throws Exception
	 */
	@Test
	void testTargetVersionTooHigh() throws Exception {
		String folder = "TestVersion";
		String actualName = "Actual.java";
		String expectedName = null;
		String targetVersion = "1.21";

		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);

		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			processor.preprocess(targetVersion, lines.left(), getExtension(actualName));
		});

		assertEquals("The target version 1.21 was not found in line 4", exception.getMessage());
	}

	/**
	 * TargetVersion: 1.16.1 Expected: Fail
	 * 
	 * @throws Exception
	 */
	@Test
	void testNonExistingVersion() throws Exception {
		String folder = "TestVersionFail";
		String actualName = "Actual.java";
		String expectedName = null;
		String targetVersion = "1.16.1";

		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);

		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			processor.preprocess(targetVersion, lines.left(), getExtension(actualName));
		});

		assertEquals("The specified version CrazyVersionName in line 6 was not found", exception.getMessage());
	}

	/**
	 * TargetVersion: null Expected: All comment out
	 * 
	 * @throws Exception
	 */
	@Test
	void testNoTargetVersion() throws Exception {
		String folder = "TestVersion";
		String actualName = "Actual.java";
		String expectedName = "Expected1.14.1.txt";
		String targetVersion = null;

		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);

		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), getExtension(actualName));

		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());

		assertEquals(expected, actual);
	}

	/**
	 * An additional end Expected: Error
	 * 
	 * @throws Exception
	 */
	@Test
	void testEndTooMuch() throws Exception {
		String folder = "TestVersionFail";
		String actualName = "Actual2.java";
		String expectedName = null;
		String targetVersion = null;

		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);

		Exception exception = assertThrows(Exception.class, () -> {
			processor.preprocess(targetVersion, lines.left(), getExtension(actualName));
		});

		assertEquals("Unexpected 'end' found in line 11", exception.getMessage());
	}

	/**
	 * Duplicate version Expected: Error
	 * 
	 * @throws Exception
	 */
	@Test
	void testDuplicate() throws Exception {
		String folder = "TestVersionFail";
		String actualName = "Actual3.java";
		String expectedName = null;
		String targetVersion = null;

		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);

		Exception exception = assertThrows(Exception.class, () -> {
			processor.preprocess(targetVersion, lines.left(), getExtension(actualName));
		});

		assertEquals("Duplicate version definition 1.16.1 found in line 8", exception.getMessage());
	}
}
