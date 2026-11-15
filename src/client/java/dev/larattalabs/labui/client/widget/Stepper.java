package dev.larattalabs.labui.client.widget;

import com.mojang.blaze3d.platform.InputConstants;
import dev.larattalabs.labui.client.hud.UiBits;
import dev.larattalabs.labui.client.ui.UiStyle;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import org.jspecify.annotations.Nullable;

/**
 * The shared stepper (0.5.0): kit {@link Button}s at their 18 px minimum around a value, "− 4 +" or with a name "W − 12 +". From
 * Architect's set dialog (SD) and its editor's W/H/D, which each drew their own. Each end can be disabled with a reason
 * ({@link #canDecrease(boolean, String)}); while focused, Left/Right (and -/+) step it. The value keeps a fixed width when
 * {@link #valueWidth} is set, so a column of steppers lines up. The arithmetic is {@link dev.larattalabs.labui.ui.Steps}.
 *
 * <pre>
 * Stepper rows = new Stepper(() -> n--, () -> n++).name("Rows");
 * rows.value(Integer.toString(n)).canDecrease(n > 1, "At least one row").canIncrease(n < 8, "Eight rows at most");
 * x += rows.draw(g, font, x, y, mx, my);            // in extractRenderState
 * if (rows.mouseClicked(e.x(), e.y(), e.button())) … // in mouseClicked
 * </pre>
 */
public class Stepper {
	public static final int H = Button.MIN_H;
	/** Each end button's width: square-ish at 18 px tall. */
	public static final int END_W = Button.MIN_H + 2;
	private static final int GAP = 3;
	private final Button minus;
	private final Button plus;
	private String value = "";
	private @Nullable String name;
	private int valueW = -1;
	private boolean dimmed;
	private boolean focused;

	public Stepper(Runnable decrease, Runnable increase) {
		minus = new Button("−", decrease);
		plus = new Button("+", increase);
	}

	public Stepper value(String v) {
		value = v == null ? "" : v;
		return this;
	}

	public String value() {
		return value;
	}

	/** A name drawn before the stepper in muted text ("W", "Rows"); null for none. */
	public Stepper name(@Nullable String n) {
		name = n;
		return this;
	}

	/** A fixed width for the value (px), so steppers in a column line up; -1 (default) = the value's own width. */
	public Stepper valueWidth(int px) {
		valueW = px;
		return this;
	}

	/** The value in muted text (the setting is off, as a stepper that only applies with another option on). */
	public Stepper dimmed(boolean d) {
		dimmed = d;
		return this;
	}

	public Stepper canDecrease(boolean can) {
		minus.disabled(!can);
		return this;
	}

	public Stepper canDecrease(boolean can, @Nullable String whyNot) {
		minus.disabled(!can, whyNot);
		return this;
	}

	public Stepper canIncrease(boolean can) {
		plus.disabled(!can);
		return this;
	}

	public Stepper canIncrease(boolean can, @Nullable String whyNot) {
		plus.disabled(!can, whyNot);
		return this;
	}

	/** Keyboard focus: Left/Right and -/+ step it. Neither end button shows the brass ring; the stepper does. */
	public Stepper focused(boolean f) {
		focused = f;
		return this;
	}

	public boolean isFocused() {
		return focused;
	}

	/** The − button (for a screen's own control registry or tooltips). */
	public Button minus() {
		return minus;
	}

	/** The + button. */
	public Button plus() {
		return plus;
	}

	private int nameW(Font font) {
		return name == null || name.isEmpty() ? 0 : font.width(name) + 4;
	}

	private int valW(Font font) {
		return valueW >= 0 ? valueW : font.width(value);
	}

	public int width(Font font) {
		return nameW(font) + END_W + GAP + valW(font) + GAP + END_W;
	}

	/** Draws at x, y (18 px tall); returns the width. */
	public int draw(GuiGraphicsExtractor g, Font font, int x, int y, int mx, int my) {
		int w = width(font);
		if (focused) {
			Paint.focus(g, x + nameW(font), y, w - nameW(font), H);
		}
		int cx = x;
		int ty = y + (H - 8) / 2;
		if (nameW(font) > 0) {
			g.text(font, name, cx, ty, UiStyle.mutedText(), false);
			cx += nameW(font);
		}
		minus.draw(g, font, cx, y, END_W, H, mx, my);
		cx += END_W + GAP;
		int vw = valW(font);
		g.text(font, value, cx + (vw - font.width(value)) / 2, ty, dimmed ? UiStyle.mutedText() : UiBits.ink(), false);
		cx += vw + GAP;
		plus.draw(g, font, cx, y, END_W, H, mx, my);
		return w;
	}

	/** A click on either end presses it (a disabled end swallows the click). */
	public boolean mouseClicked(double mx, double my, int button) {
		return minus.mouseClicked(mx, my, button) || plus.mouseClicked(mx, my, button);
	}

	/** While focused: Left or - steps down, Right or + (=) steps up, each only when that end is enabled. */
	public boolean keyPressed(KeyEvent e) {
		if (!focused) {
			return false;
		}
		int k = e.key();
		boolean down = k == InputConstants.KEY_LEFT || k == InputConstants.KEY_MINUS;
		boolean up = k == InputConstants.KEY_RIGHT || k == InputConstants.KEY_EQUALS || k == InputConstants.KEY_ADD;
		if (down && !minus.isDisabled()) {
			minus.press();
			return true;
		}
		if (up && !plus.isDisabled()) {
			plus.press();
			return true;
		}
		return down || up;
	}

	/** Offers both ends' tooltips or disabled reasons to a screen's {@link Tooltip}. */
	public void offer(Tooltip t, int mx, int my) {
		t.offer(minus, mx, my);
		t.offer(plus, mx, my);
	}
}
