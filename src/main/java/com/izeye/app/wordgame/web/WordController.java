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

package com.izeye.app.wordgame.web;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exposes the word files available to play and their contents.
 *
 * @author Johnny Lim
 */
@RestController
@RequestMapping("/api")
public class WordController {

	private final WordLibrary library;

	public WordController(WordLibrary library) {
		this.library = library;
	}

	@GetMapping("/books")
	public List<Book> books() {
		return this.library.books();
	}

	@GetMapping("/words")
	public List<WordFileContent> words(@RequestParam("id") List<String> ids) {
		List<WordFileContent> contents = new ArrayList<>();
		for (String id : ids) {
			WordFileContent content = this.library.read(id);
			if (content == null) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown word file '%s'".formatted(id));
			}
			contents.add(content);
		}
		return contents;
	}

}
