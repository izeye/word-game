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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Main class.
 *
 * @author Johnny Lim
 */
public class Main {

	public static void main(String[] args)
			throws IOException, UnsupportedAudioFileException, LineUnavailableException, InterruptedException {
		String path = "src/main/resources/words/english_to_korean.csv";
		if (args.length == 1) {
			path = args[0];
		}

		Map<String, String> koreanToEnglish = Files.readAllLines(Path.of(path))
			.stream()
			.map((line) -> line.split(",", 2))
			.collect(Collectors.toMap((fields) -> fields[1], (fields) -> fields[0]));

		List<Map.Entry<String, String>> entries = koreanToEnglish.entrySet().stream().collect(Collectors.toList());
		Collections.shuffle(entries);
		for (Map.Entry<String, String> entry : entries) {
			System.out.println(entry.getKey());
			String line;
			while ((line = new Scanner(System.in).nextLine()) != null) {
				String trimmed = line.trim();
				if (trimmed.equals(entry.getValue())) {
					break;
				}
				System.out.println("Wrong. Try again!");
				playSound("sounds/wrong.wav");
			}
			System.out.println("Correct!");
			playSound("sounds/correct.wav");
		}
		System.out.println("Congratulation!");
	}

	private static void playSound(String soundPath)
			throws UnsupportedAudioFileException, IOException, LineUnavailableException, InterruptedException {
		InputStream inputStream = Main.class.getClassLoader().getResourceAsStream(soundPath);
		AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(inputStream);
		DataLine.Info info = new DataLine.Info(Clip.class, audioInputStream.getFormat());
		Clip clip = (Clip) AudioSystem.getLine(info);
		clip.open(audioInputStream);
		CountDownLatch latch = new CountDownLatch(1);
		clip.addLineListener((event) -> {
			if (event.getType() == LineEvent.Type.STOP) {
				latch.countDown();
			}
		});
		clip.start();
		latch.await();
		clip.close();
		audioInputStream.close();
	}

}
