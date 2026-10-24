package dev.larattalabs.labui.mixin.client;

import java.util.Map;
import net.minecraft.client.gui.components.BossHealthOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads how many boss bars vanilla shows, for the top-centre zone (lab-ui 0.3.0; {@code VanillaHud}). */
@Mixin(BossHealthOverlay.class)
public interface BossHealthOverlayAccess {
	@Accessor("events")
	Map<?, ?> labui$events();
}
