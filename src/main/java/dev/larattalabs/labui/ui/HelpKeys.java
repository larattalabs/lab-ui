package dev.larattalabs.labui.ui;

import java.util.List;

/**
 * (0.6.0, Architect PR W1) The "?" overlay's pure rules: which key opens it, the rows it lists, the "what's this" line's limit.
 */
public final class HelpKeys {
	/** 26.3's {@code InputConstants.KEY_SLASH} (Shift+/ is "?" on a US layout), {@code KEY_F1}, {@code KEY_ESCAPE} (SDL scancodes). */
	public static final int SLASH = 56;
	public static final int F1 = 58;
	public static final int ESCAPE = 41;
	/** A "what's this" line is one line: at most 90 characters. */
	public static final int MAX_TIP = 90;

	/** The overlay footer's close hint, the part every mod shares (0.6.2). */
	public static final String CLOSE_HINT = "Esc or ? closes";
	/** Where the overlay's default wording sends the player for every term: Architect's Status tab (0.6.2). */
	public static final String DEFAULT_HELP_PLACE = "Status > Help";
	/** The overlay's default footer, today's text: "Esc or ? closes · Status > Help lists every term" (0.6.2). */
	public static final String DEFAULT_FOOTER = footer(DEFAULT_HELP_PLACE);

	/** One row of the overlay: the key's label (read live, so a rebound key shows) and what it does. */
	public record Row(String key, String verb) {
	}

	private HelpKeys() {
	}

	/**
	 * True when this press toggles the overlay: Shift+/ ("?") or F1. A focused text field (search, names, notes) takes "?" as a
	 * character, so it never toggles there; F1 still does.
	 */
	public static boolean toggles(int key, boolean shift, boolean textFocused) {
		if (key == F1) {
			return true;
		}
		return key == SLASH && shift && !textFocused;
	}

	/** True when this press closes an open overlay: the toggle keys or Esc (a text field cannot be focused under it). */
	public static boolean closes(int key, boolean shift) {
		return key == ESCAPE || toggles(key, shift, false);
	}

	/** The footer naming where every term is listed: "Esc or ? closes · {place} lists every term"; null or blank: the close hint alone (0.6.2). */
	public static String footer(String place) {
		return place == null || place.isBlank() ? CLOSE_HINT : CLOSE_HINT + " · " + place + " lists every term";
	}

	/** The line under the terms that did not fit: "+{n} more in {place}"; null or blank place: "+{n} more" (0.6.2). */
	public static String moreTerms(int n, String place) {
		return "+" + n + " more" + (place == null || place.isBlank() ? "" : " in " + place);
	}

	/** The smallest hit area of a small close control, each side (0.6.2): 20 x 20, as a kit button is tall. */
	public static final int MIN_HIT = 20;

	/**
	 * (0.6.2) The "what's this" ×'s hit area as {x0, y0, x1, y1} (ends exclusive), for a glyph drawn at {@code glyphX} and {@code glyphW}
	 * wide on a line from {@code lineY}, {@code lineH} tall. It keeps the 0.6.0 box (6 px left of the glyph to 12 px right of its x, 2 px
	 * above and below the line) and grows it to at least {@link #MIN_HIT} each way: leftwards (the line's text is not clickable) and upwards
	 * (a screen's title strip, never the controls under the line). The glyph does not move.
	 */
	public static int[] closeHit(int glyphX, int glyphW, int lineY, int lineH) {
		int x1 = glyphX + Math.max(12, glyphW + 6);
		int x0 = Math.min(glyphX - 6, x1 - MIN_HIT);
		int y1 = lineY + lineH + 2;
		int y0 = Math.min(lineY - 2, y1 - MIN_HIT);
		return new int[] {x0, y0, x1, y1};
	}

	/** True when ({@code mx}, {@code my}) is inside a {@link #closeHit} box. */
	public static boolean inside(int[] box, double mx, double my) {
		return mx >= box[0] && mx < box[2] && my >= box[1] && my < box[3];
	}

	/** Every tip must fit one line. */
	public static boolean tipFits(String tip) {
		return tip != null && !tip.isBlank() && tip.length() <= MAX_TIP;
	}

	/** The rows with the overlay's own row last ("?" close), as every overlay shows it. */
	public static List<Row> withClose(List<Row> rows) {
		java.util.ArrayList<Row> out = new java.util.ArrayList<>(rows);
		out.add(new Row("? / F1", "close this"));
		return out;
	}
}
