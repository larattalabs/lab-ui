package dev.larattalabs.labui.ui;

/**
 * Scrollbar geometry (pure, unit-tested in {@code ScrollMathTest}): where the thumb sits for an offset and which offset a
 * dragged thumb or a track click means. Units are whatever the caller scrolls in (rows or pixels), as long as
 * {@code content}, {@code view} and {@code offset} agree. The thumb size matches {@code Panels.scrollbar}.
 */
public final class ScrollMath {
	/** The least thumb height (GUI px), as {@code Panels.scrollbar} draws it. */
	public static final int MIN_THUMB = 8;

	private ScrollMath() {
	}

	/** A thumb: {@code y} from the track top, height {@code h}. */
	public record Thumb(int y, int h) {
		public boolean contains(int trackY, double my) {
			return my >= trackY + y && my < trackY + y + h;
		}
	}

	public static int maxOffset(int content, int view) {
		return Math.max(0, content - view);
	}

	/** The thumb for a track of {@code trackH} px; a full-height thumb when everything fits. */
	public static Thumb thumb(int trackH, int content, int view, int offset) {
		int max = maxOffset(content, view);
		if (max == 0 || trackH <= 0) {
			return new Thumb(0, Math.max(0, trackH));
		}
		int h = Math.min(trackH, Math.max(MIN_THUMB, (int) Math.round((double) trackH * view / content)));
		int travel = trackH - h;
		int o = Math.max(0, Math.min(offset, max));
		return new Thumb((int) Math.round(travel * (double) o / max), h);
	}

	/**
	 * The offset for a thumb dragged so its top is at {@code thumbTop} px from the track top (the drag keeps the point
	 * where it was grabbed under the mouse: {@code thumbTop = mouseY - trackY - grabDy}).
	 */
	public static int offsetForThumbTop(int trackH, int content, int view, int thumbTop) {
		int max = maxOffset(content, view);
		Thumb t = thumb(trackH, content, view, 0);
		int travel = trackH - t.h();
		if (max == 0 || travel <= 0) {
			return 0;
		}
		double f = Math.max(0, Math.min(1, thumbTop / (double) travel));
		return (int) Math.round(f * max);
	}

	/** A click on the track outside the thumb pages one view towards the click: the new offset. */
	public static int pageTowards(int trackH, int content, int view, int offset, int clickY) {
		Thumb t = thumb(trackH, content, view, offset);
		int max = maxOffset(content, view);
		if (clickY < t.y()) {
			return Math.max(0, offset - view);
		}
		if (clickY >= t.y() + t.h()) {
			return Math.min(max, offset + view);
		}
		return offset;
	}
}
