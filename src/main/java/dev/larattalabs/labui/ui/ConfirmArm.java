package dev.larattalabs.labui.ui;

import org.jspecify.annotations.Nullable;

/**
 * Two-press confirm timing, keyed (pure, unit-tested in {@code ConfirmArmTest}). The first press on a key arms it; a
 * second press on the same key within the window confirms. Pressing another key re-arms on that key; Esc or a click
 * anywhere else cancels ({@link #cancel}, {@link #clickedElsewhere}). Keyed, so one instance serves a list of rows
 * (Architect's per-site OwnedConfirm) as well as one button (CrateScreen's Deconstruct). A confirm by keyboard
 * ({@link #pressByKey}) also follows {@link UiRules#keyConfirmReady}: no OS key repeat, and at least
 * {@link UiRules#KEY_CONFIRM_MS} after arming, so a held or double-tapped key never confirms.
 */
public final class ConfirmArm {
	/** The default window: long enough to read the armed label, short enough to forget a stale arm. */
	public static final long DEFAULT_WINDOW_MS = 5_000;

	public enum Result {
		/** The first press: now armed (show the countdown). */
		ARMED,
		/** The second press within the window: go ahead. */
		CONFIRMED,
		/** A key press that does not count (a repeat, or too soon after arming). */
		IGNORED
	}

	private final long windowMs;
	private @Nullable String key;
	private long armedAt;

	public ConfirmArm() {
		this(DEFAULT_WINDOW_MS);
	}

	public ConfirmArm(long windowMs) {
		this.windowMs = Math.max(1, windowMs);
	}

	public long windowMs() {
		return windowMs;
	}

	/** A press by mouse (or any input without repeats). */
	public Result press(String key, long now) {
		if (armed(key, now)) {
			cancel();
			return Result.CONFIRMED;
		}
		arm(key, now);
		return Result.ARMED;
	}

	/** A press by keyboard: {@code repeat} = an OS key repeat or a key held since the screen opened. */
	public Result pressByKey(String key, long now, boolean repeat) {
		if (armed(key, now)) {
			if (!UiRules.keyConfirmReady(armedAt, now, repeat)) {
				return Result.IGNORED;
			}
			cancel();
			return Result.CONFIRMED;
		}
		if (repeat) {
			return Result.IGNORED;
		}
		arm(key, now);
		return Result.ARMED;
	}

	private void arm(String key, long now) {
		this.key = key;
		this.armedAt = Math.max(1, now);
	}

	/** Whether {@code key} is armed right now (the next press confirms). */
	public boolean armed(String key, long now) {
		return this.key != null && this.key.equals(key) && UiRules.secondPress(armedAt, now, windowMs);
	}

	/** The armed key, or null (none, or the window ran out). */
	public @Nullable String armedKey(long now) {
		return key != null && UiRules.secondPress(armedAt, now, windowMs) ? key : null;
	}

	public long remainingMs(long now) {
		return armedKey(now) == null ? 0 : Math.max(0, armedAt + windowMs - now);
	}

	/** Whole seconds left, rounded up, for "Press again to delete (4)". */
	public int secondsLeft(long now) {
		return (int) ((remainingMs(now) + 999) / 1000);
	}

	public void cancel() {
		key = null;
		armedAt = 0;
	}

	/** A click landed on {@code clickedKey} (null = on nothing armable): anything else armed is cancelled. */
	public void clickedElsewhere(@Nullable String clickedKey) {
		if (key != null && !key.equals(clickedKey)) {
			cancel();
		}
	}
}
