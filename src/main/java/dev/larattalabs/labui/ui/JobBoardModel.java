package dev.larattalabs.labui.ui;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

/**
 * The Jobs &amp; spend panel's registry and logic (0.4.0), pure and unit-tested; {@code client.jobs.JobBoard} holds the one
 * instance. Each mod publishes and replaces its own rows and cost-ledger entries under its mod id; publishing never
 * touches another mod's. Safe to call from any thread (each publish swaps an immutable snapshot and bumps
 * {@link #version()}).
 *
 * <p>The static methods are the panel's logic: which rows are roots and which fold under a parent ({@link #roots},
 * {@link #descendants}), the group of a row ({@link #group}), the flattened list lines with group heads
 * ({@link #lines}), the filter counts, today's totals from the ledger ({@link #totals}), the needs-you count and pill
 * text, and the usage-limit banner time.
 */
public final class JobBoardModel {
	/** Panel groups, in order: Needs you first, then what runs, what a limit holds, what ended. */
	public enum Group {
		NEEDS_YOU("Needs you"), RUNNING("Running"), HELD("Held"), ENDED("Ended");

		public final String label;

		Group(String label) {
			this.label = label;
		}
	}

	/** The filter chips. */
	public enum Filter {
		ALL("All"), NEEDS_YOU("Needs you"), RUNNING("Running"), HELD("Held"), ENDED("Ended");

		public final String label;

		Filter(String label) {
			this.label = label;
		}

		boolean admits(Group g) {
			return this == ALL || name().equals(g.name());
		}
	}

	public enum GroupBy {
		STATE, MOD
	}

	/** One list line: a group head, a root row (depth 0) or a folded-in child (depth 1). Equal by kind and key. */
	public record Line(Kind kind, String key, @Nullable JobRow row, int depth, String label, int count, double usd) {
		public enum Kind {
			HEAD, ROW
		}

		public boolean head() {
			return kind == Kind.HEAD;
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof Line l && l.kind == kind && l.key.equals(key);
		}

		@Override
		public int hashCode() {
			return Objects.hash(kind, key);
		}
	}

	/** Today's spend from the ledger: the total and the split by owning mod (in first-seen order). */
	public record Totals(double total, Map<String, Double> byMod) {
	}

	// ------------------------------------------------------------------ the registry

	private final Map<String, List<JobRow>> rows = new LinkedHashMap<>();
	private final Map<String, List<JobCost>> ledger = new LinkedHashMap<>();
	private final Map<String, String> names = new HashMap<>();
	private final Set<String> hostOnly = new HashSet<>();
	private final AtomicLong version = new AtomicLong();
	private volatile List<JobRow> allRows = List.of();
	private volatile List<JobCost> allCosts = List.of();

	/** Replaces {@code mod}'s rows (each is stamped with {@code mod}). Other mods' rows stay. */
	public synchronized void publish(String mod, List<JobRow> list) {
		List<JobRow> out = new ArrayList<>(list.size());
		for (JobRow r : list) {
			out.add(r.withMod(mod));
		}
		rows.put(mod, List.copyOf(out));
		List<JobRow> all = new ArrayList<>();
		rows.values().forEach(all::addAll);
		allRows = List.copyOf(all);
		version.incrementAndGet();
	}

	/** Replaces {@code mod}'s cost-ledger entries. Other mods' entries stay. */
	public synchronized void publishLedger(String mod, List<JobCost> entries) {
		List<JobCost> out = new ArrayList<>(entries.size());
		for (JobCost c : entries) {
			out.add(c.withMod(mod));
		}
		ledger.put(mod, List.copyOf(out));
		List<JobCost> all = new ArrayList<>();
		ledger.values().forEach(all::addAll);
		allCosts = List.copyOf(all);
		version.incrementAndGet();
	}

	/** The display name of {@code mod} ("Architect"), for "Open in …", the per-mod split and the Mod grouping. */
	public synchronized void name(String mod, String displayName) {
		names.put(mod, displayName);
		version.incrementAndGet();
	}

	public synchronized String nameOf(String mod) {
		String n = names.get(mod);
		return n != null ? n : defaultName(mod);
	}

