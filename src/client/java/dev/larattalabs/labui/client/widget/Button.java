package dev.larattalabs.labui.client.widget;

import dev.larattalabs.labui.client.hud.UiBits;
import dev.larattalabs.labui.client.ui.Kit;
import dev.larattalabs.labui.client.ui.Panels;
import dev.larattalabs.labui.client.ui.TextUtil;
import dev.larattalabs.labui.client.ui.UiStyle;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The one kit button: {@code UiBits.button}'s look (primary bodies tinted Clay-dark, brass focus ring, muted shortcut
 * number, error-coloured label for danger) at any height from {@link #MIN_H} up, as a {@link Control} with hover, focus,
 * disabled and keyboard activation.
 *
 * <pre>
 * Button place = new Button("Place", this::place).primary(true);
 * place.draw(g, font, x, y, mouseX, mouseY);           // in extractRenderState
 * if (place.mouseClicked(e.x(), e.y(), e.button())) …  // in mouseClicked
 * </pre>
 */
public class Button extends Control {
	public static final int H = 20;
	public static final int MIN_H = 18;
	private static final int PRIMARY_TINT = 0xFFD3B6AA;
	private static final int PRIMARY_HOVER_TINT = 0xFFDEC3B6;
	protected String label;
	protected boolean primary;
	protected boolean danger;
	protected int number;

	public Button(String label, Runnable onPress) {
		this.label = label;
		this.onPress = onPress;
	}

	public Button label(String l) {
		label = l;
		return this;
	}

	public String label() {
		return label;
	}

	public Button primary(boolean p) {
		primary = p;
		return this;
	}

	public Button danger(boolean d) {
		danger = d;
		return this;
	}

	/** A 1-9 shortcut number drawn before the label (0 = none); the caller handles the key. */
	public Button number(int n) {
		number = n;
		return this;
	}

	public static int width(Font font, String label) {
		return UiBits.buttonWidth(font, label, 0);
	}

	public int width(Font font) {
		return UiBits.buttonWidth(font, label, number);
	}

	/** Draws at its natural width and the standard height; returns the width. */
	public int draw(GuiGraphicsExtractor g, Font font, int x, int y, int mx, int my) {
		int bw = width(font);
		draw(g, font, x, y, bw, H, mx, my);
		return bw;
	}

	public void draw(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h, int mx, int my) {
		int bh = Math.max(MIN_H, h);
		placed(x, y, w, bh);
		paint(g, font, label, number, x, y, w, bh, primary, danger, disabled, hot(mx, my), focused);
	}

	/** Paints a button without a control (for callers that hit-test themselves). */
	public static void paint(GuiGraphicsExtractor g, Font font, String label, int number, int x, int y, int w, int h, boolean primary, boolean danger,
		boolean disabled, boolean hover, boolean focused) {
		if (focused && !disabled) {
			UiBits.focusRing(g, x, y, w, h);
		}
		if (primary) {
			if (disabled) {
				Panels.sprite(g, Kit.button(true, "disabled"), x, y, w, h);
			} else {
				Panels.sprite(g, Kit.button(true, "normal"), x, y, w, h, hover ? PRIMARY_HOVER_TINT : PRIMARY_TINT);
			}
		} else {
			Panels.sprite(g, Kit.button(false, disabled ? "disabled" : hover || focused ? "hover" : "normal"), x, y, w, h);
		}
		int labelColor;
		int numColor;
		if (disabled) {
			labelColor = primary ? UiBits.cream() : UiStyle.color("paper.disabled", 0xFFA39B8E);
			numColor = labelColor;
		} else if (primary) {
			labelColor = UiBits.panelHi();
			numColor = UiStyle.withAlpha(UiBits.panelHi(), 200);
		} else {
			labelColor = danger ? UiStyle.error() : UiBits.ink();
			numColor = UiStyle.mutedText();
		}
		String num = number > 0 ? Integer.toString(number) : "";
		int numW = num.isEmpty() ? 0 : font.width(num) + 5;
		String l = TextUtil.ellipsize(font, label, w - 12 - numW);
		int tx = x + (w - numW - font.width(l)) / 2;
		int ty = y + (h - 8) / 2;
		if (!num.isEmpty()) {
			g.text(font, num, tx, ty, numColor, false);
		}
		g.text(font, l, tx + numW, ty, labelColor, false);
	}
}
