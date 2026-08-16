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

import java.util.List;

/**
 * A group of word files sharing a directory.
 *
 * @param id opaque identifier, unique across libraries
 * @param library name of the library the book was found in
 * @param name directory name as shown in the picker
 * @param files word files belonging to the book, in natural order
 * @author Johnny Lim
 */
public record Book(String id, String library, String name, List<WordFile> files) {

}
