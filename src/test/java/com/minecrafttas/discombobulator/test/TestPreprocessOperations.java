package com.minecrafttas.discombobulator.test;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.minecrafttas.discombobulator.Discombobulator;
import com.minecrafttas.discombobulator.processor.FilePreprocessor;
import com.minecrafttas.discombobulator.processor.LinePreprocessor;
import com.minecrafttas.discombobulator.utils.PathLock;

class TestPreprocessOperations extends TestBase {

	FilePreprocessor processor;

	@BeforeEach
	void beforeEach() {
		List<String> versions = new ArrayList<>();
		versions.add("1.14.4");
		versions.add("1.12.2");
		processor = new FilePreprocessor(new LinePreprocessor(versions, null), null);
		Discombobulator.pathLock = new PathLock();
	}

	@Test
	void testPreprocessBase() throws Exception {
		Path inFile = testResources.resolve("TestFilePreprocessor/src/TestClass1.java");
		Path outFile = testResources.resolve("TestFilePreprocessor/1.12.2/src/TestClass1.java");
		processor.preprocessFile(inFile, outFile, "1.12.2", ".java");

		List<String> expected = new ArrayList<>();
		String expectedString = "package TestFilePreprocessor.src;\n"
				+ "\n"
				+ "public class TestClass1 {\n"
				+ "	public static void main(String[] args) {\n"
				+ "		//# 1.14.4\n"
				+ "//$$	System.out.println(\"This is 1.14.4\")\n"
				+ "		//# 1.12.2\n"
				+ "		System.out.println(\"This is 1.12.2\");\n"
				+ "		//# end\n"
				+ "	}\n"
				+ "}\n"
				+ "";
		expected.addAll(Arrays.asList(expectedString.split("\n")));

		List<String> actual = readTestResourcesFile(Paths.get("TestFilePreprocessor/1.12.2/src/TestClass1.java"));

		assertIterableEquals(expected, actual);
		Files.delete(outFile);
	}
}
