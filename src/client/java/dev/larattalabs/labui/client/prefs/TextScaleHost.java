package dev.larattalabs.labui.client.prefs;

import dev.larattalabs.labui.ui.TextScale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;

/**
 * (0.6.0, Architect PR A3) Hosts the text size in a screen: the screen is drawn at its GUI size divided by the scale and scaled back
 * up, so its own layout reflows to the smaller size and its text and line boxes grow; mouse input is mapped the same way. Use it in
 * an opted-in mod's screen ({@link TextScale#optIn}):
 *
 * <pre>
 * public void extractRenderState(g, mx, my, a) { TextScaleHost.render(this, OWNER, g, mx, my, (gg, x, y) -&gt; draw(gg, x, y, a)); }
 * public boolean mouseClicked(MouseButtonEvent e, boolean dbl) { e = TextScaleHost.map(this, OWNER, e); ... }
 * </pre>
 *
 * The screen's {@code width} and {@code height} hold the smaller size while it draws and are restored after.
 */
public final class TextScaleHost {
	@FunctionalInterface
	public interface Body {
		void draw(GuiGraphicsExtractor g, int mx, int my);
	}

	/** The screen's real GUI size while it is drawn scaled (so {@link #result} reads the real one). */
	private static int realW;
	private static int realH;
	private static Screen drawing;

	private TextScaleHost() {
	}

	/** The scale that applies to {@code s} now (the preference, the opt-in and the clamp). */
	public static TextScale.Result result(Screen s, String owner) {
		int w = drawing == s ? realW : s.width;
		int h = drawing == s ? realH : s.height;
		return TextScale.resolve(owner, w, h);
	}

	public static void render(Screen s, String owner, GuiGraphicsExtractor g, int mx, int my, Body body) {
		TextScale.Result r = result(s, owner);
		if (r.percent() == 100 || drawing == s) {
			body.draw(g, mx, my);
			return;
		}
		float f = r.factor();
		int w = s.width;
		int h = s.height;
		realW = w;
		realH = h;
		drawing = s;
		s.width = (int) Math.floor(w / f);
		s.height = (int) Math.floor(h / f);
		g.pose().pushMatrix();
		g.pose().scale(f, f);
		try {
			body.draw(g, (int) Math.floor(mx / f), (int) Math.floor(my / f));
		} finally {
			g.pose().popMatrix();
			s.width = w;
			s.height = h;
			drawing = null;
		}
	}

	/** A mouse event in the screen's scaled coordinates. */
	public static MouseButtonEvent map(Screen s, String owner, MouseButtonEvent e) {
		float f = result(s, owner).factor();
		return f == 1f ? e : new MouseButtonEvent(e.x() / f, e.y() / f, e.buttonInfo());
	}

	/** A mouse coordinate in the screen's scaled coordinates. */
	public static double map(Screen s, String owner, double v) {
		return v / result(s, owner).factor();
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
