package dev.larattalabs.labui.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * (0.6.0, Architect PR A1) The Okabe-Ito ghost palette under protan, deutan and tritan simulation (Machado 2009, full severity): every
 * pair seen together by one renderer is at least ΔE76 15 apart (blocked = refused on purpose); the two-tone outline is at least 3:1
 * (measured 4.4) on every terrain sample. The standard palette's numbers are printed for the record, not judged.
 */
class GhostPaletteTest {
	static final double MIN_DE = 15;

	@Test
	void okabeItoPairsStayApartUnderEveryVision() {
		for (String group : List.of("ghost", "region")) {
			double m = GhostPalette.minDeltaE(GhostPalette.Palette.OKABE_ITO, group);
			System.out.printf("ghost palette %s: okabe_ito min ΔE %.1f, standard min ΔE %.1f%n", group, m, GhostPalette.minDeltaE(GhostPalette.Palette.STANDARD,
				group));
			assertTrue(m >= MIN_DE, group + ": " + m);
		}
	}

	@Test
	void theStandardPaletteIsTodaysAndFailsWhereTheContractSays() {
		// blocked vs refused 3/1 under deutan/protan (left out: equal on purpose in okabe), OK outline vs foundation under 10
		assertTrue(Cvd.deltaE(0xE0302A, 0xD0402A, Cvd.Kind.PROTAN) < 4);
		assertTrue(Cvd.minDeltaE(GhostPalette.Cat.OK_OUTLINE.standard, GhostPalette.Cat.FOUNDATION.standard) < 10);
		assertTrue(GhostPalette.minDeltaE(GhostPalette.Palette.STANDARD, "region") < MIN_DE, "region added vs path is near-equal today");
		assertEquals(0x8FA98B, GhostPalette.rgb(GhostPalette.Palette.STANDARD, GhostPalette.Cat.OK_OUTLINE));
	}

	@Test
	void theOutlineReadsOnEveryTerrain() {
		List<String> low = new ArrayList<>();
		double min = Double.MAX_VALUE;
		for (int t : GhostPalette.TERRAIN) {
			double c = GhostPalette.outlineContrast(t);
			min = Math.min(min, c);
			if (c < 3) {
				low.add(Integer.toHexString(t));
			}
		}
		System.out.printf("two-tone outline: min %.2f:1 over %d terrain samples%n", min, GhostPalette.TERRAIN.size());
		assertTrue(low.isEmpty(), "under 3:1 on " + low);
		assertTrue(min >= 4.4, "measured " + min);
	}

	@Test
	void huesAloneDoNotReachThreeToOneEverywhere() {
		// why the outline needs its core and rim: no single hue, drawn at the outline's alpha, is 3:1 on every sample
		for (int hue : new int[] {GhostPalette.Cat.OK_OUTLINE.okabe, GhostPalette.Cat.REFUSED.okabe, GhostPalette.Cat.OK_OUTLINE.standard}) {
			double worst = Double.MAX_VALUE;
			for (int t : GhostPalette.TERRAIN) {
				worst = Math.min(worst, Contrast.ratio(Contrast.over(hue, 0xE0 / 255.0, t), t));
			}
			assertTrue(worst < 3, Integer.toHexString(hue) + " " + worst);
		}
	}

	@Test
	void cuesAndPaletteSwitch() {
		assertEquals(GhostPalette.Cue.DASHED, GhostPalette.Cat.REFUSED.cue);
		assertEquals(GhostPalette.Cue.CROSS, GhostPalette.Cat.BLOCKED.cue);
		assertEquals(GhostPalette.Cue.BANG, GhostPalette.Cat.HAZARD.cue);
		assertEquals(GhostPalette.Cue.CORNER_TICKS, GhostPalette.Cat.OBSTRUCTED.cue);
		GhostPalette.use(GhostPalette.Palette.OKABE_ITO);
		assertEquals(0xD55E00, GhostPalette.rgb(GhostPalette.Cat.BLOCKED));
		assertTrue(GhostPalette.twoTone());
		GhostPalette.use(null);
		assertEquals(GhostPalette.Palette.STANDARD, GhostPalette.current());
		assertEquals(GhostPalette.Palette.OKABE_ITO, GhostPalette.Palette.of("okabe_ito"));
	}

	@Test
	void dashesCoverTheEdgeWithGaps() {
		float[] d = GhostCues.dashes(4f);
		assertEquals(0f, d[0]);
		assertTrue(d[d.length - 1] <= 4f);
		assertEquals(10, d.length); // 5 dashes of 0.5 every 0.8 over 4 blocks
		int[] n = {0};
		GhostCues.cross((a, b, c, e, f, g) -> n[0]++, 0, 0, 0);
		assertEquals(14, n[0]);
	}
}
