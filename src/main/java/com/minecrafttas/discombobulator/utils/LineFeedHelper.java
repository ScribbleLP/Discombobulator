package com.minecrafttas.discombobulator.utils;

public class LineFeedHelper {

	public static void printMessage() {
		String property = System.getProperty("line.seperator");
		if ("\\n".equals(property) || "\n".equals(property)) {
			System.out.println("Preprocessing with line seperator \033[0;32m\\n\033[0;37m\n");
		} else if ("\\r\\n".equals(property) || "\r\n".equals(property)) {
			System.out.println("Preprocessing with line seperator \033[0;32m\\r\\n\033[0;37m\n");
		} else {
			System.out.println(String.format("Preprocessing with default line seperator \033[0;32m%s\033[0;37m\nTo change this, add \033[0;33m-Dline.seperator=\"\\n\"\033[0;37m to VM arguments\n", System.lineSeparator().equals("\r\n") ? "\\r\\n" : "\\n"));
		}
	}

	public static String newLine() {
		String out = System.lineSeparator();
		String property = System.getProperty("line.seperator");
		if ("\\n".equals(property) || "\n".equals(property)) {
			out = "\n";
		} else if ("\\r\\n".equals(property) || "\r\n".equals(property)) {
			out = "\r\n";
		}
		return out;
	}
}
