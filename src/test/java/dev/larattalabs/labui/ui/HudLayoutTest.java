package dev.larattalabs.labui.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.larattalabs.labui.ui.HudLayout.Elem;
import dev.larattalabs.labui.ui.HudLayout.Frame;
import dev.larattalabs.labui.ui.HudLayout.Layout;
import dev.larattalabs.labui.ui.HudLayout.Rect;
import dev.larattalabs.labui.ui.HudLayout.Slot;
import dev.larattalabs.labui.ui.HudLayout.Vanilla;
import dev.larattalabs.labui.ui.HudLayout.Want;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import org.junit.jupiter.api.Test;

/** The whole-frame layout with the real pieces' sizes (Architect's and Steward's HUDs, the toast stack). */
class HudLayoutTest {
	/** 1920x1080 at GUI scales 2, 3 and 4, and 4K at auto scale (426x240). */
	static final int[][] SCREENS = {{960, 540}, {640, 360}, {480, 270}, {426, 240}};

	/** A scene: elements and what each wants (a function of maxW, so ellipsizing ones can follow the room). */
	static final class Scene {
		final List<Elem> elems = new ArrayList<>();
		final List<IntFunction<Want>> wants = new ArrayList<>();
		final Map<Integer, List<Integer>> asked = new HashMap<>();

		int add(HudZone z, int prio, IntFunction<Want> w) {
			elems.add(new Elem(z, prio, elems.size()));
			wants.add(w);
			return elems.size() - 1;
		}

		Layout layout(Frame f, Vanilla v, List<int[]> avoid) {
			return HudLayout.layout(f, v, elems, (i, maxW) -> {
				asked.computeIfAbsent(i, k -> new ArrayList<>()).add(maxW);
				return wants.get(i).apply(maxW);
			}, avoid);
		}
	}

	static IntFunction<Want> fixed(int w, int h) {
		return maxW -> new Want(Math.min(w, maxW), h, h, false);
	}

	/** Steward's inbox line: ellipsizes its settlement name to the room (at least 120 px of fixed text). */
	static IntFunction<Want> stewardLine(int full) {
		return maxW -> new Want(Math.max(120, Math.min(full, maxW)), 12, 12, false);
	}

	static void noOverlap(Layout l) {
		List<Slot> s = l.slots();
		for (int i = 0; i < s.size(); i++) {
			for (int j = i + 1; j < s.size(); j++) {
				Rect a = s.get(i).rect();
				Rect b = s.get(j).rect();
				boolean overlap = a.x() < b.right() && b.x() < a.right() && a.y() < b.bottom() && b.y() < a.bottom() && a.h() > 0 && b.h() > 0;
				assertFalse(overlap, "slots overlap: " + s.get(i) + " and " + s.get(j));
			}
		}
	}

	static void noOverlap(Layout l, Rect vanilla) {
		for (Slot s : l.slots()) {
			Rect a = s.rect();
			boolean overlap = a.x() < vanilla.right() && vanilla.x() < a.right() && a.y() < vanilla.bottom() && vanilla.y() < a.bottom() && a.h() > 0;
			assertFalse(overlap, s + " overlaps vanilla's " + vanilla);
		}
	}

	/** Every piece of both mods at once, the toasts wanting a full stack. */
	static Scene everything(int toastsH) {
		Scene sc = new Scene();
		sc.add(HudZone.BOTTOM_CENTER, 100, maxW -> new Want(Math.min(420, maxW), 96, 46, false)); // placement panel (HudFit KEEP rows)
		sc.add(HudZone.TOP_CENTER, 70, fixed(300, 28)); // region ghost HUD
		sc.add(HudZone.TOP_CENTER, 60, maxW -> new Want(Math.min(360, maxW), 20, 14, false)); // construction HUD, full or one line
		sc.add(HudZone.TOP_LEFT, 60, stewardLine(330)); // Steward's inbox line
		sc.add(HudZone.TOP_LEFT, 40, maxW -> new Want(Math.min(320, maxW), 30, 20, false)); // helper strip, drops its npm line
		sc.add(HudZone.TOP_RIGHT_TOASTS, 0, maxW -> new Want(196, toastsH, 0, true)); // the toast stack
		return sc;
	}

