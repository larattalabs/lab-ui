package dev.larattalabs.labui.ui;

/**
 * Selection and scroll of a list or grid with a fixed row height (pure, unit-tested in {@code ListNavTest}). Items are
 * laid out {@code cols} to a row; {@code view} rows are visible from row {@code offset}. The selection (-1 = none) is the
 * keyboard-visible row; moving it always scrolls it into view, while the wheel and the scrollbar scroll without moving it.
 *
 * <pre>
 * nav.update(items.size(), 1, visibleRows);   // every frame or when the items change
 * nav.key(ListNav.Key.DOWN);                  // false at an edge, so the caller can move focus (e.g. back to a search box)
 * nav.scrollBy(-3);                           // mouse wheel
 * </pre>
 */
public final class ListNav {
	/** Navigation keys; LEFT / RIGHT only apply to grids ({@code cols > 1}). */
	public enum Key {
		UP, DOWN, LEFT, RIGHT, HOME, END, PAGE_UP, PAGE_DOWN
	}

	private int count;
	private int cols = 1;
	private int view = 1;
	private int offset;
	private int selected = -1;

	/** Set the sizes (call every frame or when the items change); keeps the selection and offset in range. */
	public ListNav update(int count, int cols, int viewRows) {
		this.count = Math.max(0, count);
		this.cols = Math.max(1, cols);
		this.view = Math.max(1, viewRows);
		if (selected >= this.count) {
			selected = this.count - 1;
		}
		offset = clamp(offset, 0, maxOffset());
		return this;
	}

	public int count() {
		return count;
	}

	public int cols() {
		return cols;
	}

	public int view() {
		return view;
	}

	/** First visible row. */
	public int offset() {
		return offset;
	}

	public int selected() {
		return selected;
	}

	public int rows() {
		return (count + cols - 1) / cols;
	}

	public int maxOffset() {
		return Math.max(0, rows() - view);
	}

	public boolean scrollable() {
		return rows() > view;
	}

	/** Index of the first visible item. */
	public int firstVisible() {
		return Math.min(count, offset * cols);
	}

	/** Index of the last visible item (-1 when empty). */
	public int lastVisible() {
		return Math.min(count, (offset + view) * cols) - 1;
	}

	/** The item at a visible cell ({@code visibleRow} counted from the first visible row), or -1. */
	public int indexAt(int visibleRow, int col) {
		if (visibleRow < 0 || visibleRow >= view || col < 0 || col >= cols) {
			return -1;
		}
		int i = (offset + visibleRow) * cols + col;
		return i < count ? i : -1;
	}

	/** Select {@code i} (clamped; -1 or an empty list clears) and scroll it into view. */
	public void select(int i) {
		if (count == 0 || i < 0) {
			selected = -1;
			return;
		}
		selected = Math.min(i, count - 1);
		reveal();
	}

	public void clearSelection() {
		selected = -1;
	}

	/** Scroll so the selection is visible. */
	public void reveal() {
		if (selected < 0) {
			return;
		}
		int r = selected / cols;
		if (r < offset) {
			offset = r;
		} else if (r >= offset + view) {
			offset = r - view + 1;
		}
		offset = clamp(offset, 0, maxOffset());
	}

	/** Scroll by whole rows (wheel); the selection stays where it is. */
	public void scrollBy(int rows) {
		offset = clamp(offset + rows, 0, maxOffset());
	}

	public void scrollTo(int row) {
		offset = clamp(row, 0, maxOffset());
	}

	/**
	 * Move the selection. With nothing selected, any key selects the first visible item (End: the last item). Returns
	 * false when the key does not apply (an empty list, LEFT / RIGHT in a one-column list) or the selection is already at
	 * that edge (UP on the first row, DOWN on the last, LEFT on the first item, RIGHT on the last), so the caller can
	 * hand focus on (Up from the first row back to a search box).
	 */
	public boolean key(Key k) {
		if (count == 0 || (k == Key.LEFT || k == Key.RIGHT) && cols == 1) {
			return false;
		}
		if (selected < 0) {
			select(k == Key.END ? count - 1 : firstVisible());
			return true;
		}
		int s = selected;
		int row = s / cols;
		int col = s % cols;
		int page = view * cols;
		switch (k) {
			case UP -> {
				if (row == 0) {
					return false;
				}
				s -= cols;
			}
			case DOWN -> {
				if (row >= rows() - 1) {
					return false;
				}
				s = Math.min(count - 1, s + cols);
			}
			case LEFT -> {
				if (s == 0) {
					return false;
				}
				s--;
			}
			case RIGHT -> {
				if (s == count - 1) {
					return false;
				}
				s++;
			}
			case HOME -> s = 0;
			case END -> s = count - 1;
			case PAGE_UP -> {
				s = s - page >= 0 ? s - page : col;
				offset = clamp(offset - view, 0, maxOffset());
			}
			case PAGE_DOWN -> {
				if (s + page < count) {
					s += page;
				} else {
					int lastRowStart = (rows() - 1) * cols;
					s = Math.min(count - 1, lastRowStart + col);
				}
				offset = clamp(offset + view, 0, maxOffset());
			}
		}
		selected = s;
		reveal();
		return true;
	}

	static int clamp(int v, int lo, int hi) {
		return Math.max(lo, Math.min(hi, v));
	}
}
