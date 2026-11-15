package dev.larattalabs.labui.client.ui;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.KeyEvent;

/**
 * Keycap labels for key mappings (0.5.0; moved from Architect's and Steward's {@code hud/Keys} label helper, identical in both).
 * Each mod still registers its own mappings.
 */
public final class KeyLabels {
	private KeyLabels() {
	}

	/**
	 * Short label of the key a mapping is bound to ("B", "Enter", "Backtick"), "?" when unbound. Punctuation keys whose glyph is only
	 * a pixel or two in the Minecraft font are spelled out, so a keycap never looks empty.
	 */
	public static String label(KeyMapping k) {
		if (k == null || k.isUnbound()) {
			return "?";
		}
		return readable(k.getTranslatedKeyMessage().getString());
	}

	/** Spell out tiny punctuation glyphs; cut long names to 9 characters. */
	public static String readable(String s) {
		String word = switch (s) {
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
		return word.length() > 9 ? word.substring(0, 9) : word;
	}

	public static boolean matches(KeyMapping k, KeyEvent e) {
		return k != null && k.matches(e);
	}
}
