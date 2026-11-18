package dev.larattalabs.labui.ui;

/**
 * (0.6.0, Architect PR A1) The second, non-colour cue's geometry, as axis-aligned boxes a renderer draws with its own primitive
 * ({@link Box}), so every ghost (Architect's, Steward's) draws the same shapes. Coordinates are the caller's (camera-relative floats).
 */
public final class GhostCues {
	/** The caller's box primitive (min and max corners). */
	@FunctionalInterface
	public interface Box {
		void box(float x0, float y0, float z0, float x1, float y1, float z1);
	}

	/** Dash and gap lengths along a dashed outline, in blocks. */
	public static final float DASH = 0.5f;
	public static final float GAP = 0.3f;

	private GhostCues() {
	}

	/** The dashes of a segment of {@code len} blocks: [start, end] pairs from 0 (a short final dash is kept). */
	public static float[] dashes(float len) {
		int n = (int) Math.ceil(len / (DASH + GAP)) + 1;
		float[] out = new float[n * 2];
		int k = 0;
		for (float s = 0; s < len - 1e-4f; s += DASH + GAP) {
			out[k++] = s;
			out[k++] = Math.min(len, s + DASH);
		}
		return java.util.Arrays.copyOf(out, k);
	}

	/** A dashed bar along an axis-aligned edge (half-thickness {@code t}). */
	public static int dashedBar(Box b, float x0, float y0, float z0, float x1, float y1, float z1, float t) {
		float dx = x1 - x0;
		float dy = y1 - y0;
		float dz = z1 - z0;
		float len = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
		if (len < 1e-4f) {
			b.box(x0 - t, y0 - t, z0 - t, x0 + t, y0 + t, z0 + t);
			return 1;
		}
		float[] d = dashes(len);
		for (int i = 0; i + 1 < d.length; i += 2) {
			float a = d[i] / len;
			float e = d[i + 1] / len;
			float ax = x0 + dx * a;
			float ay = y0 + dy * a;
			float az = z0 + dz * a;
			float ex = x0 + dx * e;
			float ey = y0 + dy * e;
			float ez = z0 + dz * e;
			b.box(Math.min(ax, ex) - t, Math.min(ay, ey) - t, Math.min(az, ez) - t, Math.max(ax, ex) + t, Math.max(ay, ey) + t, Math.max(az, ez) + t);
		}
		return d.length / 2;
	}

	/** An × on the top face of the cell at ({@code x}, {@code y}, {@code z}): two stepped diagonals of small boxes. */
	public static int cross(Box b, float x, float y, float z) {
		int steps = 6;
		float s = 0.07f;
		float top = y + 1.02f;
		for (int i = 0; i <= steps; i++) {
			float f = 0.15f + 0.7f * i / steps;
			b.box(x + f - s, top, z + f - s, x + f + s, top + 0.04f, z + f + s);
			b.box(x + f - s, top, z + 1 - f - s, x + f + s, top + 0.04f, z + 1 - f + s);
		}
		return (steps + 1) * 2;
	}

	/** A "!" standing over the cell: a post and a dot, half a block above its top. */
	public static int bang(Box b, float x, float y, float z) {
		float cx = x + 0.5f;
		float cz = z + 0.5f;
		float w = 0.07f;
		b.box(cx - w, y + 1.55f, cz - w, cx + w, y + 2.05f, cz + w);
		b.box(cx - w, y + 1.30f, cz - w, cx + w, y + 1.44f, cz + w);
		return 2;
	}

	/** Short L-shaped ticks at the cell's four top corners. */
	public static int cornerTicks(Box b, float x, float y, float z) {
		float l = 0.3f;
		float t = 0.04f;
		float top = y + 1.01f;
		int n = 0;
		for (int cxi = 0; cxi <= 1; cxi++) {
			for (int czi = 0; czi <= 1; czi++) {
				float px = x + cxi;
				float pz = z + czi;
				float sx = cxi == 0 ? 1 : -1;
				float sz = czi == 0 ? 1 : -1;
				b.box(Math.min(px, px + sx * l), top, pz - t, Math.max(px, px + sx * l), top + 2 * t, pz + t);
				b.box(px - t, top, Math.min(pz, pz + sz * l), px + t, top + 2 * t, Math.max(pz, pz + sz * l));
				n += 2;
			}
		}
		return n;
	}
}
