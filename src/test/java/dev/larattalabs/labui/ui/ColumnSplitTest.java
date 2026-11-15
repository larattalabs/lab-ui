package dev.larattalabs.labui.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class ColumnSplitTest {
	@Test
	void bothFitWhole() {
		assertArrayEquals(new int[] {60, 132}, ColumnSplit.split(60, 40, 200, 8));
	}

	@Test
	void noSecondGivesTheTitleTheRow() {
		assertArrayEquals(new int[] {200, 0}, ColumnSplit.split(300, 0, 200, 8));
	}

	@Test
	void aLongTitleNoLongerEatsTheStep() {
		// before 0.5.0 a 300 px title left the step nothing; now the title keeps 55% and the step the rest
		assertArrayEquals(new int[] {106, 86}, ColumnSplit.split(300, 120, 200, 8));
	}

	@Test
	void aShortStepStaysWholeAndTheTitleIsCut() {
		assertArrayEquals(new int[] {162, 30}, ColumnSplit.split(300, 30, 200, 8));
	}

	@Test
	void aShortTitleStaysWhole() {
		assertArrayEquals(new int[] {50, 142}, ColumnSplit.split(50, 300, 200, 8));
	}

	@Test
	void tooNarrowForTwoColumnsDropsTheStep() {
		assertArrayEquals(new int[] {40, 0}, ColumnSplit.split(100, 100, 40, 8));
	}

	@Test
	void fixedColumnsStartAtTheSameXOnEveryRow() {
		assertArrayEquals(new int[] {106, 86}, ColumnSplit.fixed(true, 200, 8));
		assertArrayEquals(new int[] {200, 0}, ColumnSplit.fixed(false, 200, 8));
		assertArrayEquals(new int[] {40, 0}, ColumnSplit.fixed(true, 40, 8));
	}
}