	@Test
	void bothModsAtOnceNeverOverlapOnAnyScreen() {
		for (int[] s : SCREENS) {
			for (boolean bars : new boolean[] {true, false}) {
				Layout l = everything(170).layout(new Frame(s[0], s[1], bars), Vanilla.NONE, List.of());
				noOverlap(l);
				// the aimed panel is never moved for anything: centred, above the hotbar
				Slot p = l.slot(0);
				assertNotNull(p);
				assertEquals((s[0] - Math.min(420, s[0] - 16)) / 2, p.x());
				assertTrue(p.y() >= s[1] / 2 + 8, "below the crosshair at " + s[0] + "x" + s[1]);
			}
		}
	}

	@Test
	void theBottomPanelSitsWhereThePlacementHudDidOnTall() {
		// 960x540: the old y was guiHeight - 64 - h
		Layout l = everything(0).layout(new Frame(960, 540, true), Vanilla.NONE, List.of());
		assertEquals(540 - 64 - 96, l.slot(0).y());
		// 480x270 (GUI 4): it doesn't fit above the action bar, so it drops to the floor above the hearts, as before
		l = everything(0).layout(new Frame(480, 270, true), Vanilla.NONE, List.of());
		Slot p = l.slot(0);
		assertEquals(270 - 50, p.y() + p.h());
		assertEquals(270 - 50 - (135 + 8), p.h()); // shrunk to the room between the crosshair and the floor (HudFit's budget)
		assertTrue(p.collapsed());
	}

	@Test
	void topCentreStacksBelowBossBarsAndNoLongerOverlapsItself() {
		Scene sc = everything(0);
		Layout l = sc.layout(new Frame(960, 540, false), new Vanilla(VanillaHudMath.bossBottom(2, 540), 0, 0, 0), List.of());
		Slot region = l.slot(1);
		Slot site = l.slot(2);
		assertEquals(36 + 3, region.y()); // two raid bars end at 36
		assertEquals(region.y() + region.h() + HudLayout.GAP, site.y());
		noOverlap(l, new Rect(960 / 2 - 91, 3, 182, 36 - 3));
	}

	@Test
	void topLeftEndsAtAKnownRightEdgeAndTheHelperGoesUnderSteward() {
		Scene sc = everything(0);
		Layout l = sc.layout(new Frame(960, 540, false), Vanilla.NONE, List.of());
		Slot steward = l.slot(3);
		Slot helper = l.slot(4);
		assertEquals(HudLayout.LEFT_X, steward.x());
		assertEquals(HudLayout.LEFT_TOP, steward.y());
		assertEquals(steward.y() + 12 + HudLayout.GAP, helper.y());
		// the room it was given ends before the centre slot on its rows (the region HUD, 4..32) and the toast column
		int centreLeft = l.slot(1).x();
		for (int maxW : sc.asked.get(3)) {
			assertTrue(HudLayout.LEFT_X + maxW <= Math.min(centreLeft, 960 - 6 - 196) - HudLayout.GAP, "maxW " + maxW);
		}
		assertTrue(steward.x() + steward.w() <= centreLeft - HudLayout.GAP);
	}

	@Test
	void onANarrowScreenTheStewardLineStepsBelowTheCentreWhenItCantShrinkEnough() {
		Scene sc = everything(0);
		Layout l = sc.layout(new Frame(426, 240, false), Vanilla.NONE, List.of());
		noOverlap(l);
		Slot steward = l.slot(3);
		Slot site = l.slot(2);
		assertTrue(steward.y() >= site.y() + site.h(), "steward " + steward + " under the centre " + site);
	}

	@Test
	void toastsStartBelowVanillaToastsEffectsAndTheCentreSlotsInTheirColumn() {
		Scene sc = new Scene();
		int t = sc.add(HudZone.TOP_RIGHT_TOASTS, 0, maxW -> new Want(196, 120, 0, true));
		// a vanilla recipe toast in slot 0 and effects: the column starts under the lower of them
		Layout l = sc.layout(new Frame(960, 540, false), new Vanilla(0, VanillaHudMath.effectsBottom(1, 1, false), 32, 0), List.of());
		assertEquals(Math.max(32 + HudLayout.GAP, 51 + 2), l.slot(t).y());
		assertEquals(960 - 6 - 196, l.slot(t).x());
		// nothing of vanilla's: 8 as in 0.2
		l = sc.layout(new Frame(960, 540, false), Vanilla.NONE, List.of());
		assertEquals(8, l.slot(t).y());
		// a wide centre slot reaching into the column on a narrow screen: the toasts start under it
		Scene n = everything(120);
		l = n.layout(new Frame(426, 240, false), Vanilla.NONE, List.of());
		noOverlap(l);
	}

