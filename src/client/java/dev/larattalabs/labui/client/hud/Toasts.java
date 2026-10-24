package dev.larattalabs.labui.client.hud;

import dev.larattalabs.labui.LabUi;
import dev.larattalabs.labui.client.ui.Kit;
import dev.larattalabs.labui.client.ui.Panels;
import dev.larattalabs.labui.client.ui.TextUtil;
import dev.larattalabs.labui.client.ui.UiStyle;
import dev.larattalabs.labui.ui.HudZone;
import dev.larattalabs.labui.ui.ToastStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

/**
 * In-game paper toasts (moved from Architect and Steward in 0.2.0): a title, two lines of text, an optional key hint;
 * "need you" toasts get a clay stripe. They slide in at the top right, stack (need-you first, newest on top, three at most,
 * {@link ToastStack}); what does not fit waits and is counted on one "+N more" line. Not drawn while a screen is open.
 *
 * <p>One stack for every mod (Fabric loads one lab-ui): call {@link #install()} from your client initializer instead of
 * registering a HUD element of your own, and {@link #avoid} for any HUD panel of yours the stack must stay above that is
 * not a {@link HudZones} element.
 *
 * <p>0.3.0: the stack is the {@link HudZone#TOP_RIGHT_TOASTS} zone's lowest-priority element ({@code lab_ui:toasts},
 * also its {@code Guard} kind), drawn by {@code lab_ui:hud/zones}: it starts below vanilla's toasts and effect icons and
 * ends above whatever the other zones place in its column. {@link #push(Spec)} adds up to three body lines and a title
 * {@link Spec#subject subject} that is shortened first. Registering {@code new Toasts()} yourself still draws the 0.2
 * stack at the fixed top right (and draws every toast twice next to {@link #install()}).
 */
public final class Toasts implements HudElement {
	public enum Level { INFO, WARN, NEED }

	private static final int W = 196;
	private static final long QUEUE_MS = 30_000;
	private static final long QUEUE_NEED_MS = 120_000;
	private static final int MORE_H = 13;
	private static final int SLIDE_MS = 180;
	private static final int FADE_MS = 350;
	private static final List<Toast> ACTIVE = new ArrayList<>();
	private static int shown;

	/**
	 * A toast (0.3.0): its level, title, body and optional key hint ("B" "open"); {@code maxBodyLines} 1-3 (clamped; 2 in
	 * the plain overloads); {@code subject}, a prefix of the title shortened first when the title is too wide
	 * ("Stilt Swamp Fishing… needs you"), or null for the plain ellipsis.
	 */
	public record Spec(Level level, String title, String body, @Nullable String hintKey, @Nullable String hintVerb, int maxBodyLines,
		@Nullable String subject) {
		public static Spec of(Level level, String title, String body) {
			return new Spec(level, title, body, null, null, 2, null);
		}

		public Spec hint(@Nullable String key, @Nullable String verb) {
			return new Spec(level, title, body, key, verb, maxBodyLines, subject);
		}

		public Spec bodyLines(int n) {
			return new Spec(level, title, body, hintKey, hintVerb, n, subject);
		}

		public Spec subject(@Nullable String prefixOfTitle) {
			return new Spec(level, title, body, hintKey, hintVerb, maxBodyLines, prefixOfTitle);
		}
	}

	private static final class Toast {
		final Level level;
		final String title;
		final String body;
		final String icon;
		final long arrived;
		long start = -1;
		final long life;
		final @Nullable String hintKey;
		final String hintVerb;
		final int maxBodyLines;
		final @Nullable String subject;

		Toast(Level level, String title, String body, String icon, long arrived, long life, @Nullable String hintKey, String hintVerb,
			int maxBodyLines, @Nullable String subject) {
			this.maxBodyLines = ToastStack.bodyLines(maxBodyLines);
			this.subject = subject;
			this.level = level;
			this.title = title;
			this.body = body;
			this.icon = icon;
			this.arrived = arrived;
			this.life = life;
			this.hintKey = hintKey;
			this.hintVerb = hintVerb;
		}

