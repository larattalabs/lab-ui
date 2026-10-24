package dev.larattalabs.labui.client.widget;

import dev.larattalabs.labui.client.ui.TextUtil;
import dev.larattalabs.labui.client.ui.UiStyle;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * An 18 px chip: a choice in a row (selected = the clay-tinted fill), a toggle, or a dashed "more…" / "+N" chip. It
 * may lead with a material swatch or an item icon. Hover, keyboard focus (brass outline), disabled and Enter / Space
 * activation come from {@link Control}.
 */
public class Chip extends Control {
	public static final int H = 18;
	private static final int PAD = 7;
	private static final int ICON = 10;

	public enum Kind {
		/** A choice or toggle. */
		NORMAL,
		/** "more…" / "+N": dashed, no fill, link-coloured. */
		MORE
	}

	private String label;
	private Kind kind = Kind.NORMAL;
	private boolean selected;
	private @Nullable Integer swatch;
	private @Nullable Supplier<ItemStack> icon;

	public Chip(String label) {
		this.label = label;
	}

	public Chip(String label, Runnable onPress) {
		this.label = label;
		this.onPress = onPress;
	}

	public Chip label(String l) {
		label = l;
		return this;
	}

	public String label() {
		return label;
	}

	public Chip kind(Kind k) {
		kind = k;
		return this;
	}

	public Kind kind() {
		return kind;
	}

	public Chip selected(boolean s) {
		selected = s;
		return this;
	}

	public boolean isSelected() {
		return selected;
	}

	/** A leading 10 px material swatch (RGB). */
	public Chip swatch(@Nullable Integer rgb) {
		swatch = rgb;
		return this;
	}

	/** A leading 10 px item icon. */
	public Chip icon(@Nullable Supplier<ItemStack> stack) {
		icon = stack;
		return this;
	}

	private boolean hasLead() {
		return swatch != null || icon != null;
	}

	public int width(Font font) {
		return PAD + (hasLead() ? ICON + 4 : 0) + font.width(label) + PAD;
	}

	/** Draws at its natural width; returns the width. */
	public int draw(GuiGraphicsExtractor g, Font font, int x, int y, int mx, int my) {
		int cw = width(font);
		draw(g, font, x, y, cw, mx, my);
		return cw;
	}

	/** Draws at width {@code w} (the label is ellipsized to fit). */
	public void draw(GuiGraphicsExtractor g, Font font, int x, int y, int w, int mx, int my) {
		placed(x, y, w, H);
		boolean hover = hot(mx, my);
		if (focused && !disabled) {
			Paint.focus(g, x, y, w, H);
		}
		int text;
		if (kind == Kind.MORE) {
			if (hover) {
				g.fill(x, y, x + w, y + H, UiStyle.rowHover());
			}
			Paint.dashed(g, x, y, w, H, disabled ? UiStyle.rule() : UiStyle.selectedEdge());
			text = disabled ? UiStyle.color("paper.disabled", 0xFFA39B8E) : UiStyle.link();
		} else if (disabled) {
			Paint.box(g, x, y, w, H, UiStyle.withAlpha(UiStyle.chip(), 140), UiStyle.rule());
			text = UiStyle.color("paper.disabled", 0xFFA39B8E);
		} else if (selected) {
			Paint.box(g, x, y, w, H, UiStyle.selected(), UiStyle.selectedEdge());
			text = UiStyle.INK;
		} else {
			Paint.box(g, x, y, w, H, hover ? UiStyle.rowHover() : UiStyle.chip(), hover ? UiStyle.selectedEdge() : UiStyle.chipEdge());
			text = UiStyle.INK;
		}
		int tx = x + PAD;
		if (swatch != null) {
			Paint.swatch(g, tx, y + 4, ICON, ICON, swatch);
			tx += ICON + 4;
		} else if (icon != null) {
			Paint.item(g, icon.get(), tx, y + 4, ICON);
			tx += ICON + 4;
		}
		g.text(font, TextUtil.ellipsize(font, label, x + w - PAD - tx + 2), tx, y + 5, text, false);
	}
}
