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
	void theClampTable() {
		TextScale.optIn("architect_mc");
		// a 1920 x 1018 window: GUI 2 = 960 x 509, GUI 3 = 640 x 339, GUI 4 = 480 x 254
		assertEquals(new TextScale.Result(150, false), TextScale.resolve("architect_mc", 150, 960, 509));
		assertEquals(new TextScale.Result(125, true), TextScale.resolve("architect_mc", 150, 640, 339), "426 x 226 is under 240 high: 125 %");
		assertEquals(new TextScale.Result(100, true), TextScale.resolve("architect_mc", 150, 480, 254), "GUI 4 has no room: back to 100 %, said");
		assertEquals(new TextScale.Result(100, true), TextScale.resolve("architect_mc", 125, 480, 254));
		assertEquals(new TextScale.Result(100, false), TextScale.resolve("architect_mc", 100, 480, 254));
		assertEquals(new TextScale.Result(125, false), TextScale.resolve("architect_mc", 125, 640, 339));
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