	/** {@code mod} says its Claude work runs in the host's game (a LAN guest or a dedicated server's client). */
	public synchronized void hostOnly(String mod, boolean b) {
		if (b ? hostOnly.add(mod) : hostOnly.remove(mod)) {
			version.incrementAndGet();
		}
	}

	public synchronized boolean anyHostOnly() {
		return !hostOnly.isEmpty();
	}

	/** Bumped by every change: redraw-or-rebuild check for the panel. */
	public long version() {
		return version.get();
	}

	/** Every published row, mod by mod in first-publish order. */
	public List<JobRow> rows() {
		return allRows;
	}

	public List<JobCost> ledger() {
		return allCosts;
	}

	public @Nullable JobRow row(String id) {
		for (JobRow r : allRows) {
			if (r.id().equals(id)) {
				return r;
			}
		}
		return null;
	}

	/** "architect_mc" → "Architect", "steward_mc" → "Steward": the id before "_mc", capitalised. */
	public static String defaultName(String mod) {
		String s = mod.endsWith("_mc") ? mod.substring(0, mod.length() - 3) : mod;
		s = s.replace('_', ' ');
		return s.isEmpty() ? mod : Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}

	// ------------------------------------------------------------------ tree

	/** The id of the root a row folds under: walks published parents (a missing parent makes the row its own root). */
	public static String rootId(JobRow r, Map<String, JobRow> byId) {
		JobRow cur = r;
		Set<String> seen = new HashSet<>();
		while (cur.parentId() != null && seen.add(cur.id())) {
			JobRow p = byId.get(cur.parentId());
			if (p == null || p.id().equals(cur.id())) {
				break;
			}
			cur = p;
		}
		return cur.id();
	}

	private static Map<String, JobRow> byId(List<JobRow> rows) {
		Map<String, JobRow> m = new LinkedHashMap<>();
		for (JobRow r : rows) {
			m.putIfAbsent(r.id(), r);
		}
		return m;
	}

	/** Rows that stand on their own: no parent, or a parent that is not published (yet). */
	public static List<JobRow> roots(List<JobRow> rows) {
		Map<String, JobRow> m = byId(rows);
		List<JobRow> out = new ArrayList<>();
		for (JobRow r : m.values()) {
			if (rootId(r, m).equals(r.id())) {
				out.add(r);
			}
		}
		return out;
	}

	/** Everything that folds under {@code rootId}, newest first. */
	public static List<JobRow> descendants(List<JobRow> rows, String rootId) {
		Map<String, JobRow> m = byId(rows);
		List<JobRow> out = new ArrayList<>();
		for (JobRow r : m.values()) {
			if (!r.id().equals(rootId) && rootId(r, m).equals(rootId)) {
				out.add(r);
			}
		}
		out.sort(NEWEST_FIRST);
		return out;
	}

	/** A root waits for the player when it does, or anything folded under it does. */
	static boolean rootWaiting(JobRow root, List<JobRow> kids) {
		if (root.state().ended()) {
			return false;
		}
		if (root.waiting()) {
			return true;
		}
		for (JobRow k : kids) {
			if (k.waiting()) {
				return true;
			}
		}
		return false;
	}

	/** The rows folded under {@code rootId} that wait for the player (what lifts a root into Needs you), newest first. */
	public static List<JobRow> waitingUnder(List<JobRow> rows, String rootId) {
		List<JobRow> out = new ArrayList<>();
		for (JobRow k : descendants(rows, rootId)) {
			if (k.waiting()) {
				out.add(k);
			}
		}
		return out;
	}

	/** The group of a row on its own. */
	public static Group group(JobRow r) {
		if (r.waiting()) {
			return Group.NEEDS_YOU;
		}
		return switch (r.state()) {
			case NEEDS_YOU -> Group.NEEDS_YOU;
			case RUNNING, QUEUED -> Group.RUNNING;
			case HELD, PAUSED_BUDGET -> Group.HELD;
			case DONE, FAILED, CANCELLED -> Group.ENDED;
		};
	}

	private static Group rootGroup(JobRow root, List<JobRow> kids) {
		return rootWaiting(root, kids) ? Group.NEEDS_YOU : group(root);
	}

	static final Comparator<JobRow> NEWEST_FIRST = Comparator.comparingLong(JobRow::createdAt).reversed().thenComparing(JobRow::id);

