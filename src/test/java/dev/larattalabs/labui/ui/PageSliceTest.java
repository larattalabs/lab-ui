package dev.larattalabs.labui.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PageSliceTest {
	@Test
	void slices() {
		PageSlice s = PageSlice.of(41, 8, 1);
		assertEquals(new PageSlice(1, 6, 0, 8, 41), s);
		assertEquals(33, s.after());
		assertFalse(s.hasPrev());
		assertTrue(s.hasNext());
		PageSlice last = PageSlice.of(41, 8, 6);
		assertEquals(40, last.from());
		assertEquals(41, last.to());
		assertEquals(0, last.after());
		assertFalse(last.hasNext());
	}

	@Test
	void clampsAndHandlesEmpty() {
		assertEquals(6, PageSlice.of(41, 8, 99).page());
		assertEquals(1, PageSlice.of(41, 8, -3).page());
		PageSlice e = PageSlice.of(0, 8, 2);
		assertEquals(new PageSlice(1, 1, 0, 0, 0), e);
		assertEquals(1, PageSlice.of(5, 0, 1).size(), "page size at least 1");
	}
}
