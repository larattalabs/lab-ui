package dev.larattalabs.labui.client.prefs;

import dev.larattalabs.labui.ui.TextScale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;

/**
 * (0.6.0, Architect PR A3) Hosts the text size in a screen: while an opted-in mod's screen draws, its text runs are drawn and measured at
 * the scale ({@code TextScale.activeFactor}), so its layout (built on {@code font.width}, {@code TextUtil} and {@code TextUtil.lh})
 * reflows to the bigger text. Mouse coordinates are unchanged (the layout is in GUI pixels).
 *
 * <pre>
 * public void extractRenderState(g, mx, my, a) { TextScaleHost.render(this, OWNER, g, mx, my, (gg, x, y) -&gt; draw(gg, x, y, a)); }
 * </pre>
 */
public final class TextScaleHost {
	@FunctionalInterface
	public interface Body {
		void draw(GuiGraphicsExtractor g, int mx, int my);
	}

	private TextScaleHost() {
	}

	/** The scale that applies to {@code s} now (the preference and the opt-in). */
	public static TextScale.Result result(Screen s, String owner) {
		return TextScale.resolve(owner, s.width, s.height);
	}

	public static void render(Screen s, String owner, GuiGraphicsExtractor g, int mx, int my, Body body) {
		TextScale.Result r = result(s, owner);
		float was = TextScale.activate(r.factor());
		try {
			body.draw(g, mx, my);
		} finally {
			TextScale.activate(was);
		}
	}

	/** Runs {@code r} with the screen's text scale active (for key or mouse handlers that measure text). */
	public static void with(Screen s, String owner, Runnable r) {
		float was = TextScale.activate(result(s, owner).factor());
		try {
			r.run();
		} finally {
			TextScale.activate(was);
		}
	}

	/** Mouse events are in GUI pixels (kept for callers of 0.6.0 previews; returns {@code e}). */
	public static MouseButtonEvent map(Screen s, String owner, MouseButtonEvent e) {
		return e;
	}

	/** Kept for callers; returns {@code v}. */
	public static double map(Screen s, String owner, double v) {
		return v;
	}

	/** (PR A3) The stored text size (default 100). */
	public static int stored() {
		return TextScale.snap(LabUiPrefs.integer("textScale", 100));
	}

	/** Sets, stores and applies the text size. */
	public static void set(int percent) {
		LabUiPrefs.set("textScale", TextScale.snap(percent));
		TextScale.prefer(percent);
	}
}
