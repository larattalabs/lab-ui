package dev.larattalabs.labui.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * (KLW) Keycap labels, pure: a key's name kept whole ({@link #readable}) and cut only by measured width where the caller knows its
 * room ({@link #fit}), ending with {@link #ELLIPSIS} and never empty. Before KLW, {@code KeyLabels.readable} cut every name at 9
 * characters ("Mouse Button 4" showed as "Mouse But"). Same rules as Architect's {@code PlacingKeys.fit} / {@code fitHints} (PR3).
 * Widths come from the caller ({@code font::width} on the client), so the cut follows the active text size.
 */
public final class KeyFit {
	/** The ellipsis a fitted label ends with. */
	public static final String ELLIPSIS = "…";

	private KeyFit() {
	}

	/**
	 * A key's name as a keycap shows it: punctuation whose glyph is only a pixel or two in the Minecraft font is spelled out, so a
	 * keycap never looks empty. Long names ("Mouse Button 4", a localized name) stay whole; {@link #fit} cuts them by width.
	 */
	public static String readable(String s) {
		if (s == null) {
			return "";
		}
		return switch (s) {
			case "`" -> "Backtick";
			case "'" -> "Quote";
			case "´" -> "Accent";
			case "," -> "Comma";
			case "." -> "Period";
			case ";" -> "Semicolon";
			case ":" -> "Colon";
			case "|" -> "Bar";
			default -> s;
		};
	}

	/**
	 * {@code s} when it is at most {@code max} wide, else its longest prefix (whole code points, trailing spaces dropped) that fits
	 * with {@link #ELLIPSIS} after it. Never empty for a non-empty {@code s}: when not even one character fits with the ellipsis, the
	 * first character alone. Null is "".
	 */
	public static String fit(String s, int max, ToIntFunction<String> width) {
		if (s == null || s.isEmpty() || width.applyAsInt(s) <= max) {
			return s == null ? "" : s;
		}
		String best = null;
		int end = s.offsetByCodePoints(0, 1);
		while (end < s.length()) {
			String head = s.substring(0, end).stripTrailing();
			if (!head.isEmpty()) {
				String cand = head + ELLIPSIS;
				if (width.applyAsInt(cand) <= max) {
					best = cand;
				} else if (best != null) {
					break;
				}
			}
			end = s.offsetByCodePoints(end, 1);
		}
		return best != null ? best : s.substring(0, s.offsetByCodePoints(0, 1));
	}

	/**
	 * A "key, verb, key, verb…" hint row with each key fitted so its pair fits {@code inner} on its own (keycap padding
	 * {@code capPad}, then 3 px, then the verb, as {@code UiBits.hints} draws it); keys that already fit are unchanged. A new array;
	 * the verbs are kept.
	 */
	public static String[] fitHints(String[] keyThenVerb, int inner, int capPad, ToIntFunction<String> width) {
		String[] o = keyThenVerb.clone();
		for (int i = 0; i + 1 < o.length; i += 2) {
			o[i] = fit(o[i], inner - capPad - 3 - width.applyAsInt(o[i + 1]), width);
		}
		return o;
	}

	/**
	 * The "?" overlay's rows with each key fitted to a key column {@code keyCol} wide (the keycap's label room is
	 * {@code keyCol - capPad}), so no keycap reaches the verb. A new list; the verbs are kept.
	 */
	public static List<HelpKeys.Row> fitRows(List<HelpKeys.Row> rows, int keyCol, int capPad, ToIntFunction<String> width) {
		List<HelpKeys.Row> out = new ArrayList<>(rows.size());
		for (HelpKeys.Row r : rows) {
			out.add(new HelpKeys.Row(fit(r.key(), keyCol - capPad, width), r.verb()));
		}
		return out;
	}
}
