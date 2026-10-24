package dev.larattalabs.labui.client.ui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.larattalabs.labui.LabUi;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Colour and metric tokens of the Warm Studio UI, read from the synced art files
 * {@code assets/lab_ui/gui/ui-style.json} (monitor/paper/status/agents/ink_ui/metrics, see
 * AgentCraft Worlds' assets-src/ui-style.md) and {@code assets/lab_ui/palette.json} ({@code colors.*}, {@code ui.*}).
 * Never hard-code a colour in a screen or renderer: ask here.
 *
 * <pre>
 * UiStyle.color("paper.text")       // 0xFF1F1E1D (ARGB, opaque)
 * UiStyle.color("monitor.diff_add")
 * UiStyle.status("working")         // status palette by family: idle thinking working waiting error done
 * UiStyle.metric("metrics.gui_panel_padding", 8)
 * </pre>
 */
public final class UiStyle {
	public static final int CREAM = 0xFFF4EFE6;
	public static final int PAPER = 0xFFE9E1D3;
	public static final int CLAY = 0xFFD97757;
	public static final int CLAY_DARK = 0xFFB4553A;
	public static final int WALNUT = 0xFF3B2A20;
	public static final int BRASS = 0xFFC9A227;
	public static final int SAGE = 0xFF8FA98B;
	public static final int INK = 0xFF1F1E1D;
	public static final int TEAL = 0xFF2FA3A0;

	private static final Map<String, Integer> COLORS = new HashMap<>();
	private static final Map<String, Double> NUMBERS = new HashMap<>();

	static {
		load("/assets/lab_ui/gui/ui-style.json", "");
		load("/assets/lab_ui/palette.json", "palette.");
	}

	private UiStyle() {
	}

	/** Opaque ARGB colour for a token path, e.g. "paper.text", "palette.colors.clay", "palette.ui.panel". */
	public static int color(String path) {
		Integer c = COLORS.get(path);
		if (c == null) {
			LabUi.LOGGER.debug("UiStyle: unknown colour token {}", path);
			return 0xFFFF00FF;
		}
		return c;
	}

	public static int color(String path, int fallback) {
		Integer c = COLORS.get(path);
		return c == null ? fallback : c;
	}

	/** Status colour by family (idle, thinking, working, waiting, error, done). */
	public static int status(String family) {
		return color("status." + family, 0xFF9C9488);
	}

	// ------------------------------------------------------------------ semantic tokens (0.2.0; ui-style.json "semantic")

	/** Error text on paper (= {@code UiBits.errorText()}, 6.5:1 on cream). */
	public static int error() {
		return color("semantic.error", 0xFF9A2F2B);
	}

	/** Warning text on paper (5.4:1 on cream, 4.8:1 on the inset). */
	public static int warn() {
		return color("semantic.warn", 0xFF85570F);
	}

	/** Positive text on paper (= {@code UiBits.okText()}). */
	public static int ok() {
		return color("semantic.ok", 0xFF455746);
	}

	/** Secondary text on paper (= {@code UiBits.muted()}). */
	public static int mutedText() {
		return color("semantic.muted", 0xFF655E55);
	}

	/** Fill of a selected row, cell or chip (with {@link #selectedEdge()} as its border or left bar). */
	public static int selected() {
		return color("semantic.selected", 0xFFF7D9C9);
	}

	public static int selectedEdge() {
		return color("semantic.selected_edge", CLAY_DARK);
	}

	/** The keyboard focus ring (= {@code UiBits.focusRing}'s brass). */
	public static int focusRing() {
		return color("semantic.focus_ring", BRASS);
	}

	/** Fill of a hovered row or cell (with {@link #rowHoverEdge()} as its border). */
	public static int rowHover() {
		return color("semantic.row_hover", 0xFFFFFAF0);
	}

	public static int rowHoverEdge() {
		return color("semantic.row_hover_edge", 0xFFC5B398);
	}

	/** Hairline between areas of a panel. */
	public static int rule() {
		return color("semantic.rule", 0xFFD6C9B4);
	}

	/** Chip body and edge. */
	public static int chip() {
		return color("semantic.chip", PAPER);
	}

	public static int chipEdge() {
		return color("semantic.chip_edge", 0xFFA89378);
	}

	/** Search / text field body drawn by fills. */
	public static int field() {
		return color("semantic.field", 0xFFFFFDF8);
	}

	/** Full-screen title bar (walnut) and its text (cream). */
	public static int titleBar() {
		return color("semantic.title_bar", WALNUT);
	}

	public static int titleText() {
		return color("semantic.title_text", CREAM);
	}

	/** An armed destructive control's fill and its label (6.2:1). */
	public static int danger() {
		return color("semantic.danger", 0xFFA63A2D);
	}

	public static int dangerText() {
		return color("semantic.danger_text", 0xFFFFFBF4);
	}

	/** Section headings and links on paper (= {@code paper.link}). */
	public static int link() {
		return color("semantic.link", 0xFF874431);
	}

	// ------------------------------------------------------------------ on-dark tokens (0.3.0; ui-style.json "semantic_on_dark")

	/**
	 * Text and marks on dark surfaces: the HUD ink panel ({@code Kit.TOOLTIP}), nameplates, dark monitors
	 * ({@code ScreenStyle.dark}) and a walnut slate. The {@code semantic.*} tokens above are for paper and come out about
	 * 2:1 here. Every text token is at least 4.5:1 on each of those surfaces as drawn, also with the 90%-opaque HUD panel
	 * over a white world ({@code semantic_on_dark_contrast} in ui-style.json; checked by {@code OnDarkContrastTest}).
	 */
	public static final class OnDark {
		private OnDark() {
		}

		/** Body text (cream; 9.3:1 at worst). */
		public static int text() {
			return color("semantic_on_dark.text", CREAM);
		}

		/** Secondary text, key hints (= {@code ink_ui.activity}; 5.7:1). */
		public static int muted() {
			return color("semantic_on_dark.muted", 0xFFC4BDB2);
		}

		/** Errors, refusals (4.8:1). */
		public static int error() {
			return color("semantic_on_dark.error", 0xFFF2948B);
		}

		/** Cautions (amber, a different hue from {@link #error()}; 5.9:1). */
		public static int warn() {
			return color("semantic_on_dark.warn", 0xFFF2B66A);
		}

		/** Ready, done (6.2:1). */
		public static int ok() {
			return color("semantic_on_dark.ok", 0xFFB8CBB3);
		}

		/** Notes and links on dark (teal; 6.1:1). */
		public static int accent() {
			return color("semantic_on_dark.accent", 0xFF80D2CD);
		}

		/** A selected or armed choice's text and edge (brass light; 6.4:1). */
		public static int selected() {
			return color("semantic_on_dark.selected", 0xFFE6C659);
		}

		/** The fill behind a selected or armed choice: {@link #selected()} at alpha 0x30. */
		public static int selectedFill() {
			return withAlpha(selected(), 0x30);
		}

		/** The keyboard focus ring on dark (a mark, 4.4:1 at worst; = the paper {@link UiStyle#focusRing()}). */
		public static int focus() {
			return color("semantic_on_dark.focus", BRASS);
		}

		/** The HUD panel's body (the tooltip sprite's colour) and its rim. Surfaces, not text. */
		public static int panel() {
			return color("semantic_on_dark.panel", 0xFF2B2927);
		}

		public static int panelEdge() {
			return color("semantic_on_dark.panel_edge", 0xFF4A4642);
		}

		/** Chip edges and hairlines on dark (= {@code monitor_dark.rule}). */
		public static int rule() {
			return color("semantic_on_dark.rule", 0xFF4A423B);
		}

		/** A progress bar's fill (= {@code status.working}; a mark, 3.5:1 at worst) and its track. */
		public static int bar() {
			return color("semantic_on_dark.bar", TEAL);
		}

		public static int track() {
			return withAlpha(muted(), 0x60);
		}
	}

	public static int metric(String path, int fallback) {
		Double d = NUMBERS.get(path);
		return d == null ? fallback : (int) Math.round(d);
	}

	/** {@code argb} with its alpha replaced (0..255). */
	public static int withAlpha(int argb, int alpha) {
		return (Math.max(0, Math.min(255, alpha)) << 24) | (argb & 0xFFFFFF);
	}

	private static void load(String resource, String prefix) {
		try (InputStream in = UiStyle.class.getResourceAsStream(resource)) {
			if (in == null) {
				LabUi.LOGGER.warn("UiStyle: {} missing ", resource);
				return;
			}
			JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
			walk(root, prefix.isEmpty() ? "" : prefix.substring(0, prefix.length() - 1));
		} catch (Exception e) {
			LabUi.LOGGER.warn("UiStyle: could not read {}", resource, e);
		}
	}

	private static void walk(JsonElement el, String path) {
		if (el.isJsonObject()) {
			for (var e : el.getAsJsonObject().entrySet()) {
				walk(e.getValue(), path.isEmpty() ? e.getKey() : path + "." + e.getKey());
			}
		} else if (el.isJsonPrimitive()) {
			var p = el.getAsJsonPrimitive();
			if (p.isString()) {
				int c = parseColor(p.getAsString());
				if (c != -1 && p.getAsString().startsWith("#")) {
					COLORS.put(path, 0xFF000000 | c);
				}
			} else if (p.isNumber()) {
				NUMBERS.put(path, p.getAsDouble());
			}
		}
	}

	/** {@code #RRGGBB} -> 0xRRGGBB, or -1. */
	static int parseColor(String s) {
		if (s == null || !s.startsWith("#") || s.length() != 7) {
			return -1;
		}
		try {
			return Integer.parseInt(s.substring(1), 16);
		} catch (NumberFormatException e) {
			return -1;
		}
	}
}
