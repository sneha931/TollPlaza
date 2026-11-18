package com.freightfox.tollplaza.loader;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal RFC-4180 line parser: handles quoted fields containing commas and escaped
 * quotes ({@code ""}). Enough for the toll plaza CSV without pulling in a CSV library.
 */
final class CsvLineParser {

	private CsvLineParser() {
	}

	static List<String> parse(String line) {
		List<String> fields = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean inQuotes = false;
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (inQuotes) {
				if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
					current.append('"');
					i++;
				}
				else if (c == '"') {
					inQuotes = false;
				}
				else {
					current.append(c);
				}
			}
			else if (c == '"') {
				inQuotes = true;
			}
			else if (c == ',') {
				fields.add(current.toString().trim());
				current.setLength(0);
			}
			else {
				current.append(c);
			}
		}
		fields.add(current.toString().trim());
		return fields;
	}
}
