package dev.larattalabs.labui.client.picker;

import dev.larattalabs.labui.LabUi;
import dev.larattalabs.labui.ui.PickerMemory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.fabricmc.loader.api.FabricLoader;
import org.jspecify.annotations.Nullable;

/**
 * Recents and favourites of every picker, and the open state of every {@code CollapsibleSection}, in
 * {@code config/lab_ui/pickers.json} ({@link PickerMemory}). Loaded on first
 * use, written (atomically) after each change. Client thread.
 */
public final class PickerStore {
	private static @Nullable PickerMemory memory;

	private PickerStore() {
	}

	public static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("lab_ui").resolve("pickers.json");
	}

	public static PickerMemory memory() {
		if (memory == null) {
			PickerMemory m = new PickerMemory();
			try {
				Path f = file();
				if (Files.isRegularFile(f)) {
					m = PickerMemory.fromJson(Files.readString(f, StandardCharsets.UTF_8));
				}
			} catch (IOException e) {
				LabUi.LOGGER.warn("lab_ui: could not read {}", file(), e);
			}
			memory = m;
		}
		return memory;
	}

	/** Records a pick and saves. */
	public static void used(String picker, String id) {
		memory().used(picker, id);
		save();
	}

	/** Remembers a collapsible section's open state and saves. */
	public static void section(String key, boolean open) {
		memory().section(key, open);
		save();
	}

	/** Toggles a favourite and saves; returns the new state. */
	public static boolean toggleFavourite(String picker, String id) {
		boolean on = memory().toggleFavourite(picker, id);
		save();
		return on;
	}

	public static void save() {
		try {
			Path f = file();
			Files.createDirectories(f.getParent());
			Path tmp = f.resolveSibling("pickers.json.tmp");
			Files.writeString(tmp, memory().toJson(), StandardCharsets.UTF_8);
			Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			LabUi.LOGGER.warn("lab_ui: could not write {}", file(), e);
		}
	}
}
