package dev.larattalabs.labui.client.prefs;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.larattalabs.labui.LabUi;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;

/**
 * (0.6.0) lab-ui's own client preferences in {@code <gameDir>/config/lab_ui.json}, shared by every mod that bundles lab-ui:
 * <ul>
 * <li>{@code tips.hidden[]}: the "what's this" lines hidden with their ×, by key, for mods that use the default store
 * ({@link #tips()}). Architect keeps its own in {@code architect_mc_ui.json}.</li>
 * <li>{@code ghostPalette}: {@code standard} (default) or {@code okabe_ito} ({@code GhostPalette}, PR A1).</li>
 * <li>{@code textScale}: 100 (default), 125 or 150 ({@code TextScale}, PR A3).</li>
 * </ul>
 * Unknown keys are kept on save.
 */
public final class LabUiPrefs {
	private static JsonObject root;
	private static final Set<String> hidden = new LinkedHashSet<>();

	private LabUiPrefs() {
	}

	static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("lab_ui.json");
	}

	private static synchronized JsonObject load() {
		if (root != null) {
			return root;
		}
		root = new JsonObject();
		try {
			if (Files.exists(file())) {
				root = JsonParser.parseString(Files.readString(file(), StandardCharsets.UTF_8)).getAsJsonObject();
			}
			if (root.has("tips") && root.getAsJsonObject("tips").has("hidden")) {
				root.getAsJsonObject("tips").getAsJsonArray("hidden").forEach(e -> hidden.add(e.getAsString()));
			}
		} catch (Exception e) {
			LabUi.LOGGER.warn("Could not read {}; lab-ui's preferences stay at their defaults", file(), e);
			root = new JsonObject();
		}
		return root;
	}

	private static synchronized void save() {
		JsonObject o = load();
		JsonObject tips = new JsonObject();
		JsonArray a = new JsonArray();
		hidden.forEach(a::add);
		tips.add("hidden", a);
		o.add("tips", tips);
		try {
			Files.createDirectories(file().getParent());
			Files.writeString(file(), new GsonBuilder().setPrettyPrinting().create().toJson(o), StandardCharsets.UTF_8);
		} catch (Exception e) {
			LabUi.LOGGER.warn("Could not write {}", file(), e);
		}
	}

	public static synchronized String string(String key, String dflt) {
		JsonObject o = load();
		return o.has(key) ? o.get(key).getAsString() : dflt;
	}

	public static synchronized int integer(String key, int dflt) {
		JsonObject o = load();
		try {
			return o.has(key) ? o.get(key).getAsInt() : dflt;
		} catch (Exception e) {
			return dflt;
		}
	}

	public static synchronized void set(String key, String value) {
		load().addProperty(key, value);
		save();
	}

	public static synchronized void set(String key, int value) {
		load().addProperty(key, value);
		save();
	}

	/** (PR A1) The stored ghost palette (default standard). */
	public static dev.larattalabs.labui.ui.GhostPalette.Palette ghostPalette() {
		return dev.larattalabs.labui.ui.GhostPalette.Palette.of(string("ghostPalette", "standard"));
	}

	/** (PR A1) Sets, stores and applies the ghost palette (every mod's ghosts follow it). */
	public static void setGhostPalette(dev.larattalabs.labui.ui.GhostPalette.Palette p) {
		set("ghostPalette", p.id());
		dev.larattalabs.labui.ui.GhostPalette.use(p);
	}

	/** (PR A1) Applies the stored settings for this session; a consumer calls it at client init (idempotent). */
	public static void applyStored() {
		dev.larattalabs.labui.ui.GhostPalette.use(ghostPalette());
		dev.larattalabs.labui.ui.TextScale.prefer(integer("textScale", 100));
	}

	/** The default tips store (lab-ui's file). */
	public static dev.larattalabs.labui.client.help.WhatsThis.Store tips() {
		return TIPS;
	}

	private static final dev.larattalabs.labui.client.help.WhatsThis.Store TIPS = new dev.larattalabs.labui.client.help.WhatsThis.Store() {
		@Override
		public boolean hidden(String key) {
			synchronized (LabUiPrefs.class) {
				load();
				return hidden.contains(key);
			}
		}

		@Override
		public void hide(String key) {
			synchronized (LabUiPrefs.class) {
				load();
				if (hidden.add(key)) {
					save();
				}
			}
		}

		@Override
		public void showAll() {
			synchronized (LabUiPrefs.class) {
				load();
				hidden.clear();
				save();
			}
		}
	};
}
