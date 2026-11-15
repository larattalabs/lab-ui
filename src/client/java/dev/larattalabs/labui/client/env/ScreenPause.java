package dev.larattalabs.labui.client.env;

import dev.larattalabs.labui.ui.UiRules;
import org.jspecify.annotations.Nullable;

/**
 * Whether a mod's screens pause a singleplayer game (0.5.0; moved from Architect's and Steward's {@code ScreenPause}, contract C6
 * of Architect's docs/FIXWAVE.md): like vanilla menus, so a survival or Hardcore world never runs on behind a screen. Dev runs and
 * clients with the DevBridge on keep the world running; {@code overrideEnv} (e.g. {@code ARCHITECT_PAUSE=0|1}) overrides both, and
 * {@link #set} changes it at runtime (QA). One instance per mod; each screen's {@code isPauseScreen()} returns {@link #pauses()}.
 * The rule is {@link UiRules#screensPause}.
 */
public final class ScreenPause {
	private final boolean devBridge;
	private final String overrideEnv;
	private volatile boolean pauses;

	public ScreenPause(boolean devBridge, String overrideEnv) {
		this.devBridge = devBridge;
		this.overrideEnv = overrideEnv;
		this.pauses = byEnvironment();
	}

	public boolean pauses() {
		return pauses;
	}

	/** QA: force pausing on/off for this session (null = back to the environment's default). */
	public void set(@Nullable Boolean on) {
		pauses = on != null ? on : byEnvironment();
	}

	private boolean byEnvironment() {
		Boolean override = ClientEnv.raw(overrideEnv) == null ? null : ClientEnv.flag(overrideEnv, true);
		return UiRules.screensPause(ClientEnv.DEV_RUN, devBridge, override);
	}
}
