package dev.larattalabs.labui.client.widget;

import dev.larattalabs.labui.client.text.TextFieldView;
import dev.larattalabs.labui.client.text.TextKeys;
import dev.larattalabs.labui.client.text.TextModel;
import dev.larattalabs.labui.client.ui.Kit;
import dev.larattalabs.labui.client.ui.Panels;
import dev.larattalabs.labui.client.ui.TextUtil;
import dev.larattalabs.labui.client.ui.UiStyle;
import dev.larattalabs.labui.ui.ListNav;
import dev.larattalabs.labui.ui.ScrollMath;
import dev.larattalabs.labui.ui.TextMatch;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

/**
 * A scrolling list with a fixed row height, in a kit inset: mouse wheel, a draggable scrollbar (a click on the track
 * pages), arrow keys, Home / End, PgUp / PgDn, an optional filter line above it (typing while the list is focused goes
 * there), an {@link EmptyState} when nothing is listed, and a selection that stays visible to the keyboard (selected
 * fill and clay bar; a brass outline while the list has focus). Enter or a double click activates the selection.
 *
 * <pre>
 * ScrollList&lt;Site&gt; list = new ScrollList&lt;&gt;(ScrollList.ROW_H, (g, font, s, x, y, w, h, sel, hov) -&gt;
 *     g.text(font, s.name(), x + 4, y + 5, UiStyle.INK, false)).filter(Site::name, "Filter sites…");
 * list.items(sites);
 * list.draw(g, font, x, y, w, h, mx, my);  // and forward mouse / key / char events
 * </pre>
 */
public final class ScrollList<T> {
	/** The standard row height (one line of text with room for an 16 px icon). */
	public static final int ROW_H = 18;
	public static final int BAR_W = 6;
	public static final int FILTER_GAP = 3;
	private static final long DOUBLE_CLICK_MS = 350;
	private static final TextFieldView.Style FILTER_HIT = new TextFieldView.Style(null, 0, null, null, null, 0, 1);

	/** Paints one row's content (the list draws the selection and hover backgrounds first). */
	@FunctionalInterface
	public interface RowPainter<T> {
		void paint(GuiGraphicsExtractor g, Font font, T item, int x, int y, int w, int h, boolean selected, boolean hovered);
	}

	private final int rowH;
	private final RowPainter<T> painter;
	private final ListNav nav = new ListNav();
	private List<T> items = List.of();
	private List<T> shown = List.of();
	private @Nullable Function<T, String> filterText;
	private Function<T, Collection<String>> filterExtra = t -> List.of();
	private final TextModel filter = new TextModel(100);
	private final TextFieldView filterView = new TextFieldView();
	private String filterPlaceholder = "Filter…";
	private final EmptyState empty = new EmptyState("Nothing here yet.");
	private @Nullable Consumer<T> onActivate;
	private @Nullable Consumer<T> onSelect;
	private boolean focused;
	private String lastQuery = "";
	// last drawn geometry
	private int lx;
	private int ly;
	private int lw;
	private int lh;
	private int fx;
	private int fy;
	private int fw;
	private int barX;
	private int barY;
	private int barH;
	private long drawnAt = -1;
	private int dragGrab = -1;
	private long lastClick;
	private int lastClickIndex = -1;

	public ScrollList(int rowHeight, RowPainter<T> painter) {
		this.rowH = Math.max(10, rowHeight);
		this.painter = painter;
	}

	/** A plain text list. */
	public static <T> ScrollList<T> text(Function<T, String> label) {
		return new ScrollList<>(ROW_H, (g, font, t, x, y, w, h, sel, hov) -> g.text(font, TextUtil.ellipsize(font, label.apply(t), w - 8), x + 5, y + (h - 8)
			/ 2, UiStyle.INK, false));
	}

	/** Shows a filter line matching {@code text} (and {@code extra}, e.g. tags). */
	public ScrollList<T> filter(Function<T, String> text, String placeholder) {
		filterText = text;
		filterPlaceholder = placeholder;
		refilter();
		return this;
	}

	public ScrollList<T> filterExtra(Function<T, Collection<String>> extra) {
		filterExtra = extra;
		refilter();
		return this;
	}

	public ScrollList<T> items(List<T> items) {
		T sel = selected();
		this.items = List.copyOf(items);
		refilter();
		if (sel != null) {
			int i = shown.indexOf(sel);
			if (i >= 0) {
				nav.select(i);
			}
		}
		return this;
	}

	public List<T> items() {
		return items;
	}

