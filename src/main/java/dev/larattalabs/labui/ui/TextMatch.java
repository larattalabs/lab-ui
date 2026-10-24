package dev.larattalabs.labui.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Search matching for filters and pickers (pure, unit-tested in {@code TextMatchTest}). Case-insensitive; {@code _ - : / .}
 * count as spaces, so "spruce pl" finds {@code minecraft:spruce_planks}. Every word of the query must match the label or
 * one of the extra fields (id, group, tags); a word scores more for a whole word than for a word prefix than for a plain
 * substring, and more in the label than in the extras. A label that equals or starts with the whole query ranks first.
 */
public final class TextMatch {
	private TextMatch() {
	}

	/** Lower case, separators as spaces, single spaces, trimmed. */
	public static String norm(String s) {
		if (s == null) {
			return "";
		}
		return s.toLowerCase(Locale.ROOT).replaceAll("[_\\-:/.]+", " ").replaceAll("\\s+", " ").trim();
	}

	/** Score of {@code label} plus {@code extra} for {@code query}: -1 = no match, 0 = empty query, higher = better. */
	public static int score(String query, String label, Collection<String> extra) {
		String q = norm(query);
		if (q.isEmpty()) {
			return 0;
		}
		String l = norm(label);
		List<String> ex = new ArrayList<>(extra.size());
		for (String e : extra) {
			ex.add(norm(e));
		}
		int total = 0;
		for (String tok : q.split(" ")) {
			int best = word(tok, l, 100, 60, 25);
			for (String e : ex) {
				best = Math.max(best, word(tok, e, 40, 30, 10));
			}
			if (best < 0) {
				return -1;
			}
			total += best;
		}
		if (l.equals(q)) {
			total += 1000;
		} else if (l.startsWith(q)) {
			total += 500;
		}
		return total;
	}

	public static boolean matches(String query, String label, Collection<String> extra) {
		return score(query, label, extra) >= 0;
	}

	private static int word(String tok, String field, int whole, int prefix, int sub) {
		if (field.isEmpty()) {
			return -1;
		}
		int best = -1;
		for (String w : field.split(" ")) {
			if (w.equals(tok)) {
				return whole;
			}
			if (w.startsWith(tok)) {
				best = prefix;
			}
		}
		if (best < 0 && field.contains(tok)) {
			best = sub;
		}
		return best;
	}

	/** The items that match, best first (ties keep their order); an empty query keeps every item in order. */
	public static <T> List<T> rank(List<T> items, String query, Function<T, String> label, Function<T, Collection<String>> extra) {
		if (norm(query).isEmpty()) {
			return new ArrayList<>(items);
		}
		record Scored<T>(T item, int score, int order) {
		}
		List<Scored<T>> s = new ArrayList<>();
		for (int i = 0; i < items.size(); i++) {
			T t = items.get(i);
			int sc = score(query, label.apply(t), extra.apply(t));
			if (sc >= 0) {
				s.add(new Scored<>(t, sc, i));
			}
		}
		s.sort(Comparator.<Scored<T>>comparingInt(x -> -x.score()).thenComparingInt(Scored::order));
		List<T> out = new ArrayList<>(s.size());
		for (Scored<T> x : s) {
			out.add(x.item());
		}
		return out;
	}
}
