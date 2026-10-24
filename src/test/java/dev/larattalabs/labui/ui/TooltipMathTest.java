package dev.larattalabs.labui.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TooltipMathTest {
	@Test
	void delayRestartsOnANewTarget() {
		TooltipMath.Delay d = new TooltipMath.Delay(400);
		assertFalse(d.update("a", 0));
		assertFalse(d.update("a", 399));
		assertTrue(d.update("a", 400));
		assertFalse(d.update("b", 500));
		assertTrue(d.update("b", 900));
		assertFalse(d.update(null, 2000));
		assertFalse(d.update("b", 2001), "leaving and coming back waits again");
	}

	@Test
	void placedRightBelowTheCursorWhenItFits() {
		assertEquals(new TooltipMath.Pos(110, 60), TooltipMath.place(100, 50, 40, 20, 400, 300, 4));
	}

	@Test
	void flipsAtTheRightAndBottomEdges() {
		TooltipMath.Pos p = TooltipMath.place(390, 290, 40, 20, 400, 300, 4);
		assertEquals(390 - 10 - 40, p.x());
		assertEquals(290 - 5 - 20, p.y());
	}

	@Test
	void clampedInsideTheScreen() {
		TooltipMath.Pos wide = TooltipMath.place(20, 5, 380, 20, 400, 300, 4);
		assertEquals(4, wide.x(), "neither side fits: clamped to the margin");
		TooltipMath.Pos huge = TooltipMath.place(200, 150, 1000, 1000, 400, 300, 4);
		assertEquals(new TooltipMath.Pos(4, 4), huge);
	}
}
