# lab-ui 0.3 draft: on-dark tokens and shared HUD zones

Status: draft for review (Steward, coordinator). Nothing here is built yet. Branch `ui-0.3`, worktree `../lab-ui-0.3` (the
`../lab-ui` checkout stays on main at `v0.2.0`, so Architect's and Steward's tag-matching composite rule still picks it up).

0.3 is additive. No 0.2 signature changes. A mod built against 0.2 runs unchanged on a 0.3 jar. Its `depends` range
(`<0.3.0`) still has to move before both mods can ship 0.3 together.

## 1. On-dark (in-world) colour tokens

### Why

The `semantic.*` tokens (0.2.0) are for text on cream or paper. On the dark HUD panel they reach about 2:1
(`semantic.error` #9A2F2B is 1.94:1 on #2B2927). Architect added `HudInk` to pick the `monitor_dark.*` / `ink_ui.*` colours
itself. 0.3 gives that set names in lab-ui, and HudInk becomes a thin alias.

### The surfaces measured

These are the real backgrounds, taken from the sprites and the code. The draw tint and the sprite alpha are both counted.

| Surface | Where it's drawn | Colour used for the check |
|---|---|---|
| HUD panel (`Kit.TOOLTIP`), opaque | Architect's PlacementHud, PlotHud, SiteHud, RegionGhost HUD, MassingReview, HelperProgressHud | sprite body #2B2927 |
| HUD panel over a white world | same. Sprite alpha 0.957 × tint 0xF0 = 0.90 effective, so snow or quartz shows through | #403E3C (worst case) |
| HUD panel over the sky | same, over #78A7FF | #33363C |
| Nameplate (`Kit.NAMEPLATE`) over a white world | StewardPlate, `WorldUi` plates; sprite #1F1E1D at alpha 0.86 | #3E3D3C (worst case) |
| Dark monitor (`ScreenStyle.dark`) | `DisplayDraw` face displays | `monitor_dark.bg` #26221F, `bg_top` / `header_bg` #2E2925 |
| Settlement board slate | Steward's BoardRenderer (`UiStyle.WALNUT`) | #3B2A20 |

All in-world UI is full-bright (`WorldUi.uiLight()` = `FULL_BRIGHT`), so the world's light level never darkens it.

The speech bubble (`Kit.BUBBLE`, #FFFBF4) and the paper monitor (#EDE5D7, scanline #E7DECE) are light. They keep the paper
tokens, which pass there:

| Paper token on a light surface | bubble #FFFBF4 | monitor.bg | scanline |
|---|---|---|---|
| paper.text #1F1E1D | 16.1 | 13.3 | 12.5 |
| semantic.error #9A2F2B | 7.2 | 6.0 | 5.6 |
| semantic.warn #85570F | 6.1 | 5.0 | 4.7 |
| semantic.ok #455746 | 7.5 | 6.2 | 5.8 |
| semantic.muted #655E55 | 6.2 | 5.1 | 4.8 |
| paper.link #874431 (the bubble's @name) | 7.0 | 5.8 | 5.4 |

### The token set (`ui-style.json` → `semantic_on_dark`; `UiStyle.OnDark.*`)

Contrast is WCAG 2.x, with each surface composited as in the table above. Text tokens need at least 4.5:1 everywhere.
Non-text tokens (focus ring, bar) need at least 3:1.

| Token | Hex | HUD panel | panel over white | panel over sky | nameplate over white | monitor_dark.bg | bg_top | board slate | min | Today in HudInk |
|---|---|---|---|---|---|---|---|---|---|---|
| `text` | #F4EFE6 | 12.65 | 9.30 | 10.58 | 9.46 | 13.78 | 12.56 | 11.92 | **9.30** | `text()` = CREAM (same) |
| `muted` | #C4BDB2 | 7.78 | 5.71 | 6.50 | 5.82 | 8.47 | 7.72 | 7.33 | **5.71** | `soft()` = ink_ui.activity (same) |
| `error` | #F2948B | 6.48 | 4.76 | 5.41 | 4.84 | 7.05 | 6.43 | 6.10 | **4.76** | `error()` #EA847B: **4.10** over white, fails |
| `warn` | #F2B66A | 8.06 | 5.92 | 6.73 | 6.03 | 8.77 | 8.00 | 7.59 | **5.92** | `warn()` #F4A585 (5.36). Passes, but its hue is next to error's |
| `ok` | #B8CBB3 | 8.44 | 6.20 | 7.06 | 6.31 | 9.19 | 8.38 | 7.95 | **6.20** | `ok()` (same) |
| `accent` | #80D2CD | 8.29 | 6.09 | 6.93 | 6.20 | 9.03 | 8.23 | 7.81 | **6.09** | `note()` (same) |
| `selected` | #E6C659 | 8.69 | 6.38 | 7.26 | 6.50 | 9.46 | 8.62 | 8.18 | **6.38** | `armed()` (same) |
| `focus` (ring, non-text) | #C9A227 | 5.99 | 4.40 | 5.01 | 4.48 | 6.52 | 5.95 | 5.64 | **4.40** (needs 3) | brass, the paper `focusRing()` too |
| `panel` (surface) | #2B2927 | n/a | | | | | | | | the tooltip sprite's body |
| `panel_edge` (surface) | #4A4642 | n/a | | | | | | | | the tooltip sprite's rim |

Three more that HudInk needs. They are surfaces or marks, not text:

| Token | Hex | Use | Contrast |
|---|---|---|---|
| `rule` | #4A423B | chip edges and hairlines on ink (= `monitor_dark.rule`, HudInk `chipEdge()`) | decorative |
| `bar` | #2FA3A0 | progress fill (= `status.working`, HudInk `bar()`) | 4.74 opaque, 3.48 over white (≥ 3) |
| `track` | `muted` at alpha 0x60 | progress track (HudInk `track()`) | decorative |

`selected_fill` is `withAlpha(selected, 0x30)`, the fill PlacementHud's "on" chips already use. It is a method, not a JSON
entry.

**Two value changes, both for review** (the rest are today's HudInk values):
- **error #EA847B → #F2948B.** The old value drops to 4.10:1 when the panel is over snow or quartz (the panel is 90%
  opaque). The new one is a touch lighter, in the same hue.
- **warn #F4A585 → #F2B66A (amber).** The old warn passes, but next to the new error the two are both salmon. Amber sits
  between error (rose) and selected (yellow). The HUD's glyphs (✖ ▲ !) stay the second cue. If Architect wants zero visible
  change in UI-2b's HUD, keep #F4A585 (min 5.36). Then error and warn differ by lightness only.

### API

```java
public final class UiStyle {
	// ... 0.2 unchanged ...
	/** Text and marks on dark surfaces: HUD ink panels, nameplates, dark monitors, the board slate (ui-style.json semantic_on_dark). */
	public static final class OnDark {
		public static int text();      public static int muted();    public static int error();   public static int warn();
		public static int ok();        public static int accent();   public static int selected(); public static int selectedFill();
		public static int focus();     public static int panel();    public static int panelEdge(); public static int rule();
		public static int bar();       public static int track();
	}
}
```

`ui-style.json` gains `semantic_on_dark` and a `semantic_on_dark_contrast` block with the "min" column (like `paper_contrast`).
A unit test recomputes every ratio from the JSON against the surface list. It fails if a text token drops under 4.5 or a
mark under 3. That needs a small pure `Contrast` helper in `dev.larattalabs.labui.ui`.

HudInk after 0.3, in Architect's own slice: `text→OnDark.text`, `soft→muted`, `error`, `warn`, `ok`, `note→accent`,
`armed→selected`, `bar`, `track`, `chipEdge→rule`.

### Colour-literal audit

**lab-ui itself** is almost clean already:
- **`WorldUi`.** No colour literals: only the Javadoc example and the 0xFFFFFFFF sprite tints, which aren't colours.
- **`DisplayDraw`.** No colour literals: only the white dynamic texture and the alpha and mix maths.
- **`ScreenStyle`.** Every colour is a token. The literal fallbacks equal the JSON values. Keep them.
- **`SpeechBubble`.** Reads `paper.text` and `paper.link`. Route `paper.link` → `UiStyle.link()` (same #874431). Behaviour-neutral.
- **Also in lab-ui, also behaviour-neutral:**
  - `UiBits.focusRing` uses `UiStyle.BRASS`; switch it to `UiStyle.focusRing()`.
  - `UiBits.hints(onDark=true)` uses `activityOnInk()`; switch it to `OnDark.muted()`.
  - `Toasts` draws its WARN title in `UiBits.errorText()`; switch it to `UiStyle.error()`.
  - All three keep the same values.

**Consumer literals** (their own slices; listed here so the token set covers them):

| Consumer | Literal today | Contrast (worst surface) | Token |
|---|---|---|---|
| Architect RegionGhost HUD | `UiStyle.CREAM` head | 9.30 | `text` |
| | `0xFFF0A060` "NOT ok" | 5.02 | `error` (it's a refusal) |
| | `UiStyle.SAGE` verdict | 4.17, **fails** | `ok` |
| Architect MassingReview HUD | `CREAM` | 9.30 | `text` |
| | `UiStyle.SAGE` hint lines | 4.17, **fails** | `ok` (or `accent`) |
| | `0xFFF07060` status error | 3.65, **fails** | `error` |
| | `activityOnInk` | 5.71 | `muted` |
| Architect PlacementHud, PlotHud, SiteHud, HelperProgressHud, ProgressStrip | `HudInk.*` | see above | aliases |
| Steward StewardPlate | name in `UiStyle.BRASS` | 6.88 on the opaque plate, **4.48** over white | `selected` (6.50), or keep brass and accept 4.48 |
| | activity in `ink_ui.activity` | 5.82 | `muted` |
| | the "!" (clay with an ink outline, a mark) | fine | unchanged |
| Steward BoardRenderer | slate `WALNUT`, band `CREAM`, title `INK` on the band (14.5) | | unchanged surfaces |
| | rows `CREAM` | 11.92 | `text` |
| | status `BRASS` | 5.64 | `selected` |
| | the rest `ink_ui.activity` | 7.33 | `muted` |
| Steward StewardHud | ink text on the paper pill (#FFFBF4) | 16.1 | paper tokens, unchanged |

## 2. HudZones

### What overlaps today (with both mods installed)

Read from the code, at GUI coordinates:

1. **Top left.** Architect's HelperProgressHud (x 4, y 4, up to 320 wide, first run) sits under Steward's inbox line (x 6, y 6).
   Vanilla's "Now playing" music toast is also top left (`NowPlayingToast.xPos` = `width*portion - width`, 30 px tall).
2. **Top centre.**
   - SiteHud (y 4) and the RegionGhost HUD (y 6) draw over each other.
   - Both sit under vanilla boss bars: y 12, 19 px per bar, up to `guiHeight/3`. Raids show a boss bar, in settlements too.
3. **Top right.**
   - Steward still draws its own toast stack (`steward:hud/toasts`, x `guiWidth-6-196`, y 8) in the same column as lab-ui's shared
     `Toasts`. The two stacks overlap each other.
   - Both overlap vanilla's recipe and advancement toasts (`ToastManager`: right-aligned, 160 wide, 32 px slots from y 0, drawn after
     the HUD, so on top).
   - Both overlap status-effect icons: 24 px icons, 25 px apart from the right edge. The beneficial row is at y 1, the harmful row
     26 lower, and demo mode adds 15.
4. **Bottom centre.** PlacementHud, PlotHud and MassingReview all use `guiHeight-64-h`. Ad-hoc rules keep them apart today
   (MassingReview hides while placing; PlotHud hides its stale status under the placement panel). Those stay. The zone only
   guarantees that two shown at once stack instead of overlapping.
5. **Toasts against the placement panel.** `Toasts.avoid(BuildPlacement::hudRect)` keeps the toasts clear of it today. That
   becomes automatic once the panel is a zone element.

### Model

**Retained registration, immediate drawing.** A mod registers an element once. Then every frame lab-ui's one HUD element
(`lab_ui:hud/zones`, installed by `HudZones.install()` or `Toasts.install()`) does three things:
1. asks each element to *measure* itself (its size this frame, or null for hidden);
2. lays out every zone with pure, unit-tested code;
3. calls each element's *draw* with its rect.

Nothing is kept between frames except the last rects (for QA and for `rect(id)`). This is the `Toasts.install()` pattern of
0.2 made general. Two phases are needed because stacking by priority needs every height before the first element is drawn.

```java
package dev.larattalabs.labui.client.hud;

public final class HudZones {
	/** Append-only. Bottom-left (chat) and bottom-right (subtitles) are vanilla's and not zones. */
	public enum Zone { TOP_LEFT, TOP_CENTER, TOP_RIGHT_TOASTS, LEFT, RIGHT, BOTTOM_CENTER }

	/** What every element can ask this frame (also static: HudZones.hudHidden(), screenOpen()). */
	public record Frame(int guiWidth, int guiHeight, boolean hudHidden, boolean screenOpen, boolean survivalBars, long nowMs) {}

	/** Wanted size. The layout gives h, or anything down to minH when space is short; optional ones may be hidden. */
	public record Size(int w, int h, int minH, boolean optional) {
		public static Size of(int w, int h);          // fixed: minH = h, not optional
		public Size shrinkTo(int minH);                // "may collapse to minH"
		public Size optional();                        // "may be hidden when even minH doesn't fit"
	}

	/** The rect an element draws in. collapsed = h < its wanted h (draw the short form). */
	public record Slot(int x, int y, int w, int h, boolean collapsed) {}

	public interface Element {
		/** Size this frame or null (not shown). maxW: the room at this element's place (ellipsize to it). Client thread. */
		@Nullable Size measure(Font font, Frame f, int maxW);
		void draw(GuiGraphicsExtractor g, Font font, Frame f, Slot slot);
	}

	public static void install();                                           // idempotent; Toasts.install() calls it
	public static Handle add(Identifier id, Zone zone, Element e);          // id: <modid>:<name>; returns a handle to tune it
	public static void remove(Identifier id);
	public interface Handle {
		Handle priority(int p);           // higher = nearer the zone's anchor edge, shrinks and hides last (default 50)
		Handle whileScreenOpen(boolean b); // default false: hidden while any screen is open
		Handle whenHudHidden(boolean b);   // default false: hidden with F1
	}
	/** A rect (x, y, w, h, or null) the zone must end above, as Toasts.avoid; for HUD pieces not (yet) zone elements. */
	public static void avoid(Zone zone, Supplier<int @Nullable []> rect);

	public static boolean hudHidden();  public static boolean screenOpen();
	public static @Nullable Slot rect(Identifier id);                       // last frame's slot, or null when not drawn
	public static JsonObject json();                                        // QA: every zone, its bounds and its slots
}
```

Each draw runs inside `Guard` with kind `<id>` (prefixed by the mod id, as now). A throwing element is logged once and skipped.
It does not take the other zones down.

### Zone geometry (GUI px; `bars` = survival hearts and food)

| Zone | Anchor and direction | Extent |
|---|---|---|
| `BOTTOM_CENTER` | centred; stacks **up** from `guiHeight-64` (above the action bar and the item name) | top limit `guiHeight/2+8` (crosshair clear). On a short screen the floor drops to `guiHeight-(bars?50:26)`, as PlacementHud does now |
| `TOP_CENTER` | centred; stacks **down** from `max(4, bossBarsBottom+2)` | bottom limit `guiHeight/2-12` |
| `TOP_LEFT` | x 4; stacks down from `max(4, nowPlayingBottom+2)` | `maxW` runs to the nearer of the left edge of any TOP_CENTER slot at that height or the toast column, minus 6. If that is narrower than the element can use, it moves below the TOP_CENTER slots |
| `TOP_RIGHT_TOASTS` | right margin 6, column 196 wide; stacks down from `max(8, effectsBottom+2, vanillaToastsBottom+4)` | bottom limit 4 px above the highest of: `avoid` rects, slots already placed in other zones that cross the column, and `guiHeight-(bars?50:26)` (today's fallback) |
| `LEFT` / `RIGHT` | x 4 / right margin 6; the stack is centred on `guiHeight/2` | between the TOP_* and BOTTOM_* slots on that side. For later panels (Jobs and spend); nothing maps there now |

**Layout order:** BOTTOM_CENTER, TOP_CENTER, TOP_LEFT, TOP_RIGHT_TOASTS, LEFT, RIGHT. A slot placed earlier is an obstacle for
the zones after it. The placement panel comes first because it is aimed and must never move for a toast.

**Within a zone** (pure `ZoneStack`, unit-tested):
1. Order by priority, high first, then registration order. A gap of 4 between elements.
2. When the stack is taller than the zone, shrink the lowest-priority shrinkable element to its `minH`, then the next one up.
3. Then hide the lowest-priority `optional` element, then the next.
4. Whatever still doesn't fit is placed anyway, clipped at the zone edge, and flagged `overflow` in `json()`. A fixed panel is
   never hidden silently.

A shrinkable element gets any height in `[minH, h]`. PlacementHud already does this with HudFit (drop rows until the budget
fits). A two-state element (SiteHud full or one line) sets `minH` to its one-line height and draws the line when
`slot.collapsed()`.

### Reading vanilla's own HUD (lab-ui's `VanillaHud`, client; the maths pure in `VanillaHudMath`)

- **Vanilla toasts.** Minecraft 26.3 has no public accessor for them, which I checked with javap on 26.3:
  - `ToastManager` (`mc.toastManager()`) keeps `visibleToasts` (a `List<ToastInstance<?>>`), `occupiedSlots` and `nowPlayingToast`,
    all private.
  - `ToastInstance` is package-private, with `firstSlotIndex`, `visiblePortion` and a public `getToast()`.
  - `Toast.yPos(slot) = slot*height()`, `height()` defaults to `SLOT_HEIGHT` 32, and `width()` defaults to 160.

  The visible bottom is `max(toast.yPos(firstSlotIndex) + toast.height())` over instances with `visiblePortion > 0`.
  lab-ui gets its first mixin config, `lab_ui.client.mixins.json`, which holds only accessors:
  - `ToastManagerAccess` reads `visibleToasts` and `nowPlayingToast`;
  - `ToastInstanceAccess`, via `@Mixin(targets = "...ToastManager$ToastInstance")`, reads `firstSlotIndex` and `visiblePortion`.

  The config is `"required": false` and every read is caught. If a later Minecraft renames a field, the zone just starts at
  y 8 as today, logged once. A broken accessor can't crash startup for every mod that bundles lab-ui. Vanilla skips its toasts
  under F1, and so does the measure.
- **Now playing (top left).** `nowPlayingToast` is drawn when `options.musicToast().get().renderToast()`, and no screen is open
  or the screen is the pause screen. It is 30 tall and its width varies.
- **Boss bars.** `mc.gui.getBossOverlay()` is public, but its `events` map is private, so a third accessor reads its size.
  The bottom is `12 + 19*n`, capped where vanilla stops drawing (`guiHeight/3`).
- **Effect icons.** No accessor needed: `player.getActiveEffects()`, filtered on `showIcon()`, split by `isBeneficial()`. The
  beneficial row is at y 1, the others 26 lower, and demo mode adds 15 (the constants are from 26.3's `Hud.extractEffects`
  bytecode). They are 24 tall and `25*n` wide from the right edge. The toast zone starts below them only when the column
  crosses them, which is always the case with 196 px.

I'll confirm each value in the demo screenshots (§4) before release.

### Toasts as a special case (compatible)

- `Toasts.install()` installs HudZones and adds `lab_ui:toasts` to `TOP_RIGHT_TOASTS`, at priority 0. Its `measure` returns
  the whole stack's height, `shrinkTo(0)`, `optional()`. Its `draw` runs the unchanged `ToastStack.layout(items, slot.y,
  slot.y+slot.h, …)`, so the toasts still give way first and fold into "+N more".
- **`Toasts.avoid(rect)`** becomes `HudZones.avoid(TOP_RIGHT_TOASTS, rect)`, with the same rule: the stack ends 4 px above
  the highest avoided top. Architect's existing call keeps working, and becomes redundant once its placement panel is a
  zone element.
- **The HUD element id.** `lab_ui:hud/toasts` stops being registered. `lab_ui:hud/zones` draws the toasts now, so the guard kind
  becomes `lab_ui.hud.zones`, with `lab_ui:toasts` inside it. No consumer references the old id; I checked Architect, Steward
  and AgentCraft (which still has its own toasts).
- `push`, `lastText()`, `shown()` and `json()` are unchanged.
- **For Steward's toasts.** The column is already identical: 196 wide, a 6 px margin, sliding in from the right, hidden under
  screens. The one difference is that Steward allows **3** body lines and lab-ui allows 2. The proposal is an additive overload,
  `Toasts.push(level, title, body, hintKey, hintVerb, maxBodyLines)` (1-3; the existing overloads keep 2). That's
  behaviour-neutral for Architect, and Steward passes 3.

### Usage: Architect (≈10 lines)

```java
// ArchitectClient.onInitializeClient: these replace five HudElementRegistry.addLast calls
Toasts.install();                                            // also installs HudZones
HudZones.add(Architect.id("placement"), Zone.BOTTOM_CENTER, new PlacementHud()).priority(100).whileScreenOpen(true);
HudZones.add(Architect.id("plot_marker"), Zone.BOTTOM_CENTER, new PlotHud()).priority(90);
HudZones.add(Architect.id("massing_review"), Zone.BOTTOM_CENTER, new MassingReview.Hud()).priority(80);
HudZones.add(Architect.id("region_ghost"), Zone.TOP_CENTER, new RegionGhost.Hud()).priority(70);
HudZones.add(Architect.id("construction"), Zone.TOP_CENTER, new SiteHud()).priority(60);
HudZones.add(Architect.id("helper_progress"), Zone.TOP_LEFT, new HelperProgressHud()).priority(40);

// SiteHud as an element: full (line + bar) or collapsed to its line when the zone is short
public @Nullable Size measure(Font font, Frame f, int maxW) {
	Ghost gh = shown(mc); if (gh == null || mode() == HIDDEN) return null;
	text = TextUtil.ellipsize(font, mode() == FULL ? line(gh) : shortLine(gh), Math.min(360, maxW) - PAD);
	return Size.of(font.width(text) + PAD, mode() == FULL ? LINE_H + 6 : LINE_H).shrinkTo(LINE_H);
}
public void draw(GuiGraphicsExtractor g, Font font, Frame f, Slot s) { /* panel at s.x(), s.y(); bar only if FULL && !s.collapsed() */ }
```

PlacementHud keeps HudFit. `measure` builds the rows once and returns `Size.of(w, all rows).shrinkTo(KEEP rows)`. `draw`
runs `HudFit.keep(…, budget = slot.h)` on the cached rows. `lastRect` is set from the slot, so DevBridge `hudRect` is unchanged.

### Usage: Steward (≈10 lines)

```java
// StewardClient.onInitializeClient: drop the own toast HUD element; StewardHud becomes a zone element
Toasts.install();
HudZones.add(Steward.id("inbox"), Zone.TOP_LEFT, new StewardHud()).priority(60);   // above Architect's helper strip (40)

// StewardHud: one row, ~12 px, ellipsized to the zone's right edge (no more 60% / columnWidth guess)
public @Nullable Size measure(Font font, Frame f, int maxW) {
	Entry first = firstWaiting(); if (first == null) return null;           // F1 and open screens: the zone already hides it
	String tail = " waits for you" + (more > 0 ? " (+" + more + ")" : "");
	int fixed = UiBits.dotPillWidth(font, "Steward: " + tail) + 4 + UiBits.keycapWidth(font, key);
	text = "Steward: " + TextUtil.ellipsize(font, first.name(), Math.max(30, maxW - fixed)) + tail;
	return Size.of(UiBits.dotPillWidth(font, text) + 4 + UiBits.keycapWidth(font, key), 12);
}
public void draw(GuiGraphicsExtractor g, Font font, Frame f, Slot s) {
	int w = UiBits.dotPill(g, font, "thinking", text, s.x(), s.y(), UiBits.ink());
	UiBits.keycap(g, font, key, s.x() + w + 4, s.y());
}
// and toasts: Toasts.push(Toasts.Level.NEED, title, body, "Y", "open", 3)
```

### Mapping of every HUD piece

| Mod | Piece | Today | Zone | Priority | Size | Screen open | Notes |
|---|---|---|---|---|---|---|---|
| Architect | PlacementHud | bottom centre, own crosshair floor | BOTTOM_CENTER | 100 | `shrinkTo(KEEP rows)` via HudFit | **shown** (as now) | anchors the zone. `Toasts.avoid` becomes redundant |
| Architect | PlotHud | bottom centre | BOTTOM_CENTER | 90 | fixed (it already drops a hint row on narrow screens) | hidden | keeps its "no stale status under the placement panel" rule |
| Architect | MassingReview HUD | bottom centre | BOTTOM_CENTER | 80 | `shrinkTo(head + hints)` | hidden | keeps "hidden while placing" |
| Architect | RegionGhost HUD | top centre y 6 | TOP_CENTER | 70 | fixed, 2 lines | hidden | no longer under SiteHud or boss bars |
| Architect | SiteHud | top centre y 4 | TOP_CENTER | 60 | full, `shrinkTo(line)` | hidden | its J key and "one line while placing" UiPrefs rule stay |
| Architect | HelperProgressHud | top left (4, 4) | TOP_LEFT | 40 | `shrinkTo(strip)` (drops the npm line) | hidden | below Steward's line, no overlap |
| Architect | toasts | lab-ui `Toasts` | TOP_RIGHT_TOASTS | 0 | flexible, optional | hidden | below vanilla toasts and effects |
| Steward | StewardHud inbox line | top left (6, 6), `60% - columnWidth` | TOP_LEFT | 60 | fixed 12 | hidden; F1 hides it | `maxW` is the known right edge |
| Steward | toasts | own `steward:hud/toasts` | TOP_RIGHT_TOASTS (lab-ui `Toasts`) | 0 | as Architect's | hidden | one column with Architect's; `maxBodyLines` 3 |
| Steward | board, plate, bubbles | in world | none | | | | colour tokens only |
| lab-ui | `Toasts` | `lab_ui:hud/toasts` | TOP_RIGHT_TOASTS | 0 | | | drawn by `lab_ui:hud/zones` |

**F1.** Steward already hides under F1. Architect's HUDs don't check it today. With the zone default (`whenHudHidden(false)`)
they hide under F1 as well. That is a small visible change for Architect, and I think a correct one. Architect can opt out
per element.

### Pure, unit-tested parts (`dev.larattalabs.labui.ui`)

- **`ZoneStack`.** Priority order, shrink, hide, overflow, the gap and both stacking directions. Tests: equal priorities;
  shrink before hide; never hiding a fixed element; an empty zone; a zone of 0 px.
- **`HudGeometry`.** Zone bounds from a frame, vanilla occupancy and the obstacles; the TOP_LEFT `maxW` and the step down;
  the toast limit from `avoid` and earlier slots. Tests at 1920×1080 GUI scales 2, 3 and 4 (960×540, 640×360, 480×270),
  426×240, survival and creative.
- **`VanillaHudMath`.** The toast bottom from (slot, height, portion) tuples, the boss-bar bottom with its `guiHeight/3` cap,
  and the effect rows (demo too).
- **`Contrast`.** The WCAG ratio and alpha compositing, for the token test.
- **The existing `ToastStackTest`**, which stays green unchanged.

## 3. Composite-build issue (found by Steward)

**It is not lab-ui, and it is not Steward's settings.gradle.** It is a consumer-side `build.gradle` block that Steward lacks.
I reproduced it in scratch clones, not in either repo:
- **Steward fails.** I cloned Steward main, set `lab_ui_version=0.2.0`, and built against `~/Developer/LarattaLabs/lab-ui` at
  `v0.2.0`. `compileClientJava` fails with 100 errors of the form `package dev.larattalabs.labui.client.ui does not exist`.
- **Architect compiles.** I built Architect main with `-Plab_ui.dir=` set to the same checkout.
- **The cause.** `dependencyInsight` on `clientCompileClasspath` shows the difference. Steward requests
  `org.gradle.libraryelements=classes` and gets lab-ui's `classes` secondary variant, which is `main`'s classes directory only.
  Architect requests `jar`, which holds main and client, like the published jar. The jar is only the right request for a
  composite build: it can't be pinned from the lab-ui side, because Gradle prefers the classes variant inside a composite.
- **Architect's fix.** `architect-mc/mod/build.gradle` ~lines 36-45 (commit 413dab1c) forces the jar when a checkout is
  included. AgentCraft has the same block (`agentcraft/mod/build.gradle:62`). Steward's settings.gradle is fine:
  `includeBuild(labUiDir)` is the same call. Steward's build.gradle just doesn't have the attribute block.

**The fix for Steward**, verified in the scratch clone (`compileClientJava` then succeeds, resolving `:lab-ui:jar`):

```groovy
// mod/settings.gradle, in `if (labUiDir != null) {`
	includeBuild(labUiDir)
	gradle.ext.labUiDir = labUiDir.path

// mod/build.gradle, at the end
// A composite lab-ui (settings.gradle includeBuild) hands compile classpaths its main classes directory only; its jar also has
// the client classes (split source sets), as the published jar does. So compile against the jar (as Architect does).
if (gradle.ext.has("labUiDir")) {
	afterEvaluate {
		configurations.matching { it.name.endsWith("ompileClasspath") }.configureEach {
			attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements, LibraryElements.JAR))
		}
	}
}
```

**An optional lab-ui-side hardening (not done; your call).** lab-ui's `apiElements` could carry the client classes directory in
its `classes` variant (or drop that secondary variant). Then no consumer would need the block, and a fourth consumer couldn't
hit this. It's a small `build.gradle` change on `ui-0.3`, but per your note I left lab-ui's build alone.

## 4. Build plan after approval

1. On `ui-0.3`:
   - `mod_version=0.3.0`;
   - `semantic_on_dark` in `ui-style.json`, and `UiStyle.OnDark`;
   - the behaviour-neutral re-routes in `SpeechBubble`, `UiBits` and `Toasts`;
   - the pure `Contrast`, `ZoneStack`, `HudGeometry` and `VanillaHudMath` with tests;
   - `HudZones` and `VanillaHud`, and the accessor mixin config;
   - `Toasts` on zones, with the `maxBodyLines` overload;
   - README "What changed in 0.3.0", CHANGELOG, and the CI jar-content check for the mixin json.
2. **Screenshots.**
   - **Setup.** A throwaway Architect worktree under scratch, built with `LAB_UI_DIR=../lab-ui-0.3`, never committed to Architect.
     A demo class registers fake elements in every zone, at the real sizes of the Steward line, SiteHud, RegionGhost, the
     placement panel and the toasts.
   - **Vanilla pieces.** Trigger them in the dev world: a recipe toast, `/bossbar add` ×2, `/effect` with a beneficial and a
     harmful effect.
   - **Captures.** At 1920×1080, GUI scales 2 and 4, in survival and creative: before (0.2 drawing) and after (zones), plus an
     on-dark token swatch panel.
   - **Ports.** A free pair of 8896-8905, named in the report.
3. Not on this branch: Architect's and Steward's adoption (HudInk aliases, the zone registrations, the `depends` range).
   Those are their own slices against 0.3.0.

## Open questions for review

1. Should error and warn change value (#F2948B, #F2B66A), or should warn stay #F4A585 for zero change in Architect's HUD?
2. Steward's plate name: `OnDark.selected` (6.50), or keep brass (4.48 over a white world)?
3. Hiding Architect's HUDs under F1 by default: yes?
4. `Toasts.push(…, maxBodyLines)` as an overload, or 3 lines for everyone?
5. Mixin accessors (`required: false`, caught), or plain reflection? I recommend the mixins. They are checked against 26.3 at
   build time, and a miss degrades to "no vanilla offset" instead of a crash.
