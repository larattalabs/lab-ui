package dev.larattalabs.labui.ui;

/**
 * One page of a list (pure, unit-tested in {@code PageSliceTest}): items {@code [from, to)} of {@code total}, page
 * {@code page} of {@code pages} (1-based; an out-of-range page is clamped, and an empty list has one empty page).
 */
public record PageSlice(int page, int pages, int from, int to, int total) {
	public static PageSlice of(int total, int pageSize, int page) {
		int t = Math.max(0, total);
		int size = Math.max(1, pageSize);
		int pages = Math.max(1, (t + size - 1) / size);
		int p = Math.max(1, Math.min(page, pages));
		int from = Math.min(t, (p - 1) * size);
		int to = Math.min(t, from + size);
		return new PageSlice(p, pages, from, to, t);
	}

	public boolean hasPrev() {
		return page > 1;
	}

	public boolean hasNext() {
		return page < pages;
	}

	/** Items after this page ("…and K more"). */
	public int after() {
		return total - to;
	}

	public int size() {
		return to - from;
	}
}