	// ------------------------------------------------------------------ lines, counts

	/** Roots per filter chip ("All 11", "Needs you 2", …). */
	public static Map<Filter, Integer> counts(List<JobRow> rows) {
		Map<Filter, Integer> out = new EnumMap<>(Filter.class);
		for (Filter f : Filter.values()) {
			out.put(f, 0);
		}
		for (JobRow r : roots(rows)) {
			Group g = rootGroup(r, descendants(rows, r.id()));
			out.merge(Filter.ALL, 1, Integer::sum);
			out.merge(Filter.valueOf(g.name()), 1, Integer::sum);
		}
		return out;
	}

	/**
	 * The panel's list: a head per non-empty group, then its roots; a root's children follow it (depth 1) while
	 * {@code expanded} says so for its id. By STATE: groups Needs you, Running, Held, Ended, newest first in each. By MOD:
	 * a group per mod (by display name), its rows by state group, then newest first. A folded group ({@code folded} on
	 * its key) shows its head only. Group keys: {@code state:needs_you} … or {@code mod:<modid>}.
	 */
	public static List<Line> lines(List<JobRow> rows, Filter filter, GroupBy by, Function<String, String> modName, Predicate<String> folded,
		Predicate<String> expanded) {
		Map<String, List<JobRow>> kidsOf = new HashMap<>();
		List<JobRow> roots = roots(rows);
		Map<String, Group> groupOf = new HashMap<>();
		for (JobRow r : roots) {
			List<JobRow> kids = descendants(rows, r.id());
			kidsOf.put(r.id(), kids);
			groupOf.put(r.id(), rootGroup(r, kids));
		}
		LinkedHashMap<String, String> heads = new LinkedHashMap<>();
		Map<String, List<JobRow>> members = new HashMap<>();
		if (by == GroupBy.STATE) {
			for (Group g : Group.values()) {
				heads.put("state:" + g.name().toLowerCase(Locale.ROOT), g.label);
			}
		} else {
			List<String> mods = new ArrayList<>();
			for (JobRow r : roots) {
				if (!mods.contains(r.mod())) {
					mods.add(r.mod());
				}
			}
			mods.sort(Comparator.comparing(m -> modName.apply(m).toLowerCase(Locale.ROOT)));
			for (String m : mods) {
				heads.put("mod:" + m, modName.apply(m));
			}
		}
		for (JobRow r : roots) {
			Group g = groupOf.get(r.id());
			if (!filter.admits(g)) {
				continue;
			}
			String key = by == GroupBy.STATE ? "state:" + g.name().toLowerCase(Locale.ROOT) : "mod:" + r.mod();
			members.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
		}
		List<Line> out = new ArrayList<>();
		for (Map.Entry<String, String> h : heads.entrySet()) {
			List<JobRow> g = members.get(h.getKey());
			if (g == null || g.isEmpty()) {
				continue;
			}
			Comparator<JobRow> order = by == GroupBy.MOD ? Comparator.<JobRow>comparingInt(r -> groupOf.get(r.id()).ordinal()).thenComparing(NEWEST_FIRST)
				: NEWEST_FIRST;
			g.sort(order);
			double sum = 0;
			for (JobRow r : g) {
				sum += r.spentUsd();
			}
			out.add(new Line(Line.Kind.HEAD, h.getKey(), null, 0, h.getValue(), g.size(), sum));
			if (folded.test(h.getKey())) {
				continue;
			}
			for (JobRow r : g) {
				out.add(new Line(Line.Kind.ROW, r.id(), r, 0, r.title(), 0, r.spentUsd()));
				if (expanded.test(r.id())) {
					for (JobRow k : kidsOf.get(r.id())) {
						out.add(new Line(Line.Kind.ROW, k.id(), k, 1, k.title(), 0, k.spentUsd()));
					}
				}
			}
		}
		return out;
	}

	/** The root id a row (root or child) belongs to, or null when it is not published. */
	public static @Nullable String rootOf(List<JobRow> rows, @Nullable String id) {
		if (id == null) {
			return null;
		}
		Map<String, JobRow> m = byId(rows);
		JobRow r = m.get(id);
		return r == null ? null : rootId(r, m);
	}

	// ------------------------------------------------------------------ needs you, banner

