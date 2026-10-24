package dev.larattalabs.labui.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;
import org.jspecify.annotations.Nullable;

/**
 * Lays out chips left to right in rows of {@code avail} px, at most {@code maxRows} rows (pure, unit-tested in
 * {@code ChipFlowTest}). What does not fit collapses into one "+N" chip at the end of the last row: chips are taken off
 * the end until the "+N" chip (its width from {@code moreWidth.applyAsInt(N)}) fits after the last one kept. A chip wider
 * than the row is clamped to the row (the caller ellipsizes its label).
 */
public final class ChipFlow {
	private ChipFlow() {
	}

	/** A placed chip: input index ({@code -1} = the "+N" chip), x from the row start, row, width. */
	public record Placed(int index, int x, int row, int w) {
	}

	/** {@code more} is null when everything fits; {@code hidden} counts the chips folded into it. */
	public record Layout(List<Placed> placed, int hidden, @Nullable Placed more, int rows) {
	}

	public static Layout layout(int[] widths, int avail, int gap, int maxRows, IntUnaryOperator moreWidth) {
		int rowsMax = Math.max(1, maxRows);
		int width = Math.max(1, avail);
		List<Placed> out = new ArrayList<>();
		int x = 0;
		int row = 0;
		boolean overflow = false;
		for (int i = 0; i < widths.length; i++) {
			int w = Math.min(Math.max(0, widths[i]), width);
			if (x > 0 && x + w > width) {
				row++;
				x = 0;
			}
			if (row >= rowsMax) {
				overflow = true;
				break;
			}
			out.add(new Placed(i, x, row, w));
			x += w + gap;
		}
		if (!overflow) {
			return new Layout(List.copyOf(out), 0, null, out.isEmpty() ? 0 : out.get(out.size() - 1).row() + 1);
		}
		int lastRow = rowsMax - 1;
		while (true) {
			int hidden = widths.length - out.size();
			int mw = Math.min(width, Math.max(0, moreWidth.applyAsInt(hidden)));
			Placed last = out.isEmpty() ? null : out.get(out.size() - 1);
			int endX = last != null && last.row() == lastRow ? last.x() + last.w() + gap : 0;
			if (endX + mw <= width || last == null || last.row() != lastRow) {
				Placed more = new Placed(-1, endX, lastRow, mw);
				return new Layout(List.copyOf(out), hidden, more, rowsMax);
			}
			out.remove(out.size() - 1);
		}
	}
}
