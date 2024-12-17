package com.minecrafttas.discombobulator.utils;

public class LineFeedHelper {

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
