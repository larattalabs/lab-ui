package dev.larattalabs.labui.client.jobs;

import com.mojang.blaze3d.platform.InputConstants;
import dev.larattalabs.labui.client.hud.UiBits;
import dev.larattalabs.labui.client.ui.Kit;
import dev.larattalabs.labui.client.ui.Panels;
import dev.larattalabs.labui.client.ui.TextUtil;
import dev.larattalabs.labui.client.ui.UiStyle;
import dev.larattalabs.labui.client.widget.Button;
import dev.larattalabs.labui.client.widget.Chip;
import dev.larattalabs.labui.client.widget.CollapsibleSection;
import dev.larattalabs.labui.client.widget.Confirm;
import dev.larattalabs.labui.client.widget.EmptyState;
import dev.larattalabs.labui.client.widget.Paint;
import dev.larattalabs.labui.client.widget.ScrollList;
import dev.larattalabs.labui.ui.ConfirmArm;
import dev.larattalabs.labui.ui.JobBoardModel;
import dev.larattalabs.labui.ui.JobBoardModel.Filter;
import dev.larattalabs.labui.ui.JobBoardModel.GroupBy;
import dev.larattalabs.labui.ui.JobBoardModel.Line;
import dev.larattalabs.labui.ui.JobRow;
import dev.larattalabs.labui.ui.JobState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * The Jobs &amp; spend panel (0.4.0), as the approved mock: a title bar with the key hints; today's total from the cost
 * ledger (an estimate) with the split per mod; a usage-limit banner while any row is held; filter chips (All / Needs
 * you / Running / Held / Ended) and group by State or Mod; the rows ({@link ScrollList}: group heads that fold, a root's
 * children under it while it or one of them is selected) and a detail pane (spend against budget, its Claude jobs,
 * times, where Open goes, and the owning mod's actions: Open, Approve, Raise, Cancel through {@link Confirm}'s two
 * presses).
 *
 * <p>Keys: arrows move, Enter opens the row in its mod (on a group head: folds it), A approves, the panel key or Esc
 * closes (Esc disarms an armed Cancel first). The panel never changes a row itself: it waits for the owning mod's next
 * publish.
 */
public class JobsScreen extends Screen {
	private static final int TITLE_H = 18;
	private static final int TOP_H = 26;
	private static final int BANNER_H = 14;
	private static final int FILTER_H = 24;
	private static final int ROW_H = 16;
	private static final int KIND_W = 76;
	private static final String FOLD_PREFIX = "lab_ui.jobs.";
	private static final int[] KIND_FILLS = {0xFFE3D5BF, 0xFFDFE6D6, 0xFFE6DCEB};
	private static final int BANNER_FILL = 0xFFF1E3B6;
	private static final int BANNER_EDGE = 0xFFD9C27A;
	private static final int BANNER_TEXT = 0xFF5E4818;

	private static Filter lastFilter = Filter.ALL;
	private static GroupBy lastGroupBy = GroupBy.STATE;

	private final @Nullable Screen parent;
	private final ScrollList<Line> list;
	private final Map<Filter, Chip> filterChips = new HashMap<>();
	private final Chip byState = new Chip("State");
	private final Chip byMod = new Chip("Mod");
	private final ConfirmArm arm = new ConfirmArm();
	private final Map<String, Confirm> cancels = new HashMap<>();
	private final Button open = new Button("Open", this::openSelected);
	private final Button approve = new Button("Approve", this::approveSelected);
	private final Button raise = new Button("Raise", this::raiseSelected);
	private final EmptyState empty = new EmptyState("");
	private Filter filter = lastFilter;
	private GroupBy groupBy = lastGroupBy;
	private @Nullable String selectedKey;
	private @Nullable String pendingSelect;
	private boolean mouseClick;
	private @Nullable String flash;
	private @Nullable String flashFor;
	private List<JobRow> rows = List.of();
	private final List<Button> shownButtons = new ArrayList<>();
	private @Nullable Confirm shownCancel;

