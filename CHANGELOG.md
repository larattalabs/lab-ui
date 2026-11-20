# Changelog

## Unreleased (for 0.6.2, LUI3: Steward's accessibility asks)

Additive, so a patch: no member removed or renamed, no signature changed, `mod_version` stays 0.6.1 until the release. **Visible default
changes:** only for ideas (`JobRow.offer`): the Jobs panel's detail pane now draws an idea with the still idle paper dot and "idea", as its
list row already did, not the "working" dot and "queued". No consumer publishes ideas today except through Steward's offers, so Architect
and AgentCraft Worlds draw exactly as in 0.6.1. Notify AgentCraft Worlds, Architect and Steward.

**New API**
- `ui.StatusDots` (pure): the six status families' colours in `GhostPalette.Palette.STANDARD` (today's, the default) and `OKABE_ITO`
  (Okabe &amp; Ito's colour-blind-safe hues, `status_okabe` / `status_okabe_paper` in ui-style.json), `sprite(...)`, `minDeltaE(...)`.
  Sprites `dot_<family>_okabe.png`, `_okabe_halo.png`, `_okabe_paper.png`. Under protan, deutan and tritan simulation the Okabe dots are at
  least ΔE76 22.2 apart on the HUD and 9.9 on paper (the standard dots: 5.7 and 2.8); the paper set is at least 3:1 on every paper surface.
  `StatusDotsTest`.
- Palette overloads, all opt-in (null or STANDARD draws today's dot): `Kit.dot(family, halo, palette)`, `Kit.paperDot(family, palette)`,
  `Panels.dot(g, family, x, y, halo, palette)`, `Panels.paperDot(g, family, x, y, palette)`, `UiBits.dotPill(..., needsYou, palette)`,
  `UiBits.pulsingDot(g, family, x, y, palette)`, `DisplayDraw.dot(family, halo, palette)` (cached per palette). Pass
  `GhostPalette.current()` to follow the player's colour-blind setting, or `OKABE_ITO` from a mod's own setting, for the inbox, the
  building pill and the nameplate.
- `JobBoardModel.dotFamily(JobState)`, `dotFamily(JobRow)` (an idea is idle) and `statusLabel(JobRow)` ("idea" for an idea).
- `KeysOverlay.helpPlace(String)`, `footer(String)`, `moreTerms(IntFunction<String>)` (fluent) and `footer()` / `moreTerms(int)`: the footer
  and the "+N more" line, Architect's "Status &gt; Help" wording by default. `HelpKeys.CLOSE_HINT`, `DEFAULT_HELP_PLACE`, `DEFAULT_FOOTER`,
  `footer(place)`, `moreTerms(n, place)`.
- `HelpKeys.closeHit(glyphX, glyphW, lineY, lineH)`, `inside(box, mx, my)`, `MIN_HIT` (20).

**Looks**
- `WhatsThis`'s × takes clicks over at least 20 x 20 (it was 18 x 16 at 100 % text), grown left and up from the 0.6.0 box so it never
  reaches the controls under the line; the glyph is where it was. Its muted text (`semantic.muted` #655E55) is 4.92:1 on the inset
  (#E9E1D3) and 5.58:1 on cream (#F4EFE6, where `PickerScreen` draws it), so its colour is unchanged (`HelpKeysTest`).

## 0.6.1 (2026-11-19, KLW: key labels fitted by width)

Additive: no member removed or renamed. **Visible default changes:** `KeyLabels.readable` / `KeyLabels.label` no longer cut names at
9 characters ("Mouse Button 4" stays whole, not "Mouse But"); a consumer that drew `label()` into a fixed-width keycap should cut it
with `KeyLabels.fit` / `fitHints`. The "?" `KeysOverlay` now cuts a key wider than its column (a third of the panel) by measured width
with "…", so it no longer draws over the verb, at 100/125/150 % text. Notify AgentCraft Worlds, Architect and Steward (Architect's PR3
`fitted()` wrapper becomes redundant but harmless).

**New API**
- `ui.KeyFit` (pure): `readable` (punctuation spelled out, names whole), `fit(s, max, width)` (cut by measured width, ends with "…",
  never empty, whole code points), `fitHints` (each hint pair fits the row alone), `fitRows` (the overlay's key column), `ELLIPSIS`.
  Same rules as Architect's `PlacingKeys.fit` / `fitHints`. `KeyFitTest`.
- `client.ui.KeyLabels.fit(Font, label, maxCapW)` and `fitHints(Font, keyThenVerb, inner)`: the same with the keycap padding.

## 0.6.0 (2026-11-18, L-0.6: Architect's player-ready pass)

Additive: every 0.5.x class and method keeps working. Visible default changes: none (the ghost palette stays `standard`, the
text size 100 %, and only mods that opt in are scaled). Notify AgentCraft Worlds, Architect and Steward.

**New API**
- `ui.KeyDefaults`: lab-ui's default key codes as plain numbers (`JOBS` = K) and Steward's reserved Y/I/U (`STEWARD_RESERVED`), so a
  consumer's unit test can check its defaults without a bootstrap. `JobBoard` registers from it.
- `ui.Glossary` (sections per mod, a reserved slot with the words it must define: Steward's, S-PR-3), `ui.WordingLint` (the "design,
  never blueprint" rule over lang values and source literals, for any mod's test), `client.help.GlossaryView`.
- `client.help.WhatsThis` (a screen's one-line "what's this" with a ×, persisted by key in a `Store`; any mod passes its own or
  `LabUiPrefs.tips()`), `client.help.KeysOverlay` (the "?" overlay: Shift+/ or F1, keys read live, the screen's terms), `ui.HelpKeys`.
- `PickerScreen` hooks: `whatsThis()`, `keysOverlay()`, `searchFocused()`, `textScaleOwner()` (all default to none).
- `ui.GhostPalette` (standard and Okabe-Ito ghost colours by category, the two-tone outline, each category's second cue), `ui.GhostCues`
  (the cue geometry over a caller's box primitive), `ui.Cvd` (Machado 2009 simulation, ΔE76). `GhostPaletteTest`: Okabe-Ito min
  ΔE 16.7 in the ghost and region groups; the outline >= 4.43:1 on every terrain sample.
- `ui.TextScale` (100/125/150 %, per-mod opt-in, default off for every mod; the clamp to 320 x 240 logical) and
  `client.prefs.TextScaleHost` (draws a screen at its GUI size over the scale, maps the mouse).
- `client.prefs.LabUiPrefs`: `config/lab_ui.json` (`tips.hidden[]`, `ghostPalette`, `textScale`); `applyStored()` at a consumer's init.

## 0.5.0 (2026-11-15)

A minor release: additive except for the visible default changes listed under **Looks**. Every 0.4.x class and method keeps
working (`JobRow`'s and `HudLayout.Vanilla`'s old constructors stay). Consumers move their range to `>=0.5.0 <0.6.0` when they use
anything below. From LEFTOVERS "lab-ui L-0.5": UI-AUDIT items 9 and 15, Architect SD's leftovers, Steward S-J4's and S-UI-actions' asks.

**New API**
- `Stepper` (widget) and `Steps` (pure): kit Buttons at 18 px around a value, an optional name, a fixed value width, each end disabled
  with a reason, Left/Right and -/+ while focused.
- `Control.disabled(boolean, String reason)`, `disabledReason()`, `Control.reasonAt(mx, my, controls...)`: a disabled control says why;
  `Tooltip.offer(Control, …)` shows the reason while it is disabled. The plain `disabled(boolean)` clears it.
- `Toasts.clear()`; the stack also clears itself on disconnect (queued toasts outlived the world by up to 2 minutes).
- `ToastStack.fitTitle(title, subject, w, m, dropTail)`: a need-you toast whose name would be cut drops " needs you" and keeps the
  whole name ("Stilt Swamp Fishing Village", not "Stilt Swamp Fi… needs you"). `Toasts` uses it for need-you toasts.
- Ideas: `JobRow.offer` (`Builder.offer(true)`), `JobRow.idea()`, `JobBoardModel.Group.IDEAS` / `Filter.IDEAS` ("Ideas", after Needs
  you), `ideaRoots`, `pillIdleText`, and the idle pill (`JobBoard.showIdlePill`, default on): when nothing waits but ideas do, a still
  idle dot and "N ideas for you · title" in muted text. Ideas never pulse or count as needing the player. The Ideas filter chip shows
  only while there are ideas.
- `ColumnSplit` (pure): a title column and a step column. The Jobs panel's rows use it, so the step is no longer cut first.
- `Kit.paperDot(family)`, `Panels.paperDot(...)`, sprites `dot_<family>_paper`: status dots darkened to at least 3:1 on every paper
  surface (`status_paper` in ui-style.json, checked by `PaperContrastTest`).
- `UiStyle.disabledText()` (`semantic.disabled_text` #7D7569, 3.5:1 on the disabled fill).
- `Panels.headerBand(y)`: the header strip's band above its clay rule, for pills and buttons placed in it.
- `HudLayout.Vanilla` gains `chatRight`, `chatTop` (the visible chat column); `VanillaHud` reads it through the new optional accessor
  mixin `ChatComponentAccess`.
- Shared helpers moved from both mods: `client.ui.HasParent`, `client.env.ClientEnv`, `client.env.ScreenPause`, `client.ui.KeyLabels`
  (README "Class map").

**Looks (visible default changes)**
- BOTTOM_CENTER keeps clear of vanilla's chat: it lifts above the visible chat lines when it fits there, else moves right of the chat
  column, else stays (UI-AUDIT 9; `HudLayoutTest`).
- A disabled primary button is the plain disabled button with `disabledText()`; its pale clay sprite read as hover and its cream label
  was 1.6:1. `button_primary_disabled.png` now matches `button_disabled.png`. Disabled buttons and chips use `disabledText()`.
- `Panels.header` / `headerTextY` draw the title 1 px higher, so descenders end above the clay rule.
- `TextFieldView`: a focused, empty field's placeholder starts 3 px in (`PLACEHOLDER_INSET`), so the caret is not drawn over it.
- `ScrollList` no longer shifts a selected row's content 2 px right; the Jobs panel's selected row stays put.
- `UiBits.dotPill` and the Jobs panel's rows use the paper dots.
- `PickerScreen`: on a short detail pane (under 200 px, GUI scale 4) the big thumbnail is left out (the selected card shows it)
  (`PickerScreen.detailThumbH`; none under 200 px), so a consumer's extra buttons no longer cover every detail line; "Compare with… (later)" uses
  `disabledText()`.


## 0.4.1

Additive: every 0.4.0 class and method is unchanged. One visible default changes: `TextFieldView`'s placeholder colour.
A patch-level release fits (consumers keep `>=0.4.0 <0.5.0`); the mods read the new tokens with a fallback, so they draw the
same on 0.4.0.

From the 2026-11-12 UI audit of Architect and Steward (design-critic over both mods' screens; contrast computed with `Contrast`):

**Paper text tokens.** The new `semantic_contrast` block in `ui-style.json` records each semantic text token's worst ratio over
six paper surfaces: paper, the inset, an inactive tab, the selected, hovered and field fills. `PaperContrastTest` recomputes it
from the JSON, as `OnDarkContrastTest` does for the HUD.
- `semantic.clay_text` #9A3F27, `UiStyle.clayText()`: clay as text (labels, section titles, "Needs you"). It is 4.86:1 at worst.
  `CLAY_DARK` as text was 4.26:1 on paper and 3.76:1 on the inset, and both mods drew it that way. `CLAY` and `CLAY_DARK` stay
  for fills, edges, rules and dots.
- `semantic.placeholder` (= muted #655E55), `UiStyle.placeholder()`: a field's empty-state words. It is 4.61:1 at worst.
  `TextFieldView` now draws its placeholder in it. It was `ink_ui.ghost_on_paper` #BCAD95, 1.9:1 on the field. The typed-ahead
  ghost completion keeps #BCAD95.

**Rows.**
- `Panels.selectedRow(g, x, y, w, h)`: `semantic.selected` with a 2 px `selected_edge` bar. Muted text is 4.79:1 on it. A
  translucent clay wash over the inset, as Steward's inbox drew, left it at 3.95:1.
- `Panels.hoverRow(...)`: `row_hover` with its edge.

**The kit's own defaults move to the tokens** (Noah, 2026-11-12: "Keep recommendations"):
- The toasts' need-you titles and "+N need you", the Jobs panel's waiting lines and picker heads draw in `clayText()`, not
  `CLAY_DARK`.
- `PickerScreen`'s "nothing picked yet" uses `placeholder()`.
- `UiBits.dotPill` keeps the brass-edged pill sprite only for the `waiting` family, which is what needs the player. Every other
  state pill gets the chip fill and edge, because brass on a plain state read as focus.
- `dotPill(..., boolean needsYou)` chooses the edge explicitly.

**Header with a meta line.** `Panels.header(g, font, title, meta, metaArgb, x, y, w)` draws the right-hand meta on the title's
baseline. Callers placed it by eye, 1 to 2 px off. The title is ellipsized first, so the meta stays whole. `Panels.headerTextY(y)`
gives that baseline for other text.

## 0.4.0

Additive: every 0.3.x class and method is unchanged (`Toasts.Spec` keeps its 7-argument constructor). Consumers move their
range to `>=0.4.0 <0.5.0`.

**Jobs & spend panel.** `JobBoard` (client): a registry each mod publishes its rows (`JobRow`, `JobState`) and cost-ledger
entries (`JobCost`) into, replacing only its own; `JobsScreen`, the panel (today's ledger total with the per-mod split, a
usage-limit banner, filter chips, group by state or mod, rows that fold their children, a detail pane with spend against
budget and the owning mod's Open / Approve / Raise / Cancel); and the needs-you pill (`lab_ui:jobs_pill`, `TOP_LEFT`). The
logic is pure and unit-tested (`JobBoardModel`, `JobBoardModelTest`): grouping, ordering, folding, filter counts, today's
totals with each job counted once, the needs-you count and the banner time. `JobBoard.install()` registers the panel key
`key.lab_ui.jobs` (default K, category "Lab UI", `assets/lab_ui/lang/en_us.json`) once, however many mods call it.

**Toast refs.** `Toasts.Spec.ref(rowId)` and `Toasts.refShown()`: while such a toast shows, the panel key opens the panel on
that row; without a hint of its own the toast hints "[K] see it".

## 0.3.1

Patch: behaviour fix, no API change (`HudLayout.VANILLA_TOAST_W` is a new constant). Consumers keep `>=0.3.0 <0.4.0`.

**TOP_CENTER clears vanilla's toasts.** The zone already started below the boss bars and "Now playing"; it now also starts
4 px below vanilla's visible toasts (160 wide, against the right edge) when its widest piece reaches them. At GUI 480x270
with two advancement toasts, Architect's 246-px region verdict panel overlapped the toast column by 1 px. The need-you
title-row reservation and the priority rules are unchanged. `HudLayoutTest` adds that exact layout and checks vanilla's
toast rect in the 72-layout no-overlap test.

## 0.3.0

Additive: every 0.2.0 class and method is unchanged. Consumers move their range to `>=0.3.0 <0.4.0`.

**On-dark tokens.** `UiStyle.OnDark` (`ui-style.json` `semantic_on_dark`): text, muted, error, warn, ok, accent, selected,
focus, panel, panel edge, rule, bar, track. Every text token is at least 4.5:1 on the HUD ink panel (also over a white
world), nameplates, dark monitors and the board slate; `OnDarkContrastTest` checks it from the JSON (`Contrast`).

**HUD zones.** `HudZones` + `HudZone`: mods register HUD pieces into `TOP_LEFT`, `TOP_CENTER`, `TOP_RIGHT_TOASTS`, `LEFT`,
`RIGHT` and `BOTTOM_CENTER` with a priority and a size that may shrink or be hidden; one `lab_ui:hud/zones` element lays
them out (pure `HudLayout`, `ZoneStack`) clear of each other and of vanilla's boss bars, effect icons, toasts and "Now
playing" (`VanillaHud`, `VanillaHudMath`; read through `lab_ui.client.mixins.json`, three non-required accessors).

**Toasts.** The stack is the toast zone's element (below vanilla's toasts and effects); `Toasts.Spec` and `push(Spec)` add
up to three body lines and a title subject shortened first (`ToastStack.fitTitle`, `clipBody`). `avoid` is unchanged in
meaning. A need-you toast is never folded away: the stack claims its title row ahead of the top-centre
status pieces, which collapse first, and draws it compact when space is short (`ToastStack.layoutNeedFirst`).

**Build.** The client classes are in the variant a composite (`includeBuild`) consumer compiles against.

README.md "What changed in 0.3.0" lists the seams.

## 0.2.0

Additive: every 0.1.0 class and method is unchanged. Consumers move their range to `>=0.2.0 <0.3.0`.

**Components.** Shared list and picker building blocks for Architect and Steward (slice UI-1), in
`dev.larattalabs.labui.client.widget` and `.picker`, with their logic pure and unit-tested in `dev.larattalabs.labui.ui`:
- `Button` (the one kit button, 18 px minimum) and `Chip` (selected, more, disabled and focus states; swatch or item lead);
- `ChipRow`, with a hookable "+N" overflow;
- `ScrollList`: fixed rows, wheel, draggable scrollbar, keys, filter line, empty state;
- `CollapsibleSection` (open state kept on disk by key), `Tooltip`, `EmptyState`, and `Confirm` (a two-press arm with a countdown, replacing
  Architect's OwnedConfirm and CrateScreen's Deconstruct patterns);
- `CompactPicker` plus the full-screen `PickerScreen` (search, family rail, grouped cells or cards, detail pane,
  favourites and recents per picker id in `config/lab_ui/pickers.json`; names wrap to two lines with the full name in
  a tooltip; cells and cards widen to fill the row; Esc clears a typed search before going back);
- `ChatPager` (common code): chat lists in pages with "…and K more" and clickable [prev]/[next]/[filter…].

**Tokens.** `UiStyle.error()`, `warn()`, `ok()`, `mutedText()`, `selected()`, `focusRing()`, `rowHover()` and the
picker surface tokens, from the new `semantic` block of `ui-style.json`.

**Moved in from Architect and Steward** (identical in both): `TextModel`, `TextFieldView`, `TextKeys`, `Toasts`,
`ToastStack`, `UiRules`. `Toasts` gains `install()` (one shared HUD element) and `avoid(rect)` in place of Architect's
placement-HUD dodge; README.md "What changed in 0.2.0" lists the seams.

## 0.1.0

First release: the Warm Studio kit, world-space UI and crash guard, moved out of AgentCraft Worlds.
