package dev.larattalabs.labui.ui;

/**
 * WCAG 2.x contrast for colour tokens (pure, unit-tested in {@code ContrastTest}; {@code OnDarkContrastTest} checks every
 * {@code semantic_on_dark} token against the surfaces it is drawn on). Colours are 0xRRGGBB or 0xAARRGGBB (alpha ignored
 * unless {@link #over} composites it).
 */
public final class Contrast {
	private Contrast() {
	}

	/** Relative luminance of an sRGB colour, 0..1. */
	public static double luminance(int rgb) {
		return 0.2126 * channel((rgb >> 16) & 0xFF) + 0.7152 * channel((rgb >> 8) & 0xFF) + 0.0722 * channel(rgb & 0xFF);
	}

	private static double channel(int c8) {
		double c = c8 / 255.0;
		return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
	}

	/** The contrast ratio of two opaque colours, 1..21. */
	public static double ratio(int a, int b) {
		double la = luminance(a);
		double lb = luminance(b);
		return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
	}

	/** {@code fg} at opacity {@code alpha} (0..1) over the opaque {@code bg}, as 0xRRGGBB (rounded per channel, sRGB blend as drawn). */
	public static int over(int fg, double alpha, int bg) {
		int out = 0;
		for (int shift = 16; shift >= 0; shift -= 8) {
			int f = (fg >> shift) & 0xFF;
			int b = (bg >> shift) & 0xFF;
			out |= (int) Math.round(alpha * f + (1 - alpha) * b) << shift;
		}
		return out;
	}

	/** {@code #RRGGBB} as 0xRRGGBB; throws on anything else. */
	public static int hex(String s) {
		if (s == null || s.length() != 7 || s.charAt(0) != '#') {
			throw new IllegalArgumentException("not #RRGGBB: " + s);
		}
		return Integer.parseInt(s.substring(1), 16);
	}

	/**
	 * A surface as ui-style.json writes it: {@code #RRGGBB} (opaque), or {@code #RRGGBB@alpha/#RRGGBB} (the first colour at
	 * that opacity over the second, e.g. the HUD panel over a white world).
	 */
	public static int surface(String s) {
		int at = s.indexOf('@');
		if (at < 0) {
			return hex(s);
		}
		int slash = s.indexOf('/', at);
		return over(hex(s.substring(0, at)), Double.parseDouble(s.substring(at + 1, slash)), hex(s.substring(slash + 1)));
	}
}
