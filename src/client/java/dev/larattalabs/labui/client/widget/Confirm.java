package dev.larattalabs.labui.client.widget;

import dev.larattalabs.labui.client.ui.TextUtil;
import dev.larattalabs.labui.client.ui.UiStyle;
import dev.larattalabs.labui.ui.ConfirmArm;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.util.Util;

/**
 * A two-press destructive button: the first press arms it and it turns danger-red with a countdown ("Press again to
 * delete (4)"); a second press within the window confirms. Esc ({@link #escape}) or a click anywhere else
 * ({@link #mouseClicked} returns false and disarms) cancels. Several Confirms may share one {@link ConfirmArm} (one armed
 * at a time per screen); the key tells them apart. By keyboard the arm follows {@link ConfirmArm#pressByKey}. The width
 * fits the armed label too, so arming never shifts the layout.
 */
public final class Confirm extends Control {
	private final ConfirmArm arm;
	private final String key;
	private String label;
	private String verb;
	private final Runnable onConfirm;

	/** {@code verb} completes "Press again to …" (e.g. "delete"). */
	public Confirm(ConfirmArm arm, String key, String label, String verb, Runnable onConfirm) {
		this.arm = arm;
		this.key = key;
		this.label = label;
		this.verb = verb;
		this.onConfirm = onConfirm;
	}

	public Confirm(String label, String verb, Runnable onConfirm) {
		this(new ConfirmArm(), label, label, verb, onConfirm);
	}

	public ConfirmArm arm() {
		return arm;
	}

	public boolean armed() {
		return arm.armed(key, Util.getMillis());
	}

	public String armedLabel(int seconds) {
		return "Press again to " + verb + " (" + seconds + ")";
	}

	public int width(Font font) {
		return Math.max(Button.width(font, label), font.width(armedLabel(9)) + 16);
	}

	public int draw(GuiGraphicsExtractor g, Font font, int x, int y, int mx, int my) {
		int cw = width(font);
		draw(g, font, x, y, cw, Button.H, mx, my);
		return cw;
	}

	public void draw(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h, int mx, int my) {
		int bh = Math.max(Button.MIN_H, h);
		placed(x, y, w, bh);
		long now = Util.getMillis();
		if (arm.armed(key, now)) {
			if (focused) {
				Paint.focus(g, x, y, w, bh);
			}
			Paint.box(g, x, y, w, bh, UiStyle.danger(), 0xFF6E241C);
			String l = TextUtil.ellipsize(font, armedLabel(arm.secondsLeft(now)), w - 8);
			g.text(font, l, x + (w - font.width(l)) / 2, y + (bh - 8) / 2, UiStyle.dangerText(), false);
		} else {
			Button.paint(g, font, label, 0, x, y, w, bh, false, true, disabled, hot(mx, my), focused);
		}
	}

	@Override
	public void press() {
		if (arm.press(key, Util.getMillis()) == ConfirmArm.Result.CONFIRMED) {
			onConfirm.run();
		}
	}

	/** A click on it presses it; a click elsewhere disarms it (and returns false, so the click goes on). */
	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (contains(mx, my)) {
			return super.mouseClicked(mx, my, button);
		}
		arm.clickedElsewhere(null);
		return false;
	}

	@Override
	public boolean keyPressed(KeyEvent e) {
		return keyPressed(e, false);
	}

	/** {@code repeat}: an OS key repeat (see {@code UiRules.KeyRepeat}); repeats never arm or confirm. */
	public boolean keyPressed(KeyEvent e, boolean repeat) {
		if (!focused || disabled || !shown() || !NavKeys.activates(e)) {
			return false;
		}
		if (arm.pressByKey(key, Util.getMillis(), repeat) == ConfirmArm.Result.CONFIRMED) {
			onConfirm.run();
		}
		return true;
	}

	/** Esc: disarms and returns true when armed (consume the Esc), false otherwise (let it close the screen). */
	public boolean escape() {
		if (armed()) {
			arm.cancel();
			return true;
		}
		return false;
	}
}
