package dev.larattalabs.labui.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.larattalabs.labui.ui.JobBoardModel.Filter;
import dev.larattalabs.labui.ui.JobBoardModel.GroupBy;
import dev.larattalabs.labui.ui.JobBoardModel.Line;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JobBoardModelTest {
	static final long NOW = 1_000_000_000L;

	static JobRow row(String id, JobState s, long created) {
		return JobRow.builder(id, "design", "T " + id, s).created(created).build();
	}

	static List<String> keys(List<Line> lines) {
		List<String> out = new ArrayList<>();
		for (Line l : lines) {
			out.add((l.head() ? "#" : l.depth() > 0 ? "  " : "") + l.key());
		}
		return out;
	}

	@Test
	void publishingOneModLeavesTheOtherModsRowsAndLedgerAlone() {
		JobBoardModel m = new JobBoardModel();
		m.publish("architect_mc", List.of(row("a:1", JobState.RUNNING, 1), row("a:2", JobState.DONE, 2)));
		m.publish("steward_mc", List.of(row("s:1", JobState.HELD, 3)));
		m.publishLedger("architect_mc", List.of(JobCost.of("j1", "architect_mc:g1", 1.0, NOW)));
		m.publishLedger("steward_mc", List.of(JobCost.of("j2", "steward_mc:settlement/1", 2.0, NOW)));
		long v = m.version();
		m.publish("architect_mc", List.of(row("a:3", JobState.NEEDS_YOU, 4)));
		assertTrue(m.version() > v);
		assertEquals(List.of("a:3", "s:1"), m.rows().stream().map(JobRow::id).toList());
		assertEquals("architect_mc", m.row("a:3").mod(), "the board stamps the publishing mod");
		m.publishLedger("architect_mc", List.of());
		assertEquals(1, m.ledger().size());
		assertEquals("j2", m.ledger().get(0).jobId());
	}

	@Test
	void namesDefaultFromTheModIdAndHostOnlyIsPerMod() {
		JobBoardModel m = new JobBoardModel();
		assertEquals("Architect", m.nameOf("architect_mc"));
		assertEquals("Steward", m.nameOf("steward_mc"));
		m.name("agentcraft_worlds", "AgentCraft");
		assertEquals("AgentCraft", m.nameOf("agentcraft_worlds"));
		assertFalse(m.anyHostOnly());
		m.hostOnly("architect_mc", true);
		assertTrue(m.anyHostOnly());
		m.hostOnly("architect_mc", false);
		assertFalse(m.anyHostOnly());
	}

	@Test
	void groupsAreNeedsYouRunningHeldEndedAndNewestFirst() {
		List<JobRow> rows = List.of(row("done", JobState.DONE, 50), row("run-old", JobState.RUNNING, 10), row("held", JobState.HELD, 30),
			row("pb", JobState.PAUSED_BUDGET, 31), row("need", JobState.NEEDS_YOU, 5), row("q", JobState.QUEUED, 40),
			row("run-new", JobState.RUNNING, 20), row("fail", JobState.FAILED, 60), row("flag", JobState.RUNNING, 1));
		rows = new ArrayList<>(rows);
		rows.set(8, JobRow.builder("flag", "set", "Flagged", JobState.RUNNING).needsYou(true).created(1).build());
		List<Line> l = JobBoardModel.lines(rows, Filter.ALL, GroupBy.STATE, JobBoardModel::defaultName, k -> false, id -> false);
		assertEquals(List.of("#state:needs_you", "need", "flag", "#state:running", "q", "run-new", "run-old", "#state:held", "pb", "held", "#state:ended",
			"fail", "done"), keys(l));
		assertEquals(2, l.get(0).count());
	}

	@Test
	void groupByModOrdersByStateThenNewest() {
		List<JobRow> rows = List.of(row("s1", JobState.DONE, 90), row("a1", JobState.RUNNING, 80), row("a2", JobState.NEEDS_YOU, 10), row("s2",
			JobState.HELD, 5));
		JobBoardModel m = new JobBoardModel();
		m.publish("steward_mc", List.of(rows.get(0), rows.get(3)));
		m.publish("architect_mc", List.of(rows.get(1), rows.get(2)));
		List<Line> l = JobBoardModel.lines(m.rows(), Filter.ALL, GroupBy.MOD, m::nameOf, k -> false, id -> false);
		assertEquals(List.of("#mod:architect_mc", "a2", "a1", "#mod:steward_mc", "s2", "s1"), keys(l));
		assertEquals("Architect", l.get(0).label());
	}

	@Test
	void childrenFoldUnderTheirParentAndOrphansStandAlone() {
		JobRow settlement = JobRow.builder("steward_mc:settlement/7", "settlement", "Willow Bend", JobState.RUNNING).created(100).build();
		JobRow set = JobRow.builder("architect_mc:set/1", "set", "Set", JobState.RUNNING).parent("steward_mc:settlement/7").created(110).spent(5).build();
		JobRow bible = JobRow.builder("architect_mc:bible/1", "bible", "Bible", JobState.DONE).parent("architect_mc:set/1").created(105).build();
		JobRow orphan = JobRow.builder("architect_mc:set/2", "set", "Orphan", JobState.RUNNING).parent("steward_mc:settlement/9").created(90).build();
		List<JobRow> rows = List.of(set, bible, orphan, settlement);
		assertEquals(List.of("architect_mc:set/2", "steward_mc:settlement/7"), JobBoardModel.roots(rows).stream().map(JobRow::id).sorted().toList());
		assertEquals(List.of("architect_mc:set/1", "architect_mc:bible/1"), JobBoardModel.descendants(rows, "steward_mc:settlement/7").stream()
			.map(JobRow::id).toList(), "grandchildren fold under the root too, newest first");
		List<Line> folded = JobBoardModel.lines(rows, Filter.ALL, GroupBy.STATE, JobBoardModel::defaultName, k -> false, id -> false);
		assertEquals(List.of("#state:running", "steward_mc:settlement/7", "architect_mc:set/2"), keys(folded));
		List<Line> open = JobBoardModel.lines(rows, Filter.ALL, GroupBy.STATE, JobBoardModel::defaultName, k -> false,
			id -> id.equals("steward_mc:settlement/7"));
		assertEquals(List.of("#state:running", "steward_mc:settlement/7", "  architect_mc:set/1", "  architect_mc:bible/1", "architect_mc:set/2"),
			keys(open));
		assertEquals("steward_mc:settlement/7", JobBoardModel.rootOf(rows, "architect_mc:bible/1"));
		assertNull(JobBoardModel.rootOf(rows, "nope"));
	}

	@Test
	void aChildThatNeedsYouLiftsItsRootAndCountsOnce() {
		JobRow root = JobRow.builder("s:1", "settlement", "Willow Bend", JobState.NEEDS_YOU).created(10).build();
		JobRow kid = JobRow.builder("a:1", "massing", "Massing", JobState.NEEDS_YOU).parent("s:1").created(11).build();
		JobRow kid2 = JobRow.builder("a:2", "massing", "Massing 2", JobState.RUNNING).needsYou(true).parent("s:1").created(12).build();
		JobRow other = JobRow.builder("s:2", "settlement", "Oakridge", JobState.RUNNING).created(5).build();
		JobRow otherKid = JobRow.builder("a:3", "massing", "Massing 3", JobState.NEEDS_YOU).parent("s:2").created(6).build();
		JobRow ended = JobRow.builder("a:9", "design", "Old", JobState.DONE).needsYou(true).created(50).build();
		List<JobRow> rows = List.of(root, kid, kid2, other, otherKid, ended);
		assertEquals(2, JobBoardModel.needsYouCount(rows));
		assertEquals("2 jobs need you · Willow Bend", JobBoardModel.pillText(rows));
		Map<Filter, Integer> c = JobBoardModel.counts(rows);
		assertEquals(3, c.get(Filter.ALL));
		assertEquals(2, c.get(Filter.NEEDS_YOU));
		assertEquals(0, c.get(Filter.RUNNING), "Oakridge is lifted to needs you by its child");
		assertEquals(1, c.get(Filter.ENDED), "an ended row never waits");
		assertNull(JobBoardModel.pillText(List.of(ended)));
		assertEquals("1 job needs you · Oakridge", JobBoardModel.pillText(List.of(other, otherKid)));
		assertEquals(List.of("a:3"), JobBoardModel.waitingUnder(rows, "s:2").stream().map(JobRow::id).toList());
		assertEquals(List.of("a:2", "a:1"), JobBoardModel.waitingUnder(rows, "s:1").stream().map(JobRow::id).toList());
	}

	@Test
	void filtersKeepOnlyTheirGroupAndFoldedGroupsShowTheirHead() {
		List<JobRow> rows = List.of(row("n", JobState.NEEDS_YOU, 1), row("r", JobState.RUNNING, 2), row("h", JobState.HELD, 3), row("d",
			JobState.CANCELLED, 4));
		assertEquals(List.of("#state:held", "h"), keys(JobBoardModel.lines(rows, Filter.HELD, GroupBy.STATE, JobBoardModel::defaultName, k -> false,
			id -> false)));
		assertEquals(List.of(), JobBoardModel.lines(List.of(row("r", JobState.RUNNING, 2)), Filter.ENDED, GroupBy.STATE, JobBoardModel::defaultName,
			k -> false, id -> false));
		assertEquals(List.of("#state:needs_you", "n", "#state:running", "#state:held", "h", "#state:ended", "d"), keys(JobBoardModel.lines(rows,
			Filter.ALL, GroupBy.STATE, JobBoardModel::defaultName, k -> k.equals("state:running"), id -> false)));
	}

	@Test
	void linesAreEqualByKindAndKeySoASelectionSurvivesARepublish() {
		JobRow a = JobRow.builder("x", "design", "A", JobState.RUNNING).link("here", () -> {
		}).build();
		JobRow b = JobRow.builder("x", "design", "A, renamed", JobState.HELD).link("here", () -> {
		}).build();
		assertEquals(new Line(Line.Kind.ROW, "x", a, 0, "A", 0, 0), new Line(Line.Kind.ROW, "x", b, 0, "B", 0, 1));
		assertFalse(new Line(Line.Kind.HEAD, "x", null, 0, "", 0, 0).equals(new Line(Line.Kind.ROW, "x", a, 0, "", 0, 0)));
	}

	@Test
	void totalsCountEachJobOnceTodayAndSplitByOwnerNamespace() {
		ZoneId zone = ZoneOffset.ofHours(-7);
		long noon = LocalDateTime.of(2026, 10, 25, 12, 0).toInstant(ZoneOffset.ofHours(-7)).toEpochMilli();
		long justAfterMidnight = LocalDateTime.of(2026, 10, 25, 0, 1).toInstant(ZoneOffset.ofHours(-7)).toEpochMilli();
		long justBefore = LocalDateTime.of(2026, 10, 24, 23, 59).toInstant(ZoneOffset.ofHours(-7)).toEpochMilli();
		JobBoardModel m = new JobBoardModel();
		m.publishLedger("architect_mc", List.of(JobCost.of("g1", "architect_mc:group/g1", 1.25, noon), JobCost.of("g2", "steward_mc:settlement/7", 5.0,
			noon), JobCost.of("old", "architect_mc:group/old", 9.0, justBefore), JobCost.of("early", "", 0.5, justAfterMidnight)));
		// Steward reports the same sidecar job g2 with an older, smaller figure, and its own talk job
		m.publishLedger("steward_mc", List.of(JobCost.of("g2", "steward_mc:settlement/7", 4.0, noon - 60_000), JobCost.of("talk1", "steward_mc:talk",
			0.05, noon)));
		JobBoardModel.Totals t = JobBoardModel.totals(m.ledger(), noon + 3_600_000, zone);
		assertEquals(1.25 + 5.0 + 0.5 + 0.05, t.total(), 1e-9);
		assertEquals(1.75, t.byMod().get("architect_mc"), 1e-9, "an empty owner counts for the publishing mod");
		assertEquals(5.05, t.byMod().get("steward_mc"), 1e-9, "Steward-owned work Architect ran is Steward's");
		assertEquals(0, JobBoardModel.totals(m.ledger(), noon + 86_400_000L, zone).total(), 1e-9, "tomorrow starts at zero");
	}

	@Test
	void theBannerTakesTheSoonestResetStillAhead() {
		JobRow a = JobRow.builder("a", "design", "A", JobState.HELD).held("usage limit", NOW + 7_200_000).build();
		JobRow b = JobRow.builder("b", "design", "B", JobState.HELD).held("usage limit", NOW + 3_600_000).build();
		JobRow past = JobRow.builder("c", "design", "C", JobState.HELD).held("usage limit", NOW - 1).build();
		JobRow budget = JobRow.builder("d", "design", "D", JobState.PAUSED_BUDGET).held("budget", NOW + 60_000).build();
		assertTrue(JobBoardModel.held(List.of(a, b)));
		assertFalse(JobBoardModel.held(List.of(budget)), "a budget pause is not the usage limit");
		assertEquals(NOW + 3_600_000, JobBoardModel.resetAt(List.of(a, b, past, budget), NOW));
		assertEquals(0, JobBoardModel.resetAt(List.of(past), NOW));
	}

	@Test
	void spendText() {
		assertEquals("$0.31 of $2", JobBoardModel.spendText(JobRow.builder("a", "k", "t", JobState.RUNNING).spent(0.31).budget(2, "job").build()));
		assertEquals("$1.00 of $2.50", JobBoardModel.spendText(JobRow.builder("a", "k", "t", JobState.RUNNING).spent(1).budget(2.5, "job").build()));
		assertEquals("$1.92", JobBoardModel.spendText(JobRow.builder("a", "k", "t", JobState.DONE).spent(1.92).build()));
		assertEquals("—", JobBoardModel.spendText(JobRow.builder("a", "k", "t", JobState.RUNNING).build()));
		assertEquals(1.0, JobBoardModel.spendFraction(JobRow.builder("a", "k", "t", JobState.FAILED).spent(3).budget(2, "job").build()));
		assertEquals("12 min ago", JobBoardModel.age(NOW - 12 * 60_000, NOW));
		assertEquals("1 h 5 min ago", JobBoardModel.age(NOW - 65 * 60_000, NOW));
	}
}
