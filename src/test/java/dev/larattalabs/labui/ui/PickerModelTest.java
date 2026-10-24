package dev.larattalabs.labui.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.larattalabs.labui.ui.ListNav.Key;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PickerModelTest {
	record M(String id, String name, String group, List<String> tags) {
	}

	private static final List<M> MATS = List.of(new M("oak", "Oak", "Wood", List.of()), new M("spruce", "Spruce", "Wood", List.of("dark")),
		new M("dark_oak", "Dark oak", "Wood", List.of("dark")), new M("cobblestone", "Cobblestone", "Stone", List.of()),
		new M("deepslate_tile", "Deepslate tile", "Stone", List.of("dark")), new M("cut_copper", "Cut copper", "Copper", List.of()));

	private static PickerModel<M> model() {
		return new PickerModel<>(MATS, m -> new PickerModel.Key(m.id(), m.name(), m.group(), m.tags()), List.of("Stone"));
	}

	private static List<String> ids(List<M> ms) {
		return ms.stream().map(M::id).toList();
	}

	@Test
	void groupsFollowTheGivenOrderThenAppearance() {
		assertEquals(List.of("Stone", "Wood", "Copper"), model().groups());
	}

	@Test
	void sectionsAndCounts() {
		PickerModel<M> p = model();
		assertEquals(List.of("cobblestone", "deepslate_tile", "oak", "spruce", "dark_oak", "cut_copper"), ids(p.visible()));
		p.setQuery("dark");
		assertEquals(List.of("deepslate_tile", "dark_oak", "spruce"), ids(p.visible()), "label match outranks a tag match");
		assertEquals(3, p.count(null));
		assertEquals(2, p.count("Wood"));
		assertEquals(0, p.count("Copper"));
		p.setGroup("Wood");
		assertEquals(List.of("dark_oak", "spruce"), ids(p.visible()));
	}

	@Test
	void favouritesOnly() {
		PickerModel<M> p = model();
		p.setFavourites(Set.of("oak", "cut_copper"));
		p.setFavouritesOnly(true);
		assertEquals(List.of("oak", "cut_copper"), ids(p.visible()));
		assertTrue(p.isFavourite("oak"));
	}

	@Test
	void selectionIsKeptByIdAcrossFilters() {
		PickerModel<M> p = model();
		p.select("spruce");
		assertEquals(3, p.selectedIndex());
		p.setQuery("spr");
		assertEquals(0, p.selectedIndex());
		p.setQuery("stone");
		assertEquals(-1, p.selectedIndex());
		assertNull(p.selected());
		p.setQuery("");
		assertEquals("spruce", p.selected().id());
	}

	@Test
	void keyboardMovesThroughTheGroupedGrid() {
		PickerModel<M> p = model();
		// 2 columns, headings: H | cobble deepslate | H | oak spruce | dark_oak | H | cut_copper
		assertTrue(p.move(Key.DOWN, 2, true, 3));
		assertEquals("cobblestone", p.selectedId());
		assertFalse(p.move(Key.UP, 2, true, 3), "Up from the first row: back to search");
		assertTrue(p.move(Key.RIGHT, 2, true, 3));
		assertTrue(p.move(Key.DOWN, 2, true, 3));
		assertEquals("spruce", p.selectedId());
		assertTrue(p.move(Key.DOWN, 2, true, 3));
		assertEquals("dark_oak", p.selectedId());
		assertTrue(p.move(Key.END, 2, true, 3));
		assertEquals("cut_copper", p.selectedId());
	}

	@Test
	void resolveAndCompact() {
		PickerModel<M> p = model();
		assertEquals(List.of("spruce", "oak"), ids(p.resolve(List.of("spruce", "gone", "oak"))));
		java.util.function.Predicate<String> known = id -> p.byId(id) != null;
		assertEquals(List.of("oak", "spruce", "dark_oak"), PickerModel.compact("oak", List.of("oak", "spruce"), List.of("spruce", "dark_oak", "cobblestone"), 3,
			known));
		assertEquals(List.of("spruce"), PickerModel.compact("gone", List.of("spruce"), List.of(), 3, known));
		assertEquals(List.of(), PickerModel.compact(null, List.of(), List.of(), 3, known));
	}
}
