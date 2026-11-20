package dev.larattalabs.labui.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** (0.6.0) The "?" overlay's keys: Shift+/ or F1; "?" yields to a focused text field; Esc closes; tips fit one line. */
class HelpKeysTest {
	@Test
	void questionMarkAndF1Toggle() {
		assertTrue(HelpKeys.toggles(HelpKeys.SLASH, true, false));
		assertFalse(HelpKeys.toggles(HelpKeys.SLASH, false, false), "plain / is not ?");
		assertTrue(HelpKeys.toggles(HelpKeys.F1, false, false));
	}

	@Test
	void questionMarkYieldsToATextField() {
		assertFalse(HelpKeys.toggles(HelpKeys.SLASH, true, true), "a search field types ?");
		assertTrue(HelpKeys.toggles(HelpKeys.F1, false, true), "F1 still opens it");
	}

	@Test
	void escCloses() {
		assertTrue(HelpKeys.closes(HelpKeys.ESCAPE, false));
		assertTrue(HelpKeys.closes(HelpKeys.SLASH, true));
		assertFalse(HelpKeys.closes(4, false)); // A
	}

	@Test
	void tipsFitOneLine() {
		assertTrue(HelpKeys.tipFits("Your designs. Pick one to place it here, or make a variant."));
		assertFalse(HelpKeys.tipFits("x".repeat(91)));
		assertFalse(HelpKeys.tipFits(" "));
	}

	@Test
	void closeRowComesLast() {
		List<HelpKeys.Row> r = HelpKeys.withClose(List.of(new HelpKeys.Row("R", "rotate")));
		assertEquals("R", r.get(0).key());
		assertEquals("close this", r.get(1).verb());
	}

	@Test
	void theOverlayWordingDefaultsToTodaysTextAndTakesAModsPlace() {
		assertEquals("Esc or ? closes · Status > Help lists every term", HelpKeys.DEFAULT_FOOTER);
		assertEquals("+3 more in Status > Help", HelpKeys.moreTerms(3, HelpKeys.DEFAULT_HELP_PLACE));
		assertEquals("Esc or ? closes · the Steward book lists every term", HelpKeys.footer("the Steward book"));
		assertEquals("+1 more in the Steward book", HelpKeys.moreTerms(1, "the Steward book"));
		assertEquals("Esc or ? closes", HelpKeys.footer(null));
		assertEquals("+2 more", HelpKeys.moreTerms(2, " "));
	}

	@Test
	void theCloseHitAreaIsAtLeastTwentyEachWayAndHoldsTheOldBoxAndTheGlyph() {
		for (int lineH : new int[] {12, 15, 18}) { // the 12 px line at 100, 125 and 150 % text
			for (int glyphW : new int[] {5, 6, 9}) {
				int gx = 300;
				int ly = 40;
				int[] b = HelpKeys.closeHit(gx, glyphW, ly, lineH);
				assertTrue(b[2] - b[0] >= HelpKeys.MIN_HIT, "width " + (b[2] - b[0]));
				assertTrue(b[3] - b[1] >= HelpKeys.MIN_HIT, "height " + (b[3] - b[1]));
				// the 0.6.0 box (cx-6..cx+12, cy-2..cy+lh+2) stays inside, so nothing that hit before misses now
				assertTrue(b[0] <= gx - 6 && b[2] >= gx + 12 && b[1] <= ly - 2 && b[3] == ly + lineH + 2);
				// the glyph (drawn at gx, ly+2, 8 px tall) is inside
				assertTrue(HelpKeys.inside(b, gx, ly + 2) && HelpKeys.inside(b, gx + glyphW - 1, ly + 9));
				// it never grows down past the line's own 2 px, where a screen's controls start
				assertFalse(HelpKeys.inside(b, gx, ly + lineH + 2));
			}
		}
		// 0.6.0's box at 100 % was 18 x 16
		int[] b = HelpKeys.closeHit(0, 5, 0, 12);
		assertEquals(20, b[2] - b[0]);
		assertEquals(20, b[3] - b[1]);
	}

	@Test
	void theTipsMutedTextReadsOnTheInsetAndOnCream() throws Exception {
		com.google.gson.JsonObject root = OnDarkContrastTest.style();
		int muted = Contrast.hex(root.getAsJsonObject("semantic").get("muted").getAsString());
		double inset = Contrast.ratio(muted, Contrast.hex("#E9E1D3"));
		double cream = Contrast.ratio(muted, Contrast.hex("#F4EFE6"));
		System.out.printf("what's this muted text: %.2f:1 on the inset, %.2f:1 on cream%n", inset, cream);
		assertTrue(inset >= 4.5, "inset " + inset);
		assertTrue(cream >= 4.5, "cream " + cream);
	}
}
