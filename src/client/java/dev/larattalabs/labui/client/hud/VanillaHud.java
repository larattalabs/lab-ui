package dev.larattalabs.labui.client.hud;

import dev.larattalabs.labui.LabUi;
import dev.larattalabs.labui.mixin.client.BossHealthOverlayAccess;
import dev.larattalabs.labui.mixin.client.ToastInstanceAccess;
import dev.larattalabs.labui.mixin.client.ToastManagerAccess;
import dev.larattalabs.labui.ui.HudLayout;
import dev.larattalabs.labui.ui.VanillaHudMath;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Reads where vanilla draws its own top-of-screen pieces this frame, for {@link HudZones} (0.3.0): boss bars, effect icons,
 * toasts (top right) and the "Now playing" toast (top left). Toasts and boss bars have no public accessor in 26.3, so
 * lab-ui's accessor mixins ({@code lab_ui.client.mixins.json}, not required) read them; when a read fails (a renamed field
 * in a later Minecraft) that piece counts as absent, logged once, and the zones start where they did in 0.2.
 */
final class VanillaHud {
	private static boolean toastsFailed;
	private static boolean bossFailed;

	private VanillaHud() {
	}

	static HudLayout.Vanilla read(Minecraft mc, int guiHeight) {
		return new HudLayout.Vanilla(boss(mc, guiHeight), effects(mc), toasts(mc), nowPlaying(mc));
	}

	private static int boss(Minecraft mc, int guiHeight) {
		if (bossFailed) {
			return 0;
		}
		try {
			int n = ((BossHealthOverlayAccess) mc.gui.hud.getBossOverlay()).labui$events().size();
			return VanillaHudMath.bossBottom(n, guiHeight);
		} catch (RuntimeException | LinkageError e) {
			bossFailed = true;
			LabUi.LOGGER.warn("lab_ui: can't read vanilla's boss bars; the top-centre HUD zone ignores them", e);
			return 0;
		}
	}

	private static int effects(Minecraft mc) {
		if (mc.player == null) {
			return 0;
		}
		int good = 0;
		int other = 0;
		for (MobEffectInstance e : mc.player.getActiveEffects()) {
			if (!e.showIcon()) {
				continue;
			}
			if (e.getEffect().value().isBeneficial()) {
				good++;
			} else {
				other++;
			}
		}
		return VanillaHudMath.effectsBottom(good, other, mc.isDemo());
	}

	private static int toasts(Minecraft mc) {
		if (toastsFailed) {
			return 0;
		}
		try {
			List<int[]> boxes = new ArrayList<>();
			for (Object o : ((ToastManagerAccess) mc.gui.toastManager()).labui$visibleToasts()) {
				ToastInstanceAccess t = (ToastInstanceAccess) o;
				if (t.labui$visiblePortion() <= 0f) {
					continue;
				}
				Toast toast = t.labui$toast();
				boxes.add(new int[] {(int) Math.ceil(toast.yPos(t.labui$firstSlotIndex())), toast.height()});
			}
			return VanillaHudMath.toastsBottom(boxes);
		} catch (RuntimeException | LinkageError e) {
			toastsFailed = true;
			LabUi.LOGGER.warn("lab_ui: can't read vanilla's toasts; the toast HUD zone ignores them", e);
			return 0;
		}
	}

	private static int nowPlaying(Minecraft mc) {
		if (toastsFailed) {
			return 0;
		}
		try {
			Object inst = ((ToastManagerAccess) mc.gui.toastManager()).labui$nowPlayingToast();
			if (inst == null || !mc.options.musicToast().get().renderToast()
				|| mc.gui.screen() instanceof PauseScreen) {
				// (the pause screen draws it itself)
				return 0;
			}
			ToastInstanceAccess t = (ToastInstanceAccess) inst;
			if (t.labui$visiblePortion() <= 0f) {
				return 0;
			}
			Toast toast = t.labui$toast();
			return (int) Math.ceil(toast.yPos(0)) + toast.height();
		} catch (RuntimeException | LinkageError e) {
			toastsFailed = true;
			LabUi.LOGGER.warn("lab_ui: can't read vanilla's toasts; the HUD zones ignore them", e);
			return 0;
		}
	}
}
