package dev.larattalabs.labui.client.widget;

import net.minecraft.client.input.KeyEvent;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

/**
 * Base of the immediate-mode controls: the screen draws a control where its layout puts it each frame (which records its
 * rect) and forwards mouse and key events to it. A control not drawn in the last {@link #STALE_MS} ignores clicks (a
 * collapsed section's chips, a closed popup's rows), so callers need no bookkeeping. Keyboard focus is the caller's: set
 * {@link #focused} on the control that Enter / Space should press.
 */
public abstract class Control {
	/** A control drawn longer ago than this is treated as hidden. */
	public static final long STALE_MS = 250;
	protected int x;
	protected int y;
	protected int w;
	protected int h;
	private long drawnAt = -1;
	protected boolean focused;
	protected boolean disabled;
	protected @Nullable String tooltip;
	protected @Nullable String disabledReason;
	protected Runnable onPress = () -> {
	};

	/** Records where the control was drawn this frame (subclasses call it from their draw). */
	protected void placed(int x, int y, int w, int h) {
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		this.drawnAt = Util.getMillis();
	}

	public boolean shown() {
		return drawnAt >= 0 && Util.getMillis() - drawnAt <= STALE_MS;
	}

	/** Forget the last draw (a hidden control). */
	public void hide() {
		drawnAt = -1;
	}

	public boolean contains(double mx, double my) {
		return shown() && mx >= x && mx < x + w && my >= y && my < y + h;
	}

	/** Hover for drawing (the mouse over an enabled control). */
	protected boolean hot(int mx, int my) {
		return !disabled && mx >= x && mx < x + w && my >= y && my < y + h;
	}

	public Control onPress(Runnable r) {
		onPress = r;
		return this;
	}

	public Control focused(boolean f) {
		focused = f;
		return this;
	}

	public boolean isFocused() {
		return focused;
	}

	public Control disabled(boolean d) {
		disabled = d;
		disabledReason = null;
		return this;
	}

	public boolean isDisabled() {
		return disabled;
	}

	/**
	 * Disabled, with why (0.5.0): "Bundled designs can't be deleted", "Pick a Style first". The reason shows as the control's
	 * tooltip while it is disabled ({@link Tooltip#offer(Control, int, int)}) and through {@link #reasonAt} for a screen's
	 * status line; enabling clears it. A null or blank reason is the plain {@link #disabled(boolean)}.
	 */
	public Control disabled(boolean d, @Nullable String reason) {
		disabled = d;
		disabledReason = d && reason != null && !reason.isBlank() ? reason : null;
		return this;
	}

	/** Why the control is disabled, or null (enabled, or disabled without a reason). Since 0.5.0. */
	public @Nullable String disabledReason() {
		return disabled ? disabledReason : null;
	}

	/**
	 * The reason of the first disabled control under the mouse (0.5.0), for a status or footer line ("Delete: bundled designs
	 * can't be deleted"), or null. Controls not drawn this frame never match.
	 */
	public static @Nullable String reasonAt(double mx, double my, Control... controls) {
		for (Control c : controls) {
			if (c != null && c.contains(mx, my) && c.disabledReason() != null) {
				return c.disabledReason();
			}
		}
		return null;
	}

	/** Tooltip text; while disabled with a reason ({@link #disabled(boolean, String)}) the reason shows instead. */
	public Control tooltip(@Nullable String t) {
		tooltip = t;
		return this;
	}

	public @Nullable String tooltip() {
		return tooltip;
	}

	public void press() {
		onPress.run();
	}

	/** A left click on the control presses it; a click on a disabled control is swallowed. */
	public boolean mouseClicked(double mx, double my, int button) {
		if (button != 0 || !contains(mx, my)) {
			return false;
		}
		if (!disabled) {
			press();
		}
		return true;
	}

	/** Enter or Space presses a focused, enabled control. */
	public boolean keyPressed(KeyEvent e) {
		if (focused && !disabled && shown() && NavKeys.activates(e)) {
			press();
			return true;
		}
		return false;
	}

	public int x() {
		return x;
	}

	public int y() {
		return y;
	}

	public int width() {
		return w;
	}

	public int height() {
		return h;
	}
}
