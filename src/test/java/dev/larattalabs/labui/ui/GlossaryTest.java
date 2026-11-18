package dev.larattalabs.labui.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** (0.6.0) The shared glossary's sections and reserved slots, and the wording lint. */
class GlossaryTest {
	@AfterEach
	void clear() {
		Glossary.clearForTest();
	}

	@Test
	void stewardsSlotIsReservedButHiddenUntilFilled() {
		assertEquals(10, Glossary.reservedWords(Glossary.STEWARD).size());
		assertTrue(Glossary.sections().isEmpty());
		Glossary.register(new Glossary.Section("architect_mc", "Architect", List.of(new Glossary.Term("lot", "A building's plot in a settlement."))));
		Glossary.register(new Glossary.Section(Glossary.STEWARD, "Steward", List.of(new Glossary.Term("lot", "A settlement's building plot."))));
		// the reserved slot keeps its place (first), though Architect registered first
		assertEquals(List.of(Glossary.STEWARD, "architect_mc"), Glossary.sections().stream().map(Glossary.Section::owner).toList());
		assertEquals("architect_mc", Glossary.sections("architect_mc").get(0).owner());
		assertEquals(2, Glossary.lookup("LOT").size());
		assertEquals(9, Glossary.missing(Glossary.section(Glossary.STEWARD)).size());
		assertTrue(Glossary.missing(Glossary.section("architect_mc")).isEmpty());
	}

	@Test
	void longMeaningsAreRefused() {
		assertThrows(IllegalArgumentException.class, () -> Glossary.register(new Glossary.Section("x", "X", List.of(new Glossary.Term("w", "a".repeat(141))))));
		assertNull(Glossary.section("x"));
	}

	@Test
	void wordingLintFindsPlayerTextOnly() {
		assertEquals(1, WordingLint.breaks("Pick a Blueprint to place").size());
		assertEquals(1, WordingLint.breaks("two blueprints").size());
		assertTrue(WordingLint.breaks("blueprintId").isEmpty());
		assertTrue(WordingLint.breaks("cabin.blueprint.json is missing").isEmpty());
		assertEquals(1, WordingLint.lang(Map.of("k", "The blueprint")).size());
		List<String> src = List.of(
			"say(\"Placed the blueprint here\");", // player text
			"o.addProperty(\"blueprint\", id);", // a key
			"// the blueprint of a site", // a comment
			"DevBridge.register(\"dev.x\", 1, \"{blueprint} - a dev hook\");"); // skipped
		List<WordingLint.Hit> hits = WordingLint.source("F.java", src, Pattern.compile("DevBridge\\.register"));
		assertEquals(1, hits.size(), hits.toString());
		assertEquals("F.java:1", hits.get(0).where());
	}
}
