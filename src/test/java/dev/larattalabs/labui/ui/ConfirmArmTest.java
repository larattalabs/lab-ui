package dev.larattalabs.labui.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.larattalabs.labui.ui.ConfirmArm.Result;
import org.junit.jupiter.api.Test;

class ConfirmArmTest {
	@Test
	void secondPressWithinTheWindowConfirms() {
		ConfirmArm c = new ConfirmArm(5000);
		assertEquals(Result.ARMED, c.press("delete", 1000));
		assertTrue(c.armed("delete", 3000));
		assertEquals(Result.CONFIRMED, c.press("delete", 3000));
		assertNull(c.armedKey(3001), "confirming disarms");
	}

	@Test
	void theWindowRunsOutAndCountsDown() {
		ConfirmArm c = new ConfirmArm(5000);
		c.press("delete", 1000);
		assertEquals(5, c.secondsLeft(1000));
		assertEquals(4, c.secondsLeft(2001));
		assertEquals(1, c.secondsLeft(5999));
		assertEquals(0, c.secondsLeft(6001));
		assertEquals(Result.ARMED, c.press("delete", 6001), "a stale arm arms again");
	}

	@Test
	void keysAreSeparateAndClicksElsewhereCancel() {
		ConfirmArm c = new ConfirmArm();
		c.press("site:a", 10);
		assertEquals(Result.ARMED, c.press("site:b", 20), "another row re-arms on that row");
		assertFalse(c.armed("site:a", 20));
		c.clickedElsewhere("site:b");
		assertTrue(c.armed("site:b", 30), "a click on the armed control keeps it");
		c.clickedElsewhere(null);
		assertFalse(c.armed("site:b", 30));
		c.press("x", 40);
		c.cancel();
		assertNull(c.armedKey(41));
	}

	@Test
	void keyboardConfirmIgnoresRepeatsAndTooQuickPresses() {
		ConfirmArm c = new ConfirmArm(5000);
		assertEquals(Result.IGNORED, c.pressByKey("merge", 100, true), "a held key never arms");
		assertEquals(Result.ARMED, c.pressByKey("merge", 100, false));
		assertEquals(Result.IGNORED, c.pressByKey("merge", 200, false), "within KEY_CONFIRM_MS");
		assertEquals(Result.IGNORED, c.pressByKey("merge", 500, true), "a repeat");
		assertEquals(Result.CONFIRMED, c.pressByKey("merge", 100 + UiRules.KEY_CONFIRM_MS, false));
	}
}