	/** Roots waiting for the player (a root and its children count once), in panel order (newest first). */
	public static List<JobRow> waitingRoots(List<JobRow> rows) {
		List<JobRow> out = new ArrayList<>();
		for (JobRow r : roots(rows)) {
			if (rootWaiting(r, descendants(rows, r.id()))) {
				out.add(r);
			}
		}
		out.sort(NEWEST_FIRST);
		return out;
	}

	public static int needsYouCount(List<JobRow> rows) {
		return waitingRoots(rows).size();
	}

	/** "1 job needs you · Willow Bend", "3 jobs need you · Market stall v2"; null when nothing waits. */
	public static @Nullable String pillText(List<JobRow> rows) {
		List<JobRow> w = waitingRoots(rows);
		if (w.isEmpty()) {
			return null;
		}
		return w.size() + (w.size() == 1 ? " job needs you" : " jobs need you") + " · " + w.get(0).title();
	}

	/** True when any row is held by the usage limit (state HELD). */
	public static boolean held(List<JobRow> rows) {
		for (JobRow r : rows) {
			if (r.state() == JobState.HELD) {
				return true;
			}
		}
		return false;
	}

	/** The soonest usage-limit reset still ahead of {@code now} among held rows, or 0 when none is known. */
	public static long resetAt(List<JobRow> rows, long now) {
		long best = 0;
		for (JobRow r : rows) {
			if (r.state() == JobState.HELD && r.resetAt() > now && (best == 0 || r.resetAt() < best)) {
				best = r.resetAt();
			}
		}
		return best;
	}

	// ------------------------------------------------------------------ totals

	/**
	 * Today's spend (the local day of {@code now} in {@code zone}) from the ledger. Entries with the same job id count once
	 * (the latest charge, then the larger amount, wins), whichever mod published them; the split goes by the owner's
	 * namespace, so a Steward-owned job that Architect runs counts as Steward's.
	 */
	public static Totals totals(List<JobCost> ledger, long now, ZoneId zone) {
		LocalDate today = LocalDate.ofInstant(Instant.ofEpochMilli(now), zone);
		Map<String, JobCost> once = new LinkedHashMap<>();
		for (JobCost c : ledger) {
			JobCost prev = once.get(c.jobId());
			if (prev == null || c.at() > prev.at() || c.at() == prev.at() && c.usd() > prev.usd()) {
				once.put(c.jobId(), c);
			}
		}
		double total = 0;
		Map<String, Double> byMod = new LinkedHashMap<>();
		for (JobCost c : once.values()) {
			if (!LocalDate.ofInstant(Instant.ofEpochMilli(c.at()), zone).equals(today)) {
				continue;
			}
			total += c.usd();
			byMod.merge(c.ownerMod(), c.usd(), Double::sum);
		}
		return new Totals(total, byMod);
	}

	// ------------------------------------------------------------------ text

	/** "$1.92". */
	public static String usd(double v) {
		return String.format(Locale.ROOT, "$%.2f", v);
	}

	/** A budget: "$2" when whole, else "$2.50". */
	public static String budget(double v) {
		return Math.abs(v - Math.rint(v)) < 0.005 ? String.format(Locale.ROOT, "$%.0f", v) : usd(v);
	}

	/** A row's spend: "$0.31 of $2", "$1.92", or "—" when it has cost nothing and has no budget. */
	public static String spendText(JobRow r) {
		if (r.hasBudget()) {
			return usd(r.spentUsd()) + " of " + budget(r.budgetUsd());
		}
		return r.spentUsd() > 0 ? usd(r.spentUsd()) : "—";
	}

	/** Spent over budget, 0..1 (0 without a budget). */
	public static double spendFraction(JobRow r) {
		return r.hasBudget() ? Math.max(0, Math.min(1, r.spentUsd() / r.budgetUsd())) : 0;
	}

	/** "just now", "12 min ago", "1 h 5 min ago": the age between two epoch-ms times. */
	public static String age(long from, long now) {
		long m = Math.max(0, now - from) / 60_000;
		if (m < 1) {
			return "just now";
		}
		if (m < 60) {
			return m + " min ago";
		}
		long h = m / 60;
		return h + " h " + (m % 60) + " min ago";
	}
}
