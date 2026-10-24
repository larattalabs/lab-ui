package dev.larattalabs.labui.client.picker;

import com.mojang.blaze3d.platform.InputConstants;
import dev.larattalabs.labui.client.text.TextFieldView;
import dev.larattalabs.labui.client.text.TextKeys;
import dev.larattalabs.labui.client.text.TextModel;
import dev.larattalabs.labui.client.ui.Kit;
import dev.larattalabs.labui.client.ui.Panels;
import dev.larattalabs.labui.client.ui.TextUtil;
import dev.larattalabs.labui.client.ui.UiStyle;
import dev.larattalabs.labui.client.widget.Button;
import dev.larattalabs.labui.client.widget.Chip;
import dev.larattalabs.labui.client.widget.EmptyState;
import dev.larattalabs.labui.client.widget.NavKeys;
import dev.larattalabs.labui.client.widget.Paint;
import dev.larattalabs.labui.client.widget.ScrollList;
import dev.larattalabs.labui.client.widget.Tooltip;
import dev.larattalabs.labui.ui.ChipFlow;
import dev.larattalabs.labui.ui.ListNav;
import dev.larattalabs.labui.ui.PickerModel;
import dev.larattalabs.labui.ui.ScrollMath;
import dev.larattalabs.labui.ui.SectionGrid;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * The full-screen picker that "more…" opens. A title bar with ‹ Back and the key hints; a search box (focused on open)
 * with a Favourites toggle and room for Tags and Compare (shown disabled until they exist); a Recent strip; then a
 * family rail on the left (All first, with counts), a grouped grid of swatch cells or preview cards in the middle, and a
 * detail pane on the right (big preview, name, id, the caller's preview, detail lines, Pick and Favourite). Hover or the
 * keyboard selection fills the detail pane.
 *
 * <p>Keys: typing searches; Down from the search box enters the grid; arrows move through it (Up from its first row goes
 * back to search); Tab cycles search, grid and rail; Enter picks; F toggles the favourite; Esc goes back to the parent
 * screen. Recents and favourites persist per picker id ({@link PickerStore}). It pauses when its parent does.
 */
public class PickerScreen extends Screen {
	private static final int TITLE_H = 22;
	private static final int ROW_GAP = 5;
	private static final int RAIL_ROW = 14;
	private static final int HEAD_H = 13;
	private static final int GAP = 4;
	private static final int BAR_W = 6;

	private enum Focus {
		SEARCH, GRID, RAIL
	}

	private final PickerSpec spec;
	private final @Nullable Screen parent;
	private final PickerModel<PickerEntry> model;
	private final TextModel search = new TextModel(80);
	private final TextFieldView searchView = new TextFieldView();
	private final Tooltip tooltip = new Tooltip();
	private final Button back = new Button("‹ Back", this::back);
	private final Chip favOnly = new Chip("★ Favourites");
	private final Chip tags = new Chip("Tags ▾");
	private final Chip compare = new Chip("Compare");
	private final Button pick = new Button("Pick", this::pickSelected);
	private final Button fav = new Button("☆ Favourite", this::toggleFavourite);
	private final EmptyState empty = new EmptyState("");
	private final List<Chip> recentChips = new ArrayList<>();
	private List<String> recentIds = List.of();
	private Focus focus = Focus.SEARCH;
	private @Nullable String hoveredId;
	/** The cell under the mouse fills the detail pane only once the mouse has moved onto it since the last key. */
	private @Nullable String hoverShown;
	private boolean keyLast = true;
	private boolean revealPending = true;
	private int gridScroll;
	private int railScroll;
	private int dragGrab = -1;
	// layout of the last frame
	private int sx;
	private int sy;
	private int sw;
	private int gx;
	private int gy;
	private int gw;
	private int gh;
	private int rx;
	private int ry;
	private int rw;
	private int rh;
	private int cols = 1;
	private int cellW;
	private int cellH;
	private int[] rowY = new int[0];
	private int contentH;
	private @Nullable SectionGrid grid;
	private List<PickerEntry> visible = List.of();

	public PickerScreen(PickerSpec spec, @Nullable Screen parent) {
		super(Component.literal(spec.title()));
		this.spec = spec;
		this.parent = parent;
		this.model = new PickerModel<>(spec.entries(), PickerEntry::key, spec.groupOrder());
		model.setFavourites(PickerStore.memory().favourites(spec.id()));
		model.select(spec.current());
		favOnly.onPress(() -> {
			model.setFavouritesOnly(!model.favouritesOnly());
			gridScroll = 0;
		});
		tags.disabled(true).tooltip("Tag filters are coming");
		compare.disabled(true).tooltip("Side-by-side compare is coming");
		pick.primary(true);
		empty.action("Clear search", this::clearSearch);
	}

	public PickerSpec spec() {
		return spec;
	}

	public PickerModel<PickerEntry> model() {
		return model;
	}

	public @Nullable Screen parent() {
		return parent;
	}

	@Override
	protected void init() {
		minecraft.onTextInputFocusChange(this, true);
	}

	@Override
	public void removed() {
		minecraft.onTextInputFocusChange(this, false);
		super.removed();
	}

	@Override
	public boolean isPauseScreen() {
		return parent != null && parent.isPauseScreen();
	}

	@Override
	public void onClose() {
		back();
	}

	/** Back to the parent screen (or close). */
	public void back() {
		minecraft.gui.setScreen(parent);
	}

	/** QA / DevBridge: type into the search box. */
	public void setQuery(String q) {
		search.set(q);
		queryChanged();
	}

	private void clearSearch() {
		search.clear();
		model.setFavouritesOnly(false);
		queryChanged();
		focus = Focus.SEARCH;
	}

	private void queryChanged() {
		if (!search.value().equals(model.query())) {
			model.setQuery(search.value());
			model.setGroup(null);
			gridScroll = 0;
		}
	}

	private @Nullable PickerEntry detailEntry() {
		if (visible.isEmpty()) {
			return null;
		}
		if (hoveredId != null && !keyLast) {
			PickerEntry h = spec.byId(hoveredId);
			if (h != null) {
				return h;
			}
		}
		PickerEntry s = model.selected();
		if (s != null) {
			return s;
		}
		if (!visible.isEmpty() && focus != Focus.SEARCH) {
			return visible.get(0);
		}
		PickerEntry cur = spec.byId(spec.current());
		return cur != null ? cur : visible.isEmpty() ? null : visible.get(0);
	}

	/** Picks the selection (or, from the search box, the first match). */
	public void pickSelected() {
		PickerEntry e = model.selected();
		if (e == null && !visible.isEmpty()) {
			e = visible.get(0);
		}
		if (e != null) {
			pick(e);
		}
	}

	private void pick(PickerEntry e) {
		spec.pick(e);
		back();
	}

	private void toggleFavourite() {
		PickerEntry e = detailEntry();
		if (e != null) {
			PickerStore.toggleFavourite(spec.id(), e.id());
			model.setFavourites(PickerStore.memory().favourites(spec.id()));
		}
	}

	// ------------------------------------------------------------------ drawing

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		g.fillGradient(0, 0, width, height, UiStyle.withAlpha(UiStyle.INK, 110), UiStyle.withAlpha(UiStyle.INK, 160));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float a) {
		super.extractRenderState(g, mx, my, a);
		Font f = font;
		int px = 6;
		int py = 6;
		int pw = width - 12;
		int ph = height - 12;
		// panel: cream with a walnut frame
		g.fill(px, py, px + pw, py + ph, UiStyle.CREAM);
		Paint.border(g, px, py, pw, ph, UiStyle.WALNUT);
		Paint.border(g, px + 1, py + 1, pw - 2, ph - 2, UiStyle.WALNUT);
		Paint.border(g, px + 2, py + 2, pw - 4, ph - 4, UiStyle.PAPER);
		int ix = px + 2;
		int iw = pw - 4;
		int y = py + 2;
		drawTitle(g, f, ix, y, iw, mx, my);
		y += TITLE_H;
		y += drawHead(g, f, ix + 6, y + ROW_GAP, iw - 12, mx, my) + ROW_GAP * 2;
		g.fill(ix, y - ROW_GAP, ix + iw, y - ROW_GAP + 1, UiStyle.rule());
		y += drawRecents(g, f, ix + 6, y, iw - 12, mx, my) + ROW_GAP;
		g.fill(ix, y - 1, ix + iw, y, UiStyle.rule());
		int mainY = y;
		int mainH = py + ph - 2 - mainY;
		int railW = Math.max(70, Math.min(130, iw / 6));
		int detailW = Math.max(104, Math.min(200, iw * 26 / 100));
		int gridX = ix + railW + 1;
		int gridW = iw - railW - detailW - 2;
		drawRail(g, f, ix, mainY, railW, mainH, mx, my);
		g.fill(ix + railW, mainY, ix + railW + 1, mainY + mainH, UiStyle.rule());
		drawGrid(g, f, gridX, mainY, gridW, mainH, mx, my);
		g.fill(gridX + gridW, mainY, gridX + gridW + 1, mainY + mainH, UiStyle.rule());
		drawDetail(g, f, gridX + gridW + 1, mainY, detailW, mainH, mx, my);
		tooltip.offer(tags, mx, my);
		tooltip.offer(compare, mx, my);
		for (Chip c : recentChips) {
			tooltip.offer(c, mx, my);
		}
		tooltip.draw(g, f, mx, my);
	}

	private void drawTitle(GuiGraphicsExtractor g, Font f, int x, int y, int w, int mx, int my) {
		g.fill(x, y, x + w, y + TITLE_H, UiStyle.titleBar());
		int bw = f.width(back.label()) + 14;
		back.draw(g, f, x + 3, y + 2, bw, Button.MIN_H, mx, my);
		int tx = x + 3 + bw + 8;
		String hint = spec.subtitle() != null ? spec.subtitle() : "Arrows move · Enter pick · Esc back";
		int hw = f.width(hint);
		int room = x + w - 8 - tx;
		boolean showHint = hw + 12 + Math.min(f.width(spec.title()), 80) <= room;
		g.text(f, TextUtil.ellipsize(f, spec.title(), showHint ? room - hw - 12 : room), tx, y + 7, UiStyle.titleText(), false);
		if (showHint) {
			g.text(f, hint, x + w - 8 - hw, y + 7, UiStyle.withAlpha(UiStyle.titleText(), 190), false);
		}
	}

	private TextFieldView.Style searchStyle() {
		String hint = search.isEmpty() ? null : model.count(null) + " found";
		return new TextFieldView.Style(ScrollList.SEARCH_GLYPH, UiStyle.mutedText(), "Search " + spec.entries().size() + " " + spec.noun() + "…", null, hint, UiStyle.mutedText(), 1);
	}

	private int drawHead(GuiGraphicsExtractor g, Font f, int x, int y, int w, int mx, int my) {
		favOnly.selected(model.favouritesOnly());
		int chipsW = favOnly.width(f) + tags.width(f) + compare.width(f) + 12;
		boolean showChips = w - chipsW >= 120;
		sw = showChips ? w - chipsW - 6 : w;
		sx = x;
		sy = y;
		searchView.draw(g, f, search, sx, sy, sw, focus == Focus.SEARCH, searchStyle());
		if (showChips) {
			int cx = x + sw + 6;
			cx += favOnly.draw(g, f, cx, y, mx, my) + 4;
			cx += tags.draw(g, f, cx, y, mx, my) + 4;
			compare.draw(g, f, cx, y, mx, my);
		} else {
			favOnly.hide();
			tags.hide();
			compare.hide();
		}
		return TextFieldView.BASE_H;
	}

	private int drawRecents(GuiGraphicsExtractor g, Font f, int x, int y, int w, int mx, int my) {
		List<String> ids = PickerStore.memory().recents(spec.id());
		if (!ids.equals(recentIds)) {
			recentIds = List.copyOf(ids);
			recentChips.clear();
			for (PickerEntry e : spec.resolve(ids)) {
				Chip c = new Chip(e.label()).swatch(e.swatch()).icon(e.swatch() == null ? e.icon() : null);
				c.tooltip(e.subtitle());
				c.onPress(() -> pick(e));
				recentChips.add(c);
			}
		}
		String label = "Recent";
		g.text(f, label, x, y + 5, UiStyle.mutedText(), false);
		int cx = x + f.width(label) + 8;
		if (recentChips.isEmpty()) {
			g.text(f, "nothing picked yet", cx, y + 5, UiStyle.color("ink_ui.ghost_on_paper", 0xFFBCAD95), false);
		}
		for (Chip c : recentChips) {
			int cw = c.width(f);
			if (cx + cw > x + w) {
				c.hide();
				continue;
			}
			c.draw(g, f, cx, y, mx, my);
			cx += cw + 4;
		}
		return Chip.H;
	}

	// ------------------------------------------------------------------ rail

	private List<@Nullable String> railItems() {
		List<@Nullable String> out = new ArrayList<>();
		out.add(null);
		out.addAll(model.groups());
		return out;
	}

	private void drawRail(GuiGraphicsExtractor g, Font f, int x, int y, int w, int h, int mx, int my) {
		rx = x;
		ry = y + 3;
		rw = w;
		rh = h - 6;
		List<@Nullable String> items = railItems();
		int view = Math.max(1, rh / RAIL_ROW);
		railScroll = Math.max(0, Math.min(railScroll, Math.max(0, items.size() - view)));
		g.enableScissor(rx, ry, rx + rw, ry + rh);
		for (int i = railScroll; i < items.size() && (i - railScroll) < view; i++) {
			String grp = items.get(i);
			int yy = ry + (i - railScroll) * RAIL_ROW;
			boolean on = grp == null ? model.group() == null : grp.equals(model.group());
			boolean hov = mx >= rx && mx < rx + rw && my >= yy && my < yy + RAIL_ROW;
			if (on) {
				g.fill(rx, yy, rx + rw, yy + RAIL_ROW, UiStyle.selected());
				g.fill(rx, yy, rx + 3, yy + RAIL_ROW, UiStyle.CLAY);
				if (focus == Focus.RAIL) {
					Paint.border(g, rx, yy, rw, RAIL_ROW, UiStyle.focusRing());
				}
			} else if (hov) {
				g.fill(rx, yy, rx + rw, yy + RAIL_ROW, UiStyle.rowHover());
			}
			int n = model.count(grp);
			String count = Integer.toString(n);
			int cw = f.width(count);
			String name = grp == null ? "All" : grp.isEmpty() ? "Other" : grp;
			g.text(f, TextUtil.ellipsize(f, name, rw - cw - 16), rx + 7, yy + 3, n == 0 && !on ? UiStyle.color("paper.disabled", 0xFFA39B8E) : UiStyle.INK, false);
			g.text(f, count, rx + rw - 5 - cw, yy + 3, UiStyle.mutedText(), false);
		}
		g.disableScissor();
	}

	// ------------------------------------------------------------------ grid

	private boolean cards() {
		return spec.layout() == PickerSpec.Layout.CARDS;
	}

	private void layoutGrid(int w) {
		int minW = cards() ? 96 : 58;
		cellH = cards() ? 66 : 38;
		int inner = w - 8 - BAR_W - 2;
		cols = Math.max(1, (inner + GAP) / (minW + GAP));
		cellW = (inner - GAP * (cols - 1)) / cols;
		visible = model.visible();
		grid = model.grid(cols, true);
		rowY = new int[grid.rows()];
		int yy = 0;
		for (int r = 0; r < grid.rows(); r++) {
			rowY[r] = yy;
			yy += grid.heading(r) ? HEAD_H : cellH + GAP;
		}
		contentH = yy;
	}

	private void drawGrid(GuiGraphicsExtractor g, Font f, int x, int y, int w, int h, int mx, int my) {
		gx = x;
		gy = y;
		gw = w;
		gh = h;
		layoutGrid(w);
		hoveredId = null;
		if (visible.isEmpty()) {
			if (model.favouritesOnly() && search.isEmpty()) {
				empty.line("No favourites yet: select one and press F, or use ☆ Favourite.");
				empty.action("Show all", () -> model.setFavouritesOnly(false));
			} else {
				empty.line("Nothing matches “" + search.value().strip() + "”.");
				empty.action("Clear search", this::clearSearch);
			}
			empty.draw(g, f, x, y, w, h, mx, my);
			return;
		}
		int viewH = h - 8;
		gridScroll = Math.max(0, Math.min(gridScroll, ScrollMath.maxOffset(contentH, viewH)));
		int top = y + 4;
		int left = x + 4;
		g.enableScissor(x + 1, top, x + w - BAR_W - 3, top + viewH);
		int sel = model.selectedIndex();
		SectionGrid sg = grid;
		List<PickerModel.Section<PickerEntry>> sections = model.sections();
		for (int r = 0; r < sg.rows(); r++) {
			int yy = top + rowY[r] - gridScroll;
			int rowH = sg.heading(r) ? HEAD_H : cellH;
			if (yy + rowH < top || yy > top + viewH) {
				continue;
			}
			if (sg.heading(r)) {
				String name = sections.get(sg.section(r)).group();
				g.text(f, name.isEmpty() ? "Other" : name, left + 1, yy + 3, UiStyle.mutedText(), false);
				continue;
			}
			for (int c = 0; c < sg.rowLength(r); c++) {
				int i = sg.at(r, c);
				PickerEntry e = visible.get(i);
				int cx = left + c * (cellW + GAP);
				boolean hov = mx >= cx && mx < cx + cellW && my >= Math.max(yy, top) && my < Math.min(yy + cellH, top + viewH) && mx < x + w - BAR_W - 3;
				if (hov) {
					hoveredId = e.id();
				}
				if (cards()) {
					drawCard(g, f, e, cx, yy, cellW, cellH, i == sel, hov, focus == Focus.GRID && i == sel);
				} else {
					drawCell(g, f, e, cx, yy, cellW, cellH, i == sel, hov, focus == Focus.GRID && i == sel);
				}
			}
		}
		g.disableScissor();
		if (revealPending) {
			// the first frame: a mouse resting where the screen opened does not count as a hover
			revealPending = false;
			hoverShown = hoveredId;
			revealSelection();
		}
		if (hoveredId != null && !hoveredId.equals(hoverShown)) {
			keyLast = false;
		}
		hoverShown = hoveredId;
		if (contentH > viewH) {
			int bx = x + w - BAR_W - 2;
			Panels.sprite(g, Kit.SCROLL_TRACK, bx, top, BAR_W, viewH);
			ScrollMath.Thumb t = ScrollMath.thumb(viewH, contentH, viewH, gridScroll);
			boolean hot = dragGrab >= 0 || mx >= bx && t.contains(top, my) && mx < bx + BAR_W;
			Panels.sprite(g, hot ? Kit.SCROLL_THUMB_HOVER : Kit.SCROLL_THUMB, bx, top + t.y(), BAR_W, t.h());
		}
	}

	private void drawThumb(GuiGraphicsExtractor g, Font f, PickerEntry e, int x, int y, int w, int h, int iconSize) {
		if (e.preview() != null) {
			e.preview().draw(g, f, x, y, w, h);
		} else if (e.swatch() != null) {
			Paint.swatch(g, x, y, w, h, e.swatch());
		} else if (e.icon() != null) {
			Paint.box(g, x, y, w, h, UiStyle.PAPER, UiStyle.rule());
			Paint.item(g, e.icon().get(), x + (w - iconSize) / 2, y + (h - iconSize) / 2, iconSize);
		} else {
			Paint.box(g, x, y, w, h, UiStyle.PAPER, UiStyle.rule());
			String l = e.label().isEmpty() ? "?" : e.label().substring(0, 1);
			g.text(f, l, x + (w - f.width(l)) / 2, y + (h - 8) / 2, UiStyle.mutedText(), false);
		}
	}

	private String name(PickerEntry e) {
		return (model.isFavourite(e.id()) ? "★ " : "") + e.label();
	}

	private void drawCell(GuiGraphicsExtractor g, Font f, PickerEntry e, int x, int y, int w, int h, boolean sel, boolean hov, boolean focusOn) {
		if (sel) {
			Paint.box(g, x, y, w, h, UiStyle.selected(), UiStyle.selectedEdge());
		} else if (hov) {
			Paint.box(g, x, y, w, h, UiStyle.rowHover(), UiStyle.rowHoverEdge());
		}
		if (focusOn) {
			Paint.focus(g, x, y, w, h);
		}
		drawThumb(g, f, e, x + 3, y + 3, w - 6, h - 16, 16);
		g.text(f, TextUtil.ellipsize(f, name(e), w - 6), x + 3, y + h - 11, UiStyle.INK, false);
	}

	private void drawCard(GuiGraphicsExtractor g, Font f, PickerEntry e, int x, int y, int w, int h, boolean sel, boolean hov, boolean focusOn) {
		Paint.box(g, x, y, w, h, UiStyle.rowHover(), sel ? UiStyle.selectedEdge() : hov ? UiStyle.selectedEdge() : UiStyle.rowHoverEdge());
		if (sel) {
			Paint.border(g, x + 1, y + 1, w - 2, h - 2, UiStyle.selectedEdge());
		}
		if (focusOn) {
			Paint.focus(g, x, y, w, h);
		}
		int sheetH = h - 26;
		drawThumb(g, f, e, x + 1, y + 1, w - 2, sheetH, 24);
		g.fill(x + 1, y + 1 + sheetH, x + w - 1, y + 2 + sheetH, UiStyle.rowHoverEdge());
		g.text(f, TextUtil.ellipsize(f, name(e), w - 8), x + 4, y + sheetH + 4, UiStyle.INK, false);
		drawTags(g, f, e.tags(), x + 4, y + sheetH + 14, w - 8);
	}

	/** Small tag boxes in one row, the rest folded into "+N". */
	private static void drawTags(GuiGraphicsExtractor g, Font f, List<String> tags, int x, int y, int w) {
		int[] widths = new int[tags.size()];
		for (int i = 0; i < widths.length; i++) {
			widths[i] = f.width(tags.get(i)) + 6;
		}
		ChipFlow.Layout l = ChipFlow.layout(widths, w, 3, 1, n -> f.width("+" + n) + 6);
		for (ChipFlow.Placed p : l.placed()) {
			tag(g, f, TextUtil.ellipsize(f, tags.get(p.index()), p.w() - 6), x + p.x(), y, p.w());
		}
		if (l.more() != null) {
			tag(g, f, "+" + l.hidden(), x + l.more().x(), y, l.more().w());
		}
	}

	private static void tag(GuiGraphicsExtractor g, Font f, String s, int x, int y, int w) {
		g.fill(x, y, x + w, y + 10, 0xFFE3D5BF);
		g.text(f, s, x + 3, y + 1, 0xFF5A4636, false);
	}

	// ------------------------------------------------------------------ detail

	private void drawDetail(GuiGraphicsExtractor g, Font f, int x, int y, int w, int h, int mx, int my) {
		PickerEntry e = detailEntry();
		int pad = 6;
		int cx = x + pad;
		int cw = w - pad * 2;
		int cy = y + pad;
		if (e == null) {
			pick.hide();
			fav.hide();
			g.text(f, TextUtil.ellipsize(f, "Nothing to show", cw), cx, cy, UiStyle.mutedText(), false);
			return;
		}
		int bigH = cards() ? Math.min(60, h / 3) : 30;
		drawThumb(g, f, e, cx, cy, cw, bigH, 32);
		cy += bigH + 5;
		g.text(f, TextUtil.ellipsize(f, e.label(), cw), cx, cy, UiStyle.link(), false);
		cy += 10;
		if (e.subtitle() != null) {
			g.text(f, TextUtil.ellipsize(f, e.subtitle(), cw), cx, cy, UiStyle.mutedText(), false);
			cy += 10;
		}
		int buttonsH = Button.H + 6;
		boolean stack = Button.width(f, spec.pickLabel()) + fav.width(f) + 4 > cw;
		if (stack) {
			buttonsH += Button.H + 4;
		}
		int bottom = y + h - pad - buttonsH;
		if (spec.detailPreview() != null && cy + 6 + 40 <= bottom) {
			cy += 3;
			spec.detailPreview().draw(g, f, e, cx, cy, cw, 40);
			cy += 44;
		}
		List<String> lines = new ArrayList<>();
		if (!e.group().isEmpty()) {
			lines.add("Family: " + e.group());
		}
		if (cards() && !e.tags().isEmpty()) {
			lines.add("Tags: " + String.join(", ", e.tags()));
		}
		lines.addAll(e.details());
		cy += 2;
		for (String line : lines) {
			for (String l : TextUtil.wrapPlain(f, line, cw)) {
				if (cy + 9 > bottom) {
					break;
				}
				g.text(f, l, cx, cy, UiStyle.INK, false);
				cy += 10;
			}
		}
		if (cards() && cy + 9 <= bottom) {
			g.text(f, TextUtil.ellipsize(f, "Compare with… (later)", cw), cx, cy + 2, UiStyle.color("paper.disabled", 0xFFA39B8E), false);
		}
		boolean isFav = model.isFavourite(e.id());
		fav.label(isFav ? "★ Unfavourite" : "☆ Favourite");
		pick.label(spec.pickLabel());
		int by = y + h - pad - Button.H;
		if (stack) {
			pick.draw(g, f, cx, by - Button.H - 4, cw, Button.H, mx, my);
			fav.draw(g, f, cx, by, cw, Button.H, mx, my);
		} else {
			int pw = pick.width(f);
			pick.draw(g, f, cx, by, pw, Button.H, mx, my);
			fav.draw(g, f, cx + pw + 4, by, fav.width(f), Button.H, mx, my);
		}
	}

	// ------------------------------------------------------------------ input

	private void revealSelection() {
		int i = model.selectedIndex();
		if (i < 0 || grid == null || i >= grid.count()) {
			return;
		}
		int r = grid.rowOf(i);
		int top = rowY[r];
		if (r > 0 && grid.heading(r - 1)) {
			top = rowY[r - 1];
		}
		int bottom = rowY[r] + cellH;
		int viewH = gh - 8;
		if (top < gridScroll) {
			gridScroll = top;
		} else if (bottom > gridScroll + viewH) {
			gridScroll = bottom - viewH;
		}
	}

	private int pageRows() {
		return Math.max(1, (gh - 8) / (cellH + GAP));
	}

	@Override
	public boolean keyPressed(KeyEvent e) {
		keyLast = true;
		if (e.isEscape()) {
			back();
			return true;
		}
		if (NavKeys.isTab(e)) {
			Focus[] order = Focus.values();
			int d = e.hasShiftDown() ? order.length - 1 : 1;
			focus = order[(focus.ordinal() + d) % order.length];
			if (focus == Focus.GRID && model.selectedIndex() < 0) {
				model.move(ListNav.Key.HOME, cols, true, pageRows());
			}
			revealSelection();
			return true;
		}
		ListNav.Key k = NavKeys.nav(e);
		switch (focus) {
			case SEARCH -> {
				if (k == ListNav.Key.DOWN || k == ListNav.Key.PAGE_DOWN) {
					focus = Focus.GRID;
					if (model.selectedIndex() < 0) {
						model.move(ListNav.Key.HOME, cols, true, pageRows());
					}
					revealSelection();
					return true;
				}
				if (NavKeys.isEnter(e)) {
					pickSelected();
					return true;
				}
				if (TextKeys.handle(e, search)) {
					queryChanged();
					return true;
				}
				return super.keyPressed(e);
			}
			case GRID -> {
				if (k != null) {
					if (!model.move(k, cols, true, pageRows()) && k == ListNav.Key.UP) {
						focus = Focus.SEARCH;
					}
					revealSelection();
					return true;
				}
				if (NavKeys.activates(e)) {
					pickSelected();
					return true;
				}
				if (e.key() == InputConstants.KEY_F && !e.hasControlDown()) {
					toggleFavourite();
					return true;
				}
				if (e.key() == InputConstants.KEY_BACKSPACE) {
					focus = Focus.SEARCH;
					return TextKeys.handle(e, search) && setQueryFromField();
				}
				return super.keyPressed(e);
			}
			case RAIL -> {
				List<@Nullable String> items = railItems();
				int at = items.indexOf(model.group());
				if (k == ListNav.Key.UP || k == ListNav.Key.DOWN) {
					int next = Math.max(0, Math.min(items.size() - 1, at + (k == ListNav.Key.UP ? -1 : 1)));
					model.setGroup(items.get(next));
					gridScroll = 0;
					int view = Math.max(1, rh / RAIL_ROW);
					if (next < railScroll) {
						railScroll = next;
					} else if (next >= railScroll + view) {
						railScroll = next - view + 1;
					}
					return true;
				}
				if (k == ListNav.Key.RIGHT || NavKeys.activates(e)) {
					focus = Focus.GRID;
					if (model.selectedIndex() < 0) {
						model.move(ListNav.Key.HOME, cols, true, pageRows());
					}
					revealSelection();
					return true;
				}
				return super.keyPressed(e);
			}
		}
		return super.keyPressed(e);
	}

	private boolean setQueryFromField() {
		queryChanged();
		return true;
	}

	@Override
	public boolean charTyped(CharacterEvent e) {
		if (e.codepoint() < 32) {
			return false;
		}
		if (focus != Focus.SEARCH) {
			// F is the favourite key in the grid; any other letter starts a search
			if (focus == Focus.GRID && (e.codepoint() == 'f' || e.codepoint() == 'F')) {
				return true;
			}
			focus = Focus.SEARCH;
		}
		search.insert(e.codepointAsString());
		queryChanged();
		return true;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
		double mx = e.x();
		double my = e.y();
		int b = e.button();
		if (back.mouseClicked(mx, my, b) || favOnly.mouseClicked(mx, my, b) || tags.mouseClicked(mx, my, b) || compare.mouseClicked(mx, my, b)
			|| pick.mouseClicked(mx, my, b) || fav.mouseClicked(mx, my, b)) {
			return true;
		}
		for (Chip c : List.copyOf(recentChips)) {
			if (c.mouseClicked(mx, my, b)) {
				return true;
			}
		}
		if (mx >= sx && mx < sx + sw && my >= sy && my < sy + TextFieldView.BASE_H) {
			focus = Focus.SEARCH;
			int at = searchView.hit(font, search, sx, sy, sw, searchStyle(), mx, my);
			if (at >= 0) {
				search.moveTo(at, false);
			}
			return true;
		}
		if (mx >= rx && mx < rx + rw && my >= ry && my < ry + rh) {
			int i = railScroll + (int) ((my - ry) / RAIL_ROW);
			List<@Nullable String> items = railItems();
			if (i >= 0 && i < items.size()) {
				model.setGroup(items.get(i));
				gridScroll = 0;
				focus = Focus.RAIL;
			}
			return true;
		}
		if (mx >= gx && mx < gx + gw && my >= gy && my < gy + gh) {
			if (visible.isEmpty()) {
				return empty.mouseClicked(mx, my, b);
			}
			int top = gy + 4;
			int viewH = gh - 8;
			int bx = gx + gw - BAR_W - 2;
			if (contentH > viewH && mx >= bx) {
				ScrollMath.Thumb t = ScrollMath.thumb(viewH, contentH, viewH, gridScroll);
				if (t.contains(top, my)) {
					dragGrab = (int) my - top - t.y();
				} else {
					gridScroll = ScrollMath.pageTowards(viewH, contentH, viewH, gridScroll, (int) my - top);
				}
				return true;
			}
			if (hoveredId != null) {
				PickerEntry h = spec.byId(hoveredId);
				model.select(hoveredId);
				focus = Focus.GRID;
				if (h != null) {
					pick(h);
				}
			}
			return true;
		}
		return super.mouseClicked(e, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
		if (dragGrab >= 0) {
			int viewH = gh - 8;
			gridScroll = ScrollMath.offsetForThumbTop(viewH, contentH, viewH, (int) e.y() - (gy + 4) - dragGrab);
			return true;
		}
		return super.mouseDragged(e, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent e) {
		if (dragGrab >= 0) {
			dragGrab = -1;
			return true;
		}
		return super.mouseReleased(e);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
		if (mx >= gx && mx < gx + gw && my >= gy && my < gy + gh) {
			gridScroll = Math.max(0, Math.min(gridScroll - (int) Math.signum(scrollY) * (cellH + GAP) / 2 * 2, ScrollMath.maxOffset(contentH, gh - 8)));
			return true;
		}
		if (mx >= rx && mx < rx + rw && my >= ry && my < ry + rh) {
			railScroll = Math.max(0, railScroll - (int) Math.signum(scrollY));
			return true;
		}
		return super.mouseScrolled(mx, my, scrollX, scrollY);
	}

	/** QA: what the screen shows, for scripted checks. */
	public String debugState() {
		PickerEntry d = detailEntry();
		return "focus=" + focus + " query=" + search.value() + " group=" + model.group() + " visible=" + visible.size() + " selected="
			+ model.selectedId() + " detail=" + (d == null ? null : d.id()) + " favs=" + PickerStore.memory().favourites(spec.id());
	}
}
