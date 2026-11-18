package dev.larattalabs.labui.ui;

import java.util.List;

/**
 * (0.6.0, Architect PR K1) lab-ui's default key codes as plain GLFW key numbers, so a consumer's unit test can check its own
 * defaults against them without a Minecraft bootstrap. {@code JobBoard} registers its mapping from {@link #JOBS}.
 * <p>
 * The reserved set: keys another Laratta Labs mod binds by default, which no lab-ui or Architect default may take (Steward,
 * S-PR-2: Y inspect, I inbox, U survey).
 */
public final class KeyDefaults {
	/** GLFW_KEY_K: the Jobs &amp; spend panel ({@code key.lab_ui.jobs}). */
	public static final int JOBS = 75;
	/** GLFW_KEY_Y, GLFW_KEY_I, GLFW_KEY_U: Steward's inspect, inbox and survey defaults. */
	public static final List<Integer> STEWARD_RESERVED = List.of(89, 73, 85);

	private KeyDefaults() {
	}

	/** lab-ui's own global defaults (acting with no screen open). */
	public static List<Integer> globals() {
		return List.of(JOBS);
	}
}
