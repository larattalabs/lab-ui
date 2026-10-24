package dev.larattalabs.labui.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.larattalabs.labui.ui.ChipFlow.Placed;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChipFlowTest {
	@Test
	void everythingFitsOnOneRow() {
		ChipFlow.Layout l = ChipFlow.layout(new int[] {20, 30, 10}, 100, 4, 1, n -> 20);
		assertNull(l.more());
		assertEquals(0, l.hidden());
		assertEquals(List.of(new Placed(0, 0, 0, 20), new Placed(1, 24, 0, 30), new Placed(2, 58, 0, 10)), l.placed());
		assertEquals(1, l.rows());
	}

	@Test
	void wrapsIntoMoreRows() {
		ChipFlow.Layout l = ChipFlow.layout(new int[] {40, 40, 40}, 90, 4, 2, n -> 20);
		assertNull(l.more());
		assertEquals(new Placed(2, 0, 1, 40), l.placed().get(2));
		assertEquals(2, l.rows());
	}

	@Test
	void overflowFoldsIntoPlusNAndDropsChipsUntilItFits() {
		// row: 30 + 4 + 30 + 4 + 30 = 98 of 100; "+N" needs 20, so the third chip goes too
		ChipFlow.Layout l = ChipFlow.layout(new int[] {30, 30, 30, 30, 30}, 100, 4, 1, n -> 20);
		assertEquals(2, l.placed().size());
		assertEquals(3, l.hidden());
		assertEquals(new Placed(-1, 68, 0, 20), l.more());
	}

	@Test
	void plusNWidthDependsOnN() {
		int[] w = new int[12];
		java.util.Arrays.fill(w, 10);
		// widths 10, gap 0, avail 100: 10 fit, 2 hidden; "+2" is 10 wide -> drop one (9 kept, +3 at 90)
		ChipFlow.Layout l = ChipFlow.layout(w, 100, 0, 1, n -> n < 3 ? 10 : 10);
		assertEquals(9, l.placed().size());
		assertEquals(3, l.hidden());
		assertEquals(90, l.more().x());
	}

	@Test
	void wideChipIsClampedAndMoreGoesOnTheLastRow() {
		ChipFlow.Layout l = ChipFlow.layout(new int[] {500, 30, 30}, 100, 4, 2, n -> 20);
		assertEquals(new Placed(0, 0, 0, 100), l.placed().get(0));
		assertEquals(new Placed(1, 0, 1, 30), l.placed().get(1));
		assertEquals(new Placed(2, 34, 1, 30), l.placed().get(2));
		assertNull(l.more());
		ChipFlow.Layout two = ChipFlow.layout(new int[] {500, 500, 30}, 100, 4, 2, n -> 20);
		assertEquals(1, two.placed().size(), "the second wide chip makes way for +2");
		assertEquals(new Placed(-1, 0, 1, 20), two.more());
		assertEquals(2, two.hidden());
	}

	@Test
	void emptyInput() {
		ChipFlow.Layout l = ChipFlow.layout(new int[0], 100, 4, 1, n -> 20);
		assertEquals(0, l.rows());
		assertNull(l.more());
	}
}
