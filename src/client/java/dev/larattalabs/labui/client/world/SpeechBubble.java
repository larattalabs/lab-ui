package dev.larattalabs.labui.client.world;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.larattalabs.labui.client.ui.Kit;
import dev.larattalabs.labui.client.ui.TextUtil;
import dev.larattalabs.labui.client.ui.UiStyle;
import dev.larattalabs.labui.client.ui.WorldUi;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/**
 * The speech bubble of one speaker (in AgentCraft: an agent's {@code agent.say}): the kit {@code bubble} nine-slice with its
 * tail pointing down at the nameplate, ink text wrapped to at most three lines (then an ellipsis),
 * and the addressee first ("@Juniper", "@Alex") in that speaker's paper colour, so you can tell who
 * talks to whom. It pops in, stays for a time that grows with the text (3.5-11 s), then fades out.
 * Drawn in plate space right above the plate; the caller reserves {@link #stackHeight} above the
 * plate (AgentCraft's PlateLayout), so the plates around it move out of its way instead of being covered.
 *
 * <p>Who an addressee is ({@code to}: an id, "user", "all" or null) comes from {@link Addressees}: the
 * mod that owns the speakers maps ids to display names and name colours. "user" is always the local
 * player's name and "all"/null no prefix.
 */
public final class SpeechBubble {
	/** Max text width inside the bubble (GUI px). */
	public static final int MAX_TEXT = 148;
	public static final int MAX_LINES = 3;
	/** Gap between the plate top and the tail tip (px). */
	public static final int GAP = 1;
	public static final int TAIL_H = 5;
	private static final int IN_TICKS = 3;
	private static final int OUT_TICKS = 8;

	/** What is being said. {@code to}: agent id, "user", "all" or null. */
	public record Line(String text, @Nullable String to, long ts) {
	}

	/** Laid-out bubble (px): {@code width}/{@code height} of the body, without the tail. */
	public record Layout(List<FormattedCharSequence> lines, int[] lineWidths, int width, int height) {
	}

	/** Display names and "@name" colours of addressee ids (never called for "user", "all", null or ""). */
	public interface Addressees {
		/** No names known: the id itself, in {@code paper.link}. */
		Addressees NONE = new Addressees() {
			@Override
			public @Nullable String name(String id) {
				return null;
			}

			@Override
			public @Nullable Integer color(String id) {
				return null;
			}
		};

		/** The display name of {@code id}, or null to show the id itself. */
		@Nullable String name(String id);

		/** The ARGB colour of the "@name" prefix for {@code id}, or null for {@code paper.link}. */
		@Nullable Integer color(String id);
	}

	private final Addressees addressees;
	private @Nullable Line line;
	private int start;
	private int end;
	private @Nullable Layout layout;

	public SpeechBubble() {
		this(Addressees.NONE);
	}

	public SpeechBubble(Addressees addressees) {
		this.addressees = addressees;
	}

	/** Show {@code text} said to {@code to} at {@code ts} (epoch ms); {@code age} is the speaker's tick age. */
	public void show(String rawText, @Nullable String to, long ts, int age) {
		String text = rawText.replaceAll("\\s+", " ").trim();
		if (text.isEmpty()) {
			return;
		}
		boolean showing = visible();
		line = new Line(text, to, ts);
		layout = null;
		start = showing ? age - IN_TICKS : age;
		end = age + Math.max(70, Math.min(220, 50 + (int) (text.length() * 1.15)));
	}

	/** Show a say that arrived before this speaker existed in the world, with the time already gone by. */
	public void showLate(String text, @Nullable String to, long ts, int age, int ticksAgo) {
		show(text, to, ts, age);
		start -= ticksAgo;
		end -= ticksAgo;
		if (end <= age) {
			line = null;
		}
	}

	public void tick(int age) {
		if (line != null && age > end + OUT_TICKS) {
			line = null;
			layout = null;
		}
	}

	public boolean visible() {
		return line != null;
	}

	public @Nullable Line current() {
		return line;
	}

	public void clear() {
		line = null;
		layout = null;
	}

	/** 0 (hidden) .. 1 (fully shown), partial-tick accurate. */
	public float visibility(int age, float pt) {
		if (line == null) {
			return 0f;
		}
		float t = age + pt;
		if (t < start + IN_TICKS) {
			return Math.max(0f, (t - start) / IN_TICKS);
		}
		if (t > end) {
			return Math.max(0f, 1f - (t - end) / OUT_TICKS);
		}
		return 1f;
	}

	/** The bubble's layout (render thread), cached until the text changes. */
	public @Nullable Layout layout() {
		Line l = line;
		if (l == null) {
			return null;
		}
		if (layout == null) {
			layout = build(Minecraft.getInstance().font, l, addressees);
		}
		return layout;
	}

