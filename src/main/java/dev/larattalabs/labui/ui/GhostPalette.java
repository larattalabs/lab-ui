package dev.larattalabs.labui.ui;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * (0.6.0, Architect PR A1) The in-world ghost colours, by category, in two palettes, plus each category's second, non-colour cue.
 * Pure; renderers ask {@link #rgb(Cat)} (the player's palette, {@link #current()}) and keep their own alpha.
 * <ul>
 * <li>{@link Palette#STANDARD}: today's colours (the default, N-PR-4).</li>
 * <li>{@link Palette#OKABE_ITO}: Okabe &amp; Ito's colour-blind-safe set, with a two-tone outline ({@link #CORE} and {@link #RIM}) so the
 * outline reads on every terrain. Blocked equals refused on purpose: the cue splits them.</li>
 * </ul>
 * The cue is on in both palettes: refused outlines are dashed, blocked cells get an ×, hazards a "!", obstructed cells corner ticks.
 * Set from Architect's Status tab; stored as {@code ghostPalette} in {@code config/lab_ui.json}; Steward's ghosts follow it.
 */
public final class GhostPalette {
	public enum Palette {
		STANDARD, OKABE_ITO;

		public String id() {
			return name().toLowerCase(Locale.ROOT);
		}

		public static Palette of(String s) {
			for (Palette p : values()) {
				if (p.id().equalsIgnoreCase(s)) {
					return p;
				}
			}
			return STANDARD;
		}

		public String label() {
			return this == STANDARD ? "Standard" : "Colour-blind safe (Okabe-Ito)";
		}
	}

	/** The second cue a category carries. */
	public enum Cue {
		NONE, DASHED, CROSS, BANG, CORNER_TICKS
	}

	/** Where a colour is used. {@link #group} names the renderer whose colours are seen together. */
	public enum Cat {
		// the placement ghost (Architect GhostRenderer) and its HUD swatches
		OK_OUTLINE("ghost", 0x8FA98B, 0x0072B2, Cue.NONE),
		REFUSED("ghost", 0xD0402A, 0xD55E00, Cue.DASHED),
		BLOCKED("ghost", 0xE0302A, 0xD55E00, Cue.CROSS),
		OBSTRUCTED("ghost", 0xE0782A, 0xE69F00, Cue.CORNER_TICKS),
		WATER("ghost", 0x3C78E6, 0x56B4E9, Cue.NONE),
		LAVA("ghost", 0xFFB000, 0xF0E442, Cue.BANG),
		HAZARD("ghost", 0xD040C8, 0xCC79A7, Cue.BANG),
		PATH("ghost", 0xC8A060, 0xC8A060, Cue.NONE),
		FOUNDATION("ghost", 0x989490, 0x5C5C5C, Cue.NONE),
		CLEARED("ghost", 0xF4EBD8, 0xF0F0F0, Cue.NONE),
		// the region ghost (Architect RegionGhost)
		REGION_ADDED("region", 0x3CCB5A, 0x009E73, Cue.NONE),
		REGION_PATH("region", 0xC8A060, 0xC8A060, Cue.NONE),
		REGION_LOT("region", 0x4A90E2, 0x0072B2, Cue.NONE),
		REGION_FLOATING("region", 0xB070E0, 0xCC79A7, Cue.NONE),
		REGION_REFUSED("region", 0xE0302A, 0xD55E00, Cue.DASHED),
		// a construction site's next cells (Architect SiteGhostRenderer)
		SITE_NEXT("site", 0x5FD068, 0x56B4E9, Cue.NONE),
		// version and composite previews: neutral in both
		DELTA("delta", 0xD8D8D8, 0xD8D8D8, Cue.NONE),
		COMPOSITE("composite", 0xB0B0B0, 0xB0B0B0, Cue.NONE),
		// (0.6.0, appended) the composite and region previews' changed and kept cells; their added and removed cells use REGION_ADDED and
		// REGION_REFUSED, so the region plan's outline and removed mesh follow the palette too
		COMPOSITE_CHANGED("region", 0xF2A21C, 0xE69F00, Cue.NONE),
		COMPOSITE_KEPT("region", 0xF5E11E, 0xF0E442, Cue.NONE);

		public final String group;
		public final int standard;
		public final int okabe;
		public final Cue cue;

		Cat(String group, int standard, int okabe, Cue cue) {
			this.group = group;
			this.standard = standard;
			this.okabe = okabe;
			this.cue = cue;
		}
	}

	/** The two-tone outline (Okabe-Ito): a dark core and a white rim; the better of the two is at least 4.4:1 on every terrain sample. */
	public static final int CORE = 0x111111;
	public static final int RIM = 0xFFFFFF;
	/** Pairs that are equal on purpose (the cue tells them apart). */
	public static final Set<Set<Cat>> SAME_ON_PURPOSE = Set.of(EnumSet.of(Cat.REFUSED, Cat.BLOCKED));
	/**
	 * Approximate top-face averages of common terrain (grass, stone, sand, dirt, snow, water, deepslate, leaves, netherrack), for the
	 * outline's contrast check.
	 */
	public static final List<Integer> TERRAIN = List.of(0x5E9D34, 0x7D7D7D, 0xDBCFA3, 0x866043, 0xF4F8F8, 0x3F76E4, 0x505053, 0x3B6E1F, 0x6F3634);

	private static volatile Palette current = Palette.STANDARD;

	private GhostPalette() {
	}

	public static Palette current() {
		return current;
	}

	/** Sets the palette for this session (the client's prefs persist it). */
	public static void use(Palette p) {
		current = p == null ? Palette.STANDARD : p;
	}

	public static int rgb(Palette p, Cat c) {
		return p == Palette.OKABE_ITO ? c.okabe : c.standard;
	}

	/** The category's colour in the player's palette, as 0xRRGGBB. */
	public static int rgb(Cat c) {
		return rgb(current, c);
	}

	/** The category's colour in the player's palette at {@code alpha} (0..255), as ARGB. */
	public static int argb(Cat c, int alpha) {
		return (alpha & 0xFF) << 24 | rgb(c);
	}

	/** True when the player's palette draws the two-tone outline. */
	public static boolean twoTone() {
		return current == Palette.OKABE_ITO;
	}

	/** The smallest pair ΔE (any vision) within each renderer group of {@code p}; pairs equal on purpose are left out. */
	public static double minDeltaE(Palette p, String group) {
		double m = Double.MAX_VALUE;
		Cat[] cs = Cat.values();
		for (int i = 0; i < cs.length; i++) {
			for (int j = i + 1; j < cs.length; j++) {
				if (!cs[i].group.equals(group) || !cs[j].group.equals(group) || SAME_ON_PURPOSE.contains(EnumSet.of(cs[i], cs[j]))) {
					continue;
				}
				m = Math.min(m, Cvd.minDeltaE(rgb(p, cs[i]), rgb(p, cs[j])));
			}
		}
		return m;
	}

	/** The better of the outline's core and rim against {@code terrain}, as a WCAG ratio. */
	public static double outlineContrast(int terrain) {
		return Math.max(Contrast.ratio(CORE, terrain), Contrast.ratio(RIM, terrain));
	}
}