	public JobsScreen(@Nullable Screen parent, @Nullable String rowId) {
		super(Component.literal("Jobs & spend"));
		this.parent = parent;
		this.pendingSelect = rowId;
		this.selectedKey = rowId;
		for (Filter f : Filter.values()) {
			filterChips.put(f, new Chip(f.label, () -> setFilter(f)));
		}
		byState.onPress(() -> setGroupBy(GroupBy.STATE));
		byMod.onPress(() -> setGroupBy(GroupBy.MOD));
		open.primary(true);
		list = new ScrollList<>(ROW_H, this::paintLine).onSelect(l -> {
			selectedKey = l.key();
			if (!l.key().equals(flashFor)) {
				flash = null;
			}
			if (l.head() && mouseClick) {
				toggleFold(l.key());
			}
		}).onActivate(l -> {
			if (l.head()) {
				if (!mouseClick) {
					toggleFold(l.key());
				}
			} else {
				openSelected();
			}
		});
		list.focused(true);
		list.emptyState().action("Show all", () -> setFilter(Filter.ALL));
	}

	/** Shows only {@code f}'s rows (the chip). */
	public JobsScreen filter(Filter f) {
		setFilter(f);
		return this;
	}

	/** Groups the rows by state or by mod (the chip). */
	public JobsScreen groupBy(GroupBy g) {
		setGroupBy(g);
		return this;
	}

	private void setFilter(Filter f) {
		filter = f;
		lastFilter = f;
	}

	private void setGroupBy(GroupBy g) {
		groupBy = g;
		lastGroupBy = g;
	}

	private static boolean folded(String key) {
		return !CollapsibleSection.isOpen(FOLD_PREFIX + key, true);
	}

	private static void toggleFold(String key) {
		CollapsibleSection.setOpen(FOLD_PREFIX + key, folded(key));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}

	/** The selected row (null on a group head or when nothing is selected). */
	public @Nullable JobRow selectedRow() {
		Line l = list.selected();
		return l == null || l.head() ? null : JobBoard.model().row(l.key());
	}

	/** QA: filter, grouping, selection and the visible lines. */
	public String debugState() {
		StringBuilder sb = new StringBuilder("filter=" + filter + " groupBy=" + groupBy + " selected=" + selectedKey + " lines=");
		for (Line l : list.items()) {
			sb.append(l.head() ? "#" : l.depth() > 0 ? "~" : "").append(l.key()).append(',');
		}
		return sb.toString();
	}

	private String modName(String mod) {
		return JobBoard.model().nameOf(mod);
	}

	private void refresh() {
		rows = JobBoard.model().rows();
		int kw = 30;
		for (JobRow r : rows) {
			kw = Math.max(kw, font.width(r.kind()) + 10);
		}
		kindW = Math.min(KIND_W, kw);
		String root = JobBoardModel.rootOf(rows, selectedKey);
		List<Line> lines = JobBoardModel.lines(rows, filter, groupBy, this::modName, JobsScreen::folded, id -> id.equals(root));
		list.items(lines);
		if (pendingSelect != null || list.selected() == null) {
			String want = pendingSelect != null ? pendingSelect : selectedKey;
			Line pick = null;
			for (Line l : lines) {
				if (!l.head() && l.key().equals(want)) {
					pick = l;
				}
			}
			if (pick == null) {
				for (Line l : lines) {
					if (!l.head()) {
						pick = l;
						break;
					}
				}
			}
			if (pick == null && !lines.isEmpty()) {
				pick = lines.get(0);
			}
			if (pick != null && list.nav().view() > 0) {
				list.select(pick);
				pendingSelect = null;
			}
		}
	}

