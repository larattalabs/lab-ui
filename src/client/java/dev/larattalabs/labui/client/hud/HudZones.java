package dev.larattalabs.labui.client.hud;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.larattalabs.labui.LabUi;
import dev.larattalabs.labui.ui.Guard;
import dev.larattalabs.labui.ui.HudLayout;
import dev.larattalabs.labui.ui.HudZone;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

/**
 * Shared HUD zones (0.3.0): mods register their HUD pieces into named zones ({@link HudZone}) instead of drawing them at
 * fixed spots, so pieces of different mods never overlap each other or vanilla's boss bars, effect icons and toasts.
 *
 * <p>Retained registration, immediate drawing: {@link #add} once (client init); then every frame lab-ui's one HUD element
 * ({@code lab_ui:hud/zones}) asks each element to {@link Element#measure measure} itself, lays out every zone
 * ({@link HudLayout}, pure and unit-tested) and calls {@link Element#draw draw} with the element's {@link Slot}. Within a
 * zone, higher priority sits nearer the anchor edge and shrinks or hides last; an element may get any height between its
 * {@link Size#minH} and {@link Size#h} ({@link Slot#collapsed} tells it to draw its short form). Each measure and draw runs
 * in {@link Guard} under the element's id, so one failing element never takes the others down.
 *
 * <pre>
 * HudZones.add(MyMod.id("status"), HudZone.TOP_LEFT, new StatusLine()).priority(60);
 * </pre>
 *
 * <p>Fabric draws HUD elements only while the HUD is shown (not under F1), so zone elements are hidden with F1 like the rest
 * of the HUD. Under an open screen an element is hidden unless {@link Handle#whileScreenOpen}. Client thread only.
 */
public final class HudZones {
	/** Default priority. */
	public static final int DEFAULT_PRIORITY = 50;

	/** What every element can ask this frame. */
	public record Frame(int guiWidth, int guiHeight, boolean hudHidden, boolean screenOpen, boolean survivalBars, long nowMs) {
	}

	/** An element's wanted size: {@code h}, or anything down to {@code minH} when space is short; {@code hideable} ones ({@link #optional()}) may be hidden. */
	public record Size(int w, int h, int minH, boolean hideable) {
		/** A fixed size: never shrunk, never hidden (placed even when it overflows). */
		public static Size of(int w, int h) {
			return new Size(w, h, h, false);
		}

		/** May shrink to {@code minH} (clamped to 0..h). */
		public Size shrinkTo(int minH) {
			return new Size(w, h, Math.max(0, Math.min(h, minH)), hideable);
		}

		/** May be hidden when even {@code minH} does not fit. */
		public Size optional() {
			return new Size(w, h, minH, true);
		}
	}

	/** Where an element draws: its rect, and {@code collapsed} when it got less height than it wanted. */
	public record Slot(int x, int y, int w, int h, boolean collapsed) {
	}

	/** A HUD piece in a zone. */
	public interface Element {
		/**
		 * The size this frame, or null when not shown. {@code maxW}: the room at this element's place (ellipsize to it).
		 * May be called twice in a frame (TOP_LEFT re-measures with a narrower room).
		 */
		@Nullable Size measure(Font font, Frame f, int maxW);

		void draw(GuiGraphicsExtractor g, Font font, Frame f, Slot slot);
	}

	/** Tunes a registration. */
	public interface Handle {
		/** Higher = nearer the zone's anchor edge, shrinks and hides last (default {@value #DEFAULT_PRIORITY}). */
		Handle priority(int p);

		/** Shown under open screens too (default false: hidden while any screen is open). */
		Handle whileScreenOpen(boolean b);
	}

	private static final class Reg implements Handle {
		final Identifier id;
		final HudZone zone;
		final Element element;
		final int order;
		volatile int priority = DEFAULT_PRIORITY;
		volatile boolean whileScreenOpen;
		volatile @Nullable Slot last;
		@Nullable Size size;

