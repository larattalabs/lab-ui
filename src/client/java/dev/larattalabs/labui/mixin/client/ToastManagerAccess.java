package dev.larattalabs.labui.mixin.client;

import java.util.List;
import net.minecraft.client.gui.components.toasts.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads vanilla's visible toasts for the HUD zones (lab-ui 0.3.0; {@code VanillaHud}). Non-required: a miss means no vanilla offset.
 * ({@code nowPlayingToast} is typed with the package-private ToastInstance, which an accessor can't name: VanillaHud reads it by
 * reflection.) */
@Mixin(ToastManager.class)
public interface ToastManagerAccess {
	@Accessor("visibleToasts")
	List<?> labui$visibleToasts();
}
