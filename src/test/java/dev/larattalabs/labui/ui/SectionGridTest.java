package dev.larattalabs.labui.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.larattalabs.labui.ui.ListNav.Key;
import org.junit.jupiter.api.Test;

class SectionGridTest {
	// sections of 5 and 2, 3 columns, with headings:
	// row0 H(a) | row1 0 1 2 | row2 3 4 | row3 H(b) | row4 5 6
	private final SectionGrid g = new SectionGrid(new int[] {5, 0, 2}, 3, true);

	@Test
	void rowsAndCells() {
		assertEquals(7, g.count());
		assertEquals(5, g.rows());
		assertTrue(g.heading(0));
		assertTrue(g.heading(3));
		assertEquals(2, g.section(4), "empty sections are skipped but keep their index");
		assertEquals(2, g.rowOf(4));
		assertEquals(1, g.colOf(4));
		assertEquals(6, g.at(4, 1));
		assertEquals(-1, g.at(4, 2));
		assertEquals(-1, g.at(3, 0));
	}

	@Test
	void verticalMovesSkipHeadingsAndKeepTheColumn() {
		assertEquals(4, g.move(2, Key.DOWN, 1), "column 2 into a short row: its last item");
		assertEquals(6, g.move(4, Key.DOWN, 1), "over the heading");
		assertEquals(3, g.move(5, Key.UP, 1));
		assertEquals(-1, g.move(1, Key.UP, 1), "Up on the first row of items: edge");
		assertEquals(-1, g.move(6, Key.DOWN, 1));
	}

	@Test
	void horizontalHomeEndAndPages() {
		assertEquals(5, g.move(4, Key.RIGHT, 1), "Right crosses into the next section");
		assertEquals(-1, g.move(0, Key.LEFT, 1));
		assertEquals(-1, g.move(6, Key.RIGHT, 1));
		assertEquals(0, g.move(4, Key.HOME, 1));
		assertEquals(6, g.move(0, Key.END, 1));
		assertEquals(5, g.move(0, Key.PAGE_DOWN, 2));
		assertEquals(6, g.move(6, Key.PAGE_DOWN, 2), "already on the last row: the last item");
		assertEquals(0, g.move(1, Key.PAGE_UP, 5), "already on the first row: the first item");
	}

	@Test
	void sparseGridsGetFewerWiderColumns() {
		assertEquals(6, SectionGrid.columns(600, 4, 96, 12), "a full grid: as many as fit");
		assertEquals(3, SectionGrid.columns(600, 4, 96, 3), "sections of at most 3: three wide columns");
		assertEquals(1, SectionGrid.columns(50, 4, 96, 12), "never fewer than one");
		assertEquals(1, SectionGrid.columns(600, 4, 96, 0));
		assertEquals(197, SectionGrid.cellWidth(600, 4, 3, 240));
		assertEquals(160, SectionGrid.cellWidth(600, 4, 2, 160), "capped");
	}

	@Test
	void nothingSelected() {
		assertEquals(0, g.move(-1, Key.DOWN, 1));
		assertEquals(6, g.move(-1, Key.END, 1));
		assertEquals(-1, new SectionGrid(new int[0], 3, true).move(-1, Key.DOWN, 1));
	}
}