	/** The items after the filter, in list order. */
	public List<T> visible() {
		return shown;
	}

	public EmptyState emptyState() {
		return empty;
	}

	public ScrollList<T> empty(String line) {
		empty.line(line);
		return this;
	}

	public ScrollList<T> onActivate(Consumer<T> c) {
		onActivate = c;
		return this;
	}

	public ScrollList<T> onSelect(Consumer<T> c) {
		onSelect = c;
		return this;
	}

	public ScrollList<T> focused(boolean f) {
		focused = f;
		return this;
	}

	public boolean isFocused() {
		return focused;
	}

	public TextModel filterModel() {
		return filter;
	}

	public ListNav nav() {
		return nav;
	}

	public @Nullable T selected() {
		int i = nav.selected();
		return i >= 0 && i < shown.size() ? shown.get(i) : null;
	}

	public void select(@Nullable T item) {
		nav.select(item == null ? -1 : shown.indexOf(item));
		fireSelect();
	}

	private void fireSelect() {
		T s = selected();
		if (s != null && onSelect != null) {
			onSelect.accept(s);
		}
	}

	private void refilter() {
		String q = filter.value();
		lastQuery = q;
		shown = filterText == null || q.isBlank() ? items : TextMatch.rank(items, q, filterText, filterExtra);
		nav.update(shown.size(), 1, nav.view());
	}

	private boolean shownNow() {
		return drawnAt >= 0 && Util.getMillis() - drawnAt <= Control.STALE_MS;
	}

	public void draw(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h, int mx, int my) {
		drawnAt = Util.getMillis();
		if (!filter.value().equals(lastQuery)) {
			refilter();
		}
		int top = y;
		if (filterText != null) {
			fx = x;
			fy = y;
			fw = w;
			filterView.draw(g, font, filter, x, y, w, focused, new TextFieldView.Style("⌕", UiStyle.mutedText(), filterPlaceholder, null,
				filter.isEmpty() ? null : shown.size() + "/" + items.size(), UiStyle.mutedText(), 1));
			top += TextFieldView.BASE_H + FILTER_GAP;
		}
		lx = x;
		ly = top;
		lw = w;
		lh = Math.max(rowH, y + h - top);
		Panels.inset(g, lx, ly, lw, lh);
		int innerX = lx + 2;
		int innerY = ly + 2;
		int innerH = lh - 4;
		int rows = Math.max(1, innerH / rowH);
		nav.update(shown.size(), 1, rows);
		boolean bar = nav.scrollable();
		int rowW = lw - 4 - (bar ? BAR_W + 1 : 0);
		if (shown.isEmpty()) {
			empty.draw(g, font, lx, ly, lw, lh, mx, my);
			return;
		}
		g.enableScissor(innerX, innerY, innerX + rowW, innerY + innerH);
		for (int r = 0; r < rows; r++) {
			int i = nav.indexAt(r, 0);
			if (i < 0) {
				break;
			}
			int ry = innerY + r * rowH;
			boolean sel = i == nav.selected();
			boolean hov = mx >= innerX && mx < innerX + rowW && my >= ry && my < ry + rowH && my < innerY + innerH;
			if (sel) {
				g.fill(innerX, ry, innerX + rowW, ry + rowH, UiStyle.selected());
				g.fill(innerX, ry, innerX + 2, ry + rowH, UiStyle.selectedEdge());
			} else if (hov) {
				g.fill(innerX, ry, innerX + rowW, ry + rowH, UiStyle.rowHover());
			}
			painter.paint(g, font, shown.get(i), innerX + (sel ? 2 : 0), ry, rowW - (sel ? 2 : 0), rowH, sel, hov);
			if (sel && focused) {
				Paint.border(g, innerX, ry, rowW, rowH, UiStyle.focusRing());
			}
		}
		g.disableScissor();
		if (bar) {
			barX = lx + lw - 2 - BAR_W;
			barY = innerY;
			barH = innerH;
			Panels.sprite(g, Kit.SCROLL_TRACK, barX, barY, BAR_W, barH);
			ScrollMath.Thumb t = ScrollMath.thumb(barH, nav.rows(), rows, nav.offset());
			boolean hot = dragGrab >= 0 || mx >= barX && mx < barX + BAR_W && t.contains(barY, my);
			Panels.sprite(g, hot ? Kit.SCROLL_THUMB_HOVER : Kit.SCROLL_THUMB, barX, barY + t.y(), BAR_W, t.h());
			if (t.h() >= 10) {
				Panels.sprite(g, Kit.SCROLL_GRIP, barX + 2, barY + t.y() + t.h() / 2 - 1, 2, 3);
			}
		} else {
			barH = 0;
		}
	}

