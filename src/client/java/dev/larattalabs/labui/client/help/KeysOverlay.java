package dev.larattalabs.labui.client.help;

import dev.larattalabs.labui.client.hud.UiBits;
import dev.larattalabs.labui.client.ui.Panels;
import dev.larattalabs.labui.client.ui.TextUtil;
import dev.larattalabs.labui.client.ui.UiStyle;
import dev.larattalabs.labui.ui.Glossary;
import dev.larattalabs.labui.ui.HelpKeys;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;

/**
 * (0.6.0, Architect PR W1) A screen's "?" overlay: Shift+/ (or F1) opens a panel over the screen listing its keys and the glossary
 * terms it uses. The rows come from a supplier read each frame, so a key rebound in Controls shows its new label
 * ({@code KeyLabels.label}). "?" yields to a focused text field ({@link HelpKeys#toggles}). Any mod's screen can host one: call
 * {@link #keyPressed} first in its key handler, {@link #mouseClicked} first in its click handler, and {@link #draw} last.
 */
public final class KeysOverlay {
	private final String title;
	private final Supplier<List<HelpKeys.Row>> rows;
	private final List<String> terms;
	private boolean open;

	public KeysOverlay(String title, Supplier<List<HelpKeys.Row>> rows, List<String> terms) {
		this.title = title;
		this.rows = rows;
		this.terms = List.copyOf(terms);
	}

	public boolean isOpen() {
		return open;
	}

	public void open(boolean on) {
		open = on;
	}

	/** The rows as drawn now (DevBridge and tests: the live labels). */
	public List<HelpKeys.Row> rows() {
		return HelpKeys.withClose(rows.get());
	}

	public List<String> terms() {
		return terms;
	}

	/** First in the screen's key handler. True when the press was the overlay's (it also swallows every key while open). */
	public boolean keyPressed(KeyEvent e, boolean textFocused) {
		if (open) {
			if (HelpKeys.closes(e.key(), e.hasShiftDown())) {
				open = false;
			}
			return true;
		}
		if (HelpKeys.toggles(e.key(), e.hasShiftDown(), textFocused)) {
			open = true;
			return true;
		}
		return false;
	}

	/** Any click closes an open overlay (and is consumed). */
	public boolean mouseClicked(double mx, double my) {
		if (open) {
			open = false;
			return true;
		}
		return false;
	}

	/** Draws the overlay over a screen {@code sw} x {@code sh} (last, after everything else). */
	public void draw(GuiGraphicsExtractor g, Font font, int sw, int sh) {
		if (!open) {
			return;
		}
		g.nextStratum();
		g.fill(0, 0, sw, sh, UiStyle.withAlpha(UiStyle.INK, 120));
		List<HelpKeys.Row> rs = rows();
		int pw = Math.min(sw - 24, 420);
		int pad = 10;
		int inner = pw - pad * 2;
		int keyCol = 0;
		for (HelpKeys.Row r : rs) {
			keyCol = Math.max(keyCol, UiBits.keycapWidth(font, r.key()));
		}
		keyCol = Math.min(keyCol, inner / 3);
		List<Glossary.Term> found = new java.util.ArrayList<>();
		for (String t : terms) {
			found.addAll(Glossary.lookup(t));
		}
		int termsH = found.isEmpty() ? 0 : 16 + GlossaryView.drawSome(null, font, terms, 0, 0, inner);
		int rowsH = 0;
		for (HelpKeys.Row r : rs) {
			rowsH += Math.max(14, 10 * TextUtil.wrapPlain(font, r.verb(), inner - keyCol - 8).size() + 4);
		}
		int ph = Math.min(sh - 24, 22 + rowsH + termsH + 18);
		int px = (sw - pw) / 2;
		int py = (sh - ph) / 2;
		Panels.panel(g, px, py, pw, ph);
		Panels.header(g, font, "Keys · " + title, px + pad - 2, py + pad - 2, inner + 4);
		g.enableScissor(px, py, px + pw, py + ph - 14);
		int y = py + pad + 18;
		for (HelpKeys.Row r : rs) {
			UiBits.keycap(g, font, r.key(), px + pad, y);
			int ly = y + 2;
			for (String line : TextUtil.wrapPlain(font, r.verb(), inner - keyCol - 8)) {
				g.text(font, line, px + pad + keyCol + 8, ly, UiBits.ink(), false);
				ly += 10;
			}
			y += Math.max(14, ly - y + 2);
		}
		if (!found.isEmpty()) {
			y += 4;
			g.text(font, "Terms", px + pad, y, UiStyle.clayText(), false);
			y += 12;
			GlossaryView.drawSome(g, font, terms, px + pad, y, inner);
		}
		g.disableScissor();
		g.text(font, "Esc or ? closes · Status > Help lists every term", px + pad, py + ph - 12, UiStyle.mutedText(), false);
	}
}
