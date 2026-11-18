package dev.larattalabs.labui.ui;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * (0.6.0, Architect PR A3) The text size: 100, 125 or 150 % ({@code textScale} in {@code config/lab_ui.json}). Pure rules:
 * <ul>
 * <li><b>Per-mod opt-in</b> (Steward, S-PR-1): a screen is scaled only when its owner mod opted in ({@link #optIn}); every mod is out
 * by default. Architect opts in at client init; Steward decides later. {@link #preference()} lets a mod read the setting itself.</li>
 * <li><b>Clamp:</b> the screen is laid out at its GUI size divided by the scale, so it reflows; that logical size must stay at least
 * {@link #MIN_W} x {@link #MIN_H} (vanilla's own floor for a GUI scale). Otherwise the scale drops a step, down to 100 %, and
 * {@link Result#dropped()} says so (Status shows it).</li>
 * </ul>
 */
public final class TextScale {
	public static final List<Integer> STEPS = List.of(100, 125, 150);
	public static final int MIN_W = 320;
	public static final int MIN_H = 240;
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

	/**
	 * The scale for a screen owned by {@code owner}, {@code guiW} x {@code guiH} at the current GUI scale, with preference {@code pref}.
	 * Not opted in: 100 %, never dropped.
	 */
	public static Result resolve(String owner, int pref, int guiW, int guiH) {
		if (!optedIn(owner)) {
			return new Result(100, false);
		}
		int want = snap(pref);
		for (int i = STEPS.indexOf(want); i > 0; i--) {
			int p = STEPS.get(i);
			if (Math.floor(guiW * 100.0 / p) >= MIN_W && Math.floor(guiH * 100.0 / p) >= MIN_H) {
				return new Result(p, p != want);
			}
		}
		return new Result(100, want != 100);
	}

	/** The same with the session's preference. */
	public static Result resolve(String owner, int guiW, int guiH) {
		return resolve(owner, preference, guiW, guiH);
	}
}
