package dev.larattalabs.labui.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Stacks the HUD elements of one zone ({@code HudZones}; pure, unit-tested in {@code ZoneStackTest}). Elements are ordered
 * by priority (high first, then registration order) from the zone's anchor edge. When they don't fit, the lowest-priority
 * shrinkable element gives up height first (down to its {@code minH}), then the next one up; then the lowest-priority
 * optional element is hidden, then the next. What still doesn't fit is placed anyway and reported as overflow: a fixed
 * element is never hidden silently.
 */
public final class ZoneStack {
	private ZoneStack() {
	}

	/** One element: its priority, registration order, wanted height, the least it can make do with, and whether it may go. */
	public record Item(int priority, int order, int h, int minH, boolean optional) {
		public Item {
			if (h < 0 || minH < 0 || minH > h) {
				throw new IllegalArgumentException("need 0 <= minH <= h: " + minH + ", " + h);
			}
		}
	}

	/** One placed element: its index in the input, top y and the height it got (less than wanted = collapsed). */
	public record Placed(int index, int y, int h, boolean collapsed) {
	}

	/** Placed elements in stack order (anchor first), the hidden indices, whether it overflows, and the height used. */
	public record Result(List<Placed> placed, List<Integer> hidden, boolean overflow, int used) {
	}

	/**
	 * Stacks {@code items} against {@code anchor}: downward from it when {@code down} (anchor = the top edge), else upward
	 * (anchor = the bottom edge), within {@code space} px, {@code gap} px between elements.
	 */
	public static Result stack(List<Item> items, int anchor, boolean down, int space, int gap) {
		int n = items.size();
		List<Integer> order = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			order.add(i);
		}
		order.sort(Comparator.<Integer>comparingInt(i -> -items.get(i).priority()).thenComparingInt(i -> items.get(i).order()));
		int[] h = new int[n];
		boolean[] shown = new boolean[n];
		for (int i = 0; i < n; i++) {
			h[i] = items.get(i).h();
			shown[i] = true;
		}
		int total = total(h, shown, gap);
		// 1. shrink, lowest priority first
		for (int k = n - 1; k >= 0 && total > Math.max(0, space); k--) {
			int i = order.get(k);
			int cut = Math.min(h[i] - items.get(i).minH(), total - Math.max(0, space));
			if (cut > 0) {
				h[i] -= cut;
				total -= cut;
			}
		}
		// 2. hide optional ones, lowest priority first
		List<Integer> hidden = new ArrayList<>();
		for (int k = n - 1; k >= 0 && total > Math.max(0, space); k--) {
			int i = order.get(k);
			if (items.get(i).optional()) {
				shown[i] = false;
				hidden.add(i);
				total = total(h, shown, gap);
			}
		}
		hidden.sort(null);
		// 2b. what hiding freed goes back to the shrunk ones, highest priority first
		if (!hidden.isEmpty()) {
			for (int i : order) {
				int free = Math.max(0, space) - total;
				if (free <= 0) {
					break;
				}
				if (shown[i] && h[i] < items.get(i).h()) {
					int add = Math.min(items.get(i).h() - h[i], free);
					h[i] += add;
					total += add;
				}
			}
		}
		// 3. place
		List<Placed> placed = new ArrayList<>();
		int edge = anchor;
		for (int i : order) {
			if (!shown[i]) {
				continue;
			}
			int y = down ? edge : edge - h[i];
			placed.add(new Placed(i, y, h[i], h[i] < items.get(i).h()));
			edge = down ? y + h[i] + gap : y - gap;
		}
		return new Result(List.copyOf(placed), List.copyOf(hidden), total > Math.max(0, space), total);
	}

	private static int total(int[] h, boolean[] shown, int gap) {
		int t = 0;
		int c = 0;
		for (int i = 0; i < h.length; i++) {
			if (shown[i]) {
				t += h[i];
				c++;
			}
		}
		return c == 0 ? 0 : t + gap * (c - 1);
	}
}
