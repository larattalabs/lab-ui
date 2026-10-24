package dev.larattalabs.labui.ui;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Tooltip timing and placement (pure, unit-tested in {@code TooltipMathTest}). */
public final class TooltipMath {
	/** The default hover delay before a tooltip shows. */
	public static final long DEFAULT_DELAY_MS = 450;
	/** Offset from the cursor. */
	public static final int CURSOR_GAP = 10;

	private TooltipMath() {
	}

	/** Shows a tooltip once the same thing has been hovered for {@code delayMs}; a new key restarts the wait. */
	public static final class Delay {
		private final long delayMs;
		private @Nullable Object key;
		private long since;

		public Delay(long delayMs) {
			this.delayMs = Math.max(0, delayMs);
		}

		/** Report what is hovered now (null = nothing); true when its tooltip should show. */
		public boolean update(@Nullable Object hovered, long now) {
			if (!Objects.equals(hovered, key)) {
				key = hovered;
				since = now;
			}
			return key != null && now - since >= delayMs;
		}

		public void reset() {
			key = null;
		}
	}

	public record Pos(int x, int y) {
	}

	/**
	 * Top-left of a {@code w} x {@code h} tooltip for a cursor at {@code mx, my}: right of and below the cursor, flipped
	 * to the left / above when it would leave the screen, then clamped inside {@code margin} of every edge (a tooltip
	 * larger than the screen sits at the top-left margin).
	 */
	public static Pos place(int mx, int my, int w, int h, int screenW, int screenH, int margin) {
		int x = mx + CURSOR_GAP;
		if (x + w > screenW - margin) {
			x = mx - CURSOR_GAP - w;
		}
		int y = my + CURSOR_GAP;
		if (y + h > screenH - margin) {
			y = my - CURSOR_GAP / 2 - h;
		}
		x = Math.max(margin, Math.min(x, screenW - margin - w));
		y = Math.max(margin, Math.min(y, screenH - margin - h));
		return new Pos(x, y);
	}
}
