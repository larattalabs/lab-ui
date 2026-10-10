package dev.larattalabs.labui;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * lab-ui, the shared client UI library of the Laratta Labs mods (README.md). A library mod: no entrypoint, nothing runs
 * until a consumer calls it. Its sprites and style files live under the {@code lab_ui} namespace.
 */
public final class LabUi {
	public static final String MOD_ID = "lab_ui";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private LabUi() {
	}

	/** {@code lab_ui:<path>}. */
	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
