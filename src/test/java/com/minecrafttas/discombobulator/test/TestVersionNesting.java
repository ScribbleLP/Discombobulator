package com.minecrafttas.discombobulator.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.minecrafttas.discombobulator.Processor;
import com.minecrafttas.discombobulator.utils.Pair;

class TestVersionNesting extends TestBase {

	private List<String> allVersions = Arrays.asList("1.20.0", "1.19.3", "1.19.2", "1.19.0", "1.18.2", "1.18.1", "1.17.1", "1.16.5", "1.16.1", "infinity", "1.15.2", "1.14.4");

	private Processor processor = new Processor(allVersions, null);

	/**
	 * TargetVersion: 1.16.1 Expected: 1.16.1 only
	 * 
	 * @throws Exception
	 */
	@Test
	void testTargetVersion() throws Exception {
		String folder = "TestNesting/singlenesting";
		String actualName = "Actual.java";
		String expectedName = "Expected1.16.1.txt";
		String targetVersion = "1.16.1";

		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);

		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), actualName, getExtension(actualName));

		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());

		assertEquals(expected, actual);
	}

	/**
	 * TargetVersion: 1.16.5 Expected: 1.16.1 and 1.16.5
	 * 
	 * @throws Exception
	 */
	@Test
	void testTargetAndNestingVersion() throws Exception {
		String folder = "TestNesting/singlenesting";
		String actualName = "Actual.java";
		String expectedName = "Expected1.16.5.txt";
		String targetVersion = "1.16.5";

		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);

		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), actualName, getExtension(actualName));

		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());

		assertEquals(expected, actual);
	}

	/**
	 * TargetVersion: 1.17.1 Expected: 1.16.1 and 1.16.5
	 * 
	 * @throws Exception
	 */
	@Test
	void testTargetAndNestingVersionAbove() throws Exception {
		String folder = "TestNesting/singlenesting";
		String actualName = "Actual.java";
		String expectedName = "Expected1.16.5.txt";
		String targetVersion = "1.17.1";

		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);

		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), actualName, getExtension(actualName));

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
	void testTargetAndNestingDefault() throws Exception {
		String folder = "TestNesting/singlenesting";
		String actualName = "Actual.java";
		String expectedName = "Expected1.14.4.txt";
		String targetVersion = "1.14.4";

		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);

		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), actualName, getExtension(actualName));

		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());

		assertEquals(expected, actual);
	}

	/**
	 * TargetVersion: 1.15.2 Expected: 1.14.4, 1.15.2
	 * 
	 * @throws Exception
	 */
	@Test
	void testNestingInDefault() throws Exception {
		String folder = "TestNesting/singlenesting";
		String actualName = "Actual.java";
		String expectedName = "Expected1.15.2.txt";
		String targetVersion = "1.15.2";

		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);

		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), actualName, getExtension(actualName));

		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());

		assertEquals(expected, actual);
	}

	@Test
	void testMultipleVersionBlocksAfterNesting() throws Exception {
		String folder = "TestNesting/ahhhhh";
		String actualName = "Actual.txt";
		String expectedName = "Expected.txt";
		String targetVersion = "1.19.3";

		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);

		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), actualName, getExtension(actualName));

		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());

		assertEquals(expected, actual);
	}

	@Test
	void testAccessWidenerNesting() throws Exception {
		String folder = "TestNesting/accesswidener";
		String actualName = "Actual.accesswidener";
		String expectedName = "Expected1.16.1.txt";
		String targetVersion = "1.16.1";

		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);

		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), actualName, getExtension(actualName));

		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());

		assertEquals(expected, actual);
	}

	@Test
	void testDefaultInNesting() throws Exception {
		String folder = "TestNesting/defaultnesting";
		String actualName = "Actual.java";
		String expectedName = "Expected1.16.1.txt";
		String targetVersion = "1.16.1";

		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);

		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), actualName, getExtension(actualName));

		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());

		assertEquals(expected, actual);
	}

	// =======================================================

	/**
	 * TargetVersion: 1.16.1 Expected: 1.16.1 only
	 * 
	 * @throws Exception
	 */
	@Test
	void testTripleNesting() throws Exception {
		String folder = "TestNesting/triplenesting";
		String actualName = "Actual.java";
		String expectedName = "Expected1.16.1.txt";
		String targetVersion = "1.16.1";

		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);

		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), "Actual", getExtension(actualName));

		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());

		assertEquals(expected, actual);
	}

	/**
	 * TargetVersion: 1.16.5 Expected: 1.16.1, 1.16.5
	 * 
	 * @throws Exception
	 */
	@Test
	void testTripleNesting2() throws Exception {
		String folder = "TestNesting/triplenesting";
		String actualName = "Actual.java";
		String expectedName = "Expected1.16.5.txt";
		String targetVersion = "1.16.5";

		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);

		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), "Actual", getExtension(actualName));

		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());

		assertEquals(expected, actual);
	}

	/**
	 * TargetVersion: 1.17.1 Expected: 1.16.1, 1.16.5, 1.17.1
	 * 
	 * @throws Exception
	 */
	@Test
	void testTripleNesting3() throws Exception {
		String folder = "TestNesting/triplenesting";
		String actualName = "Actual.java";
		String expectedName = "Expected1.17.1.txt";
		String targetVersion = "1.17.1";

		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);

		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), "Actual", getExtension(actualName));

		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());

		assertEquals(expected, actual);
	}

	/**
	 * TargetVersion: 1.18.2 Expected: 1.16.1, 1.16.5, 1.18.2
	 * 
	 * @throws Exception
	 */
	@Test
	void testTripleNesting4() throws Exception {
		String folder = "TestNesting/triplenesting";
		String actualName = "Actual.java";
		String expectedName = "Expected1.18.2.txt";
		String targetVersion = "1.18.2";

		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);

		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), "Actual", getExtension(actualName));

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
		String folder = "TestNesting/errors";
		String actualName = "Actual2.java";
		String expectedName = null;
		String targetVersion = "1.14.4";

		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);

		Exception exception = assertThrows(Exception.class, () -> {
			processor.preprocess(targetVersion, lines.left(), actualName, getExtension(actualName));
		});

		assertEquals("The version in the nesting block is smaller than in the parent block. Nested: infinity, Parent: 1.20.0, Line: 10, File: Actual2.java", exception.getMessage());
	}

	/**
	 * Test additional end in nesting
	 * 
	 * @throws Exception
	 */
	@Test
	void testOneEndTooMuch() throws Exception {
		String folder = "TestNesting/errors";
		String actualName = "Actual3.java";
		String expectedName = null;
		String targetVersion = "1.14.4";

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
		String folder = "TestNesting/errors";
		String actualName = "Actual4.java";
		String expectedName = null;
		String targetVersion = "1.14.4";

		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);

		Exception exception = assertThrows(Exception.class, () -> {
			processor.preprocess(targetVersion, lines.left(), actualName, getExtension(actualName));
		});

		assertEquals("Missing an end for nesting before line 13 in Actual4.java", exception.getMessage());
	}

	/**
	 * Test skipping a nesting level downwards
	 * 
	 * @throws Exception
	 */
	@Test
	void testSkippingNestingDown() throws Exception {
		String folder = "TestNesting/errors";
		String actualName = "Actual5.java";
		String expectedName = null;
		String targetVersion = "1.14.4";

		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);

		Exception exception = assertThrows(Exception.class, () -> {
			processor.preprocess(targetVersion, lines.left(), actualName, getExtension(actualName));
		});

		assertEquals("Unexpected nesting level in line 10 in Actual5.java", exception.getMessage());
	}

	/**
	 * Test skipping a nesting level upwards
	 * 
	 * @throws Exception
	 */
	@Test
	void testSkippingNestingUp() throws Exception {
		String folder = "TestNesting/errors";
		String actualName = "Actual6.java";
		String expectedName = null;
		String targetVersion = "1.14.4";

		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);

		Exception exception = assertThrows(Exception.class, () -> {
			processor.preprocess(targetVersion, lines.left(), actualName, getExtension(actualName));
		});

		assertEquals("Unexpected nesting level in line 14 in Actual6.java", exception.getMessage());
	}

	/**
	 * Test a duplicate nesting definition in a 3 level nesting block
	 * 
	 * @throws Exception
	 */
	@Test
	void testDuplicateInNesting() throws Exception {
		String folder = "TestNesting/errors";
		String actualName = "Actual7.java";
		String expectedName = null;
		String targetVersion = "1.14.4";

		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);

		Exception exception = assertThrows(Exception.class, () -> {
			processor.preprocess(targetVersion, lines.left(), actualName, getExtension(actualName));
		});

		assertEquals("Duplicate version definition 1.18.2 found in line 14 in Actual7.java", exception.getMessage());
	}

	/**
	 * Test a duplicate nesting definition in a 3 level nesting block
	 * 
	 * @throws Exception
	 */
	@Test
	void testDuplicateDefInNesting() throws Exception {
		String folder = "TestNesting/errors";
		String actualName = "Actual8.java";
		String expectedName = null;
		String targetVersion = "1.14.4";

		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);

		Exception exception = assertThrows(Exception.class, () -> {
			processor.preprocess(targetVersion, lines.left(), actualName, getExtension(actualName));
		});

		assertEquals("Duplicate version definition def found in line 16 in Actual8.java", exception.getMessage());
	}
}
