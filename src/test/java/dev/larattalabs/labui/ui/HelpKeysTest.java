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
		assertFalse(HelpKeys.closes(65, false));
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
}
