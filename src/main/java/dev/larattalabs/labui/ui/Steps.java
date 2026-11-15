package dev.larattalabs.labui.ui;

/**
 * A stepper's arithmetic (0.5.0; the kit's {@code Stepper}): one step within [min, max], and whether each end can still move.
 * Pure, tested in {@code StepsTest}.
 */
public final class Steps {
	private Steps() {
	}

	/** {@code value} moved by {@code delta} steps of {@code step}, clamped to [min, max] (a value outside snaps inside first). */
	public static int step(int value, int delta, int step, int min, int max) {
		long v = (long) Math.max(min, Math.min(max, value)) + (long) delta * Math.max(1, step);
		return (int) Math.max(min, Math.min(max, v));
	}

	public static boolean canDecrease(int value, int min) {
		return value > min;
	}

	public static boolean canIncrease(int value, int max) {
		return value < max;
	}
}
