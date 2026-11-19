'use strict';

// Mirrors the CLI: the question is the second CSV field, the answer the first.
// Scores are computed exactly as Main.getScore does, so a round played here and a
// round played in the terminal report the same numbers.

const state = {
	books: [],
	book: null,
	selected: new Set(),
	rounds: [],
	roundIndex: 0,
	results: [],
	awaitingNext: false,
	options: { shuffle: true, hint: true, pronounce: false }
};

const el = (id) => document.getElementById(id);

const setup = el('setup');
const game = el('game');
const results = el('results');

/* ---------- Audio ---------- */

// Voices load asynchronously, and getVoices() is empty until they do.
let voice = null;

function pickVoice() {
	if (voice || !('speechSynthesis' in window)) {
		return voice;
	}
	const voices = window.speechSynthesis.getVoices();
	voice = voices.find((v) => v.lang === 'en-US') || voices.find((v) => v.lang.startsWith('en')) || null;
	return voice;
}

function playUrl(url) {
	return new Promise((resolve) => {
		const audio = new Audio(url);
		audio.addEventListener('ended', resolve, { once: true });
		audio.addEventListener('error', resolve, { once: true });
		audio.play().catch(resolve);
	});
}

// Pronunciation is always browser speech synthesis.
function playWord(word) {
	return new Promise((resolve) => {
		if (!('speechSynthesis' in window)) {
			resolve();
			return;
		}
		window.speechSynthesis.cancel();
		const utterance = new SpeechSynthesisUtterance(word);
		utterance.lang = 'en-US';
		const selected = pickVoice();
		if (selected) {
			utterance.voice = selected;
		}
		utterance.onend = resolve;
		utterance.onerror = resolve;
		window.speechSynthesis.speak(utterance);
	});
}

// The win/lose cues are sound effects, not speech, so they stay as audio files.
function playSound(name) {
	return playUrl(`/api/sound/${name}`);
}

/* ---------- Setup ---------- */

async function loadBooks() {
	const response = await fetch('/api/books');
	if (!response.ok) {
		setup.insertAdjacentHTML('afterbegin',
			'<p class="feedback bad">Could not load word files. Is the server running?</p>');
		return;
	}
	state.books = await response.json();
	const select = el('book');
	select.innerHTML = '';
	state.books.forEach((book, index) => {
		const option = document.createElement('option');
		option.value = String(index);
		option.textContent = `${book.name} (${book.library}) — ${book.files.length} files`;
		select.appendChild(option);
	});
	if (state.books.length) {
		selectBook(0);
	}
}

function selectBook(index) {
	state.book = state.books[index];
	state.selected.clear();
	renderFiles();
}

function renderFiles() {
	const container = el('files');
	container.innerHTML = '';
	if (!state.book) {
		return;
	}
	for (const file of state.book.files) {
		const label = document.createElement('label');
		label.className = 'file';
		label.title = file.name;

		const checkbox = document.createElement('input');
		checkbox.type = 'checkbox';
		checkbox.checked = state.selected.has(file.id);
		checkbox.disabled = file.wordCount === 0;
		checkbox.addEventListener('change', () => {
			if (checkbox.checked) {
				state.selected.add(file.id);
			}
			else {
				state.selected.delete(file.id);
			}
			renderSelectionSummary();
		});

		const name = document.createElement('span');
		name.textContent = file.name;

		const count = document.createElement('span');
		count.className = 'count';
		count.textContent = file.wordCount;

		label.append(checkbox, name, count);
		container.appendChild(label);
	}
	renderSelectionSummary();
}

function renderSelectionSummary() {
	const files = state.book ? state.book.files.filter((f) => state.selected.has(f.id)) : [];
	const words = files.reduce((sum, f) => sum + f.wordCount, 0);
	el('selection-summary').textContent = files.length
		? `${files.length} file(s), ${words} words`
		: 'Select at least one file.';
	el('start').disabled = files.length === 0;
}

function leadingNumber(name) {
	const match = name.match(/^(\d+)/);
	return match ? parseInt(match[1], 10) : null;
}

function parseRange(text) {
	const wanted = new Set();
	for (const part of text.split(',')) {
		const trimmed = part.trim();
		if (!trimmed) {
			continue;
		}
		const range = trimmed.match(/^(\d+)\s*-\s*(\d+)$/);
		if (range) {
			const from = Math.min(+range[1], +range[2]);
			const to = Math.max(+range[1], +range[2]);
			for (let i = from; i <= to; i++) {
				wanted.add(i);
			}
		}
		else if (/^\d+$/.test(trimmed)) {
			wanted.add(parseInt(trimmed, 10));
		}
	}
	return wanted;
}

function applyRange() {
	const wanted = parseRange(el('range').value);
	if (!wanted.size || !state.book) {
		return;
	}
	state.selected.clear();
	for (const file of state.book.files) {
		const number = leadingNumber(file.name);
		if (number !== null && wanted.has(number) && file.wordCount > 0) {
			state.selected.add(file.id);
		}
	}
	renderFiles();
}

/* ---------- Diff ---------- */

