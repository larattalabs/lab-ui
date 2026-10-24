package dev.larattalabs.labui.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ScrollMathTest {
	@Test
	void thumbFillsTheTrackWhenEverythingFits() {
		assertEquals(new ScrollMath.Thumb(0, 100), ScrollMath.thumb(100, 5, 10, 0));
	}

	@Test
	void thumbSizeAndPositionFollowTheOffset() {
		ScrollMath.Thumb top = ScrollMath.thumb(100, 40, 10, 0);
		assertEquals(25, top.h());
		assertEquals(0, top.y());
		assertEquals(75, ScrollMath.thumb(100, 40, 10, 30).y());
		assertEquals(38, ScrollMath.thumb(100, 40, 10, 15).y());
		assertEquals(ScrollMath.MIN_THUMB, ScrollMath.thumb(100, 10_000, 10, 0).h(), "never smaller than the minimum");
	}

	@Test
	void dragMapsBackToTheOffset() {
		for (int o = 0; o <= 30; o++) {
			ScrollMath.Thumb t = ScrollMath.thumb(100, 40, 10, o);
			assertEquals(o, ScrollMath.offsetForThumbTop(100, 40, 10, t.y()), "round trip at " + o);
		}
		assertEquals(0, ScrollMath.offsetForThumbTop(100, 40, 10, -50));
		assertEquals(30, ScrollMath.offsetForThumbTop(100, 40, 10, 500));
		assertEquals(0, ScrollMath.offsetForThumbTop(100, 5, 10, 40));
	}

	@Test
	void trackClickPagesTowardsTheClick() {
		assertEquals(20, ScrollMath.pageTowards(100, 40, 10, 10, 95));
		assertEquals(0, ScrollMath.pageTowards(100, 40, 10, 5, 0));
		assertEquals(30, ScrollMath.pageTowards(100, 40, 10, 25, 99));
		int thumbY = ScrollMath.thumb(100, 40, 10, 10).y();
		assertEquals(10, ScrollMath.pageTowards(100, 40, 10, 10, thumbY + 1), "a click on the thumb does not page");
	}
}
