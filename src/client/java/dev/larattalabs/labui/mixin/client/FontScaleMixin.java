package dev.larattalabs.labui.mixin.client;

import dev.larattalabs.labui.ui.TextScale;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * (0.6.0, Architect PR A3) While an opted-in screen draws, text is measured at its drawn size (widths times the factor) and cut or
 * wrapped to widths divided by it, so layouts built on {@code font.width} and lab-ui's {@code TextUtil} reflow to the bigger text.
 */
@Mixin(Font.class)
public abstract class FontScaleMixin {
	private static boolean labui$scaling() {
		return TextScale.activeFactor() != 1f && !TextScale.drawing();
	}

	private static int labui$up(int w) {
		return (int) Math.ceil(w * TextScale.activeFactor());
	}

	@Inject(method = "width(Ljava/lang/String;)I", at = @At("RETURN"), cancellable = true)
	private void labui$widthString(String s, CallbackInfoReturnable<Integer> cir) {
		if (labui$scaling()) {
			cir.setReturnValue(labui$up(cir.getReturnValue()));
		}
	}

	@Inject(method = "width(Lnet/minecraft/network/chat/FormattedText;)I", at = @At("RETURN"), cancellable = true)
	private void labui$widthText(FormattedText s, CallbackInfoReturnable<Integer> cir) {
		if (labui$scaling()) {
			cir.setReturnValue(labui$up(cir.getReturnValue()));
		}
	}

	@Inject(method = "width(Lnet/minecraft/util/FormattedCharSequence;)I", at = @At("RETURN"), cancellable = true)
	private void labui$widthSeq(FormattedCharSequence s, CallbackInfoReturnable<Integer> cir) {
		if (labui$scaling()) {
			cir.setReturnValue(labui$up(cir.getReturnValue()));
		}
	}

	@ModifyVariable(method = "plainSubstrByWidth(Ljava/lang/String;IZ)Ljava/lang/String;", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private int labui$cutWidth3(int w) {
		return labui$scaling() ? TextScale.unscaled(w) : w;
	}

	@ModifyVariable(method = "plainSubstrByWidth(Ljava/lang/String;I)Ljava/lang/String;", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private int labui$cutWidth2(int w) {
		return labui$scaling() ? TextScale.unscaled(w) : w;
	}

	@ModifyVariable(method = "split(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private int labui$splitWidth(int w) {
		return labui$scaling() ? TextScale.unscaled(w) : w;
	}
}
