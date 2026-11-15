package dev.larattalabs.labui.mixin.client;

import java.util.List;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads the chat's wrapped lines (newest first), so BOTTOM_CENTER can keep clear of the visible ones (lab-ui 0.5.0; {@code VanillaHud}). */
@Mixin(ChatComponent.class)
public interface ChatComponentAccess {
	@Accessor("trimmedMessages")
	List<GuiMessage.Line> labui$trimmedMessages();
}