	private static Layout build(Font font, Line l, Addressees addressees) {
		String prefix = prefix(l.to(), addressees);
		String all = prefix.isEmpty() ? l.text() : prefix + " " + l.text();
		List<String> plain = TextUtil.wrapPlain(font, all, MAX_TEXT);
		if (plain.size() > MAX_LINES) {
			StringBuilder rest = new StringBuilder(plain.get(MAX_LINES - 1));
			for (int i = MAX_LINES; i < plain.size(); i++) {
				rest.append(' ').append(plain.get(i));
			}
			List<String> cut = new ArrayList<>(plain.subList(0, MAX_LINES - 1));
			cut.add(TextUtil.ellipsize(font, rest.toString(), MAX_TEXT));
			plain = cut;
		}
		int ink = UiStyle.color("paper.text", UiStyle.INK);
		int toColor = toColor(l.to(), addressees);
		List<FormattedCharSequence> lines = new ArrayList<>(plain.size());
		int[] widths = new int[plain.size()];
		int w = 0;
		for (int i = 0; i < plain.size(); i++) {
			String s = plain.get(i);
			MutableComponent c;
			if (i == 0 && !prefix.isEmpty() && s.startsWith(prefix)) {
				c = Component.literal(prefix).withColor(toColor & 0xFFFFFF).append(Component.literal(s.substring(prefix.length())).withColor(ink & 0xFFFFFF));
			} else {
				c = Component.literal(s).withColor(ink & 0xFFFFFF);
			}
			lines.add(c.getVisualOrderText());
			widths[i] = font.width(s);
			w = Math.max(w, widths[i]);
		}
		Kit.Padding pad = Kit.padding("bubble");
		int width = Math.max(24, w + pad.left() + pad.right());
		int height = pad.top() + 9 + 10 * (lines.size() - 1) + pad.bottom();
		return new Layout(lines, widths, width, height);
	}

	/** "@Juniper" / "@Alex" / "" (said to the room). */
	public static String prefix(@Nullable String to, Addressees addressees) {
		if (to == null || to.equals("all") || to.isEmpty()) {
			return "";
		}
		if (to.equals("user")) {
			var p = Minecraft.getInstance().player;
			return "@" + (p != null ? p.getGameProfile().name() : "you");
		}
		String name = addressees.name(to);
		return "@" + (name != null ? name : to);
	}

	private static int toColor(@Nullable String to, Addressees addressees) {
		if (to == null || to.equals("user") || to.equals("all") || to.isEmpty()) {
			return UiStyle.link();
		}
		Integer c = addressees.color(to);
		return c != null ? c : UiStyle.link();
	}

	/**
	 * The text sits this far (pixel units, towards the camera) in front of the bubble's paper: on the paper's own plane
	 * the polygon offset alone lost glyphs to the paper at stand-up distances (whole letters missing in screenshots).
	 */
	public static final float TEXT_LIFT = 0.5f;

	/** Total stacked height above the plate (body + tail + gap), px. */
	public static int stackHeight(Layout l) {
		return l.height() + TAIL_H - 1 + GAP;
	}

	/**
	 * Draw the bubble in plate space with its tail tip {@code GAP} px above {@code plateTop}.
	 * {@code vis} 0..1 pops it in (scale) and fades it out (translucent while not fully shown).
	 */
	public static void submit(PoseStack ps, SubmitNodeCollector c, Layout l, float plateTop, float vis, int light) {
		Kit.Padding pad = Kit.padding("bubble");
		float tip = plateTop - GAP;
		float y1 = tip - TAIL_H + 1; // body bottom (the tail's first row overlaps the border)
		float y0 = y1 - l.height();
		float x0 = -l.width() / 2f;
		boolean solid = vis >= 0.999f;
		int alpha = solid ? 255 : Math.max(8, (int) (vis * 255));
		int tint = (alpha << 24) | 0xFFFFFF;
		ps.pushPose();
		if (!solid) {
			float sc = 0.86f + 0.14f * vis;
			ps.translate(0, tip, 0);
			ps.scale(sc, sc, 1f);
			ps.translate(0, -tip, 0);
		}
		WorldUi.Layer layer = solid ? WorldUi.Layer.SOLID : WorldUi.Layer.BASE;
		WorldUi.submitNineSlice(ps, c, layer, Kit.BUBBLE, x0, y0, l.width(), l.height(), tint, light);
		WorldUi.submitSprite(ps, c, layer, Kit.BUBBLE_TAIL, -4.5f, y1 - 1, 9, TAIL_H, solid ? 0f : 0.05f, tint, light);
		float ty = y0 + pad.top();
		int ink = UiStyle.withAlpha(UiStyle.color("paper.text", UiStyle.INK), alpha);
		int inner = l.width() - pad.left() - pad.right();
		ps.pushPose();
		ps.translate(0, 0, TEXT_LIFT);
		for (int i = 0; i < l.lines().size(); i++) {
			float tx = x0 + pad.left() + (inner - l.lineWidths()[i]) / 2f;
			WorldUi.submitText(ps, c, l.lines().get(i), tx, ty + i * 10, ink, light);
		}
		ps.popPose();
		ps.popPose();
	}
}