		Reg(Identifier id, HudZone zone, Element element, int order) {
			this.id = id;
			this.zone = zone;
			this.element = element;
			this.order = order;
		}

		@Override
		public Handle priority(int p) {
			priority = p;
			return this;
		}

		@Override
		public Handle whileScreenOpen(boolean b) {
			whileScreenOpen = b;
			return this;
		}

		String kind() {
			return id.toString();
		}
	}

	private static final List<Reg> REGS = new CopyOnWriteArrayList<>();
	private static final Map<HudZone, List<Supplier<int @Nullable []>>> AVOID = new EnumMap<>(HudZone.class);
	private static int nextOrder;
	private static boolean installed;
	private static volatile HudLayout.@Nullable Layout lastLayout;
	private static volatile List<Reg> lastShown = List.of();
	private static volatile HudLayout.Vanilla lastVanilla = HudLayout.Vanilla.NONE;

	private HudZones() {
	}

	/** Registers lab-ui's one zone HUD element ({@code lab_ui:hud/zones}) once; later calls do nothing. {@link Toasts#install()} calls it. */
	public static synchronized void install() {
		if (installed) {
			return;
		}
		installed = true;
		HudElementRegistry.addLast(LabUi.id("hud/zones"), (g, dt) -> Guard.run("lab_ui.hud.zones", () -> render(g)));
	}

	/**
	 * Adds {@code element} to {@code zone} under {@code id} ({@code <modid>:<name>}; also its {@link Guard} kind). Replaces an
	 * element already registered under that id. Installs the zones when needed.
	 */
	public static synchronized Handle add(Identifier id, HudZone zone, Element element) {
		install();
		remove(id);
		Reg r = new Reg(id, zone, element, nextOrder++);
		REGS.add(r);
		return r;
	}

	public static synchronized void remove(Identifier id) {
		REGS.removeIf(r -> r.id.equals(id));
	}

	/**
	 * A rect ({x, y, w, h} GUI px, or null when not shown) the zone must end above. Only {@link HudZone#TOP_RIGHT_TOASTS}
	 * honours it (the stack ends 4 px above the highest such rect, as {@code Toasts.avoid} since 0.2.0); for HUD pieces that
	 * are not zone elements. Zone elements need none: each zone keeps clear of the slots placed before it.
	 */
	public static synchronized void avoid(HudZone zone, Supplier<int @Nullable []> rect) {
		AVOID.computeIfAbsent(zone, z -> new CopyOnWriteArrayList<>()).add(rect);
	}

	/** True while the HUD is hidden (F1). */
	public static boolean hudHidden() {
		return Minecraft.getInstance().gui.hud.isHidden();
	}

	/** True while any screen is open. */
	public static boolean screenOpen() {
		return Minecraft.getInstance().gui.screen() != null;
	}

	/** The slot {@code id} was drawn in last frame, or null when it was not drawn. */
	public static @Nullable Slot rect(Identifier id) {
		for (Reg r : REGS) {
			if (r.id.equals(id)) {
				return r.last;
			}
		}
		return null;
	}

	// ------------------------------------------------------------------ the frame

