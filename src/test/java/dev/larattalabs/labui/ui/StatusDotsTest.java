package dev.larattalabs.labui.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * (0.6.2, Steward LUI3) The Okabe-Ito status dots: the six families stay apart under protan, deutan and tritan simulation (Machado 2009,
 * full severity), the paper set is at least 3:1 on every paper surface, the colours match ui-style.json, and every sprite exists.
 */
class StatusDotsTest {
	static final GhostPalette.Palette STD = GhostPalette.Palette.STANDARD;
	static final GhostPalette.Palette OKABE = GhostPalette.Palette.OKABE_ITO;
	static final String SPRITES = "/assets/lab_ui/textures/gui/sprites/kit/";

	@Test
	void okabeDotsStayApartUnderEveryVisionAndTodaysDoNot() {
		double dark = StatusDots.minDeltaE(OKABE, false);
		double paper = StatusDots.minDeltaE(OKABE, true);
		System.out.printf("status dots min ΔE: okabe dark %.1f, okabe paper %.1f; standard dark %.1f, standard paper %.1f%n", dark, paper,
			StatusDots.minDeltaE(STD, false), StatusDots.minDeltaE(STD, true));
		assertTrue(dark >= 15, "okabe dark " + dark);
		assertTrue(paper >= 9.5, "okabe paper " + paper);
		// the reason for the opt-in: the standard paper dots' thinking and waiting are under 3 apart for some vision
		assertTrue(StatusDots.minDeltaE(STD, true) < 5);
	}

	@Test
	void everyOkabePaperDotIsAtLeastThreeToOneOnEveryPaperSurface() throws Exception {
		JsonObject surfaces = OnDarkContrastTest.style().getAsJsonObject("semantic_contrast").getAsJsonObject("_surfaces");
		for (String fam : StatusDots.FAMILIES) {
			int fg = StatusDots.rgb(OKABE, fam, true);
			for (Map.Entry<String, com.google.gson.JsonElement> s : surfaces.entrySet()) {
				double r = Contrast.ratio(fg, Contrast.surface(s.getValue().getAsString()));
				assertTrue(r >= 3.0, fam + " okabe paper dot is " + r + ":1 on " + s.getKey());
			}
		}
	}

	@Test
	void theColoursAreUiStylesBlocks() throws Exception {
		JsonObject root = OnDarkContrastTest.style();
		check(root.getAsJsonObject("status"), STD, false);
		check(root.getAsJsonObject("status_paper"), STD, true);
		check(root.getAsJsonObject("status_okabe"), OKABE, false);
		check(root.getAsJsonObject("status_okabe_paper"), OKABE, true);
	}

	private static void check(JsonObject block, GhostPalette.Palette p, boolean paper) {
		for (String fam : StatusDots.FAMILIES) {
			assertEquals(Contrast.hex(block.get(fam).getAsString()), StatusDots.rgb(p, fam, paper), p + " " + fam + (paper ? " paper" : ""));
		}
	}

	@Test
	void theStandardNamesAreTodaysAndEveryOkabeSpriteExists() {
		assertEquals("dot_working", StatusDots.sprite("working", STD, false, false));
		assertEquals("dot_working_halo", StatusDots.sprite("Working", STD, true, false));
		assertEquals("dot_working_paper", StatusDots.sprite("working", null, false, true));
		assertEquals("dot_error_okabe_halo", StatusDots.sprite("error", OKABE, true, false));
		for (String fam : StatusDots.FAMILIES) {
			for (GhostPalette.Palette p : GhostPalette.Palette.values()) {
				for (String name : new String[] {StatusDots.sprite(fam, p, false, false), StatusDots.sprite(fam, p, true, false),
					StatusDots.sprite(fam, p, false, true)}) {
					assertTrue(StatusDotsTest.class.getResource(SPRITES + name + ".png") != null, name);
				}
			}
		}
	}

	@Test
	void anUnknownFamilyIsIdle() {
		assertEquals(StatusDots.rgb(OKABE, "idle", false), StatusDots.rgb(OKABE, "nonsense", false));
		assertEquals(0x9C9488, StatusDots.rgb(STD, null, false));
	}
}
