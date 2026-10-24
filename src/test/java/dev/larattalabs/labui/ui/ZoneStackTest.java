package dev.larattalabs.labui.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.larattalabs.labui.ui.ZoneStack.Item;
import dev.larattalabs.labui.ui.ZoneStack.Placed;
import dev.larattalabs.labui.ui.ZoneStack.Result;
import java.util.List;
import org.junit.jupiter.api.Test;

class ZoneStackTest {
	static Item fixed(int prio, int order, int h) {
		return new Item(prio, order, h, h, false);
	}

	@Test
	void highPriorityNearestTheAnchorThenRegistrationOrder() {
		Result r = ZoneStack.stack(List.of(fixed(10, 0, 12), fixed(60, 1, 20), fixed(10, 2, 8)), 6, true, 200, 4);
		assertEquals(List.of(new Placed(1, 6, 20, false), new Placed(0, 30, 12, false), new Placed(2, 46, 8, false)), r.placed());
		assertEquals(48, r.used());
		assertFalse(r.overflow());
	}

	@Test
	void upwardStacksFromTheBottomEdge() {
		Result r = ZoneStack.stack(List.of(fixed(100, 0, 40), fixed(80, 1, 20)), 200, false, 150, 4);
		assertEquals(List.of(new Placed(0, 160, 40, false), new Placed(1, 136, 20, false)), r.placed());
	}

	@Test
	void theLowestPriorityShrinksFirstAndOnlyAsMuchAsNeeded() {
		Item high = new Item(100, 0, 50, 20, false);
		Item low = new Item(10, 1, 30, 10, false);
		Result r = ZoneStack.stack(List.of(high, low), 0, true, 70, 4);
		// 84 wanted, 70 room: the low one gives 14 (30 -> 16), the high one keeps 50
		assertEquals(List.of(new Placed(0, 0, 50, false), new Placed(1, 54, 16, true)), r.placed());
		assertFalse(r.overflow());
		// less room: the low one is at its minimum, then the high one shrinks
		r = ZoneStack.stack(List.of(high, low), 0, true, 44, 4);
		assertEquals(List.of(new Placed(0, 0, 30, true), new Placed(1, 34, 10, true)), r.placed());
	}

	@Test
	void shrinkBeforeHideAndHiddenRoomGoesBackToTheShrunk() {
		Item panel = new Item(100, 0, 60, 30, false);
		Item toasts = new Item(0, 1, 50, 0, true);
		Result r = ZoneStack.stack(List.of(panel, toasts), 0, true, 40, 4);
		// the toasts shrink to 0 first (64 left), then the panel gives 24 (60 -> 36): 36 + 4 + 0 fits, nothing hidden
		assertTrue(r.hidden().isEmpty());
		assertEquals(List.of(new Placed(0, 0, 36, true), new Placed(1, 40, 0, true)), r.placed());
		assertEquals(40, r.used());
		// a fixed high element and an optional one that can't shrink: the optional one goes, the fixed one stays whole
		Item status = new Item(60, 0, 12, 12, false);
		Item strip = new Item(40, 1, 30, 30, true);
		r = ZoneStack.stack(List.of(status, strip), 0, true, 20, 4);
		assertEquals(List.of(1), r.hidden());
		assertEquals(List.of(new Placed(0, 0, 12, false)), r.placed());
		assertFalse(r.overflow());
	}

	@Test
	void whatHidingFreesRegrowsTheShrunkHighestFirst() {
		Item a = new Item(90, 0, 40, 10, false);
		Item b = new Item(50, 1, 40, 40, true);
		Result r = ZoneStack.stack(List.of(a, b), 0, true, 30, 4);
		// a shrinks to 10, b (fixed height) still too much -> b hidden, a regrows to 30
		assertEquals(List.of(1), r.hidden());
		assertEquals(List.of(new Placed(0, 0, 30, true)), r.placed());
	}

	@Test
	void aFixedElementIsNeverHiddenSilentlyItOverflows() {
		Result r = ZoneStack.stack(List.of(fixed(50, 0, 30), fixed(40, 1, 30)), 0, true, 40, 4);
		assertEquals(2, r.placed().size());
		assertTrue(r.overflow());
		assertEquals(64, r.used());
	}

	@Test
	void emptyAndZeroSpaceZones() {
		Result r = ZoneStack.stack(List.of(), 10, true, 100, 4);
		assertTrue(r.placed().isEmpty());
		assertEquals(0, r.used());
		r = ZoneStack.stack(List.of(new Item(0, 0, 50, 0, true)), 10, true, 0, 4);
		assertTrue(r.placed().isEmpty() || r.placed().get(0).h() == 0);
		assertFalse(r.overflow());
		r = ZoneStack.stack(List.of(new Item(0, 0, 50, 0, true)), 10, true, -5, 4);
		assertFalse(r.overflow());
	}

	@Test
	void itemsAreValidated() {
		assertThrows(IllegalArgumentException.class, () -> new Item(0, 0, 10, 11, false));
		assertThrows(IllegalArgumentException.class, () -> new Item(0, 0, -1, 0, false));
	}
}
