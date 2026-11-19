package dev.larattalabs.labui.client.ui;

import dev.larattalabs.labui.ui.KeyFit;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
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

	/**
	 * Spell out tiny punctuation glyphs ({@link KeyFit#readable}). (KLW) Long names stay whole ("Mouse Button 4"); before KLW they
	 * were cut to 9 characters. Where the room is known, cut by width with {@link #fit}.
	 */
	public static String readable(String s) {
		return KeyFit.readable(s);
	}

	/**
	 * (KLW) {@code label} cut by measured width so its keycap ({@code UiBits.keycap}, with the keycap padding) is at most
	 * {@code maxCapW} wide, ending with "…", never empty ({@link KeyFit#fit}).
	 */
	public static String fit(Font font, String label, int maxCapW) {
		Kit.Padding p = Kit.padding("keycap");
		return KeyFit.fit(label, maxCapW - p.left() - p.right(), font::width);
	}

	/** (KLW) A hint row ({@code UiBits.hints}) with every key fitted so each pair fits {@code inner} alone ({@link KeyFit#fitHints}). */
	public static String[] fitHints(Font font, String[] keyThenVerb, int inner) {
		Kit.Padding p = Kit.padding("keycap");
		return KeyFit.fitHints(keyThenVerb, inner, p.left() + p.right(), font::width);
	}

	public static boolean matches(KeyMapping k, KeyEvent e) {
		return k != null && k.matches(e);
	}
}
