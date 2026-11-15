package dev.larattalabs.labui.ui;

/**
 * Splits a row's room between a title column and a secondary column (0.5.0; the Jobs panel's title and step, from Steward's
 * S-J4 asks). Before, the step followed the title on one line and was cut first, so a long title left no step at all.
 * Pure: widths in px, tested in {@code ColumnSplitTest}.
 */
public final class ColumnSplit {
	/** The least a secondary column is worth drawing at; below it the title takes the whole row. */
	public static final int MIN_SECOND = 24;
	/** The share of the row a title keeps when both columns must be cut. */
	public static final double TITLE_SHARE = 0.55;

	private ColumnSplit() {
	}

	/**
	 * {titleW, secondW} for a row of {@code room} px with {@code gap} px between the columns. Both whole when they fit; else the
	 * shorter one stays whole if the other keeps at least {@link #MIN_SECOND} (the title at least {@link #TITLE_SHARE}); else
	 * the title gets {@link #TITLE_SHARE} and the second the rest. {@code second} 0 (none) gives the title everything.
	 */
	public static int[] split(int title, int second, int room, int gap) {
		room = Math.max(0, room);
		if (second <= 0) {
			return new int[] {room, 0};
		}
		int avail = room - gap;
		if (title + second <= avail) {
			return new int[] {title, avail - title};
		}
		int titleMin = (int) Math.round(avail * TITLE_SHARE);
		if (avail - titleMin < MIN_SECOND) {
			return new int[] {room, 0};
		}
		if (title <= titleMin) {
			return new int[] {title, avail - title};
		}
		if (second <= avail - titleMin) {
			return new int[] {avail - second, second};
		}
		return new int[] {titleMin, avail - titleMin};
	}

	/**
	 * A table split: {titleW, secondW} with the second column starting at the same x on every row ({@link #TITLE_SHARE} of the room
	 * goes to the title whatever its length), so rows scan as columns. No second text, or a second column under {@link #MIN_SECOND},
	 * gives the title the row.
	 */
	public static int[] fixed(boolean hasSecond, int room, int gap) {
		room = Math.max(0, room);
		int avail = room - gap;
		int titleW = (int) Math.round(avail * TITLE_SHARE);
		if (!hasSecond || avail - titleW < MIN_SECOND) {
			return new int[] {room, 0};
		}
		return new int[] {titleW, avail - titleW};
	}
}
