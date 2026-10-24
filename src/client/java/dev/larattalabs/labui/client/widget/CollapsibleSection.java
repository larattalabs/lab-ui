package dev.larattalabs.labui.client.widget;

import dev.larattalabs.labui.client.ui.TextUtil;
import dev.larattalabs.labui.client.ui.UiStyle;
import dev.larattalabs.labui.client.picker.PickerStore;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jspecify.annotations.Nullable;

/**
 * A section header with a chevron, a title and a muted count / summary ("· 8 on: chimney, porch, +5"); a click, or Enter
 * / Space while focused, folds it. Its open state is remembered by key on disk ({@code config/lab_ui/pickers.json}; shared
 * by every screen and mod that uses the key, so prefix keys with your mod id: {@code architect.design.features}). Until
 * the player first folds it, the caller's default applies.
 *
 * <pre>
 * int y0 = y;
 * y += features.draw(g, font, x, y, w, mx, my);
 * if (features.open()) { y += drawFeatureChips(g, x + CollapsibleSection.INDENT, y); }
 * </pre>
 */
public final class CollapsibleSection extends Control {
	public static final int HEADER_H = 16;
	/** Indent for a section's content, under the title. */
	public static final int INDENT = 12;
	private final String key;
	private final boolean defaultOpen;
	private String title;
	private @Nullable String summary;

	public CollapsibleSection(String key, String title, boolean defaultOpen) {
		this.key = key;
		this.title = title;
		this.defaultOpen = defaultOpen;
		this.onPress = this::toggle;
	}

	public static boolean isOpen(String key, boolean defaultOpen) {
		Boolean b = PickerStore.memory().section(key);
		return b == null ? defaultOpen : b;
	}

	/** Remembers the state on disk ({@code config/lab_ui/pickers.json}, with the pickers' memory). */
	public static void setOpen(String key, boolean open) {
		PickerStore.section(key, open);
	}

	public String key() {
		return key;
	}

	public boolean open() {
		return isOpen(key, defaultOpen);
	}

	public void open(boolean open) {
		setOpen(key, open);
	}

	public void toggle() {
		open(!open());
	}

	public CollapsibleSection title(String t) {
		title = t;
		return this;
	}

	/** The muted text after the title, e.g. "8 on: chimney, porch, +5" (null = none). */
	public CollapsibleSection summary(@Nullable String s) {
		summary = s;
		return this;
	}

	/** Shorthand for a plain count. */
	public CollapsibleSection count(int n) {
		summary = Integer.toString(n);
		return this;
	}

	/** Draws the header (a rule above, chevron, title, summary); returns {@link #HEADER_H}. */
	public int draw(GuiGraphicsExtractor g, Font font, int x, int y, int w, int mx, int my) {
		placed(x, y, w, HEADER_H);
		boolean hover = hot(mx, my);
		g.fill(x, y, x + w, y + 1, UiStyle.rule());
		if (hover) {
			g.fill(x, y + 1, x + w, y + HEADER_H, UiStyle.rowHover());
		}
		if (focused) {
			Paint.focus(g, x, y + 1, w, HEADER_H - 1);
		}
		int c = UiStyle.link();
		Paint.chevron(g, x + 2, y + 6, open(), c);
		int tx = x + INDENT;
		g.text(font, title, tx, y + 5, c, false);
		if (summary != null && !summary.isEmpty()) {
			int sx = tx + font.width(title) + 4;
			g.text(font, TextUtil.ellipsize(font, "· " + summary, x + w - sx - 2), sx, y + 5, UiStyle.mutedText(), false);
		}
		return HEADER_H;
	}
}