		boolean need() {
			return level == Level.NEED;
		}

		boolean over(long now) {
			return start >= 0 ? now - start > life : now - arrived > (need() ? QUEUE_NEED_MS : QUEUE_MS);
		}
	}

	private static final List<Supplier<int @Nullable []>> AVOID = new CopyOnWriteArrayList<>();
	private static boolean installed;

	/**
	 * Installs the shared stack once (later calls do nothing; client thread): {@link HudZones#install()} and the
	 * {@code lab_ui:toasts} element in {@link HudZone#TOP_RIGHT_TOASTS} (priority 0, so it gives way first).
	 */
	public static synchronized void install() {
		if (installed) {
			return;
		}
		installed = true;
		HudZones.add(LabUi.id("toasts"), HudZone.TOP_RIGHT_TOASTS, new ZoneElement()).priority(0);
	}

	/**
	 * A HUD rect ({x, y, w, h} in GUI px, or null when not shown) the stack must end above: it stops 4 px over the
	 * highest top of all of them. Each mod adds its own; none replaces another's. Since 0.3.0 the same as
	 * {@code HudZones.avoid(HudZone.TOP_RIGHT_TOASTS, rect)}; zone elements need none.
	 */
	public static void avoid(Supplier<int @Nullable []> rect) {
		AVOID.add(rect);
		HudZones.avoid(HudZone.TOP_RIGHT_TOASTS, rect);
	}

	/** The lowest y the stack may reach: 4 px above the highest avoided rect, or {@code fallback}. */
	static int limit(int fallback) {
		int top = Integer.MAX_VALUE;
		for (Supplier<int @Nullable []> s : AVOID) {
			int[] r = s.get();
			if (r != null && r.length >= 2) {
				top = Math.min(top, r[1]);
			}
		}
		return top == Integer.MAX_VALUE ? fallback : top - 4;
	}

	private static ToastStack.Layout lastLayout = new ToastStack.Layout(List.of(), List.of(), 0, 0, -1);
	private static List<Toast> lastOrder = List.of();

	/** QA: the newest toast's title and body. */
	public static @Nullable String lastText() {
		return ACTIVE.isEmpty() ? null : ACTIVE.get(0).title + ": " + ACTIVE.get(0).body;
	}

	public static int shown() {
		return shown;
	}

	public static void push(Level level, String title, String body) {
		push(level, title, body, null, null);
	}

	/** Add a toast (client thread) with an optional key hint ("B" "open"). */
	public static void push(Level level, String title, String body, @Nullable String hintKey, @Nullable String hintVerb) {
		push(new Spec(level, title, body, hintKey, hintVerb, 2, null));
	}

	/** Add a toast (client thread; 0.3.0): body lines and a title subject as {@code spec} says. */
	public static void push(Spec spec) {
		Level level = spec.level();
		String hintKey = spec.hintKey();
		String hintVerb = spec.hintVerb();
		long life = switch (level) {
			case NEED -> 9000;
			case WARN -> 8000;
			default -> 5500;
		};
		String icon = level == Level.NEED ? "decision" : level == Level.WARN ? "test" : "message";
		ACTIVE.add(0, new Toast(level, spec.title(), spec.body(), icon, Util.getMillis(), life, hintKey, hintVerb == null ? "" : hintVerb,
			spec.maxBodyLines(), spec.subject()));
		for (int drop = ToastStack.evict(needs(ACTIVE), ToastStack.MAX_KEPT); drop >= 0; drop = ToastStack.evict(needs(ACTIVE), ToastStack.MAX_KEPT)) {
			ACTIVE.remove(drop);
		}
		shown++;
	}

	private static List<Boolean> needs(List<Toast> ts) {
		List<Boolean> out = new ArrayList<>(ts.size());
		for (Toast t : ts) {
			out.add(t.need());
		}
		return out;
	}

