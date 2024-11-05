package com.minecrafttas.discombobulator.utils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Consumer;

/**
 * Improvement to the java.nio.Files File Walker
 * @author Pancake
 *
 */
public class BetterFileWalker {

	/**
	 * Traverse all subdirectories recursively
	 * @param root Root directory
	 * @param callback File consumable
	 */
	public static void walk(Path root, Consumer<Path> callback) {
		try {
			Files.walkFileTree(root, new FileVisitor<Path>() {
				// @formatter:off
				@Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException { return FileVisitResult.CONTINUE; }
				@Override public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException { return FileVisitResult.CONTINUE; }
				@Override public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException { return FileVisitResult.CONTINUE; }
				// @formatter:on

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					callback.accept(root.relativize(file));
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Unable to walk file tree", e);
		}
	}

}
