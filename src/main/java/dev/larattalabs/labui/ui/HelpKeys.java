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
