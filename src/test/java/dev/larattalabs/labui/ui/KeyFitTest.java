package dev.larattalabs.labui.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.ToIntFunction;
import org.junit.jupiter.api.Test;

/** (KLW) Keycap labels: the whole name, cut only by measured width with an ellipsis (not at 9 characters). */
class KeyFitTest {
	/** A fake font: 6 px a character. */
	static final ToIntFunction<String> W = s -> 6 * s.codePointCount(0, s.length());

	@Test
	void readableKeepsLongNamesWhole() {
		assertEquals("Mouse Button 4", KeyFit.readable("Mouse Button 4"));
		assertEquals("Linke Umschalttaste", KeyFit.readable("Linke Umschalttaste"));
		assertEquals("Backtick", KeyFit.readable("`"));
		assertEquals("Semicolon", KeyFit.readable(";"));
		assertEquals("", KeyFit.readable(null));
	}

	@Test
	void aLabelThatFitsIsUnchanged() {
		assertEquals("Mouse Button 4", KeyFit.fit("Mouse Button 4", 14 * 6, W));
		assertEquals("R", KeyFit.fit("R", 6, W));
	}

	@Test
	void aWideLabelIsCutByWidthWithAnEllipsis() {
		String f = KeyFit.fit("Mouse Button 4", 60, W);
		assertTrue(W.applyAsInt(f) <= 60, f);
		assertEquals("Mouse But" + KeyFit.ELLIPSIS, f); // the longest prefix that fits: 9 + 1 characters at 6 px
		assertEquals("Mouse" + KeyFit.ELLIPSIS, KeyFit.fit("Mouse Button 4", 42, W)); // trailing space dropped
		// by measured width, not a character count: a wider font (150 % text) cuts sooner
		assertEquals("Mous" + KeyFit.ELLIPSIS, KeyFit.fit("Mouse Button 4", 60, s -> 12 * s.length()));
		assertEquals("Mouse B" + KeyFit.ELLIPSIS, KeyFit.fit("Mouse Button 4", 60, s -> (int) Math.ceil(7.5 * s.length())));
	}

	@Test
	void aKeycapNeverLooksEmpty() {
		assertEquals("M", KeyFit.fit("Mouse Button 4", 6, W));
		assertEquals("M", KeyFit.fit("Mouse Button 4", 0, W));
		assertFalse(KeyFit.fit("Mouse Button 4", -5, W).isEmpty());
		assertEquals("", KeyFit.fit("", 10, W));
		assertEquals("", KeyFit.fit(null, 10, W));
		// a surrogate pair is never split
		assertEquals("😀", KeyFit.fit("😀😀😀", 6, W));
	}

	@Test
	void hintsFitEachKeyToItsPairAndKeepTheVerbs() {
		String[] in = {"Mouse Button 4", "rotate", "Esc", "cancel"};
		assertArrayEquals(in, KeyFit.fitHints(in, 200, 4, W));
		// room for the key: 80 - 4 (keycap pad) - 3 - 36 ("rotate") = 37 px, so "Mouse…" (36 px)
		String[] out = KeyFit.fitHints(in, 80, 4, W);
		assertArrayEquals(new String[] {"Mouse" + KeyFit.ELLIPSIS, "rotate", "Esc", "cancel"}, out);
		assertEquals("Mouse Button 4", in[0], "the input row is not changed");
	}

	@Test
	void overlayRowsFitTheKeyColumnSoNoKeycapReachesTheVerb() {
		List<HelpKeys.Row> in = List.of(new HelpKeys.Row("Mouse Button 4", "place"), new HelpKeys.Row("R", "rotate"));
		int keyCol = 50; // label room 50 - 6 (pad) = 44 px
		List<HelpKeys.Row> out = KeyFit.fitRows(in, keyCol, 6, W);
		assertEquals("Mouse" + KeyFit.ELLIPSIS, out.get(0).key());
		assertTrue(W.applyAsInt(out.get(0).key()) + 6 <= keyCol);
		assertEquals("place", out.get(0).verb());
		assertEquals(in.get(1), out.get(1));
	}
}
