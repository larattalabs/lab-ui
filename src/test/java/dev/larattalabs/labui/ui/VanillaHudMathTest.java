package dev.larattalabs.labui.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class VanillaHudMathTest {
	@Test
	void bossBarsStepNineteenAndStopAtAThirdOfTheScreen() {
		assertEquals(0, VanillaHudMath.bossBottom(0, 540));
		assertEquals(17, VanillaHudMath.bossBottom(1, 540));
		assertEquals(36, VanillaHudMath.bossBottom(2, 540));
		// 540 / 3 = 180: bars at 12, 31, ... 164 are drawn (the next would start at 183)
		assertEquals(164 + 5, VanillaHudMath.bossBottom(20, 540));
		// a tiny screen still draws the first bar
		assertEquals(17, VanillaHudMath.bossBottom(3, 30));
	}

	@Test
	void effectRows() {
		assertEquals(0, VanillaHudMath.effectsBottom(0, 0, false));
		assertEquals(25, VanillaHudMath.effectsBottom(2, 0, false));
		assertEquals(51, VanillaHudMath.effectsBottom(0, 1, false));
		assertEquals(66, VanillaHudMath.effectsBottom(3, 1, true));
		assertEquals(75, VanillaHudMath.effectsWidth(3, 1));
		assertEquals(0, VanillaHudMath.effectsWidth(0, 0));
	}

	@Test
	void toastsBottomIsTheLowestEdge() {
		assertEquals(0, VanillaHudMath.toastsBottom(List.of()));
		assertEquals(64, VanillaHudMath.toastsBottom(List.of(new int[] {0, 32}, new int[] {32, 32})));
		assertEquals(96, VanillaHudMath.toastsBottom(List.of(new int[] {64, 32}, new int[] {0, 32})));
	}
}
