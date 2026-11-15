package dev.larattalabs.labui.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StepsTest {
	@Test
	void stepsAndClamps() {
		assertEquals(6, Steps.step(5, 1, 1, 1, 10));
		assertEquals(10, Steps.step(9, 3, 1, 1, 10));
		assertEquals(1, Steps.step(2, -5, 1, 1, 10));
		assertEquals(60, Steps.step(50, 1, 10, 0, 1000));
	}

	@Test
	void aValueOutsideSnapsInsideFirst() {
		assertEquals(10, Steps.step(40, 0, 1, 1, 10));
		assertEquals(9, Steps.step(40, -1, 1, 1, 10));
	}

	@Test
	void endsKnowWhetherTheyMove() {
		assertFalse(Steps.canDecrease(1, 1));
		assertTrue(Steps.canIncrease(9, 10));
		assertFalse(Steps.canIncrease(10, 10));
	}
}
