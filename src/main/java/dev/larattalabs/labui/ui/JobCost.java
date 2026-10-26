package dev.larattalabs.labui.ui;

/**
 * One cost-ledger entry (0.4.0): one per distinct Claude job (a sidecar job or object id), what it has cost so far and
 * who owns it. The panel's totals sum the ledger, never the rows, so a job shown under two rows counts once.
 *
 * @param jobId the distinct Claude job (the same id from two mods counts once)
 * @param owner {@code <modid>:<thing>}; its namespace decides the per-mod split ({@code steward_mc:settlement/7} is
 *              Steward's even when Architect publishes it); "" = the publishing mod
 * @param usd   the cost so far (an estimate)
 * @param at    when it was last charged (epoch ms); "today" is read from it
 * @param mod   the publishing mod (set by the board on publish)
 */
public record JobCost(String jobId, String owner, double usd, long at, String mod) {
	public static JobCost of(String jobId, String owner, double usd, long at) {
		return new JobCost(jobId, owner, usd, at, "");
	}

	/** The mod the cost belongs to: the owner's namespace, or the publishing mod. */
	public String ownerMod() {
		int c = owner == null ? -1 : owner.indexOf(':');
		return c > 0 ? owner.substring(0, c) : owner != null && !owner.isEmpty() ? owner : mod;
	}

	public JobCost withMod(String m) {
		return new JobCost(jobId, owner == null ? "" : owner, usd, at, m);
	}
}
