package dev.larattalabs.labui.client.ui;

import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

/**
 * A screen that returns to the screen it was opened from on Esc (0.5.0; moved from Architect and Steward, which each carried an
 * identical copy). QA reads it through each mod's {@code dev.state} "ui.parent".
 */
public interface HasParent {
	@Nullable Screen parent();
}
