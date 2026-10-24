package dev.larattalabs.labui.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PickerMemoryTest {
	@Test
	void recentsAreNewestFirstWithoutRepeatsAndCapped() {
		PickerMemory m = new PickerMemory();
		m.used("a.roof", "oak");
		m.used("a.roof", "spruce");
		m.used("a.roof", "oak");
		assertEquals(List.of("oak", "spruce"), m.recents("a.roof"));
		for (int i = 0; i < 20; i++) {
			m.used("a.roof", "b" + i);
		}
		assertEquals(PickerMemory.MAX_RECENTS, m.recents("a.roof").size());
		assertEquals("b19", m.recents("a.roof").get(0));
		assertEquals(List.of(), m.recents("other"), "per picker id");
	}

	@Test
	void favouritesToggle() {
		PickerMemory m = new PickerMemory();
		assertTrue(m.toggleFavourite("p", "x"));
		assertTrue(m.favourite("p", "x"));
		assertFalse(m.toggleFavourite("p", "x"));
		assertFalse(m.favourite("p", "x"));
	}

	@Test
	void jsonRoundTrip() {
		PickerMemory m = new PickerMemory();
		m.used("a.roof", "spruce");
		m.used("a.roof", "deepslate_tile");
		m.toggleFavourite("a.roof", "dark_oak");
		m.toggleFavourite("s.style", "alpine");
		PickerMemory back = PickerMemory.fromJson(m.toJson());
		assertEquals(List.of("deepslate_tile", "spruce"), back.recents("a.roof"));
		assertEquals(Set.of("dark_oak"), back.favourites("a.roof"));
		assertEquals(Set.of("alpine"), back.favourites("s.style"));
	}

	@Test
	void sectionStateRoundTrips() {
		PickerMemory m = new PickerMemory();
		assertEquals(null, m.section("architect.design.features"));
		m.section("architect.design.features", true);
		m.section("architect.design.rules", false);
		PickerMemory back = PickerMemory.fromJson(m.toJson());
		assertEquals(Boolean.TRUE, back.section("architect.design.features"));
		assertEquals(Boolean.FALSE, back.section("architect.design.rules"));
		assertEquals(null, PickerMemory.fromJson("{\"sections\": {\"x\": \"yes\"}}").section("x"), "only booleans");
	}

	@Test
	void garbageReadsAsEmpty() {
		assertEquals(List.of(), PickerMemory.fromJson("not json").recents("x"));
		assertEquals(List.of(), PickerMemory.fromJson("{\"pickers\": {\"x\": {\"recent\": [1, \"ok\"]}}}").recents("y"));
		assertEquals(List.of("ok"), PickerMemory.fromJson("{\"pickers\": {\"x\": {\"recent\": [1, \"ok\"]}}}").recents("x"));
		assertEquals(List.of(), PickerMemory.fromJson("[]").recents("x"));
	}
}