	private boolean inList(double mx, double my) {
		return mx >= lx && mx < lx + lw && my >= ly && my < ly + lh;
	}

	private boolean inFilter(double mx, double my) {
		return filterText != null && mx >= fx && mx < fx + fw && my >= fy && my < fy + TextFieldView.BASE_H;
	}

	public boolean contains(double mx, double my) {
		return shownNow() && (inList(mx, my) || inFilter(mx, my));
	}

	public boolean mouseClicked(double mx, double my, int button) {
		if (!shownNow()) {
			return false;
		}
		if (inFilter(mx, my)) {
			focused = true;
			int at = filterView.hit(net.minecraft.client.Minecraft.getInstance().font, filter, fx, fy, fw, new TextFieldView.Style("⌕", 0, null, null, null, 0, 1), mx, my);
			if (at >= 0) {
				filter.moveTo(at, false);
			}
			return true;
		}
		if (!inList(mx, my)) {
			return false;
		}
		focused = true;
		if (shown.isEmpty()) {
			return empty.mouseClicked(mx, my, button);
		}
		if (barH > 0 && mx >= barX && mx < barX + BAR_W) {
			ScrollMath.Thumb t = ScrollMath.thumb(barH, nav.rows(), nav.view(), nav.offset());
			if (t.contains(barY, my)) {
				dragGrab = (int) my - barY - t.y();
			} else {
				nav.scrollTo(ScrollMath.pageTowards(barH, nav.rows(), nav.view(), nav.offset(), (int) my - barY));
			}
			return true;
		}
		int r = (int) ((my - ly - 2) / rowH);
		int i = nav.indexAt(r, 0);
		if (i >= 0 && button == 0) {
			long now = Util.getMillis();
			boolean dbl = i == lastClickIndex && now - lastClick <= DOUBLE_CLICK_MS;
			nav.select(i);
			fireSelect();
			lastClick = now;
			lastClickIndex = i;
			if (dbl) {
				activate();
			}
		}
		return true;
	}

	public boolean mouseDragged(double mx, double my, int button) {
		if (dragGrab < 0) {
			return false;
		}
		nav.scrollTo(ScrollMath.offsetForThumbTop(barH, nav.rows(), nav.view(), (int) my - barY - dragGrab));
		return true;
	}

	public boolean mouseReleased(double mx, double my, int button) {
		if (dragGrab >= 0) {
			dragGrab = -1;
			return true;
		}
		return false;
	}

	/** Three rows per wheel notch. */
	public boolean mouseScrolled(double mx, double my, double scrollY) {
		if (!shownNow() || !inList(mx, my) || scrollY == 0) {
			return false;
		}
		nav.scrollBy(scrollY > 0 ? -3 : 3);
		return true;
	}

	/** Navigation, Enter, and (with a filter line) editing keys while focused; Esc clears a non-empty filter. */
	public boolean keyPressed(KeyEvent e) {
		if (!focused || !shownNow()) {
			return false;
		}
		ListNav.Key k = NavKeys.nav(e);
		if (k != null && k != ListNav.Key.LEFT && k != ListNav.Key.RIGHT) {
			nav.key(k);
			fireSelect();
			return true;
		}
		if (NavKeys.isEnter(e)) {
			if (shown.isEmpty()) {
				return empty.keyPressed(e);
			}
			activate();
			return true;
		}
		if (e.isEscape() && filterText != null && !filter.isEmpty()) {
			filter.clear();
			refilter();
			return true;
		}
		if (filterText != null && TextKeys.handle(e, filter)) {
			refilter();
			return true;
		}
		return false;
	}

	public boolean charTyped(CharacterEvent e) {
		if (!focused || filterText == null || e.codepoint() < 32) {
			return false;
		}
		filter.insert(e.codepointAsString());
		refilter();
		return true;
	}

	private void activate() {
		T s = selected();
		if (s != null && onActivate != null) {
			onActivate.accept(s);
		}
	}

	/** Rows that fit in {@code h} px (with the filter line when there is one): to size a list to its content. */
	public int heightFor(int rows) {
		return (filterText != null ? TextFieldView.BASE_H + FILTER_GAP : 0) + rows * rowH + 4;
	}

	/** A short list's items as an unmodifiable copy (for tests and QA). */
	public List<T> snapshot() {
		return new ArrayList<>(shown);
	}
}