// Character-level diff over an LCS table. An answer is a few dozen characters at
// most, so the O(n*m) table costs nothing, and it keeps the page free of a
// vendored diff library: there is no build step or dependency management here.
// Spreading into an array splits by code point, so a surrogate pair stays whole.
function diffChars(from, to) {
	const a = [...from];
	const b = [...to];

	// lcs[i][j] is the length of the longest common subsequence of a.slice(i) and
	// b.slice(j), so walking it forward from 0,0 yields the diff left to right.
	const lcs = Array.from({ length: a.length + 1 }, () => new Array(b.length + 1).fill(0));
	for (let i = a.length - 1; i >= 0; i--) {
		for (let j = b.length - 1; j >= 0; j--) {
			lcs[i][j] = a[i] === b[j]
				? lcs[i + 1][j + 1] + 1
				: Math.max(lcs[i + 1][j], lcs[i][j + 1]);
		}
	}

	const parts = [];
	const add = (type, text) => {
		const last = parts[parts.length - 1];
		if (last && last.type === type) {
			last.text += text;
		}
		else {
			parts.push({ type, text });
		}
	};

	let i = 0;
	let j = 0;
	while (i < a.length && j < b.length) {
		if (a[i] === b[j]) {
			add('same', a[i]);
			i++;
			j++;
		}
		else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
			add('del', a[i]);
			i++;
		}
		else {
			add('ins', b[j]);
			j++;
		}
	}
	while (i < a.length) {
		add('del', a[i]);
		i++;
	}
	while (j < b.length) {
		add('ins', b[j]);
		j++;
	}
	return parts;
}

// One side of the diff: the sign, then the text with this side's runs marked.
// The other side's runs belong to the other row, so they are left out here.
function diffRow(sign, kind, parts) {
	const row = document.createElement('div');
	row.className = `diff-row ${kind}`;

	const signNode = document.createElement('span');
	signNode.className = 'sign';
	signNode.textContent = sign;

	const text = document.createElement('span');
	text.className = 'text';
	for (const part of parts) {
		if (part.type === 'same') {
			text.append(part.text);
		}
		else if (part.type === kind) {
			const mark = document.createElement('mark');
			mark.textContent = part.text;
			text.appendChild(mark);
		}
	}

	row.append(signNode, text);
	return row;
}

// A wrong answer is shown as a diff against the right one, so the characters
// actually missed stand out instead of having to compare two words by eye.
function renderDiff(given, answer) {
	const parts = diffChars(given, answer);
	const diff = document.createElement('div');
	diff.className = 'diff';
	diff.append(diffRow('-', 'del', parts), diffRow('+', 'ins', parts));
	return diff;
}

/* ---------- Game ---------- */

function shuffle(items) {
	for (let i = items.length - 1; i > 0; i--) {
		const j = Math.floor(Math.random() * (i + 1));
		[items[i], items[j]] = [items[j], items[i]];
	}
	return items;
}

function score(size, correct) {
	return size === 0 ? 0 : (correct * 100) / size;
}

function show(section) {
	for (const node of [setup, game, results]) {
		node.hidden = node !== section;
	}
}

async function startGame() {
	const ids = state.book.files.filter((f) => state.selected.has(f.id)).map((f) => f.id);
	state.options = {
		shuffle: el('opt-shuffle').checked,
		hint: el('opt-hint').checked,
		pronounce: el('opt-pronounce').checked
	};

	const query = ids.map((id) => `id=${encodeURIComponent(id)}`).join('&');
	const response = await fetch(`/api/words?${query}`);
	if (!response.ok) {
		alert('Could not load the selected word files.');
		return;
	}
	const contents = await response.json();

	state.rounds = contents.map((content) => ({
		name: content.name,
		entries: state.options.shuffle ? shuffle(content.entries.slice()) : content.entries.slice(),
		index: 0,
		answered: 0,
		wrong: []
	})).filter((round) => round.entries.length > 0);

	if (!state.rounds.length) {
		alert('The selected files contain no usable words.');
		return;
	}

	state.roundIndex = 0;
	state.results = [];
	show(game);
	renderQuestion();
}

function currentRound() {
	return state.rounds[state.roundIndex];
}

function renderQuestion() {
	const round = currentRound();
	const entry = round.entries[round.index];
	const size = round.entries.length;
	const wrong = round.wrong.length;

	el('game-file').textContent = round.name;
	el('game-progress').textContent = `${round.index + 1} / ${size}`;
	el('game-bar').style.width = `${(round.index / size) * 100}%`;
	el('question').textContent = entry.question;
	el('hint').textContent = state.options.hint ? `hint: ${entry.answer.charAt(0)}` : '';
	el('max-score').textContent = score(size, size - wrong).toFixed(2);
	el('cur-score').textContent = score(size, round.index - wrong).toFixed(2);

	el('feedback').hidden = true;
	state.awaitingNext = false;

	const answer = el('answer');
	answer.value = '';
	answer.disabled = false;
	answer.focus();

	if (state.options.pronounce) {
		playWord(entry.answer);
	}
}

