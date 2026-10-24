package dev.larattalabs.labui.client.widget;

import dev.larattalabs.labui.client.ui.Kit;
import dev.larattalabs.labui.client.ui.Panels;
import dev.larattalabs.labui.client.ui.TextUtil;
import dev.larattalabs.labui.client.ui.UiStyle;
import dev.larattalabs.labui.ui.TooltipMath;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

/**
 * A delayed tooltip on the kit's ink plate, kept inside the screen. One per screen: while drawing, {@link #offer} the
 * hovered thing (or {@link #offer(Control, int, int)} each control), then call {@link #draw} last; it shows after the
 * delay and moves to stay on screen.
 */
public final class Tooltip {
	public static final int MAX_W = 200;
	private final TooltipMath.Delay delay;
	private @Nullable Object key;
	private @Nullable String text;

	public Tooltip() {
		this(TooltipMath.DEFAULT_DELAY_MS);
	}

	public Tooltip(long delayMs) {
		delay = new TooltipMath.Delay(delayMs);
	}

	/** This frame, {@code key} is hovered and its tooltip is {@code text} (the last offer of a frame wins). */
	public void offer(Object key, @Nullable String text) {
		if (text != null && !text.isBlank()) {
			this.key = key;
			this.text = text;
		}
	}

	/** Offers a control's tooltip when the mouse is over it. */
	public void offer(Control c, int mx, int my) {
		if (c.contains(mx, my) && c.tooltip() != null) {
			offer(c, c.tooltip());
		}
	}

	/** Call after everything else; draws the tooltip if its delay has passed, then forgets this frame's offer. */
	public void draw(GuiGraphicsExtractor g, Font font, int mx, int my) {
		boolean show = delay.update(key, Util.getMillis());
		if (show && text != null) {
			g.nextStratum();
			render(g, font, text, mx, my);
		}
		key = null;
		text = null;
	}

	/** Draws a tooltip now (no delay), clamped to the screen. */
	public static void render(GuiGraphicsExtractor g, Font font, String text, int mx, int my) {
		Kit.Padding p = Kit.padding("tooltip");
		List<String> lines = new ArrayList<>(TextUtil.wrapPlain(font, text, MAX_W));
		int tw = 0;
		for (String l : lines) {
			tw = Math.max(tw, font.width(l));
		}
		int w = tw + p.left() + p.right();
		int h = lines.size() * 10 - 2 + p.top() + p.bottom();
		TooltipMath.Pos pos = TooltipMath.place(mx, my, w, h, g.guiWidth(), g.guiHeight(), 4);
		Panels.sprite(g, Kit.TOOLTIP, pos.x(), pos.y(), w, h);
		int ty = pos.y() + p.top();
		int c = UiStyle.color("palette.ui.panel", UiStyle.CREAM);
		for (String l : lines) {
			g.text(font, l, pos.x() + p.left(), ty, c, false);
			ty += 10;
		}
	}
}
