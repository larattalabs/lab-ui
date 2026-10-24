# Changelog

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
