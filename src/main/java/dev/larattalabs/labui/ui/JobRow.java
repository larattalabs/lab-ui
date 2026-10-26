package dev.larattalabs.labui.ui;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * One row of the shared Jobs &amp; spend panel (0.4.0): a Claude job, a settlement build, a talk or an offer, published by
 * the mod that owns it ({@code JobBoard.publish(mod, rows)}). The panel only shows rows; every decision is the owning
 * mod's, through the callbacks it supplies ({@link #link}, {@link #approve}, {@link #cancel}, {@link #raise}), and the
 * row changes when that mod publishes again.
 *
 * <p>Ids are global across mods: use {@code <modid>:<thing>} ({@code architect_mc:group/g12},
 * {@code steward_mc:settlement/7}). A row whose {@link #parentId} names another published row folds under it (Architect
 * jobs owned by {@code steward_mc:settlement/<id>} under that settlement's row); while the parent is not published the row
 * stands on its own. Times are epoch ms ({@code System.currentTimeMillis()}). Build rows with {@link #builder}.
 *
 * @param mod        the publishing mod's id (set by the board on publish)
 * @param kind       a short kind label: design, massing, set, bible, settlement, talk, part offer, …
 * @param step       what it is doing or waiting for now, one line
 * @param needsYou   it waits for the player (counts on the needs-you pill even when {@code state} is not NEEDS_YOU)
 * @param budgetUsd  its budget, or 0 for none
 * @param budgetKind what the budget is: job, set, build, allowance ("" for none)
 * @param resetAt    when a usage limit holding it resets (epoch ms), 0 when nothing holds it
 * @param linkLabel  where {@link #link} goes, for the detail pane ("Architect · Designs tab")
 * @param endedAt    0 while not ended
 */
public record JobRow(String id, String mod, String kind, String title, JobState state, String step, boolean needsYou, double spentUsd,
	double budgetUsd, String budgetKind, @Nullable String heldReason, long resetAt, @Nullable Runnable link, @Nullable String linkLabel,
	long createdAt, long endedAt, @Nullable String parentId, @Nullable Action approve, @Nullable Action cancel, @Nullable Action raise) {

	/** An owner-supplied action: its button label and what it does. */
	public record Action(String label, Runnable run) {
	}

	public JobRow {
		Objects.requireNonNull(id, "id");
		mod = mod == null ? "" : mod;
		kind = kind == null ? "" : kind;
		title = title == null ? "" : title;
		state = state == null ? JobState.RUNNING : state;
		step = step == null ? "" : step;
		budgetKind = budgetKind == null ? "" : budgetKind;
	}

	/** Waits for the player: the flag, or the NEEDS_YOU state (an ended row never does). */
	public boolean waiting() {
		return !state.ended() && (needsYou || state == JobState.NEEDS_YOU);
	}

	public boolean hasBudget() {
		return budgetUsd > 0;
	}

	/** The same row published by {@code mod}. */
	public JobRow withMod(String mod) {
		return new JobRow(id, mod, kind, title, state, step, needsYou, spentUsd, budgetUsd, budgetKind, heldReason, resetAt, link, linkLabel,
			createdAt, endedAt, parentId, approve, cancel, raise);
	}

	public static Builder builder(String id, String kind, String title, JobState state) {
		return new Builder(id, kind, title, state);
	}

	/** Builds a {@link JobRow}; everything but id, kind, title and state is optional. */
	public static final class Builder {
		private final String id;
		private final String kind;
		private final String title;
		private final JobState state;
		private String step = "";
		private boolean needsYou;
		private double spent;
		private double budget;
		private String budgetKind = "";
		private @Nullable String heldReason;
		private long resetAt;
		private @Nullable Runnable link;
		private @Nullable String linkLabel;
		private long createdAt;
		private long endedAt;
		private @Nullable String parentId;
		private @Nullable Action approve;
		private @Nullable Action cancel;
		private @Nullable Action raise;

		private Builder(String id, String kind, String title, JobState state) {
			this.id = id;
			this.kind = kind;
			this.title = title;
			this.state = state;
		}

		public Builder step(String s) {
			step = s;
			return this;
		}

		public Builder needsYou(boolean b) {
			needsYou = b;
			return this;
		}

		public Builder spent(double usd) {
			spent = usd;
			return this;
		}

		/** The budget in USD and what kind it is (job, set, build, allowance). */
		public Builder budget(double usd, String kind) {
			budget = usd;
			budgetKind = kind;
			return this;
		}

		/** Why it is held (shown in the detail pane) and when the limit resets (epoch ms, 0 = unknown). */
		public Builder held(@Nullable String reason, long resetAtMs) {
			heldReason = reason;
			resetAt = resetAtMs;
			return this;
		}

		/** Opens the owning mod's screen at this item; the panel closes first. */
		public Builder link(String label, Runnable open) {
			linkLabel = label;
			link = open;
			return this;
		}

		public Builder created(long ms) {
			createdAt = ms;
			return this;
		}

		public Builder ended(long ms) {
			endedAt = ms;
			return this;
		}

		public Builder parent(@Nullable String id) {
			parentId = id;
			return this;
		}

		public Builder approve(String label, Runnable r) {
			approve = new Action(label, r);
			return this;
		}

		/** Cancel, through the panel's two-press confirm. */
		public Builder cancel(String label, Runnable r) {
			cancel = new Action(label, r);
			return this;
		}

		public Builder raise(String label, Runnable r) {
			raise = new Action(label, r);
			return this;
		}

		public JobRow build() {
			return new JobRow(id, "", kind, title, state, step, needsYou, spent, budget, budgetKind, heldReason, resetAt, link, linkLabel, createdAt,
				endedAt, parentId, approve, cancel, raise);
		}
	}
}
