/*
 * Copyright 2012-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.izeye.app.wordgame;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.stereotype.Component;

/**
 * Discovers word files under the configured libraries and reads their words.
 * <p>
 * Files are addressed by opaque IDs resolved through an index built while scanning, so a
 * request can never name a path outside a configured library.
 *
 * @author Johnny Lim
 */
@Component
public class WordLibrary {

	private static final Log logger = LogFactory.getLog(WordLibrary.class);

	private static final String VERTICAL_BAR_EXTENSION = ".vsv";

	private static final char DELIMITER_DEFAULT = ',';

	private static final char DELIMITER_VERTICAL_BAR = '|';

	private static final int MAX_SCAN_DEPTH = 8;

	private static final Comparator<String> NATURAL_ORDER = WordLibrary::compareNatural;

	private final WordGameProperties properties;

	private volatile Map<String, Path> index = Map.of();

	public WordLibrary(WordGameProperties properties) {
		this.properties = properties;
	}

	/**
	 * Scans every configured library and returns the books found, rebuilding the ID index
	 * as a side effect so that newly added files become playable without a restart.
	 * @return books found, ordered by library then by name
	 */
	public List<Book> books() {
		Map<String, Path> newIndex = new LinkedHashMap<>();
		List<Book> books = new ArrayList<>();
		for (WordGameProperties.Library library : this.properties.getLibraries()) {
			Path root = Path.of(library.getPath()).toAbsolutePath().normalize();
			if (!Files.isDirectory(root)) {
				logger.warn("Skipping library '%s': '%s' is not a directory".formatted(library.getName(), root));
				continue;
			}
			books.addAll(scan(library.getName(), root, newIndex));
		}
		this.index = newIndex;
		return books;
	}

	/**
	 * Reads the words of a single file.
	 * @param id identifier previously handed out by {@link #books()}
	 * @return the file's words, or {@code null} when the ID is unknown
	 */
	public WordFileContent read(String id) {
		Path path = this.index.get(id);
		if (path == null) {
			// The client may not have listed the books yet, or the file is new.
			books();
			path = this.index.get(id);
		}
		if (path == null || !Files.isRegularFile(path)) {
			return null;
		}
		return new WordFileContent(id, path.getFileName().toString(), parse(path));
	}

	private List<Book> scan(String libraryName, Path root, Map<String, Path> newIndex) {
		Map<Path, List<Path>> byDirectory = new TreeMap<>(Comparator.comparing(Path::toString, NATURAL_ORDER));
		try (Stream<Path> stream = Files.walk(root, MAX_SCAN_DEPTH)) {
			stream.filter(Files::isRegularFile)
				.filter(WordLibrary::isWordFile)
				.forEach((path) -> byDirectory.computeIfAbsent(path.getParent(), (key) -> new ArrayList<>()).add(path));
		}
		catch (IOException ex) {
			logger.warn("Failed to scan library '%s' at '%s'".formatted(libraryName, root), ex);
			return List.of();
		}

		List<Book> books = new ArrayList<>();
		byDirectory.forEach((directory, paths) -> {
			paths.sort(Comparator.comparing((path) -> path.getFileName().toString(), NATURAL_ORDER));
			String relativeDirectory = root.relativize(directory).toString();
			String bookName = relativeDirectory.isEmpty() ? root.getFileName().toString() : relativeDirectory;
			String bookId = libraryName + "/" + bookName;

			List<WordFile> files = new ArrayList<>();
			for (Path path : paths) {
				String fileId = libraryName + "/" + root.relativize(path);
				newIndex.put(fileId, path);
				files.add(new WordFile(fileId, path.getFileName().toString(), parse(path).size()));
			}
			books.add(new Book(bookId, libraryName, bookName, files));
		});
		return books;
	}

	private static boolean isWordFile(Path path) {
		String name = path.getFileName().toString().toLowerCase();
		return name.endsWith(".csv") || name.endsWith(VERTICAL_BAR_EXTENSION);
	}

	private List<WordEntry> parse(Path path) {
		char delimiter = path.getFileName().toString().toLowerCase().endsWith(VERTICAL_BAR_EXTENSION)
				? DELIMITER_VERTICAL_BAR : DELIMITER_DEFAULT;
		List<WordEntry> entries = new ArrayList<>();
		try {
			for (String line : Files.readAllLines(path)) {
				String trimmed = line.trim();
				int position = trimmed.indexOf(delimiter);
				if (position <= 0) {
					// Blank line, comment, or a row without a delimiter.
					continue;
				}
				String answer = trimmed.substring(0, position).trim();
				String question = trimmed.substring(position + 1).trim();
				if (!answer.isEmpty() && !question.isEmpty()) {
					entries.add(new WordEntry(question, answer));
				}
			}
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to read '%s'".formatted(path), ex);
		}
		return entries;
	}

	/**
	 * Compares strings so that embedded numbers sort by value, keeping file names such as
	 * "2.csv" and "10.csv" in the order a reader expects.
	 * @param left first string to compare
	 * @param right second string to compare
	 * @return a negative integer, zero, or a positive integer as the first argument is
	 * less than, equal to, or greater than the second
	 */
	private static int compareNatural(String left, String right) {
		int leftIndex = 0;
		int rightIndex = 0;
		while (leftIndex < left.length() && rightIndex < right.length()) {
			char leftChar = left.charAt(leftIndex);
			char rightChar = right.charAt(rightIndex);
			if (Character.isDigit(leftChar) && Character.isDigit(rightChar)) {
				int leftEnd = endOfDigits(left, leftIndex);
				int rightEnd = endOfDigits(right, rightIndex);
				int comparison = new BigInteger(left.substring(leftIndex, leftEnd))
					.compareTo(new BigInteger(right.substring(rightIndex, rightEnd)));
				if (comparison != 0) {
					return comparison;
				}
				leftIndex = leftEnd;
				rightIndex = rightEnd;
				continue;
			}
			if (leftChar != rightChar) {
				return Character.compare(leftChar, rightChar);
			}
			leftIndex++;
			rightIndex++;
		}
		return (left.length() - leftIndex) - (right.length() - rightIndex);
	}

	private static int endOfDigits(String value, int from) {
		int index = from;
		while (index < value.length() && Character.isDigit(value.charAt(index))) {
			index++;
		}
		return index;
	}

}