	// ------------------------------------------------------------------ drawing

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		g.fillGradient(0, 0, width, height, UiStyle.withAlpha(UiStyle.INK, 90), UiStyle.withAlpha(UiStyle.INK, 140));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float a) {
		super.extractRenderState(g, mx, my, a);
		refresh();
		Font f = font;
		int px = 6;
		int py = 6;
		int pw = width - 12;
		int ph = height - 12;
		g.fill(px, py, px + pw, py + ph, UiStyle.CREAM);
		Paint.border(g, px, py, pw, ph, UiStyle.WALNUT);
		Paint.border(g, px + 1, py + 1, pw - 2, ph - 2, UiStyle.WALNUT);
		Paint.border(g, px + 2, py + 2, pw - 4, ph - 4, UiStyle.PAPER);
		int ix = px + 2;
		int iw = pw - 4;
		int y = py + 2;
		drawTitle(g, f, ix, y, iw);
		y += TITLE_H;
		drawTotals(g, f, ix, y, iw);
		y += TOP_H;
		g.fill(ix, y - 1, ix + iw, y, UiStyle.rule());
		if (JobBoardModel.held(rows)) {
			drawBanner(g, f, ix, y, iw);
			y += BANNER_H;
		}
		drawFilters(g, f, ix + 6, y + 3, iw - 12, mx, my);
		y += FILTER_H;
		g.fill(ix, y - 1, ix + iw, y, UiStyle.rule());
		int mainH = py + ph - 2 - y;
		int detailW = Math.max(132, Math.min(240, iw * 34 / 100));
		int listW = iw - detailW - 1;
		if (rows.isEmpty()) {
			empty.line(JobBoard.model().anyHostOnly() ? "Claude work runs in the host's game. Open this panel there to see it."
				: "No Claude work yet. Jobs you start in any mod show here, with what they cost.").noAction();
			empty.draw(g, f, ix, y, iw, mainH, mx, my);
			return;
		}
		if (list.items().isEmpty()) {
			list.empty("Nothing in " + filter.label + " right now.");
		}
		list.draw(g, f, ix + 4, y + 4, listW - 6, mainH - 8, mx, my);
		g.fill(ix + listW, y, ix + listW + 1, y + mainH, UiStyle.rule());
		drawDetail(g, f, ix + listW + 1, y, detailW, mainH, mx, my);
	}

	private void drawTitle(GuiGraphicsExtractor g, Font f, int x, int y, int w) {
		g.fill(x, y, x + w, y + TITLE_H, UiStyle.titleBar());
		String title = "Jobs & spend";
		g.text(f, title, x + 8, y + 5, UiStyle.titleText(), false);
		String hint = "↑↓ move · Enter open · A approve · " + JobBoard.keyLabel() + "/Esc close";
		if (f.width(hint) + f.width(title) + 30 > w) {
			hint = "Enter open · A approve · Esc close";
		}
		if (f.width(hint) + f.width(title) + 30 <= w) {
			g.text(f, hint, x + w - 8 - f.width(hint), y + 5, UiStyle.withAlpha(UiStyle.titleText(), 190), false);
		}
	}

	private void drawTotals(GuiGraphicsExtractor g, Font f, int x, int y, int w) {
		JobBoardModel.Totals t = JobBoardModel.totals(JobBoard.model().ledger(), System.currentTimeMillis(), JobBoard.zone());
		int lx = x + 8;
		String total = JobBoardModel.usd(t.total());
		g.text(f, total, lx, y + 4, UiStyle.INK, false);
		g.text(f, "today · estimate", lx, y + 15, UiStyle.mutedText(), false);
		int sx = lx + Math.max(f.width(total), f.width("today · estimate")) + 16;
		g.fill(sx - 8, y + 4, sx - 7, y + TOP_H - 4, UiStyle.rule());
		List<String> parts = new ArrayList<>();
		for (Map.Entry<String, Double> e : t.byMod().entrySet()) {
			parts.add(modName(e.getKey()) + " " + JobBoardModel.usd(e.getValue()));
		}
		int room = x + w - 8 - sx;
		if (parts.isEmpty()) {
			g.text(f, TextUtil.ellipsize(f, "No Claude cost today", room), sx, y + 4, UiStyle.mutedText(), false);
		} else {
			// one per line when two fit, else all on the first line
			if (parts.size() <= 2) {
				g.text(f, TextUtil.ellipsize(f, parts.get(0), room), sx, y + 4, UiStyle.INK, false);
				if (parts.size() == 2) {
					g.text(f, TextUtil.ellipsize(f, parts.get(1), room), sx, y + 15, UiStyle.INK, false);
				}
			} else {
				g.text(f, TextUtil.ellipsize(f, String.join(" · ", parts), room), sx, y + 4, UiStyle.INK, false);
			}
		}
		int n = JobBoardModel.needsYouCount(rows);
		if (n > 0) {
			String s = UiBits.plural(n, "job needs you", "jobs need you");
			int sw = f.width(s) + 12;
			int nx = x + w - 8 - sw;
			int partsW = 0;
			for (String p : parts.subList(0, Math.min(2, parts.size()))) {
				partsW = Math.max(partsW, f.width(p));
			}
			if (nx > sx + partsW + 8) {
				UiBits.pulsingDot(g, "waiting", nx, y + 9);
				g.text(f, s, nx + 11, y + 9, UiStyle.CLAY_DARK, false);
			}
		}
	}

	private void drawBanner(GuiGraphicsExtractor g, Font f, int x, int y, int w) {
		g.fill(x, y, x + w, y + BANNER_H, BANNER_FILL);
		g.fill(x, y + BANNER_H - 1, x + w, y + BANNER_H, BANNER_EDGE);
		Panels.dot(g, "thinking", x + 8, y + 3, false);
		long reset = JobBoardModel.resetAt(rows, System.currentTimeMillis());
		String s = reset > 0 ? "Usage limit: Claude work waits until " + UiBits.clock(reset) + ", then all of it resumes."
			: "Usage limit: Claude work waits until the limit resets, then all of it resumes.";
		g.text(f, TextUtil.ellipsize(f, s, w - 28), x + 20, y + 3, BANNER_TEXT, false);
	}

	private void drawFilters(GuiGraphicsExtractor g, Font f, int x, int y, int w, int mx, int my) {
		Map<Filter, Integer> counts = JobBoardModel.counts(rows);
		byState.selected(groupBy == GroupBy.STATE);
		byMod.selected(groupBy == GroupBy.MOD);
		int rightW = byState.width(f) + 4 + byMod.width(f);
		String gl = "Group by";
		boolean label = true;
		int chipsW = 0;
		for (Filter fl : Filter.values()) {
			Chip c = filterChips.get(fl).label(fl.label + " " + counts.get(fl)).selected(filter == fl);
			chipsW += c.width(f) + 4;
		}
		if (chipsW + rightW + f.width(gl) + 12 > w) {
			label = false;
		}
		int cx = x;
		int limit = x + w - rightW - (label ? f.width(gl) + 8 : 0) - 8;
		for (Filter fl : Filter.values()) {
			Chip c = filterChips.get(fl);
			int cw = c.width(f);
			if (cx + cw > limit) {
				cw = Math.max(24, limit - cx);
			}
			if (cx >= limit) {
				c.hide();
				continue;
			}
			c.draw(g, f, cx, y, cw, mx, my);
			cx += cw + 4;
		}
		int rx = x + w - rightW;
		if (label) {
			g.text(f, gl, rx - 6 - f.width(gl), y + 5, UiStyle.mutedText(), false);
		}
		rx += byState.draw(g, f, rx, y, mx, my) + 4;
		byMod.draw(g, f, rx, y, mx, my);
	}

	private static String family(JobState s) {
		return switch (s) {
			case NEEDS_YOU -> "waiting";
			case RUNNING, QUEUED -> "working";
			case HELD, PAUSED_BUDGET -> "thinking";
			case DONE -> "done";
			case FAILED -> "error";
			case CANCELLED -> "idle";
		};
	}

	private int kindFill(String mod) {
		List<String> mods = new ArrayList<>();
		for (JobRow r : rows) {
			if (!mods.contains(r.mod())) {
				mods.add(r.mod());
			}
		}
		mods.sort(String::compareTo);
		return KIND_FILLS[Math.max(0, mods.indexOf(mod)) % KIND_FILLS.length];
	}

	private int kindW = KIND_W;

	private void paintLine(GuiGraphicsExtractor g, Font f, Line l, int x, int y, int w, int h, boolean sel, boolean hov) {
		int ty = y + (h - 8) / 2;
		if (l.head()) {
			boolean open = !folded(l.key());
			int c = UiStyle.link();
			Paint.chevron(g, x + 4, ty + 1, open, c);
			g.text(f, l.label(), x + 14, ty, c, false);
			String sum = "· " + l.count() + " · " + JobBoardModel.usd(l.usd());
			g.text(f, TextUtil.ellipsize(f, sum, w - 18 - f.width(l.label()) - 4), x + 14 + f.width(l.label()) + 4, ty, UiStyle.mutedText(), false);
			return;
		}
		JobRow r = l.row();
		if (r == null) {
			return;
		}
		int cx = x + 4 + l.depth() * 12;
		boolean lifted = l.depth() == 0 && !r.waiting() && !r.state().ended() && !JobBoardModel.waitingUnder(rows, r.id()).isEmpty();
		if (r.waiting() || lifted) {
			UiBits.pulsingDot(g, "waiting", cx, y + (h - 7) / 2);
		} else {
			Panels.dot(g, family(r.state()), cx, y + (h - 7) / 2, false);
		}
		cx += 11;
		int kw = Math.min(kindW, Math.max(30, w / 6));
		g.fill(cx, y + 2, cx + kw, y + h - 2, kindFill(r.mod()));
		String k = TextUtil.ellipsize(f, r.kind(), kw - 4);
		g.text(f, k, cx + (kw - f.width(k)) / 2, ty, 0xFF5A4636, false);
		cx += kw + 5;
		String spend = JobBoardModel.spendText(r);
		int spW = f.width(spend);
		int right = x + w - 4;
		g.text(f, spend, right - spW, ty, 0xFF5A4636, false);
		int room = right - spW - 8 - cx;
		String title = TextUtil.ellipsize(f, r.title(), room);
		g.text(f, title, cx, ty, l.depth() > 0 ? 0xFF5A4636 : UiStyle.INK, false);
		int used = f.width(title);
		if (!r.step().isEmpty() && room - used > 24) {
			g.text(f, TextUtil.ellipsize(f, " · " + r.step(), room - used), cx + used, ty, UiStyle.mutedText(), false);
		}
	}

	private void drawDetail(GuiGraphicsExtractor g, Font f, int x, int y, int w, int h, int mx, int my) {
		shownButtons.clear();
		shownCancel = null;
		int pad = 7;
		int tx = x + pad;
		int tw = w - pad * 2;
		Line sel = list.selected();
		JobRow r = selectedRow();
		if (r == null) {
			String s = sel != null && sel.head() ? sel.label() + ": " + UiBits.plural(sel.count(), "job", "jobs") + ", " + JobBoardModel.usd(sel.usd())
				+ ". Enter folds the group." : "Select a job to see its spend and what you can do.";
			int ly = y + 8;
			for (String line : TextUtil.wrapPlain(f, s, tw)) {
				g.text(f, line, tx, ly, UiStyle.mutedText(), false);
				ly += 10;
			}
			return;
		}
		// the actions, bottom up: each button on the row it fits
		List<Object> acts = new ArrayList<>();
		if (r.link() != null) {
			open.label("Open in " + modName(r.mod()));
			acts.add(open);
		}
		if (r.approve() != null && r.waiting()) {
			approve.label(r.approve().label());
			acts.add(approve);
		}
		if (r.raise() != null) {
			raise.label(r.raise().label());
			acts.add(raise);
		}
		Confirm cancel = null;
		if (r.cancel() != null && !r.state().ended()) {
			JobRow.Action c = r.cancel();
			String rid = r.id();
			String mod = modName(r.mod());
			cancel = cancels.computeIfAbsent(rid + ":cancel", k -> new Confirm(arm, k, c.label(), "cancel", () -> runAction(rid, c, "Sent to " + mod
				+ ". The row changes when " + mod + " answers.")));
			acts.add(cancel);
		}
		List<int[]> place = new ArrayList<>();
		int bx = tx;
		int rowsN = acts.isEmpty() ? 0 : 1;
		for (Object o : acts) {
			int bw = Math.min(tw, o instanceof Confirm c ? c.width(f) : ((Button) o).width(f));
			if (bx > tx && bx + bw > tx + tw) {
				bx = tx;
				rowsN++;
			}
			place.add(new int[] {bx, rowsN - 1, bw});
			bx += bw + 4;
		}
		int btnTop = y + h - pad - rowsN * (Button.MIN_H + 4) + 4;
		int flashH = 0;
		List<String> flashLines = List.of();
		if (flash != null && r.id().equals(flashFor)) {
			flashLines = TextUtil.wrapPlain(f, flash, tw - 8);
			flashH = flashLines.size() * 10 + 6;
		}
		int contentBottom = btnTop - (flashH > 0 ? flashH + 4 : 0) - 4;
		for (int i = 0; i < acts.size(); i++) {
			int[] p = place.get(i);
			int by = btnTop + p[1] * (Button.MIN_H + 4);
			if (acts.get(i) instanceof Confirm c) {
				c.draw(g, f, p[0], by, p[2], Button.MIN_H, mx, my);
				shownCancel = c;
			} else {
				Button b = (Button) acts.get(i);
				b.draw(g, f, p[0], by, p[2], Button.MIN_H, mx, my);
				shownButtons.add(b);
			}
		}
		if (flashH > 0) {
			int fy = btnTop - flashH - 4;
			g.fill(tx, fy, tx + tw, fy + flashH, 0xFFFFFAF0);
			Paint.dashed(g, tx, fy, tw, flashH, 0xFFC5B398);
			int ly = fy + 3;
			for (String line : flashLines) {
				g.text(f, line, tx + 4, ly, 0xFF5A4636, false);
				ly += 10;
			}
		}
		// the content, top down: whole lines only, above the actions
		cy = y + 7;
		cBottom = contentBottom;
		for (String line : first(TextUtil.wrapPlain(f, r.title(), tw), 2)) {
			put(g, f, line, tx, UiStyle.CLAY_DARK);
		}
		cy += 2;
		if (cy + 9 <= cBottom) {
			Panels.dot(g, family(r.state()), tx, cy, false);
			put(g, f, TextUtil.ellipsize(f, r.state().label() + " · " + modName(r.mod()) + " " + r.kind(), tw - 11), tx + 11, UiStyle.INK);
			cy += 1;
		}
		for (String line : first(TextUtil.wrapPlain(f, r.step(), tw), 3)) {
			put(g, f, line, tx, 0xFF5A4636);
		}
		List<JobRow> under = r.waiting() ? List.of() : JobBoardModel.waitingUnder(rows, r.id());
		if (!under.isEmpty()) {
			String s = "Waits for you in: " + under.get(0).title() + (under.size() > 1 ? " (+" + (under.size() - 1) + " more)" : "");
			for (String line : first(TextUtil.wrapPlain(f, s, tw), 2)) {
				put(g, f, line, tx, UiStyle.CLAY_DARK);
			}
		}
		if (r.state() == JobState.HELD || r.state() == JobState.PAUSED_BUDGET) {
			String why = r.heldReason() != null ? r.heldReason() : r.state() == JobState.HELD ? "the usage limit" : "its budget";
			String s = "Held by " + why + (r.resetAt() > 0 ? ", resets at " + UiBits.clock(r.resetAt()) : "");
			for (String line : first(TextUtil.wrapPlain(f, s, tw), 2)) {
				put(g, f, line, tx, UiStyle.warn());
			}
		}
		cy += 4;
		if (r.hasBudget()) {
			String s = "Spent " + JobBoardModel.usd(r.spentUsd()) + " of the " + (r.budgetKind().isEmpty() ? "" : r.budgetKind() + " ") + "budget "
				+ JobBoardModel.budget(r.budgetUsd());
			for (String line : first(TextUtil.wrapPlain(f, s, tw), 2)) {
				put(g, f, line, tx, UiStyle.INK);
			}
			if (cy + 7 <= cBottom) {
				double p = JobBoardModel.spendFraction(r);
				Panels.progress(g, tx, cy + 1, tw, p, p >= 1 ? "red" : p >= 0.8 ? "brass" : "sage");
				cy += 11;
			}
		} else {
			put(g, f, TextUtil.ellipsize(f, r.spentUsd() > 0 ? "No budget · " + JobBoardModel.usd(r.spentUsd()) + " so far" : "No Claude cost", tw), tx,
				UiStyle.INK);
			cy += 1;
		}
		List<JobRow> kids = JobBoardModel.descendants(rows, r.id());
		if (!kids.isEmpty() && cy + 3 + 20 <= cBottom) {
			cy += 3;
			for (String line : first(TextUtil.wrapPlain(f, "Its Claude jobs (the total counts each once):", tw), 2)) {
				put(g, f, line, tx, UiStyle.mutedText());
			}
			int room = Math.max(1, (cBottom - cy + 1) / 10);
			int max = kids.size() <= room ? kids.size() : Math.max(0, room - 1);
			for (int i = 0; i < max; i++) {
				JobRow k = kids.get(i);
				String sp = JobBoardModel.spendText(k);
				int spw = f.width(sp);
				g.text(f, sp, tx + tw - spw, cy, 0xFF5A4636, false);
				put(g, f, TextUtil.ellipsize(f, "· " + k.title(), tw - spw - 6), tx, UiStyle.INK);
			}
			if (kids.size() > max) {
				put(g, f, "+" + (kids.size() - max) + " more", tx, UiStyle.mutedText());
			}
		}
		cy += 3;
		long now = System.currentTimeMillis();
		if (r.createdAt() > 0) {
			String s = "Started " + JobBoardModel.age(r.createdAt(), now) + (r.endedAt() > 0 ? " · ended " + JobBoardModel.age(r.endedAt(), now) : "");
			for (String line : first(TextUtil.wrapPlain(f, s, tw), 2)) {
				put(g, f, line, tx, UiStyle.mutedText());
			}
		}
		if (r.linkLabel() != null) {
			put(g, f, TextUtil.ellipsize(f, "Opens: " + r.linkLabel(), tw), tx, UiStyle.mutedText());
		}
	}

	private int cy;
	private int cBottom;

	/** One detail line at the cursor when it fits whole above the actions; false (and nothing drawn) when not. */
	private boolean put(GuiGraphicsExtractor g, Font f, String s, int x, int color) {
		if (cy + 9 > cBottom) {
			return false;
		}
		g.text(f, s, x, cy, color, false);
		cy += 10;
		return true;
	}

	private static List<String> first(List<String> lines, int n) {
		return lines.size() <= n ? lines : lines.subList(0, n);
	}

	// ------------------------------------------------------------------ actions

	private void runAction(String rowId, JobRow.Action a, String note) {
		a.run().run();
		flash = note;
		flashFor = rowId;
	}

	private void openSelected() {
		JobRow r = selectedRow();
		if (r == null || r.link() == null) {
			return;
		}
		Runnable link = r.link();
		minecraft.gui.setScreen(null);
		link.run();
	}

	private void approveSelected() {
		JobRow r = selectedRow();
		if (r != null && r.approve() != null && r.waiting()) {
			String mod = modName(r.mod());
			runAction(r.id(), r.approve(), "Sent to " + mod + ". The row changes when " + mod + " answers.");
		}
	}

	private void raiseSelected() {
		JobRow r = selectedRow();
		if (r != null && r.raise() != null) {
			String mod = modName(r.mod());
			runAction(r.id(), r.raise(), "Sent to " + mod + ". The row changes when " + mod + " answers.");
		}
	}

	// ------------------------------------------------------------------ input

	@Override
	public boolean keyPressed(KeyEvent e) {
		if (e.isEscape()) {
			if (shownCancel != null && shownCancel.escape()) {
				return true;
			}
			onClose();
			return true;
		}
		KeyMapping k = JobBoard.key();
		if (k != null && k.matches(e)) {
			onClose();
			return true;
		}
		if (e.key() == InputConstants.KEY_A && !e.hasControlDown()) {
			approveSelected();
			return true;
		}
		mouseClick = false;
		if (list.keyPressed(e)) {
			return true;
		}
		return super.keyPressed(e);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
		double mx = e.x();
		double my = e.y();
		int b = e.button();
		for (Confirm c : cancels.values()) {
			if (c.mouseClicked(mx, my, b)) {
				return true;
			}
		}
		for (Chip c : filterChips.values()) {
			if (c.mouseClicked(mx, my, b)) {
				return true;
			}
		}
		if (byState.mouseClicked(mx, my, b) || byMod.mouseClicked(mx, my, b)) {
			return true;
		}
		for (Button bt : List.copyOf(shownButtons)) {
			if (bt.mouseClicked(mx, my, b)) {
				return true;
			}
		}
		if (empty.mouseClicked(mx, my, b)) {
			return true;
		}
		mouseClick = true;
		try {
			if (list.mouseClicked(mx, my, b)) {
				return true;
			}
		} finally {
			mouseClick = false;
		}
		return super.mouseClicked(e, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
		return list.mouseDragged(e.x(), e.y(), e.button()) || super.mouseDragged(e, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent e) {
		return list.mouseReleased(e.x(), e.y(), e.button()) || super.mouseReleased(e);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
		return list.mouseScrolled(mx, my, scrollY) || super.mouseScrolled(mx, my, scrollX, scrollY);
	}
}
