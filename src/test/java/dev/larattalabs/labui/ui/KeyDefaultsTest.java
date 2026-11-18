package dev.larattalabs.labui.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

/** (0.6.0) lab-ui's defaults: the jobs panel stays K, and no default takes a key Steward reserves (S-PR-2). */
class KeyDefaultsTest {
	@Test
	void jobsIsK() {
		assertEquals(14, KeyDefaults.JOBS); // InputConstants.KEY_K on 26.3 (SDL scancodes)
	}

	@Test
	void noDefaultTakesStewardsKeys() {
		for (int k : KeyDefaults.globals()) {
			assertFalse(KeyDefaults.STEWARD_RESERVED.contains(k), "lab-ui default " + k + " is one of Steward's Y/I/U");
		}
	}
}
