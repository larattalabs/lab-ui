package dev.larattalabs.labui.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ContrastTest {
	@Test
	void blackOnWhiteIs21AndAColourOnItselfIs1() {
		assertEquals(21.0, Contrast.ratio(0x000000, 0xFFFFFF), 1e-9);
		assertEquals(1.0, Contrast.ratio(0x2B2927, 0x2B2927), 1e-9);
		assertEquals(Contrast.ratio(0x123456, 0xABCDEF), Contrast.ratio(0xABCDEF, 0x123456), 1e-12);
	}

	@Test
	void knownPairsFromTheKit() {
		// paper.text on cream (ui-style.json paper_contrast: 14.5)
		assertEquals(14.5, Contrast.ratio(0x1F1E1D, 0xF4EFE6), 0.05);
		// semantic.error on the HUD panel: the reason for the on-dark set
		assertEquals(1.94, Contrast.ratio(0x9A2F2B, 0x2B2927), 0.01);
	}

	@Test
	void alphaIgnoredUnlessComposited() {
		assertEquals(Contrast.ratio(0x1F1E1D, 0xF4EFE6), Contrast.ratio(0xFF1F1E1D, 0x00F4EFE6), 1e-12);
	}

	@Test
	void compositingBlendsPerChannel() {
		assertEquals(0xFFFFFF, Contrast.over(0x000000, 0.0, 0xFFFFFF));
		assertEquals(0x2B2927, Contrast.over(0x2B2927, 1.0, 0xFFFFFF));
		// the HUD panel: the sprite's 244/255 alpha times the 0xF0 tint = 0.9006, over a white world
		assertEquals(0x403E3C, Contrast.over(0x2B2927, 0.9006, 0xFFFFFF));
		assertEquals(0x403E3C, Contrast.surface("#2B2927@0.9006/#FFFFFF"));
		assertEquals(0x26221F, Contrast.surface("#26221F"));
	}

	@Test
	void hexIsStrict() {
		assertEquals(0xF2948B, Contrast.hex("#F2948B"));
		assertThrows(IllegalArgumentException.class, () -> Contrast.hex("F2948B"));
		assertThrows(IllegalArgumentException.class, () -> Contrast.hex("#F2948B80"));
	}
}
