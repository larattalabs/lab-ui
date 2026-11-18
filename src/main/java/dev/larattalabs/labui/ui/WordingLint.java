package dev.larattalabs.labui.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * (0.6.0, Architect PR G1) The wording rule's check (UI-2b: "design", never "blueprint"), pure so any mod's unit test can run it
 * over its own player-facing strings: its lang file's values and the string literals of its client and command code. A mod opts
 * in by adding a test over its {@link Root}s; Architect's {@code GlossaryWordingTest} is the first.
 */
public final class WordingLint {
	/** A forbidden word and the word to use. */
	public record Rule(String word, String use) {
	}

	/** One hit: where (a file and line, or a lang key), the string, the rule. */
	public record Hit(String where, String text, Rule rule) {
		@Override
		public String toString() {
			return where + ": \"" + text + "\" (" + rule.word() + " -> " + rule.use() + ")";
		}
	}

	public static final List<Rule> RULES = List.of(new Rule("blueprint", "design"));
	private static final Pattern LITERAL = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"");

	private WordingLint() {
	}

	/** True when {@code s} reads as player text: at least two words (identifiers and JSON keys are one). */
	public static boolean sentenceLike(String s) {
		return s.trim().contains(" ");
	}

	/** The rules a piece of player text breaks (whole words, any case, plurals included). */
	public static List<Rule> breaks(String text) {
		List<Rule> out = new ArrayList<>();
		for (Rule r : RULES) {
			if (Pattern.compile("(?i)\\b" + Pattern.quote(r.word()) + "s?\\b(?![._(\\-])").matcher(text).find()) {
				out.add(r);
			}
		}
		return out;
	}

	/** Lang values (key -> text): every value is player text. */
	public static List<Hit> lang(Map<String, String> entries) {
		List<Hit> out = new ArrayList<>();
		entries.forEach((k, v) -> breaks(v).forEach(r -> out.add(new Hit(k, v, r))));
		return out;
	}

	/**
	 * Source lines: the sentence-like string literals of each line, skipping comments and lines {@code skip} names (dev hooks'
	 * descriptions, log calls). {@code file} labels the hits.
	 */
	public static List<Hit> source(String file, List<String> lines, Pattern skip) {
		List<Hit> out = new ArrayList<>();
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			String t = line.trim();
			if (t.startsWith("//") || t.startsWith("*") || t.startsWith("/*") || skip != null && skip.matcher(line).find()) {
				continue;
			}
			Matcher m = LITERAL.matcher(line);
			while (m.find()) {
				String s = m.group(1);
				if (!sentenceLike(s)) {
					continue;
				}
				for (Rule r : breaks(s)) {
					out.add(new Hit(file + ":" + (i + 1), s, r));
				}
			}
		}
		return out;
	}
}
