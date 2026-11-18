package dev.larattalabs.labui.mixin.client;

import dev.larattalabs.labui.ui.TextScale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * (0.6.0, Architect PR A3) While an opted-in screen draws ({@link TextScale#activeFactor()} != 1), every text run is drawn scaled about
 * its own top-left corner. Every {@code text(...)} overload ends in this one.
 */
@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiTextScaleMixin {
	@Shadow
	public abstract Matrix3x2fStack pose();

	@Shadow
	public abstract void text(Font font, FormattedCharSequence s, int x, int y, int color, boolean shadow);

	@Inject(method = "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V", at = @At("HEAD"), cancellable = true)
	private void labui$scaleText(Font font, FormattedCharSequence s, int x, int y, int color, boolean shadow, CallbackInfo ci) {
		float f = TextScale.activeFactor();
		if (f == 1f || TextScale.drawing()) {
			return;
		}
		ci.cancel();
		TextScale.drawing(true);
		Matrix3x2fStack p = pose();
		p.pushMatrix();
		try {
			p.translate(x, y);
			p.scale(f, f);
			text(font, s, 0, 0, color, shadow);
		} finally {
			p.popMatrix();
			TextScale.drawing(false);
		}
	}
}
