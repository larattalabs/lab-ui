package dev.larattalabs.labui.ui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * (0.6.0, Architect PR G1) The shared glossary: one section per mod, shown as the "Terms" page of the "?" overlay
 * ({@code KeysOverlay}) and on a mod's help page ({@code GlossaryView}). Pure; a mod registers its section at client init.
 * <p>
 * A section slot can be <em>reserved</em> with the words it must define, so the page's structure is fixed before the owner fills it:
 * lab-ui reserves Steward's ({@link #STEWARD}, S-PR-3). A reserved section that is not filled yet is not shown.
 */
public final class Glossary {
	/** One term: the word as the player reads it, and a one-line meaning (at most {@link #MAX_MEANING} characters). */
	public record Term(String word, String meaning) {
		public Term {
			if (word == null || word.isBlank() || meaning == null || meaning.isBlank()) {
				throw new IllegalArgumentException("a term needs a word and a meaning");
			}
		}
	}

	/** One mod's terms. {@code owner} is the mod id ({@code architect_mc}); {@code title} its name on the page. */
	public record Section(String owner, String title, List<Term> terms) {
		public Section {
			terms = List.copyOf(terms);
		}

		public Term find(String word) {
			for (Term t : terms) {
				if (t.word().equalsIgnoreCase(word)) {
					return t;
				}
			}
			return null;
		}
	}

	public static final int MAX_MEANING = 140;
	/** Steward's slot (S-PR-3): the words its section is to define. */
	public static final String STEWARD = "steward_mc";
	public static final List<String> STEWARD_WORDS = List.of("settlement", "steward", "Founding Stone", "lot", "district", "quarter", "module",
		"stockpile", "bill limit", "proposal");

	private static final Map<String, Section> SECTIONS = new LinkedHashMap<>();
	private static final Map<String, Reservation> RESERVED = new LinkedHashMap<>();

	private record Reservation(String title, List<String> words) {
	}

	static {
		reserve(STEWARD, "Steward", STEWARD_WORDS);
	}

	private Glossary() {
	}

	/** Reserves {@code owner}'s slot (its place on the page and the words it must define). */
	public static synchronized void reserve(String owner, String title, List<String> words) {
		RESERVED.put(owner, new Reservation(title, List.copyOf(words)));
		SECTIONS.putIfAbsent(owner, null);
	}

	/** Registers (or replaces) a mod's section; a reserved slot keeps its place. */
	public static synchronized void register(Section s) {
		for (Term t : s.terms()) {
			if (t.meaning().length() > MAX_MEANING) {
				throw new IllegalArgumentException(s.owner() + ": the meaning of '" + t.word() + "' is longer than " + MAX_MEANING);
			}
		}
		SECTIONS.put(s.owner(), s);
	}

	/** The filled sections, in page order (reserved slots first come first; unfilled ones left out). */
	public static synchronized List<Section> sections() {
		List<Section> out = new ArrayList<>();
		for (Section s : SECTIONS.values()) {
			if (s != null) {
				out.add(s);
			}
		}
		return out;
	}

	/** The filled sections with {@code first}'s (the calling mod's) at the top, the rest in page order. */
	public static synchronized List<Section> sections(String first) {
		List<Section> all = sections();
		List<Section> out = new ArrayList<>();
		all.stream().filter(s -> s.owner().equals(first)).forEach(out::add);
		all.stream().filter(s -> !s.owner().equals(first)).forEach(out::add);
		return out;
	}

	public static synchronized Section section(String owner) {
		return SECTIONS.get(owner);
	}

	/** The reserved words a section does not define yet (empty when it covers them, or nothing is reserved for it). */
	public static synchronized List<String> missing(Section s) {
		Reservation r = RESERVED.get(s.owner());
		if (r == null) {
			return List.of();
		}
		Set<String> have = s.terms().stream().map(t -> t.word().toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
		return r.words().stream().filter(w -> !have.contains(w.toLowerCase(Locale.ROOT))).toList();
	}

	/** The reserved words of {@code owner}'s slot (empty when none). */
	public static synchronized List<String> reservedWords(String owner) {
		Reservation r = RESERVED.get(owner);
		return r == null ? List.of() : r.words();
	}

	/** Every filled term across sections whose word is {@code word} (a shared word such as "lot" or "stockpile" can be in two). */
	public static synchronized List<Term> lookup(String word) {
		List<Term> out = new ArrayList<>();
		for (Section s : sections()) {
			Term t = s.find(word);
			if (t != null) {
				out.add(t);
			}
		}
		return out;
	}

	/** Test hook: drops registered sections (the reservations stay). */
	public static synchronized void clearForTest() {
		SECTIONS.replaceAll((k, v) -> null);
		SECTIONS.keySet().retainAll(RESERVED.keySet());
	}
}