	/** The 0.2 path, for a {@code new Toasts()} registered by hand: the stack at the fixed top right, ending above {@link #avoid}'s rects. */
	@Override
	public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		long now = Util.getMillis();
		ACTIVE.removeIf(t -> t.over(now));
		if (mc.player == null || ACTIVE.isEmpty() || mc.gui.screen() != null) {
			return;
		}
		boolean bars = mc.gameMode != null && mc.gameMode.getPlayerMode().isSurvival();
		drawStack(g, mc.font, 8, limit(g.guiHeight() - (bars ? 50 : 26)), now);
	}

	/** The stack as the {@link HudZone#TOP_RIGHT_TOASTS} zone's element (0.3.0): it wants the whole stack, takes what is left. */
	private static final class ZoneElement implements HudZones.Element {
		@Override
		public HudZones.@Nullable Size measure(Font font, HudZones.Frame f, int maxW) {
			long now = f.nowMs();
			ACTIVE.removeIf(t -> t.over(now));
			if (Minecraft.getInstance().player == null || ACTIVE.isEmpty()) {
				return null;
			}
			List<ToastStack.Item> items = new ArrayList<>();
			for (int i : ToastStack.order(needs(ACTIVE))) {
				items.add(new ToastStack.Item(ACTIVE.get(i).need(), height(font, ACTIVE.get(i))));
			}
			ToastStack.Layout l = ToastStack.layout(items, 0, Integer.MAX_VALUE / 2, 4, ToastStack.MAX_SHOWN, MORE_H);
			int h = 0;
			for (int k = 0; k < l.shown().size(); k++) {
				h = Math.max(h, l.ys().get(k) + items.get(l.shown().get(k)).height());
			}
			if (l.moreY() >= 0) {
				h = Math.max(h, l.moreY() + MORE_H);
			}
			return h <= 0 ? null : new HudZones.Size(W, h, 0, true);
		}

		@Override
		public void draw(GuiGraphicsExtractor g, Font font, HudZones.Frame f, HudZones.Slot slot) {
			drawStack(g, font, slot.y(), slot.y() + slot.h(), f.nowMs());
		}
	}

	private static void drawStack(GuiGraphicsExtractor g, Font font, int top, int limit, long now) {
		List<Toast> order = new ArrayList<>();
		List<ToastStack.Item> items = new ArrayList<>();
		for (int i : ToastStack.order(needs(ACTIVE))) {
			Toast t = ACTIVE.get(i);
			order.add(t);
			items.add(new ToastStack.Item(t.need(), height(font, t)));
		}
		ToastStack.Layout l = ToastStack.layout(items, top, limit, 4, ToastStack.MAX_SHOWN, MORE_H);
		lastLayout = l;
		lastOrder = order;
		for (int k = 0; k < l.shown().size(); k++) {
			Toast t = order.get(l.shown().get(k));
			if (t.start < 0) {
				t.start = now;
			}
			draw(g, font, t, now, l.ys().get(k));
		}
		if (l.moreY() >= 0) {
			String text = ToastStack.moreText(l.hidden(), l.hiddenNeed());
			int w = Math.min(W, font.width(text) + 12);
			int x = g.guiWidth() - 6 - w;
			Panels.sprite(g, Kit.PANEL_PAPER, x, l.moreY(), w, MORE_H, 0xFFFFFFFF);
			g.text(font, TextUtil.ellipsize(font, text, w - 12), x + 6, l.moreY() + 3, l.hiddenNeed() > 0 ? UiStyle.CLAY_DARK : UiBits.muted(), false);
		}
	}

	/** {@link ToastStack.Measure} over a font. */
	private static ToastStack.Measure measure(Font font) {
		return new ToastStack.Measure() {
			@Override
			public int width(String s) {
				return font.width(s);
			}

			@Override
			public String ellipsize(String s, int maxWidth) {
				return TextUtil.ellipsize(font, s, maxWidth);
			}
		};
	}

	private static List<FormattedCharSequence> lines(Font font, Toast t) {
		Kit.Padding p = Kit.padding("panel_paper");
		int textW = W - (p.left() + 20) - p.right();
		List<FormattedCharSequence> lines = TextUtil.wrap(font, UiBits.oneLine(t.body), textW);
		if (lines.size() > t.maxBodyLines) {
			// the first lines as wrapped, the last one cut with " …" (ToastStack.clipBody)
			List<String> clipped = ToastStack.clipBody(TextUtil.wrapPlain(font, UiBits.oneLine(t.body), textW), t.maxBodyLines, textW, measure(font));
			List<FormattedCharSequence> out = new ArrayList<>(lines.subList(0, t.maxBodyLines - 1));
			out.add(net.minecraft.network.chat.Component.literal(clipped.get(clipped.size() - 1)).getVisualOrderText());
			lines = out;
		}
		return lines;
	}

	private static int height(Font font, Toast t) {
		Kit.Padding p = Kit.padding("panel_paper");
		return Math.max(p.top() + 20 + p.bottom() - 2, p.top() + 10 + lines(font, t).size() * 10 + (t.hintKey != null ? 12 : 0) + p.bottom() - 2);
	}

	/** QA: the stack as last drawn. */
	public static com.google.gson.JsonObject json() {
		com.google.gson.JsonObject o = new com.google.gson.JsonObject();
		com.google.gson.JsonArray shownArr = new com.google.gson.JsonArray();
		ToastStack.Layout l = lastLayout;
		for (int k = 0; k < l.shown().size() && l.shown().get(k) < lastOrder.size(); k++) {
			Toast t = lastOrder.get(l.shown().get(k));
			com.google.gson.JsonObject j = new com.google.gson.JsonObject();
			j.addProperty("level", t.level.name().toLowerCase(Locale.ROOT));
			j.addProperty("title", t.title);
			j.addProperty("body", t.body);
			shownArr.add(j);
		}
		o.add("shown", shownArr);
		o.addProperty("queued", ACTIVE.size());
		o.addProperty("last", lastText());
		return o;
	}

	private static void draw(GuiGraphicsExtractor g, Font font, Toast t, long now, int y) {
		Kit.Padding p = Kit.padding("panel_paper");
		boolean need = t.need();
		int textX = p.left() + 20;
		int textW = W - textX - p.right();
		List<FormattedCharSequence> lines = lines(font, t);
		int h = height(font, t);
		long age = now - t.start;
		float slide = Math.min(1f, age / (float) SLIDE_MS);
		slide = 1f - (1f - slide) * (1f - slide);
		long left = t.life - age;
		float fade = left < FADE_MS ? Math.max(0f, left / (float) FADE_MS) : 1f;
		int x = g.guiWidth() - 6 - W + (int) ((1f - slide) * (W + 10));
		int a = (int) (255 * fade);
		if (a < 8) {
			return;
		}
		int tint = (a << 24) | 0xFFFFFF;
		Panels.sprite(g, Kit.PANEL_PAPER, x, y, W, h, tint);
		if (need) {
			g.fill(x + 3, y + 4, x + 5, y + h - 6, UiStyle.withAlpha(UiStyle.CLAY, a));
		}
		int px = x + p.left();
		int py = y + p.top() - 1;
		Panels.sprite(g, Kit.icon(t.icon), px + 2, py + 1, 12, 12, tint);
		int tx = x + textX;
		g.text(font, ToastStack.fitTitle(t.title, t.subject, textW, measure(font)), tx, py + 1, UiStyle.withAlpha(need ? UiStyle.CLAY_DARK
			: t.level == Level.WARN ? UiStyle.error() : UiBits.ink(), a), false);
		int ly = py + 12;
		for (FormattedCharSequence line : lines) {
			g.text(font, line, tx, ly, UiStyle.withAlpha(UiBits.ink(), a), false);
			ly += 10;
		}
		if (t.hintKey != null && a > 200) {
			int hw = UiBits.hintsWidth(font, t.hintKey, t.hintVerb);
			UiBits.hints(g, font, x + W - p.right() - hw, ly, false, t.hintKey, t.hintVerb);
		}
	}
}
