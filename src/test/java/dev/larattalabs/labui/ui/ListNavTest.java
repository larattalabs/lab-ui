package dev.larattalabs.labui.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.larattalabs.labui.ui.ListNav.Key;
import org.junit.jupiter.api.Test;

class ListNavTest {
	@Test
	void firstKeySelectsFirstVisibleAndEndSelectsLast() {
		ListNav n = new ListNav().update(30, 1, 5);
		n.scrollBy(4);
		assertTrue(n.key(Key.DOWN));
		assertEquals(4, n.selected());
		n.clearSelection();
		assertTrue(n.key(Key.END));
		assertEquals(29, n.selected());
		assertEquals(25, n.offset(), "End scrolls the last row into view");
	}

	@Test
	void arrowsMoveAndScrollAndStopAtEdges() {
		ListNav n = new ListNav().update(10, 1, 3);
		n.select(0);
		assertFalse(n.key(Key.UP), "Up on the first row hands focus back");
		for (int i = 0; i < 9; i++) {
			assertTrue(n.key(Key.DOWN));
		}
		assertEquals(9, n.selected());
		assertEquals(7, n.offset());
		assertFalse(n.key(Key.DOWN));
		assertFalse(n.key(Key.LEFT), "no Left/Right in a one-column list");
		assertTrue(n.key(Key.HOME));
		assertEquals(0, n.offset());
	}

	@Test
	void pagesKeepTheColumnAndClamp() {
		ListNav n = new ListNav().update(23, 4, 2); // 6 rows, last row has 3
		n.select(1);
		assertTrue(n.key(Key.PAGE_DOWN));
		assertEquals(9, n.selected());
		assertTrue(n.key(Key.PAGE_DOWN));
		assertEquals(17, n.selected());
		assertTrue(n.key(Key.PAGE_DOWN));
		assertEquals(21, n.selected(), "last row, same column");
		n.select(22);
		assertTrue(n.key(Key.UP));
		assertEquals(18, n.selected());
		assertTrue(n.key(Key.PAGE_UP));
		assertTrue(n.key(Key.PAGE_UP));
		assertTrue(n.key(Key.PAGE_UP));
		assertEquals(2, n.selected(), "first row, same column");
	}

	@Test
	void gridDownFromAFullRowIntoAShortLastRowGoesToTheLastItem() {
		ListNav n = new ListNav().update(10, 4, 3);
		n.select(7);
		assertTrue(n.key(Key.DOWN));
		assertEquals(9, n.selected());
		assertTrue(n.key(Key.LEFT));
		assertEquals(8, n.selected());
	}

	@Test
	void wheelScrollsWithoutMovingSelectionAndUpdateClamps() {
		ListNav n = new ListNav().update(50, 1, 10);
		n.select(3);
		n.scrollBy(100);
		assertEquals(40, n.offset());
		assertEquals(3, n.selected());
		n.update(5, 1, 10);
		assertEquals(0, n.offset());
		assertEquals(3, n.selected());
		n.update(2, 1, 10);
		assertEquals(1, n.selected());
		n.update(0, 1, 10);
		assertEquals(-1, n.selected());
		assertFalse(n.key(Key.DOWN));
	}

	@Test
	void indexAtMapsVisibleCells() {
		ListNav n = new ListNav().update(10, 3, 2);
		n.scrollTo(2);
		assertEquals(6, n.indexAt(0, 0));
		assertEquals(9, n.indexAt(1, 0));
		assertEquals(-1, n.indexAt(1, 1));
		assertEquals(-1, n.indexAt(2, 0));
		assertEquals(6, n.firstVisible());
		assertEquals(9, n.lastVisible());
	}
}
