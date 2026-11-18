package dev.larattalabs.labui.ui;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * (0.6.0, Architect PR A3) The text size: 100, 125 or 150 % ({@code textScale} in {@code config/lab_ui.json}). Text runs and line boxes
 * scale; the screens' own layout reflows to the measured text (wrapping and ellipsizing where it doesn't fit). It applies at every GUI
 * scale. Pure rules here; the client side scales the glyphs and the measurements while an opted-in screen draws ({@link #activate}).
 * <ul>
 * <li><b>Per-mod opt-in</b> (Steward, S-PR-1): a screen is scaled only when its owner mod opted in ({@link #optIn}); every mod is out
 * by default. Architect opts in at client init. {@link #preference()} lets a mod read the setting itself.</li>
 * <li><b>Line boxes:</b> {@link #lh(int)} is a line advance at the active scale (10 px at 100 % is 15 px at 150 %).</li>
 * <li>HUDs stay unscaled.</li>
 * </ul>
 */
public final class TextScale {
	public static final List<Integer> STEPS = List.of(100, 125, 150);
	private static final Set<String> OPTED_IN = ConcurrentHashMap.newKeySet();
	private static volatile int preference = 100;

	/** What applies to one screen now: the percent drawn and whether the preference was lowered to fit. */
	public record Result(int percent, boolean dropped) {
		public float factor() {
			return percent / 100f;
		}
	}

	private TextScale() {
	}

	public static void optIn(String modId) {
		OPTED_IN.add(modId);
	}

	public static void optOut(String modId) {
		OPTED_IN.remove(modId);
	}

	public static boolean optedIn(String modId) {
		return modId != null && OPTED_IN.contains(modId);
	}

	/** The player's setting (any mod may read it and decide on its own). */
	public static int preference() {
		return preference;
	}

	/** Sets the session's preference (snapped to a step; the client's prefs persist it). */
	public static void prefer(int percent) {
		preference = snap(percent);
	}

	public static int snap(int percent) {
		int best = 100;
		for (int s : STEPS) {
			if (Math.abs(s - percent) < Math.abs(best - percent)) {
				best = s;
			}
		}
		return best;
	}

	/** The next step (100 -> 125 -> 150 -> 100), for a cycling control. */
	public static int next(int percent) {
		int i = STEPS.indexOf(snap(percent));
		return STEPS.get((i + 1) % STEPS.size());
	}

	/** The scale for a screen owned by {@code owner} with preference {@code pref}: the preference when opted in, else 100 %. */
	public static Result resolve(String owner, int pref, int guiW, int guiH) {
		return optedIn(owner) ? new Result(snap(pref), false) : new Result(100, false);
	}

	// ------------------------------------------------------------------ the active scale (render thread, while a scaled screen draws)

	private static float active = 1f;
	private static boolean drawing;

	/** The factor text is drawn and measured at now (1 outside an opted-in screen's draw). */
	public static float activeFactor() {
		return active;
	}

	/** Sets the active factor (the screen host, around a draw); returns the previous one, to restore. */
	public static float activate(float f) {
		float was = active;
		active = f <= 0 ? 1f : f;
		return was;
	}

	/** True while a scaled glyph run is being emitted (its own measurements are not scaled again). */
	public static boolean drawing() {
		return drawing;
	}

	public static void drawing(boolean on) {
		drawing = on;
	}

	/** A line advance of {@code px} at the active scale (rounded). */
	public static int lh(int px) {
		return active == 1f ? px : Math.round(px * active);
	}

	/** A width in screen px as the unscaled text width it holds ({@code px / factor}), for wrapping and cutting. */
	public static int unscaled(int px) {
		return active == 1f ? px : Math.max(1, (int) Math.floor(px / active));
	}

	/** The same with the session's preference. */
	public static Result resolve(String owner, int guiW, int guiH) {
		return resolve(owner, preference, guiW, guiH);
	}
}
