package dev.larattalabs.labui.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Every semantic text token against every paper surface it is drawn on, from ui-style.json itself (ui/polish-1). */
class PaperContrastTest {
	static final String[] TEXT = {"error", "warn", "ok", "muted", "link", "clay_text", "placeholder"};

	@Test
	void everyTextTokenPassesOnEveryPaperSurfaceAndTheRecordedMinimumIsTrue() throws Exception {
		JsonObject root = OnDarkContrastTest.style();
		JsonObject tokens = root.getAsJsonObject("semantic");
		JsonObject recorded = root.getAsJsonObject("semantic_contrast");
		JsonObject surfaces = recorded.getAsJsonObject("_surfaces");
		assertEquals(6, surfaces.size());
		for (String name : TEXT) {
			int fg = Contrast.hex(tokens.get(name).getAsString());
			double min = Double.MAX_VALUE;
			for (Map.Entry<String, com.google.gson.JsonElement> s : surfaces.entrySet()) {
				min = Math.min(min, Contrast.ratio(fg, Contrast.surface(s.getValue().getAsString())));
			}
			assertTrue(min >= 4.5, name + " is " + min + ":1 on its worst paper surface, needs 4.5");
			double rec = recorded.get(name).getAsDouble();
			assertTrue(rec <= min + 1e-9 && min - rec < 0.01, "recorded minimum of " + name + " is " + rec + ", actual " + min);
		}
	}

	@Test
	void theClayWashSelectionFailsMutedTextSoRowsUseTheSelectedFill() throws Exception {
		JsonObject tokens = OnDarkContrastTest.style().getAsJsonObject("semantic");
		int muted = Contrast.hex(tokens.get("muted").getAsString());
		int inset = Contrast.hex("#E9E1D3");
		int wash = Contrast.over(Contrast.hex("#B4553A"), 0x30 / 255.0, inset);
		assertTrue(Contrast.ratio(muted, wash) < 4.5, "the 0x30 clay wash over the inset");
		assertTrue(Contrast.ratio(muted, Contrast.hex(tokens.get("selected").getAsString())) >= 4.5, "semantic.selected");
	}

	@Test
	void clayTextIsClayFamilyButNotTheFillClay() throws Exception {
		JsonObject tokens = OnDarkContrastTest.style().getAsJsonObject("semantic");
		int a = Contrast.hex(tokens.get("clay_text").getAsString());
		int r = (a >> 16) & 0xFF;
		int gr = (a >> 8) & 0xFF;
		int b = a & 0xFF;
		assertTrue(r > gr && gr > b, "a warm red-brown");
		// CLAY_DARK as text is 4.26:1 on paper and 3.76:1 on the inset: the reason this token exists
		assertTrue(Contrast.ratio(Contrast.hex("#B4553A"), Contrast.hex("#E9E1D3")) < 4.5);
	}
}
