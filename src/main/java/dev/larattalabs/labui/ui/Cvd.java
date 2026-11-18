package dev.larattalabs.labui.ui;

/**
 * (0.6.0, Architect PR A1) Colour-vision-deficiency simulation (Machado, Oliveira &amp; Fernandes 2009, full severity, applied in
 * linear sRGB) and CIELAB ΔE76, for checking that palette pairs stay apart. Pure; {@code GhostPaletteTest} uses it.
 */
public final class Cvd {
	public enum Kind {
		NONE, PROTAN, DEUTAN, TRITAN
	}

	private static final double[][] PROTAN = {{0.152286, 1.052583, -0.204868}, {0.114503, 0.786281, 0.099216}, {-0.003882, -0.048116, 1.051998}};
	private static final double[][] DEUTAN = {{0.367322, 0.860646, -0.227968}, {0.280085, 0.672501, 0.047413}, {-0.011820, 0.042940, 0.968881}};
	private static final double[][] TRITAN = {{1.255528, -0.076749, -0.178779}, {-0.078411, 0.930809, 0.147602}, {0.004733, 0.691367, 0.303900}};

	private Cvd() {
	}

	static double lin(int c8) {
		double c = c8 / 255.0;
		return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
	}

	static int enc(double l) {
		l = Math.max(0, Math.min(1, l));
		double c = l <= 0.0031308 ? l * 12.92 : 1.055 * Math.pow(l, 1 / 2.4) - 0.055;
		return (int) Math.round(c * 255);
	}

	/** Linear RGB of {@code rgb} as seen with {@code k}. */
	public static double[] simulateLinear(int rgb, Kind k) {
		double[] l = {lin((rgb >> 16) & 0xFF), lin((rgb >> 8) & 0xFF), lin(rgb & 0xFF)};
		double[][] m = switch (k) {
			case PROTAN -> PROTAN;
			case DEUTAN -> DEUTAN;
			case TRITAN -> TRITAN;
			case NONE -> null;
		};
		if (m == null) {
			return l;
		}
		double[] o = new double[3];
		for (int i = 0; i < 3; i++) {
			o[i] = Math.max(0, Math.min(1, m[i][0] * l[0] + m[i][1] * l[1] + m[i][2] * l[2]));
		}
		return o;
	}

	/** {@code rgb} as seen with {@code k}, as 0xRRGGBB (for filtered screenshots). */
	public static int simulate(int rgb, Kind k) {
		double[] o = simulateLinear(rgb, k);
		return enc(o[0]) << 16 | enc(o[1]) << 8 | enc(o[2]);
	}

	static double[] lab(double[] l) {
		double x = (0.4124 * l[0] + 0.3576 * l[1] + 0.1805 * l[2]) / 0.95047;
		double y = 0.2126 * l[0] + 0.7152 * l[1] + 0.0722 * l[2];
		double z = (0.0193 * l[0] + 0.1192 * l[1] + 0.9505 * l[2]) / 1.08883;
		double fx = f(x);
		double fy = f(y);
		double fz = f(z);
		return new double[] {116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz)};
	}

	private static double f(double t) {
		return t > 0.008856 ? Math.cbrt(t) : 7.787 * t + 16.0 / 116;
	}

	/** CIELAB ΔE76 between two colours, both seen with {@code k}. */
	public static double deltaE(int a, int b, Kind k) {
		double[] p = lab(simulateLinear(a, k));
		double[] q = lab(simulateLinear(b, k));
		return Math.sqrt((p[0] - q[0]) * (p[0] - q[0]) + (p[1] - q[1]) * (p[1] - q[1]) + (p[2] - q[2]) * (p[2] - q[2]));
	}

	/** The smallest ΔE of the pair over normal vision and the three simulations. */
	public static double minDeltaE(int a, int b) {
		double m = Double.MAX_VALUE;
		for (Kind k : Kind.values()) {
			m = Math.min(m, deltaE(a, b, k));
		}
		return m;
	}
}
