package dev.larattalabs.labui.client.help;

import dev.larattalabs.labui.client.hud.UiBits;
import dev.larattalabs.labui.client.ui.TextUtil;
import dev.larattalabs.labui.client.ui.UiStyle;
import dev.larattalabs.labui.ui.Glossary;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * (0.6.0, Architect PR G1) Draws the shared {@link Glossary} on paper: each section's title in clay, then each term as its word in
 * ink and its meaning wrapped in muted text under it. Any mod's help page or the "?" overlay's Terms page uses it. Pass
 * {@code g == null} to measure only.
 */
public final class GlossaryView {
	public static final int LINE = 10;

	private GlossaryView() {
	}

	/** Draws {@code sections} in a column {@code w} wide from {@code y}; returns the height used. */
	public static int draw(GuiGraphicsExtractor g, Font font, List<Glossary.Section> sections, int x, int y, int w) {
		int y0 = y;
		for (Glossary.Section s : sections) {
			if (g != null) {
				g.text(font, s.title(), x, y, UiStyle.clayText(), false);
			}
			y += dev.larattalabs.labui.client.ui.TextUtil.lh(12);
			for (Glossary.Term t : s.terms()) {
				if (g != null) {
					g.text(font, TextUtil.ellipsize(font, t.word(), w), x, y, UiBits.ink(), false);
				}
				y += TextUtil.lh(LINE);
				for (String line : TextUtil.wrapPlain(font, t.meaning(), w - 8)) {
					if (g != null) {
						g.text(font, line, x + 8, y, UiBits.muted(), false);
					}
					y += TextUtil.lh(LINE);
				}
				y += 2;
			}
			y += 6;
		}
		return y - y0;
	}

	/** Draws only the terms named in {@code words} (in that order, every section's match), as "word: meaning" lines; the height used. */
	public static int drawSome(GuiGraphicsExtractor g, Font font, List<String> words, int x, int y, int w) {
		int y0 = y;
		for (String word : words) {
			for (Glossary.Term t : Glossary.lookup(word)) {
				for (String line : TextUtil.wrapPlain(font, t.word() + ": " + t.meaning(), w)) {
					if (g != null) {
						g.text(font, line, x, y, UiBits.muted(), false);
					}
					y += TextUtil.lh(LINE);
				}
			}
		}
		return y - y0;
	}
}