async function submitAnswer(event) {
	event.preventDefault();

	if (state.awaitingNext) {
		advance();
		return;
	}

	const round = currentRound();
	const entry = round.entries[round.index];
	const given = el('answer').value.trim();
	if (!given) {
		return;
	}

	// Same comparison as the CLI: exact match after trimming.
	const correct = given === entry.answer;
	round.answered++;
	const feedback = el('feedback');
	if (correct) {
		feedback.className = 'feedback ok';
		feedback.textContent = 'Correct!';
	}
	else {
		// The answer given is kept with the entry so the summary can diff it too.
		// Spreading keeps question and answer in place, which is all a retry needs.
		round.wrong.push({ ...entry, given });
		feedback.className = 'feedback bad';
		const again = document.createElement('span');
		again.className = 'again';
		again.textContent = 'Press Enter to continue.';
		feedback.replaceChildren('Wrong!', renderDiff(given, entry.answer), again);
	}
	feedback.hidden = false;
	state.awaitingNext = true;
	const input = el('answer');
	input.value = '';
	input.focus();

	await playWord(entry.answer);
	playSound(correct ? 'correct' : 'wrong');
}

function advance() {
	const round = currentRound();
	round.index++;
	if (round.index < round.entries.length) {
		renderQuestion();
		return;
	}
	finishRound();
}

function finishRound() {
	const round = currentRound();
	// Score over what was actually answered, so quitting early does not credit
	// the questions never seen. A completed round has answered === entries.length.
	const size = round.answered;
	const correct = size - round.wrong.length;
	const value = score(size, correct);
	const partial = size < round.entries.length;
	state.results.push({ name: round.name, size, correct, score: value, wrong: round.wrong, partial });

	playSound(value === 100 ? 'perfect' : (value >= 80 ? 'pass' : 'fail'));

	state.roundIndex++;
	if (state.roundIndex < state.rounds.length) {
		renderQuestion();
		return;
	}
	renderResults();
}

/* ---------- Results ---------- */

function renderResults() {
	const summary = el('summary');
	summary.innerHTML = '';
	for (const result of state.results) {
		const row = document.createElement('div');
		row.className = 'result-row' + (result.score === 100 ? ' perfect' : (result.score < 80 ? ' failed' : ''));

		const name = document.createElement('span');
		name.textContent = result.partial ? `${result.name} (stopped early)` : result.name;

		const value = document.createElement('span');
		value.className = 'score';
		value.textContent = `${result.score.toFixed(2)} (${result.correct} / ${result.size})`;

		row.append(name, value);
		summary.appendChild(row);
	}

	const allWrong = state.results.flatMap((r) => r.wrong);
	const container = el('wrong-answers');
	container.innerHTML = '';
	if (allWrong.length) {
		const list = document.createElement('div');
		list.className = 'wrong-list';
		list.innerHTML = '<h3>Wrong answers</h3>';
		const ul = document.createElement('ul');
		for (const entry of allWrong) {
			const li = document.createElement('li');
			const prompt = document.createElement('div');
			prompt.className = 'prompt';
			prompt.textContent = entry.question;
			li.append(prompt, renderDiff(entry.given, entry.answer));
			ul.appendChild(li);
		}
		list.appendChild(ul);
		container.appendChild(list);
	}
	el('retry-wrong').hidden = allWrong.length === 0;

	show(results);
}

function retryWrong() {
	const allWrong = state.results.flatMap((r) => r.wrong);
	if (!allWrong.length) {
		return;
	}
	state.rounds = [{
		name: 'Wrong answers',
		entries: state.options.shuffle ? shuffle(allWrong.slice()) : allWrong.slice(),
		index: 0,
		answered: 0,
		wrong: []
	}];
	state.roundIndex = 0;
	state.results = [];
	show(game);
	renderQuestion();
}

/* ---------- Wiring ---------- */

el('book').addEventListener('change', (event) => selectBook(+event.target.value));
el('apply-range').addEventListener('click', applyRange);
el('range').addEventListener('keydown', (event) => {
	if (event.key === 'Enter') {
		event.preventDefault();
		applyRange();
	}
});
el('select-all').addEventListener('click', () => {
	state.book.files.filter((f) => f.wordCount > 0).forEach((f) => state.selected.add(f.id));
	renderFiles();
});
el('clear-all').addEventListener('click', () => {
	state.selected.clear();
	renderFiles();
});
el('start').addEventListener('click', startGame);
el('answer-form').addEventListener('submit', submitAnswer);
// Explicit, so Enter both grades and advances even where implicit submission does not fire.
el('answer').addEventListener('keydown', (event) => {
	if (event.key === 'Enter') {
		submitAnswer(event);
	}
});
el('quit').addEventListener('click', () => {
	if (state.results.length || currentRound().answered > 0) {
		finishRound();
	}
	else {
		show(setup);
	}
});
el('again').addEventListener('click', () => show(setup));
el('retry-wrong').addEventListener('click', retryWrong);

if ('speechSynthesis' in window) {
	pickVoice();
	window.speechSynthesis.addEventListener('voiceschanged', () => {
		voice = null;
		pickVoice();
	});
}

loadBooks();
