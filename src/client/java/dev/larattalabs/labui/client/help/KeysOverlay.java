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

	/**
	 * Draws the overlay over a screen {@code sw} x {@code sh} (last, after everything else). The panel is sized from its content: the
	 * keys, then as many terms as fit whole (word in ink, meaning muted and indented), a "+N more" line, and its own footer row.
	 */
	public void draw(GuiGraphicsExtractor g, Font font, int sw, int sh) {
		if (!open) {
			return;
		}
		g.nextStratum();
		g.fill(0, 0, sw, sh, UiStyle.withAlpha(UiStyle.INK, 140));
		List<HelpKeys.Row> rs = rows();
		int pw = Math.min(sw - 24, 420);
		int pad = 10;
		int inner = pw - pad * 2;
		int keyCol = 0;
		for (HelpKeys.Row r : rs) {
			keyCol = Math.max(keyCol, UiBits.keycapWidth(font, r.key()));
		}
		keyCol = Math.min(keyCol, inner / 3);
		int verbW = inner - keyCol - 8;
		int keysH = 0;
		for (HelpKeys.Row r : rs) {
			keysH += Math.max(TextUtil.lh(14), TextUtil.lh(10) * TextUtil.wrapPlain(font, r.verb(), verbW).size() + 4);
		}
		List<Glossary.Term> found = new java.util.ArrayList<>();
		for (String t : terms) {
			found.addAll(Glossary.lookup(t));
		}
		int head = TextUtil.lh(14) + 8;
		int footer = TextUtil.lh(10) + 8;
		int maxH = sh - 16;
		// as many whole terms as fit under the keys
		int room = maxH - pad - head - keysH - footer - pad;
		int termsH = 0;
		int shownTerms = 0;
		if (!found.isEmpty() && room > 24) {
			termsH = TextUtil.lh(14);
			for (Glossary.Term t : found) {
				int h = termHeight(font, t, inner);
				if (termsH + h + (shownTerms + 1 < found.size() ? TextUtil.lh(10) : 0) > room) {
					break;
				}
				termsH += h;
				shownTerms++;
			}
			if (shownTerms < found.size()) {
				termsH += dev.larattalabs.labui.client.ui.TextUtil.lh(10);
			}
			if (shownTerms == 0) {
				termsH = 0;
			}
		}
		int ph = Math.min(maxH, pad + head + keysH + (termsH > 0 ? 6 + termsH : 0) + footer + pad - 4);
		int px = (sw - pw) / 2;
		int py = (sh - ph) / 2;
		Panels.panel(g, px, py, pw, ph);
		Panels.header(g, font, "Keys · " + title, px + pad - 2, py + pad - 2, inner + 4);
		int y = py + pad + head;
		for (HelpKeys.Row r : rs) {
			UiBits.keycap(g, font, r.key(), px + pad, y);
			int ly = y + 2;
			for (String line : TextUtil.wrapPlain(font, r.verb(), verbW)) {
				g.text(font, line, px + pad + keyCol + 8, ly, UiBits.ink(), false);
				ly += dev.larattalabs.labui.client.ui.TextUtil.lh(10);
			}
			y += Math.max(TextUtil.lh(14), ly - y + 2);
		}
		if (termsH > 0) {
			y += 6;
			g.text(font, "Terms", px + pad, y, UiStyle.clayText(), false);
			y += dev.larattalabs.labui.client.ui.TextUtil.lh(14);
			for (int i = 0; i < shownTerms; i++) {
				y += drawTerm(g, font, found.get(i), px + pad, y, inner);
			}
			if (shownTerms < found.size()) {
				g.text(font, "+" + (found.size() - shownTerms) + " more in Status > Help", px + pad, y, UiStyle.mutedText(), false);
			}
		}
		int fy = py + ph - pad - TextUtil.lh(9);
		g.fill(px + pad, fy - 4, px + pw - pad, fy - 3, UiStyle.rule());
		g.text(font, TextUtil.ellipsize(font, "Esc or ? closes · Status > Help lists every term", inner), px + pad, fy, UiStyle.mutedText(), false);
	}

	private static int termHeight(Font font, Glossary.Term t, int w) {
		return TextUtil.lh(10) * (1 + TextUtil.wrapPlain(font, t.meaning(), w - 8).size()) + 2;
	}

	private static int drawTerm(GuiGraphicsExtractor g, Font font, Glossary.Term t, int x, int y, int w) {
		g.text(font, TextUtil.ellipsize(font, t.word(), w), x, y, UiBits.ink(), false);
		int ly = y + TextUtil.lh(10);
		for (String line : TextUtil.wrapPlain(font, t.meaning(), w - 8)) {
			g.text(font, line, x + 8, ly, UiBits.muted(), false);
			ly += dev.larattalabs.labui.client.ui.TextUtil.lh(10);
		}
		return ly - y + 2;
	}
}