	@Test
	void theColumnTakesTheFirstFreeRunThatHoldsTheStackElseTheTallest() {
		List<Rect> obs = List.of(new Rect(0, 39, 100, 28), new Rect(0, 71, 100, 22), new Rect(0, 143, 100, 80));
		// 36..35 and 71..67 are empty, 97..139 is 42 tall, nothing below the panel (limit 220)
		assertEquals(List.of(97, 139), List.of(HudLayout.freeRun(36, 220, obs, 128)[0], HudLayout.freeRun(36, 220, obs, 128)[1]));
		assertEquals(97, HudLayout.freeRun(36, 220, obs, 30)[0]);
		// room above the first obstacle when the stack fits there
		assertEquals(8, HudLayout.freeRun(8, 220, List.of(new Rect(0, 100, 10, 10)), 60)[0]);
		assertEquals(114, HudLayout.freeRun(8, 220, List.of(new Rect(0, 100, 10, 10)), 100)[0]);
		// no obstacles: the whole span
		int[] r = HudLayout.freeRun(8, 490, List.of(), 1000);
		assertEquals(8, r[0]);
		assertEquals(490, r[1]);
	}

	@Test
	void toastsAvoidKeepsItsZeroTwoMeaning() {
		Scene sc = new Scene();
		int t = sc.add(HudZone.TOP_RIGHT_TOASTS, 0, maxW -> new Want(196, 300, 0, true));
		Layout l = sc.layout(new Frame(960, 540, true), Vanilla.NONE, java.util.Arrays.asList(new int[] {10, 200, 40, 40}, null));
		Slot s = l.slot(t);
		assertEquals(8, s.y());
		assertEquals(200 - 4, s.y() + s.h());
		// without it: down to the hearts
		l = sc.layout(new Frame(960, 540, true), Vanilla.NONE, List.of());
		assertEquals(540 - 50, l.zones().get(HudZone.TOP_RIGHT_TOASTS).bottom());
		assertEquals(8 + 300, l.slot(t).y() + l.slot(t).h());
	}

	@Test
	void bossBarsAreAnObstacleBesideTheTopCentre() {
		// 480x270 (GUI 4) with two raid bars: the Steward line ends before the bars, the toast column starts below them
		Scene sc = everything(60);
		Vanilla v = new Vanilla(VanillaHudMath.bossBottom(2, 270), 0, 0, 0);
		Layout l = sc.layout(new Frame(480, 270, false), v, List.of());
		noOverlap(l);
		Rect bars = new Rect(240 - 91, 3, 182, 36 - 3);
		noOverlap(l, bars);
		Slot steward = l.slot(3);
		assertTrue(steward.x() + steward.w() <= bars.x() - HudLayout.GAP, "steward " + steward);
	}

	@Test
	void theTopLeftZoneStartsBelowNowPlaying() {
		Scene sc = new Scene();
		int i = sc.add(HudZone.TOP_LEFT, 50, fixed(100, 12));
		Layout l = sc.layout(new Frame(960, 540, false), new Vanilla(0, 0, 0, 30), List.of());
		assertEquals(32, l.slot(i).y());
	}

	@Test
	void hiddenAndAbsentElements() {
		Scene sc = new Scene();
		int gone = sc.add(HudZone.TOP_CENTER, 50, maxW -> null);
		int a = sc.add(HudZone.TOP_LEFT, 90, fixed(100, 200));
		int b = sc.add(HudZone.TOP_LEFT, 10, maxW -> new Want(100, 100, 100, true));
		Layout l = sc.layout(new Frame(480, 270, false), Vanilla.NONE, List.of());
		assertNull(l.slot(gone));
		assertNotNull(l.slot(a));
		assertEquals(List.of(b), l.hidden());
		assertTrue(l.overflow().contains(HudZone.TOP_LEFT)); // a fixed 200 px doesn't fit 135 - 12 - 6, and is placed anyway
	}

	@Test
	void leftAndRightCentreOnTheMiddle() {
		Scene sc = new Scene();
		int lft = sc.add(HudZone.LEFT, 50, fixed(80, 40));
		int rgt = sc.add(HudZone.RIGHT, 50, fixed(80, 40));
		Layout l = sc.layout(new Frame(960, 540, false), Vanilla.NONE, List.of());
		assertEquals(270 - 20, l.slot(lft).y());
		assertEquals(HudLayout.LEFT_X, l.slot(lft).x());
		assertEquals(960 - 6 - 80, l.slot(rgt).x());
	}
}
