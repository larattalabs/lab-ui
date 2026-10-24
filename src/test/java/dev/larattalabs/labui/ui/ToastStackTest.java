package dev.larattalabs.labui.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.larattalabs.labui.ui.ToastStack.Item;
import dev.larattalabs.labui.ui.ToastStack.Layout;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ToastStackTest {
	static List<Item> inOrder(List<Boolean> newestFirst, int h) {
		List<Item> out = new ArrayList<>();
		for (int i : ToastStack.order(newestFirst)) {
			out.add(new Item(newestFirst.get(i), h));
		}
		return out;
	}

	@Test
	void needYouToastsComeFirstNewestFirst() {
		// newest first: info, need, info, need
		assertEquals(List.of(1, 3, 0, 2), ToastStack.order(List.of(false, true, false, true)));
		assertEquals(List.of(), ToastStack.order(List.of()));
	}

	@Test
	void theQueueDropsTheOldestInfoToastNeverANeedYouOne() {
		assertEquals(-1, ToastStack.evict(List.of(true, false), 2));
		// newest first: need, info, need, info (oldest)
		assertEquals(3, ToastStack.evict(List.of(true, false, true, false), 3));
		assertEquals(1, ToastStack.evict(List.of(true, false, true, true), 3));
		// all need you: the oldest goes
		assertEquals(3, ToastStack.evict(List.of(true, true, true, true), 3));
	}

	@Test
	void atASmallGuiTheNeedYouToastShowsAndInfoWaits() {
		// 426x240 with survival bars: room for about one toast under the pill
		List<Item> items = inOrder(List.of(false, false, true), 44); // the need-you toast is the oldest
		Layout l = ToastStack.layout(items, 40, 110, 4, ToastStack.MAX_SHOWN, 13);
		assertEquals(List.of(0), l.shown());
		assertTrue(items.get(0).need());
		assertEquals(2, l.hidden());
		assertEquals(0, l.hiddenNeed());
		assertEquals(40 + 44 + 4, l.moreY());
		assertEquals("+2 more", ToastStack.moreText(l.hidden(), l.hiddenNeed()));
	}

	@Test
	void noInfoToastIsDrawnAfterANeedYouToastThatDoesNotFit() {
		// a tall need-you toast that does not fit, a short info one that would
		List<Item> items = List.of(new Item(true, 60), new Item(false, 30));
		Layout l = ToastStack.layout(items, 40, 90, 4, 3, 13);
		assertEquals(List.of(), l.shown());
		assertEquals(2, l.hidden());
		assertEquals(1, l.hiddenNeed());
		assertEquals(40, l.moreY());
		assertEquals("+2 more · 1 needs you", ToastStack.moreText(l.hidden(), l.hiddenNeed()));
		// a shorter need-you toast after it may still show
		Layout l2 = ToastStack.layout(List.of(new Item(true, 60), new Item(true, 30), new Item(false, 20)), 40, 90, 4, 3, 13);
		assertEquals(List.of(1), l2.shown());
	}

	@Test
	void aNeedYouToastShowsEvenWithoutRoomForTheMoreLine() {
		List<Item> items = List.of(new Item(true, 44), new Item(false, 44));
		Layout l = ToastStack.layout(items, 40, 90, 4, 3, 13);
		assertEquals(List.of(0), l.shown());
		assertEquals(1, l.hidden());
		assertEquals(-1, l.moreY()); // no room for the line under it
		// an info toast in the same spot keeps room for the line instead
		Layout l2 = ToastStack.layout(List.of(new Item(false, 44), new Item(false, 44)), 40, 90, 4, 3, 13);
		assertEquals(List.of(), l2.shown());
		assertEquals(40, l2.moreY());
	}

	@Test
	void threeAtMostTheRestCount() {
		List<Item> items = inOrder(List.of(false, false, false, false, true), 30);
		Layout l = ToastStack.layout(items, 40, 1000, 4, ToastStack.MAX_SHOWN, 13);
		assertEquals(List.of(0, 1, 2), l.shown());
		assertEquals(List.of(40, 74, 108), l.ys());
		assertTrue(items.get(0).need());
		assertEquals(2, l.hidden());
		assertEquals(142, l.moreY());
		// a single toast that fits: no more line
		Layout one = ToastStack.layout(List.of(new Item(false, 30)), 40, 1000, 4, 3, 13);
		assertEquals(0, one.hidden());
		assertEquals(-1, one.moreY());
	}

	@Test
	void aShorterInfoToastBelowMayStillFit() {
		List<Item> items = List.of(new Item(false, 80), new Item(false, 30));
		Layout l = ToastStack.layout(items, 40, 100, 4, 3, 13);
		assertEquals(List.of(1), l.shown());
		assertEquals(1, l.hidden());
	}

	@Test
	void aNeedYouToastShrinksToItsTitleRowRatherThanFoldAway() {
		List<Item> items = inOrder(List.of(false, true, false), 56);
		// 30 px: no full toast fits; the need-you one shows compact (22), and the "+2 more" line does not fit under it
		ToastStack.Fitted f = ToastStack.layoutNeedFirst(items, 100, 130, 4, 3, 13, 22);
		assertTrue(f.compactFirst());
		assertEquals(List.of(0), f.layout().shown());
		assertEquals(List.of(100), f.layout().ys());
		assertEquals(2, f.layout().hidden());
		assertEquals(0, f.layout().hiddenNeed());
		assertEquals(-1, f.layout().moreY());
		// 40 px: room for the "+2 more" line under it
		f = ToastStack.layoutNeedFirst(items, 100, 140, 4, 3, 13, 22);
		assertEquals(126, f.layout().moreY());
		// enough room: the plain layout, nothing compact
		f = ToastStack.layoutNeedFirst(items, 100, 400, 4, 3, 13, 22);
		assertFalse(f.compactFirst());
		assertEquals(ToastStack.layout(items, 100, 400, 4, 3, 13), f.layout());
	}

	@Test
	void infoToastsStillFoldIntoMore() {
		List<Item> items = inOrder(List.of(false, false), 56);
		ToastStack.Fitted f = ToastStack.layoutNeedFirst(items, 100, 130, 4, 3, 13, 22);
		assertFalse(f.compactFirst());
		assertTrue(f.layout().shown().isEmpty());
		// and with less than the title row, a need-you toast folds too
		f = ToastStack.layoutNeedFirst(inOrder(List.of(true), 56), 100, 115, 4, 3, 13, 22);
		assertFalse(f.compactFirst());
	}
}
