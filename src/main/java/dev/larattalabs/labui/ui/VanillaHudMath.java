package dev.larattalabs.labui.ui;

import java.util.List;

/**
 * Where vanilla Minecraft 26.3 draws its own HUD pieces that share space with the HUD zones (pure, unit-tested in
 * {@code VanillaHudMathTest}). The constants are read from 26.3's bytecode: {@code BossHealthOverlay.extractRenderState},
 * {@code Hud.extractEffects}, {@code Toast.yPos} / {@code height} and {@code NowPlayingToast}. All values are GUI px; 0
 * means "nothing there".
 */
public final class VanillaHudMath {
	/** Boss bars: the first bar's y, the step between bars (10 + the font's line height 9), the bar's height. */
	public static final int BOSS_TOP = 12;
	public static final int BOSS_STEP = 19;
	public static final int BOSS_BAR_H = 5;
	/** Effect icons: 24 px, 25 px apart from the right edge; the beneficial row at y 1, the others 26 below; demo adds 15. */
	public static final int EFFECT_SIZE = 24;
	public static final int EFFECT_STEP = 25;
	public static final int EFFECT_TOP = 1;
	public static final int EFFECT_ROW = 26;
	public static final int EFFECT_DEMO = 15;
	/** The "Now playing" music toast: top left, 30 tall. */
	public static final int NOW_PLAYING_H = 30;

	private VanillaHudMath() {
	}

	/** The bottom of the last boss bar drawn for {@code bars} bars (vanilla stops once the next bar would start at or below a third of the screen). */
	public static int bossBottom(int bars, int guiHeight) {
		if (bars <= 0) {
			return 0;
		}
		int y = BOSS_TOP;
		int last = y;
		for (int i = 0; i < bars; i++) {
			last = y;
			y += BOSS_STEP;
			if (y >= guiHeight / 3) {
				break;
			}
		}
		return last + BOSS_BAR_H;
	}

	/** The bottom of the effect icons: {@code beneficial} / {@code other} counts of icons shown. */
	public static int effectsBottom(int beneficial, int other, boolean demo) {
		int top = EFFECT_TOP + (demo ? EFFECT_DEMO : 0);
		if (other > 0) {
			return top + EFFECT_ROW + EFFECT_SIZE;
		}
		return beneficial > 0 ? top + EFFECT_SIZE : 0;
	}

	/** The width the effect icons take from the right edge (the longer row). */
	public static int effectsWidth(int beneficial, int other) {
		int n = Math.max(beneficial, other);
		return n <= 0 ? 0 : n * EFFECT_STEP;
	}

	/** The bottom of vanilla's toasts on the right: each {top, height} of a toast still (partly) on screen. */
	public static int toastsBottom(List<int[]> topAndHeight) {
		int b = 0;
		for (int[] t : topAndHeight) {
			b = Math.max(b, t[0] + t[1]);
		}
		return b;
	}
}
