package dev.larattalabs.labui.ui;

import java.util.List;
import java.util.Locale;

/**
 * (0.6.2, Steward LUI3) The status dots' colours in two palettes, and the sprite each draws. Pure.
 * <ul>
 * <li>{@link GhostPalette.Palette#STANDARD}: today's dots ({@code status} and {@code status_paper} in ui-style.json), the default.</li>
 * <li>{@link GhostPalette.Palette#OKABE_ITO}: Okabe &amp; Ito's colour-blind-safe hues (idle grey, thinking yellow, working sky blue,
 * waiting orange, error vermillion, done bluish green), each tuned so the six stay apart under protan, deutan and tritan simulation
 * ({@code status_okabe} and {@code status_okabe_paper}; {@code StatusDotsTest}). The paper set is darkened to at least 3:1 on every
 * paper surface, like the standard paper dots.</li>
 * </ul>
 * Nothing switches by itself: a consumer opts in by passing a palette to {@code Kit.dot}, {@code Kit.paperDot}, {@code Panels.dot},
 * {@code Panels.paperDot}, {@code UiBits.dotPill}, {@code UiBits.pulsingDot} or {@code DisplayDraw.dot}, for example
 * {@link GhostPalette#current()} to follow the player's colour-blind setting.
 */
public final class StatusDots {
	/** The six families, in ui-style.json's order. */
	public static final List<String> FAMILIES = List.of("idle", "thinking", "working", "waiting", "error", "done");

	// the standard dots (ui-style.json status / status_paper)
	private static final int[] STANDARD = {0x9C9488, 0xC9A227, 0x2FA3A0, 0xD97757, 0xC2413B, 0x8FA98B};
	private static final int[] STANDARD_PAPER = {0x847B6E, 0x95781D, 0x278986, 0xCE562F, 0xC2413B, 0x668361};
	// Okabe-Ito, for the dark HUD and the world (the hues lightened or darkened a little to keep the six apart under every vision)
	private static final int[] OKABE = {0xB0B0B0, 0xFFFF4C, 0x63CFFF, 0xF2A700, 0xD55E00, 0x008E68};
	// Okabe-Ito for paper: the same hues darkened to at least 3:1 on every paper surface
	private static final int[] OKABE_PAPER = {0x686868, 0x7D7722, 0x377395, 0x654600, 0xAA4B00, 0x008B65};

	private StatusDots() {
	}

	/** The family's index in {@link #FAMILIES}; an unknown family counts as idle. */
	public static int index(String family) {
		int i = family == null ? -1 : FAMILIES.indexOf(family.toLowerCase(Locale.ROOT));
		return Math.max(0, i);
	}

	/** The dot's fill as 0xRRGGBB: {@code paper} = the paper dot. */
	public static int rgb(GhostPalette.Palette p, String family, boolean paper) {
		int i = index(family);
		if (p == GhostPalette.Palette.OKABE_ITO) {
			return paper ? OKABE_PAPER[i] : OKABE[i];
		}
		return paper ? STANDARD_PAPER[i] : STANDARD[i];
	}

	/**
	 * The sprite name under {@code lab_ui:kit/}: {@code dot_<family>[_okabe][_halo|_paper]}. The standard palette gives today's names
	 * ({@code dot_working}, {@code dot_working_halo}, {@code dot_working_paper}); a null palette is the standard one.
	 */
	public static String sprite(String family, GhostPalette.Palette p, boolean halo, boolean paper) {
		String f = family.toLowerCase(Locale.ROOT);
		String okabe = p == GhostPalette.Palette.OKABE_ITO ? "_okabe" : "";
		return "dot_" + f + okabe + (paper ? "_paper" : halo ? "_halo" : "");
	}

	/** The smallest ΔE76 between any two families of {@code p} under normal, protan, deutan and tritan vision. */
	public static double minDeltaE(GhostPalette.Palette p, boolean paper) {
		double m = Double.MAX_VALUE;
		for (int i = 0; i < FAMILIES.size(); i++) {
			for (int j = i + 1; j < FAMILIES.size(); j++) {
				m = Math.min(m, Cvd.minDeltaE(rgb(p, FAMILIES.get(i), paper), rgb(p, FAMILIES.get(j), paper)));
			}
		}
		return m;
	}
}
