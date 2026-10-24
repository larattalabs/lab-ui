package dev.larattalabs.labui.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TextMatchTest {
	@Test
	void normTreatsSeparatorsAsSpaces() {
		assertEquals("minecraft spruce planks", TextMatch.norm(" Minecraft:Spruce_Planks "));
		assertEquals("", TextMatch.norm(null));
	}

	@Test
	void everyWordMustMatchSomewhere() {
		assertTrue(TextMatch.matches("spruce pl", "Spruce planks", List.of()));
		assertTrue(TextMatch.matches("wood spruce", "Spruce", List.of("Wood stairs")), "a word may match the group");
		assertTrue(TextMatch.matches("cold", "Alpine timber", List.of("cold", "timber")), "or a tag");
		assertFalse(TextMatch.matches("spruce stone", "Spruce", List.of("Wood stairs")));
		assertEquals(0, TextMatch.score("  ", "x", List.of()), "empty query matches everything");
	}

	@Test
	void ranksExactThenPrefixThenWordThenSubstring() {
		record E(String label, List<String> extra) {
		}
		List<E> items = List.of(new E("Dark oak", List.of()), new E("Cloaked", List.of()), new E("Oak", List.of()), new E("Oak log", List.of()),
			new E("Stone", List.of("oak")), new E("Birch", List.of()));
		List<String> got = TextMatch.rank(items, "oak", E::label, E::extra).stream().map(E::label).toList();
		assertEquals(List.of("Oak", "Oak log", "Dark oak", "Stone", "Cloaked"), got);
	}

	@Test
	void rankKeepsOrderForEmptyQueryAndTies() {
		List<String> items = List.of("b", "a", "c");
		assertEquals(items, TextMatch.rank(items, "", s -> s, s -> List.of()));
		assertEquals(List.of("ab", "ac"), TextMatch.rank(List.of("x", "ab", "ac"), "a", s -> s, s -> List.of()));
	}
}
