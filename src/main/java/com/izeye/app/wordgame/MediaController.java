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

import java.util.Set;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves the game's feedback sounds.
 * <p>
 * Pronunciation is not served here: the browser speaks each word with the Web Speech API,
 * so only the win/lose cues need files.
 *
 * @author Johnny Lim
 */
@RestController
@RequestMapping("/api")
public class MediaController {

	private static final MediaType AUDIO_WAV = MediaType.parseMediaType("audio/wav");

	private static final Set<String> SOUNDS = Set.of("correct", "wrong", "perfect", "pass", "fail");

	@GetMapping("/sound/{name}")
	public ResponseEntity<Resource> sound(@PathVariable("name") String name) {
		if (!SOUNDS.contains(name)) {
			return ResponseEntity.notFound().build();
		}
		ClassPathResource resource = new ClassPathResource("sounds/" + name + ".wav");
		if (!resource.exists()) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok().contentType(AUDIO_WAV).body(resource);
	}

}
