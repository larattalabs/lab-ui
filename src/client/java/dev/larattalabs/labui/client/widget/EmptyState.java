package dev.larattalabs.labui.client.widget;

import dev.larattalabs.labui.client.ui.TextUtil;
import dev.larattalabs.labui.client.ui.UiStyle;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import org.jspecify.annotations.Nullable;

/** What an empty list or grid says: one muted line, centred, and an optional action button under it ("Clear search"). */
public final class EmptyState {
	private String line;
	private @Nullable Button action;

	public EmptyState(String line) {
		this.line = line;
	}

	public EmptyState line(String l) {
		line = l;
		return this;
	}

	public String line() {
		return line;
	}

	public EmptyState action(String label, Runnable r) {
		action = new Button(label, r);
		return this;
	}

	public EmptyState noAction() {
		action = null;
		return this;
	}

	public @Nullable Button actionButton() {
		return action;
	}

	/** Draws centred in the rect; returns the height of what was drawn. */
	public int draw(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h, int mx, int my) {
		List<String> lines = TextUtil.wrapPlain(font, line, Math.max(20, w - 16));
		int bh = action != null ? Button.H + 8 : 0;
		int total = lines.size() * 10 + bh;
		int ty = y + Math.max(4, (h - total) / 2);
		for (String l : lines) {
			g.text(font, l, x + (w - font.width(l)) / 2, ty, UiStyle.mutedText(), false);
			ty += 10;
		}
		if (action != null) {
			int bw = action.width(font);
			action.draw(g, font, x + (w - bw) / 2, ty + 6, bw, Button.H, mx, my);
		}
		return total;
	}

	public boolean mouseClicked(double mx, double my, int button) {
		return action != null && action.mouseClicked(mx, my, button);
	}

	public boolean keyPressed(KeyEvent e) {
		return action != null && action.keyPressed(e);
	}
}
