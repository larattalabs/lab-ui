package dev.larattalabs.labui.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Every semantic_on_dark token against every surface it is drawn on, from ui-style.json itself (lab-ui 0.3.0). */
class OnDarkContrastTest {
	/** Marks (rings, bars) need 3:1; everything else is text and needs 4.5:1. Surfaces have no minimum. */
	static final Set<String> MARKS = Set.of("focus", "bar");
	static final Set<String> SURFACES = Set.of("panel", "panel_edge", "rule");

	static JsonObject style() throws Exception {
		try (InputStream in = OnDarkContrastTest.class.getResourceAsStream("/assets/lab_ui/gui/ui-style.json")) {
			assertNotNull(in, "ui-style.json on the test classpath");
			return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	@Test
	void everyTokenPassesOnEverySurfaceAndTheRecordedMinimumIsTrue() throws Exception {
		JsonObject root = style();
		JsonObject tokens = root.getAsJsonObject("semantic_on_dark");
		JsonObject recorded = root.getAsJsonObject("semantic_on_dark_contrast");
		JsonObject surfaces = recorded.getAsJsonObject("_surfaces");
		assertTrue(surfaces.size() >= 7);
		int checked = 0;
		for (Map.Entry<String, com.google.gson.JsonElement> t : tokens.entrySet()) {
			String name = t.getKey();
			if (name.startsWith("_") || SURFACES.contains(name)) {
				continue;
			}
			int fg = Contrast.hex(t.getValue().getAsString());
			double min = Double.MAX_VALUE;
			for (Map.Entry<String, com.google.gson.JsonElement> s : surfaces.entrySet()) {
				min = Math.min(min, Contrast.ratio(fg, Contrast.surface(s.getValue().getAsString())));
			}
			double need = MARKS.contains(name) ? 3.0 : 4.5;
			assertTrue(min >= need, name + " is " + min + ":1 on its worst surface, needs " + need);
			assertTrue(recorded.has(name), "semantic_on_dark_contrast records " + name);
			double rec = recorded.get(name).getAsDouble();
			assertTrue(rec <= min + 1e-9 && min - rec < 0.01, "recorded minimum of " + name + " is " + rec + ", actual " + min);
			checked++;
		}
		assertEquals(9, checked);
	}

	@Test
	void warnAndErrorDifferInHue() throws Exception {
		JsonObject tokens = style().getAsJsonObject("semantic_on_dark");
		double e = hue(Contrast.hex(tokens.get("error").getAsString()));
		double w = hue(Contrast.hex(tokens.get("warn").getAsString()));
		assertTrue(Math.abs(w - e) >= 20, "warn " + w + " vs error " + e);
	}

	@Test
	void thePaperTokensAreStillTooDarkOnInk() throws Exception {
		// the reason this set exists: semantic.error is about 2:1 on the HUD panel
		JsonObject sem = style().getAsJsonObject("semantic");
		assertFalse(Contrast.ratio(Contrast.hex(sem.get("error").getAsString()), 0x2B2927) >= 4.5);
	}

	static double hue(int rgb) {
		double r = ((rgb >> 16) & 0xFF) / 255.0;
		double g = ((rgb >> 8) & 0xFF) / 255.0;
		double b = (rgb & 0xFF) / 255.0;
		double max = Math.max(r, Math.max(g, b));
		double min = Math.min(r, Math.min(g, b));
		double d = max - min;
		if (d == 0) {
			return 0;
		}
		double h = max == r ? ((g - b) / d) % 6 : max == g ? (b - r) / d + 2 : (r - g) / d + 4;
		return (h * 60 + 360) % 360;
	}
}