	private static void render(GuiGraphicsExtractor g) {
		Minecraft mc = Minecraft.getInstance();
		Font font = mc.font;
		boolean screen = mc.gui.screen() != null;
		boolean bars = mc.gameMode != null && mc.gameMode.getPlayerMode().isSurvival();
		Frame f = new Frame(g.guiWidth(), g.guiHeight(), mc.gui.hud.isHidden(), screen, bars, Util.getMillis());
		List<Reg> shown = new ArrayList<>();
		for (Reg r : REGS) {
			r.last = null;
			r.size = null;
			if (!screen || r.whileScreenOpen) {
				shown.add(r);
			}
		}
		List<HudLayout.Elem> elems = new ArrayList<>(shown.size());
		for (Reg r : shown) {
			elems.add(new HudLayout.Elem(r.zone, r.priority, r.order));
		}
		HudLayout.Vanilla v = Guard.call("lab_ui.hud.zones.vanilla", () -> VanillaHud.read(mc, g.guiHeight()), HudLayout.Vanilla.NONE);
		List<int[]> avoid = new ArrayList<>();
		List<Supplier<int @Nullable []>> toastAvoid = AVOID.get(HudZone.TOP_RIGHT_TOASTS);
		if (toastAvoid != null) {
			for (Supplier<int @Nullable []> s : toastAvoid) {
				int[] rr = Guard.call("lab_ui.hud.zones.avoid", s::get, null);
				if (rr != null) {
					avoid.add(rr);
				}
			}
		}
		HudLayout.Layout layout = HudLayout.layout(new HudLayout.Frame(f.guiWidth(), f.guiHeight(), bars), v, elems, (i, maxW) -> {
			Reg r = shown.get(i);
			Size s = Guard.call(r.kind(), () -> r.element.measure(font, f, Math.max(0, maxW)), null);
			r.size = s;
			return s == null ? null : new HudLayout.Want(Math.max(0, s.w()), Math.max(0, s.h()), Math.max(0, s.minH()), s.hideable());
		}, avoid);
		lastLayout = layout;
		lastShown = shown;
		lastVanilla = v;
		for (HudLayout.Slot s : layout.slots()) {
			Reg r = shown.get(s.index());
			Slot slot = new Slot(s.x(), s.y(), s.w(), s.h(), s.collapsed());
			r.last = slot;
			Guard.run(r.kind(), () -> r.element.draw(g, font, f, slot));
		}
	}

	/** QA: the last frame's zones (bounds, overflow), slots by id, hidden ids and vanilla's pieces. */
	public static JsonObject json() {
		JsonObject o = new JsonObject();
		HudLayout.Layout l = lastLayout;
		List<Reg> shown = lastShown;
		HudLayout.Vanilla v = lastVanilla;
		JsonObject van = new JsonObject();
		van.addProperty("bossBottom", v.bossBottom());
		van.addProperty("effectsBottom", v.effectsBottom());
		van.addProperty("toastsBottom", v.toastsBottom());
		van.addProperty("nowPlayingBottom", v.nowPlayingBottom());
		o.add("vanilla", van);
		JsonObject zones = new JsonObject();
		if (l != null) {
			for (HudZone z : HudZone.values()) {
				JsonObject zo = new JsonObject();
				HudLayout.Rect b = l.zones().get(z);
				if (b != null) {
					zo.add("bounds", rect(b.x(), b.y(), b.w(), b.h()));
				}
				zo.addProperty("overflow", l.overflow().contains(z));
				JsonArray slots = new JsonArray();
				for (HudLayout.Slot s : l.slots()) {
					if (s.index() < shown.size() && shown.get(s.index()).zone == z) {
						JsonObject so = rect(s.x(), s.y(), s.w(), s.h());
						so.addProperty("id", shown.get(s.index()).id.toString());
						so.addProperty("collapsed", s.collapsed());
						slots.add(so);
					}
				}
				zo.add("slots", slots);
				JsonArray hidden = new JsonArray();
				for (int i : l.hidden()) {
					if (i < shown.size() && shown.get(i).zone == z) {
						hidden.add(shown.get(i).id.toString());
					}
				}
				zo.add("hidden", hidden);
				zones.add(z.name().toLowerCase(Locale.ROOT), zo);
			}
		}
		o.add("zones", zones);
		JsonArray regs = new JsonArray();
		for (Reg r : REGS) {
			regs.add(r.id + " " + r.zone.name().toLowerCase(Locale.ROOT) + " p" + r.priority);
		}
		o.add("registered", regs);
		return o;
	}

	private static JsonObject rect(int x, int y, int w, int h) {
		JsonObject j = new JsonObject();
		j.addProperty("x", x);
		j.addProperty("y", y);
		j.addProperty("w", w);
		j.addProperty("h", h);
		return j;
	}
}
