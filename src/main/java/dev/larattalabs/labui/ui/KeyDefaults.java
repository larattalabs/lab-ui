package dev.larattalabs.labui.ui;

import java.util.List;

/**
 * (0.6.0, Architect PR K1) lab-ui's default key codes as plain numbers (26.3's {@code InputConstants.KEY_*}: SDL scancodes, not GLFW), so a consumer's unit test can check its own
 * defaults against them without a Minecraft bootstrap. {@code JobBoard} registers its mapping from {@link #JOBS}.
 * <p>
 * The reserved set: keys another Laratta Labs mod binds by default, which no lab-ui or Architect default may take (Steward,
 * S-PR-2: Y inspect, I inbox, U survey).
 */
public final class KeyDefaults {
	/** {@code InputConstants.KEY_K}: the Jobs &amp; spend panel ({@code key.lab_ui.jobs}). */
	public static final int JOBS = 14;
	/** {@code InputConstants.KEY_Y}, {@code KEY_I}, {@code KEY_U}: Steward's inspect, inbox and survey defaults. */
	public static final List<Integer> STEWARD_RESERVED = List.of(28, 12, 24);

	private KeyDefaults() {
	}

	/** lab-ui's own global defaults (acting with no screen open). */
	public static List<Integer> globals() {
		return List.of(JOBS);
	}
}
