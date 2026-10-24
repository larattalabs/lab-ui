package dev.larattalabs.labui.client.widget;

import dev.larattalabs.labui.client.ui.UiStyle;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

/** Small fill-drawn shapes the widgets share: boxes, dashed edges, material swatches, chevrons, focus outlines, icons. */
public final class Paint {
	private Paint() {
	}

	/** A 1 px border inside {@code x, y, w, h}. */
	public static void border(GuiGraphicsExtractor g, int x, int y, int w, int h, int argb) {
		if (w <= 0 || h <= 0) {
			return;
		}
		g.fill(x, y, x + w, y + 1, argb);
		g.fill(x, y + h - 1, x + w, y + h, argb);
		g.fill(x, y + 1, x + 1, y + h - 1, argb);
		g.fill(x + w - 1, y + 1, x + w, y + h - 1, argb);
	}

	/** A filled box with a 1 px border. */
	public static void box(GuiGraphicsExtractor g, int x, int y, int w, int h, int fill, int edge) {
		g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
		border(g, x, y, w, h, edge);
	}

	/** A dashed 1 px border (2 on, 2 off), for "more…" chips. */
	public static void dashed(GuiGraphicsExtractor g, int x, int y, int w, int h, int argb) {
		for (int i = 0; i < w; i += 4) {
			int e = Math.min(x + w, x + i + 2);
			g.fill(x + i, y, e, y + 1, argb);
			g.fill(x + i, y + h - 1, e, y + h, argb);
		}
		for (int i = 0; i < h; i += 4) {
			int e = Math.min(y + h, y + i + 2);
			g.fill(x, y + i, x + 1, e, argb);
			g.fill(x + w - 1, y + i, x + w, e, argb);
		}
	}

	/** A 2 px brass keyboard-focus outline 1 px outside {@code x, y, w, h} (lighter than {@code UiBits.focusRing}, for chips and cells). */
	public static void focus(GuiGraphicsExtractor g, int x, int y, int w, int h) {
		int c = UiStyle.focusRing();
		border(g, x - 2, y - 2, w + 4, h + 4, c);
		border(g, x - 3, y - 3, w + 6, h + 6, c);
	}

	/**
	 * A material swatch: the colour with a faint texture checker and a dark hairline, so neighbouring swatches of close
	 * colours stay apart.
	 */
	public static void swatch(GuiGraphicsExtractor g, int x, int y, int w, int h, int argb) {
		g.fill(x, y, x + w, y + h, 0xFF000000 | argb);
		int cell = Math.max(2, Math.min(w, h) / 4);
		for (int cy = 0; cy < h; cy += cell) {
			for (int cx = (cy / cell) % 2 == 0 ? 0 : cell; cx < w; cx += cell * 2) {
				g.fill(x + cx, y + cy, Math.min(x + w, x + cx + cell), Math.min(y + h, y + cy + cell), 0x16000000);
			}
		}
		g.fill(x, y, x + w, y + 1, 0x22FFFFFF);
		border(g, x, y, w, h, 0x66000000);
	}

	/** A 5 px chevron at {@code x, y}: pointing down when {@code open}, right when closed. */
	public static void chevron(GuiGraphicsExtractor g, int x, int y, boolean open, int argb) {
		for (int i = 0; i < 3; i++) {
			if (open) {
				g.fill(x + i, y + 1 + i, x + 5 - i, y + 2 + i, argb);
			} else {
				g.fill(x + 1 + i, y + i, x + 2 + i, y + 5 - i, argb);
			}
		}
	}

	/** An item icon scaled to {@code size} px (16 = vanilla size). */
	public static void item(GuiGraphicsExtractor g, ItemStack stack, int x, int y, int size) {
		if (size == 16) {
			g.item(stack, x, y);
			return;
		}
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(size / 16f, size / 16f);
		g.item(stack, 0, 0);
		g.pose().popMatrix();
	}
}
