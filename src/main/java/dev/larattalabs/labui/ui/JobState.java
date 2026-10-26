package dev.larattalabs.labui.ui;

/** A {@link JobRow}'s state (0.4.0). The panel groups them: Needs you, Running (with queued), Held (with paused_budget), Ended. */
public enum JobState {
	NEEDS_YOU, RUNNING, QUEUED, HELD, PAUSED_BUDGET, DONE, FAILED, CANCELLED;

	public boolean ended() {
		return this == DONE || this == FAILED || this == CANCELLED;
	}

	/** Lower case with spaces: "needs you", "paused budget". */
	public String label() {
		return name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
	}
}
