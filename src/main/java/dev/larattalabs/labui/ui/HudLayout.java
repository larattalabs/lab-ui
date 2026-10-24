package dev.larattalabs.labui.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Lays out every HUD zone for one frame (pure, unit-tested in {@code HudLayoutTest}; drawn by the client's
 * {@code HudZones}). Zones are laid out in this order, each slot placed earlier being an obstacle for the zones after it:
 * {@link HudZone#BOTTOM_CENTER} (aimed panels never move for a toast), {@link HudZone#TOP_CENTER}, {@link HudZone#TOP_LEFT},
 * {@link HudZone#TOP_RIGHT_TOASTS}, {@link HudZone#LEFT}, {@link HudZone#RIGHT}. Within a zone {@link ZoneStack} stacks the
 * elements by priority. Vanilla's own pieces (boss bars, effect icons, toasts, the "Now playing" toast) come in as
 * {@link Vanilla}. All values are GUI px.
 */
public final class HudLayout {
	/** Gap between elements of a zone, and between a zone and an obstacle. */
	public static final int GAP = 4;
	/** TOP_LEFT's inset from the left and top edges. */
	public static final int LEFT_X = 6;
	public static final int LEFT_TOP = 6;
	/** TOP_CENTER's top without boss bars. */
	public static final int CENTER_TOP = 4;
	/** The toast column: width, right margin, top without vanilla pieces (lab-ui {@code Toasts} since 0.2.0). */
	public static final int TOAST_W = 196;
	public static final int TOAST_MARGIN = 6;
	public static final int TOAST_TOP = 8;
	/** BOTTOM_CENTER's usual bottom: this far above the screen's bottom (above the action bar and the item name). */
	public static final int BOTTOM_ABOVE = 64;

	private HudLayout() {
	}

	/** The frame: GUI size and whether survival's hearts and food sit above the hotbar. */
	public record Frame(int guiWidth, int guiHeight, boolean survivalBars) {
	}

	/** Bottoms of vanilla's pieces this frame (0 = not shown). */
	public record Vanilla(int bossBottom, int effectsBottom, int toastsBottom, int nowPlayingBottom) {
		public static final Vanilla NONE = new Vanilla(0, 0, 0, 0);
	}

	/** One registered element: its zone, priority (high = nearer the anchor, shrinks and hides last) and registration order. */
	public record Elem(HudZone zone, int priority, int order) {
	}

	/** What an element wants (see {@code HudZones.Size}). */
	public record Want(int w, int h, int minH, boolean optional) {
	}

	/** Asks element {@code index} what it wants at {@code maxW}; null when it is not shown this frame. */
	public interface Measurer {
		@Nullable Want measure(int index, int maxW);
	}

	/** A rect. */
	public record Rect(int x, int y, int w, int h) {
		public int bottom() {
			return y + h;
		}

		public int right() {
			return x + w;
		}

		boolean crossesX(int x0, int x1) {
			return x < x1 && x0 < right();
		}

		boolean crossesY(int y0, int y1) {
			return y < y1 && y0 < bottom();
		}
	}

	/** Element {@code index}'s slot; {@code collapsed} = it got less height than it wanted. */
	public record Slot(int index, int x, int y, int w, int h, boolean collapsed) {
		public Rect rect() {
			return new Rect(x, y, w, h);
		}
	}

	/** The frame's layout: the slots, hidden elements (measured but no room), each zone's bounds, zones that overflow. */
	public record Layout(List<Slot> slots, List<Integer> hidden, Map<HudZone, Rect> zones, List<HudZone> overflow) {
		public @Nullable Slot slot(int index) {
			for (Slot s : slots) {
				if (s.index() == index) {
					return s;
				}
			}
			return null;
		}
	}

	/**
	 * Lays out {@code elems}. {@code toastAvoid}: rects {x, y, w, h} the toast column ends 4 px above, wherever they are
	 * ({@code Toasts.avoid}, for HUD pieces that are not zone elements).
	 */
	public static Layout layout(Frame f, Vanilla v, List<Elem> elems, Measurer m, List<int[]> toastAvoid) {
		int gw = f.guiWidth();
		int gh = f.guiHeight();
		List<Slot> slots = new ArrayList<>();
		List<Integer> hidden = new ArrayList<>();
		Map<HudZone, Rect> zones = new EnumMap<>(HudZone.class);
		List<HudZone> overflow = new ArrayList<>();
		Map<Integer, Want> wants = new java.util.HashMap<>();

		// ---- BOTTOM_CENTER: up from gh-64; on a short screen the floor drops to just above the hotbar (bars)
		{
			List<Integer> ids = members(elems, HudZone.BOTTOM_CENTER);
			int top = gh / 2 + 8;
			int floor = gh - (f.survivalBars() ? 50 : 26);
			List<Integer> live = measureAll(ids, m, Math.max(0, gw - 16), wants);
			int total = totalWanted(live, wants);
			int bottom = top + total <= gh - BOTTOM_ABOVE ? gh - BOTTOM_ABOVE : Math.max(top, floor);
			int space = bottom - top;
			zones.put(HudZone.BOTTOM_CENTER, new Rect(0, top, gw, space));
			place(HudZone.BOTTOM_CENTER, live, elems, wants, bottom, false, space, slots, hidden, overflow, (w) -> (gw - w) / 2);
		}
		// ---- TOP_CENTER: down from below the boss bars, to the crosshair
		{
			List<Integer> ids = members(elems, HudZone.TOP_CENTER);
			int top = v.bossBottom() > 0 ? v.bossBottom() + 3 : CENTER_TOP;
			int limit = gh / 2 - 12;
			List<Integer> live = measureAll(ids, m, Math.max(0, gw - 16), wants);
			zones.put(HudZone.TOP_CENTER, new Rect(0, top, gw, Math.max(0, limit - top)));
			place(HudZone.TOP_CENTER, live, elems, wants, top, true, limit - top, slots, hidden, overflow, (w) -> (gw - w) / 2);
		}
		List<Rect> centre = rectsOf(slots, elems, HudZone.TOP_CENTER);
		int columnX = gw - TOAST_MARGIN - TOAST_W;
		// ---- TOP_LEFT: down from the corner (below "Now playing"); each element ends at the toast column or a centre slot
		{
			List<Integer> ids = members(elems, HudZone.TOP_LEFT);
			int top = v.nowPlayingBottom() > 0 ? v.nowPlayingBottom() + 2 : LEFT_TOP;
			int limit = gh / 2 - 12;
			int room0 = room(LEFT_X, top, 12, columnX, centre);
			List<Integer> live = measureAll(ids, m, room0, wants);
			zones.put(HudZone.TOP_LEFT, new Rect(LEFT_X, top, Math.max(0, columnX - GAP - 2 - LEFT_X), Math.max(0, limit - top)));
			ZoneStack.Result r = ZoneStack.stack(items(live, elems, wants), top, true, limit - top, GAP);
			for (int k : r.hidden()) {
				hidden.add(live.get(k));
			}
			if (r.overflow()) {
				overflow.add(HudZone.TOP_LEFT);
			}
			int y = top;
			for (ZoneStack.Placed p : r.placed()) {
				int idx = live.get(p.index());
				Want w = wants.get(idx);
				int room = room(LEFT_X, y, p.h(), columnX, centre);
				if (w.w() > room) {
					Want again = m.measure(idx, room);
					if (again != null) {
						w = again;
					}
				}
				// still too wide beside the centre: step below the centre slots it meets, as often as needed
				for (int step = 0; w.w() > room && step <= centre.size(); step++) {
					int below = y;
					for (Rect c : centre) {
						if (c.crossesY(y, y + Math.max(1, p.h())) && c.x() < LEFT_X + w.w() + GAP + 2) {
							below = Math.max(below, c.bottom() + GAP);
						}
					}
					if (below == y) {
						break;
					}
					y = below;
					room = room(LEFT_X, y, p.h(), columnX, centre);
					Want again = m.measure(idx, room);
					if (again != null) {
						w = again;
					}
				}
				wants.put(idx, w);
				slots.add(new Slot(idx, LEFT_X, y, Math.min(w.w(), Math.max(0, room)), p.h(), p.collapsed()));
				y += p.h() + GAP;
			}
		}
		// ---- TOP_RIGHT_TOASTS: the column, below vanilla's effects and toasts, above what the other zones placed in it
		{
			List<Integer> ids = members(elems, HudZone.TOP_RIGHT_TOASTS);
			int top = TOAST_TOP;
			if (v.effectsBottom() > 0) {
				top = Math.max(top, v.effectsBottom() + 2);
			}
			if (v.toastsBottom() > 0) {
				top = Math.max(top, v.toastsBottom() + GAP);
			}
			int limit = gh - (f.survivalBars() ? 50 : 26);
			List<Rect> obstacles = new ArrayList<>();
			for (Slot s : slots) {
				if (s.rect().crossesX(columnX, gw - TOAST_MARGIN)) {
					obstacles.add(s.rect());
				}
			}
			obstacles.sort(Comparator.comparingInt(Rect::y));
			for (Rect o : obstacles) {
				if (o.y() <= top) {
					top = Math.max(top, o.bottom() + GAP);
				} else {
					limit = Math.min(limit, o.y() - GAP);
				}
			}
			for (int[] a : toastAvoid) {
				if (a != null && a.length >= 2) {
					limit = Math.min(limit, a[1] - GAP);
				}
			}
			List<Integer> live = measureAll(ids, m, TOAST_W, wants);
			zones.put(HudZone.TOP_RIGHT_TOASTS, new Rect(columnX, top, TOAST_W, Math.max(0, limit - top)));
			place(HudZone.TOP_RIGHT_TOASTS, live, elems, wants, top, true, limit - top, slots, hidden, overflow, (w) -> gw - TOAST_MARGIN - w);
		}
		// ---- LEFT / RIGHT: centred on the middle of the screen, below what the top zones placed on that side
		for (HudZone z : new HudZone[] {HudZone.LEFT, HudZone.RIGHT}) {
			List<Integer> ids = members(elems, z);
			if (ids.isEmpty()) {
				continue;
			}
			boolean left = z == HudZone.LEFT;
			int maxW = Math.max(0, gw / 4);
			int sideX0 = left ? 0 : gw - TOAST_MARGIN - maxW;
			int sideX1 = left ? LEFT_X + maxW : gw;
			int top = gh / 4;
			for (Slot s : slots) {
				if (s.rect().crossesX(sideX0, sideX1) && s.y() < gh / 2) {
					top = Math.max(top, s.rect().bottom() + GAP);
				}
			}
			int limit = gh * 3 / 4;
			List<Integer> live = measureAll(ids, m, maxW, wants);
			int total = totalWanted(live, wants);
			int start = Math.max(top, Math.min(gh / 2 - total / 2, limit - total));
			zones.put(z, new Rect(left ? LEFT_X : gw - TOAST_MARGIN - maxW, top, maxW, Math.max(0, limit - top)));
			place(z, live, elems, wants, start, true, limit - start, slots, hidden, overflow, left ? (w) -> LEFT_X : (w) -> gw - TOAST_MARGIN - w);
		}
		return new Layout(List.copyOf(slots), List.copyOf(hidden), zones, List.copyOf(overflow));
	}

	/** The room right of {@code x} on the rows {@code y..y+h}: up to the toast column or the nearest centre slot, less a gap. */
	static int room(int x, int y, int h, int columnX, List<Rect> centre) {
		int right = columnX - GAP - 2;
		for (Rect c : centre) {
			if (c.crossesY(y, y + Math.max(1, h)) && c.x() > x) {
				right = Math.min(right, c.x() - GAP - 2);
			}
		}
		return Math.max(0, right - x);
	}

	private static List<Integer> members(List<Elem> elems, HudZone z) {
		List<Integer> out = new ArrayList<>();
		for (int i = 0; i < elems.size(); i++) {
			if (elems.get(i).zone() == z) {
				out.add(i);
			}
		}
		return out;
	}

	private static List<Integer> measureAll(List<Integer> ids, Measurer m, int maxW, Map<Integer, Want> wants) {
		List<Integer> live = new ArrayList<>();
		for (int i : ids) {
			Want w = m.measure(i, maxW);
			if (w != null && w.h() > 0) {
				wants.put(i, w);
				live.add(i);
			}
		}
		return live;
	}

	private static int totalWanted(List<Integer> live, Map<Integer, Want> wants) {
		int t = 0;
		for (int i : live) {
			t += wants.get(i).h();
		}
		return live.isEmpty() ? 0 : t + GAP * (live.size() - 1);
	}

	private static List<ZoneStack.Item> items(List<Integer> live, List<Elem> elems, Map<Integer, Want> wants) {
		List<ZoneStack.Item> items = new ArrayList<>();
		for (int i : live) {
			Want w = wants.get(i);
			int h = Math.max(0, w.h());
			items.add(new ZoneStack.Item(elems.get(i).priority(), elems.get(i).order(), h, Math.max(0, Math.min(h, w.minH())), w.optional()));
		}
		return items;
	}

	private static void place(HudZone z, List<Integer> live, List<Elem> elems, Map<Integer, Want> wants, int anchor, boolean down, int space,
		List<Slot> slots, List<Integer> hidden, List<HudZone> overflow, java.util.function.IntUnaryOperator xOf) {
		ZoneStack.Result r = ZoneStack.stack(items(live, elems, wants), anchor, down, space, GAP);
		for (int k : r.hidden()) {
			hidden.add(live.get(k));
		}
		if (r.overflow()) {
			overflow.add(z);
		}
		for (ZoneStack.Placed p : r.placed()) {
			int idx = live.get(p.index());
			int w = wants.get(idx).w();
			slots.add(new Slot(idx, xOf.applyAsInt(w), p.y(), w, p.h(), p.collapsed()));
		}
	}

	private static List<Rect> rectsOf(List<Slot> slots, List<Elem> elems, HudZone z) {
		List<Rect> out = new ArrayList<>();
		for (Slot s : slots) {
			if (elems.get(s.index()).zone() == z) {
				out.add(s.rect());
			}
		}
		return out;
	}
}
