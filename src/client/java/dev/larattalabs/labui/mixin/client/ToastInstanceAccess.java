package dev.larattalabs.labui.mixin.client;

import net.minecraft.client.gui.components.toasts.Toast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads one of vanilla's (package-private) toast instances: its toast, first slot and how far it has slid in. */
@Mixin(targets = "net.minecraft.client.gui.components.toasts.ToastManager$ToastInstance")
public interface ToastInstanceAccess {
	@Accessor("toast")
	Toast labui$toast();

	@Accessor("firstSlotIndex")
	int labui$firstSlotIndex();

	@Accessor("visiblePortion")
	float labui$visiblePortion();
}
