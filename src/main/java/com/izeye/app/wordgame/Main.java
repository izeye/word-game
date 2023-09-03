/*
 * Copyright 2012-2023 the original author or authors.
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Main class.
 *
 * @author Johnny Lim
 */
public class Main {

	public static void main(String[] args) throws IOException {
		Map<String, String> koreanToEnglish = Files.readAllLines(Path.of("src/main/resources/korean_to_english.csv"))
			.stream()
			.map((line) -> line.split(","))
			.collect(Collectors.toMap((fields) -> fields[0], (fields) -> fields[1]));

		for (Map.Entry<String, String> entry : koreanToEnglish.entrySet()) {
			System.out.println(entry.getKey());
			String line;
			while ((line = new Scanner(System.in).nextLine()) != null) {
				if (line.equals(entry.getValue())) {
					break;
				}
				System.out.println("Wrong. Try again!");
			}
			System.out.println("Correct!");
		}
		System.out.println("Congratulation!");
	}

}
