package dev.larattalabs.labui.ui;

/**
 * A grid split into sections, each starting on a new row under an optional heading row (pure, unit-tested in
 * {@code SectionGridTest}). Items are numbered across sections in order. It maps items to visual rows and moves a
 * selection by keyboard: Up / Down go to the same column of the previous / next row of items (or that row's last item),
 * skipping headings; Left / Right step through the items in order, across sections.
 */
public final class SectionGrid {
	private final int cols;
	private final int count;
	private final int[] rowOf;
	private final int[] colOf;
	/** Per visual row: the first item (-1 for a heading row) and the item count. */
	private final int[] rowFirst;
	private final int[] rowLen;
	/** Per visual row: the section it belongs to. */
	private final int[] rowSection;

	public SectionGrid(int[] sectionSizes, int cols, boolean headings) {
		this.cols = Math.max(1, cols);
		int n = 0;
		int rows = 0;
		for (int s : sectionSizes) {
			if (s <= 0) {
				continue;
			}
			n += s;
			rows += (headings ? 1 : 0) + (s + this.cols - 1) / this.cols;
		}
		count = n;
		rowOf = new int[n];
		colOf = new int[n];
		rowFirst = new int[rows];
		rowLen = new int[rows];
		rowSection = new int[rows];
		int r = 0;
		int i = 0;
		for (int sec = 0; sec < sectionSizes.length; sec++) {
			int s = sectionSizes[sec];
			if (s <= 0) {
				continue;
			}
			if (headings) {
				rowFirst[r] = -1;
				rowLen[r] = 0;
				rowSection[r] = sec;
				r++;
			}
			for (int k = 0; k < s; k++) {
				if (k % this.cols == 0) {
					rowFirst[r] = i;
					rowLen[r] = Math.min(this.cols, s - k);
					rowSection[r] = sec;
					r++;
				}
				rowOf[i] = r - 1;
				colOf[i] = k % this.cols;
				i++;
			}
		}
	}

	/**
	 * Columns for a grid {@code inner} px wide: as many as fit at {@code minW} each, but never more than the largest
	 * section holds, so a sparse grid gets fewer, wider cells instead of an empty right side. At least 1.
	 */
	public static int columns(int inner, int gap, int minW, int largestSection) {
		int fit = Math.max(1, (inner + gap) / (Math.max(1, minW) + gap));
		return Math.max(1, Math.min(fit, Math.max(1, largestSection)));
	}

	/** The cell width for {@code cols} columns filling {@code inner} px, capped at {@code maxW}. */
	public static int cellWidth(int inner, int gap, int cols, int maxW) {
		int c = Math.max(1, cols);
		return Math.max(1, Math.min(maxW, (inner - gap * (c - 1)) / c));
	}

	public int count() {
		return count;
	}

	public int cols() {
		return cols;
	}

	public int rows() {
		return rowFirst.length;
	}

	public boolean heading(int row) {
		return rowFirst[row] < 0;
	}

	/** The section index (into the sizes given) a visual row belongs to. */
	public int section(int row) {
		return rowSection[row];
	}

	public int rowFirst(int row) {
		return rowFirst[row];
	}

	public int rowLength(int row) {
		return rowLen[row];
	}

	public int rowOf(int index) {
		return rowOf[index];
	}

	public int colOf(int index) {
		return colOf[index];
	}

	/** The item at a visual row and column, or -1 (a heading, or past the row's end). */
	public int at(int row, int col) {
		if (row < 0 || row >= rows() || heading(row) || col < 0 || col >= rowLen[row]) {
			return -1;
		}
		return rowFirst[row] + col;
	}

	/**
	 * The selection after a key, or -1 when the key does not apply here (empty grid, or already at that edge: Up on the
	 * first row of items, Down on the last, Left on the first item, Right on the last). With nothing selected
	 * ({@code index < 0}) any key selects the first item (End: the last). Page keys move {@code pageRows} rows of items.
	 */
	public int move(int index, ListNav.Key key, int pageRows) {
		if (count == 0) {
			return -1;
		}
		if (index < 0 || index >= count) {
			return key == ListNav.Key.END ? count - 1 : 0;
		}
		return switch (key) {
			case LEFT -> index > 0 ? index - 1 : -1;
			case RIGHT -> index < count - 1 ? index + 1 : -1;
			case HOME -> 0;
			case END -> count - 1;
			case UP -> vertical(index, -1);
			case DOWN -> vertical(index, 1);
			case PAGE_UP, PAGE_DOWN -> {
				int dir = key == ListNav.Key.PAGE_UP ? -1 : 1;
				int at = index;
				for (int k = 0; k < Math.max(1, pageRows); k++) {
					int next = vertical(at, dir);
					if (next < 0) {
						break;
					}
					at = next;
				}
				yield at == index ? (dir < 0 ? 0 : count - 1) : at;
			}
		};
	}

	private int vertical(int index, int dir) {
		int col = colOf[index];
		for (int r = rowOf[index] + dir; r >= 0 && r < rows(); r += dir) {
			if (!heading(r)) {
				return rowFirst[r] + Math.min(col, rowLen[r] - 1);
			}
		}
		return -1;
	}
}
