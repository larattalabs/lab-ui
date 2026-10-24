package dev.larattalabs.labui.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** ToastStack.fitTitle (ported from Steward's toasts) and clipBody, with a fixed-width font: 6 px a character, "…" 6 px. */
class ToastTextTest {
	static final ToastStack.Measure M = new ToastStack.Measure() {
		@Override
		public int width(String s) {
			return s.length() * 6;
		}

		@Override
		public String ellipsize(String s, int maxWidth) {
			if (width(s) <= maxWidth) {
				return s;
			}
			if (maxWidth <= 6) {
				return "";
			}
			return s.substring(0, (maxWidth - 6) / 6).stripTrailing() + "…";
		}
	};

	@Test
	void theSubjectIsShortenedFirstAndTheRestStaysWhole() {
		String title = "Stilt Swamp Fishing Village needs you";
		String subject = "Stilt Swamp Fishing Village";
		// 37 chars = 222 px; in 150 px the tail " needs you" (60 px) stays, the subject gets 90 px
		assertEquals("Stilt Swamp Fi… needs you", ToastStack.fitTitle(title, subject, 150, M));
		// the plain ellipsis cuts the end instead
		assertEquals("Stilt Swamp Fishing Vill…", ToastStack.fitTitle(title, null, 150, M));
	}

	@Test
	void aTitleThatFitsIsUnchanged() {
		assertEquals("Hut needs you", ToastStack.fitTitle("Hut needs you", "Hut", 200, M));
	}

	@Test
	void fallsBackToThePlainEllipsis() {
		String title = "Stilt Swamp Fishing Village needs you";
		// not a prefix
		assertEquals(M.ellipsize(title, 150), ToastStack.fitTitle(title, "Village", 150, M));
		// empty subject, or the subject is the whole title
		assertEquals(M.ellipsize(title, 150), ToastStack.fitTitle(title, "", 150, M));
		assertEquals(M.ellipsize(title, 150), ToastStack.fitTitle(title, title, 150, M));
		// the rest would leave the subject under SUBJECT_MIN: plain, never wider than the room
		String r = ToastStack.fitTitle(title, "Stilt", 80, M);
		assertEquals(M.ellipsize(title, 80), r);
		assertTrue(M.width(r) <= 80);
	}

	@Test
	void bodiesCutToTheirLineCountWithAnEllipsisOnTheLast() {
		List<String> wrapped = List.of("one two", "three four", "five six", "seven");
		assertEquals(List.of("one two", "three four …"), ToastStack.clipBody(wrapped, 2, 200, M));
		assertEquals(List.of("one two", "three four", "five six …"), ToastStack.clipBody(wrapped, 3, 200, M));
		assertEquals(List.of("one two …"), ToastStack.clipBody(wrapped, 1, 200, M));
		// short bodies untouched; out of range clamps to 1..3
		assertEquals(List.of("a"), ToastStack.clipBody(List.of("a"), 2, 200, M));
		assertEquals(3, ToastStack.clipBody(wrapped, 9, 200, M).size());
		assertEquals(1, ToastStack.clipBody(wrapped, 0, 200, M).size());
		// the cut line is ellipsized to the width
		assertEquals(List.of("one two", "three…"), ToastStack.clipBody(wrapped, 2, 42, M));
	}
}
