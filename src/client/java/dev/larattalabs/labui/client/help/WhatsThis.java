package dev.larattalabs.labui.client.help;

import dev.larattalabs.labui.client.ui.TextUtil;
import dev.larattalabs.labui.client.ui.UiStyle;
import dev.larattalabs.labui.ui.HelpKeys;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * (0.6.0, Architect PR W1) A screen's "what's this": one muted line under its title (at most {@link HelpKeys#MAX_TIP} characters)
 * with a × that hides it for that screen, persisted by key in a {@link Store}. Any mod's screen can use it: pass the mod's own store
 * (Architect: {@code architect_mc_ui.json}) or lab-ui's ({@code LabUiPrefs.tips()}). Keys are namespaced ({@code architect_mc:library}).
 */
public final class WhatsThis {
	/** Where hidden tips are remembered (per install: tips are about the player, not the world). */
	public interface Store {
		boolean hidden(String key);

		void hide(String key);

		/** "Show tips again". */
		void showAll();
	}

	public static final int H = 12;
	private final Store store;
	private final String key;
	private final String text;
	private int cx = -1000;
	private int cy;

	public WhatsThis(Store store, String key, String text) {
		if (!HelpKeys.tipFits(text)) {
			throw new IllegalArgumentException(key + ": a tip is one line of at most " + HelpKeys.MAX_TIP + " characters");
		}
		this.store = store;
		this.key = key;
		this.text = text;
	}

	public String key() {
		return key;
	}

	public String text() {
		return text;
	}

	public boolean shown() {
		return !store.hidden(key);
	}

	/** The height the line takes: {@link #H}, or 0 once hidden. */
	public int height() {
		return shown() ? TextUtil.lh(H) : 0;
	}

	/** Draws the line at ({@code x}, {@code y}) in a row {@code w} wide; returns the height used (0 once hidden). */
	public int draw(GuiGraphicsExtractor g, Font font, int x, int y, int w, int mx, int my) {
		if (!shown()) {
			cx = -1000;
			return 0;
		}
		String x0 = "×";
		int xw = font.width(x0);
		cx = x + w - xw - 2;
		cy = y;
		boolean hov = over(mx, my);
		int h = TextUtil.lh(H);
		g.text(font, TextUtil.ellipsize(font, text, w - xw - 10), x, y + 2, UiStyle.mutedText(), false);
		g.text(font, x0, cx, y + 2, hov ? UiStyle.link() : UiStyle.mutedText(), false);
		return h;
	}

	private boolean over(double mx, double my) {
		return mx >= cx - 6 && mx < cx + 12 && my >= cy - 2 && my < cy + TextUtil.lh(H) + 2; // a hit area wider than the glyph
	}

	/** A click on the ×: hides the line. True when it was consumed. */
	public boolean mouseClicked(double mx, double my) {
		if (shown() && over(mx, my)) {
			store.hide(key);
			return true;
		}
		return false;
	}

	/** Hides it (DevBridge, or a HUD tip once its action is done). */
	public void hide() {
		store.hide(key);
	}
}
