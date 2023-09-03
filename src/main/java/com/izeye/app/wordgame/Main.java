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

import java.util.Map;
import java.util.Scanner;

/**
 * Main class.
 *
 * @author Johnny Lim
 */
public class Main {

	public static void main(String[] args) {
		Map<String, String> koreanToEnglish = Map.ofEntries(Map.entry("사과", "apple"), Map.entry("빨간", "red"));

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
