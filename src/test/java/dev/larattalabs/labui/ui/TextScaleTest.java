package dev.larattalabs.labui.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** (0.6.0, Architect PR A3) The text size: opt-in per mod, the clamp table, the steps. */
class TextScaleTest {
	@AfterEach
	void reset() {
		TextScale.optOut("architect_mc");
		TextScale.prefer(100);
	}

	@Test
	void modsAreOutUntilTheyOptIn() {
		assertEquals(new TextScale.Result(100, false), TextScale.resolve("steward_mc", 150, 960, 509));
		TextScale.optIn("architect_mc");
		assertEquals(new TextScale.Result(150, false), TextScale.resolve("architect_mc", 150, 960, 509));
		assertEquals(new TextScale.Result(100, false), TextScale.resolve("steward_mc", 150, 960, 509), "Steward stays out (S-PR-1)");
		assertEquals(new TextScale.Result(100, false), TextScale.resolve(null, 150, 960, 509));
	}

	@Test
	void itAppliesAtEveryGuiScale() {
		TextScale.optIn("architect_mc");
		for (int[] g : new int[][] {{960, 509}, {640, 339}, {480, 254}}) {
			assertEquals(new TextScale.Result(150, false), TextScale.resolve("architect_mc", 150, g[0], g[1]), g[0] + "x" + g[1]);
		}
	}

	@Test
	void lineBoxesAndWidthsFollowTheActiveFactor() {
		assertEquals(10, TextScale.lh(10));
		float was = TextScale.activate(1.5f);
		try {
			assertEquals(15, TextScale.lh(10));
			assertEquals(18, TextScale.lh(12));
			assertEquals(100, TextScale.unscaled(150), "a 150 px column holds 100 px of unscaled text");
		} finally {
			TextScale.activate(was);
		}
		assertEquals(1f, TextScale.activeFactor());
	}

	@Test
	void stepsSnapAndCycle() {
		assertEquals(125, TextScale.snap(130));
		assertEquals(100, TextScale.snap(10));
		assertEquals(150, TextScale.snap(999));
		assertEquals(125, TextScale.next(100));
		assertEquals(100, TextScale.next(150));
		TextScale.prefer(140);
		assertEquals(150, TextScale.preference());
		assertTrue(TextScale.STEPS.contains(TextScale.preference()));
		assertFalse(TextScale.optedIn("architect_mc"));
	}
}
