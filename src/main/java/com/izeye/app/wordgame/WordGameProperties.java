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

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the word game web app.
 *
 * @author Johnny Lim
 */
@ConfigurationProperties("wordgame")
public class WordGameProperties {

	/**
	 * Libraries scanned for word files. Each library is scanned recursively for CSV
	 * files, which are then grouped into books by their containing directory.
	 */
	private List<Library> libraries = new ArrayList<>();

	public List<Library> getLibraries() {
		return this.libraries;
	}

	public void setLibraries(List<Library> libraries) {
		this.libraries = libraries;
	}

	/**
	 * A named root directory scanned for word files.
	 */
	public static class Library {

		/**
		 * Display name, also used as the first segment of every file ID it contributes.
		 */
		private String name;

		/**
		 * Root directory, resolved against the working directory when relative.
		 */
		private String path;

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getPath() {
			return this.path;
		}

		public void setPath(String path) {
			this.path = path;
		}

	}

}
