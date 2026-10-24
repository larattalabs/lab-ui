# Changelog

## 0.2.0

Additive: every 0.1.0 class and method is unchanged. Consumers move their range to `>=0.2.0 <0.3.0`.

**Components.** Shared list and picker building blocks for Architect and Steward (slice UI-1), in
`dev.larattalabs.labui.client.widget` and `.picker`, with their logic pure and unit-tested in `dev.larattalabs.labui.ui`:
- `Button` (the one kit button, 18 px minimum) and `Chip` (selected, more, disabled and focus states; swatch or item lead);
- `ChipRow`, with a hookable "+N" overflow;
- `ScrollList`: fixed rows, wheel, draggable scrollbar, keys, filter line, empty state;
- `CollapsibleSection`, `Tooltip`, `EmptyState`, and `Confirm` (a two-press arm with a countdown, replacing
  Architect's OwnedConfirm and CrateScreen's Deconstruct patterns);
- `CompactPicker` plus the full-screen `PickerScreen` (search, family rail, grouped cells or cards, detail pane,
  favourites and recents per picker id in `config/lab_ui/pickers.json`);
- `ChatPager` (common code): chat lists in pages with "…and K more" and clickable [prev]/[next]/[filter…].

**Tokens.** `UiStyle.error()`, `warn()`, `ok()`, `mutedText()`, `selected()`, `focusRing()`, `rowHover()` and the
picker surface tokens, from the new `semantic` block of `ui-style.json`.

**Moved in from Architect and Steward** (identical in both): `TextModel`, `TextFieldView`, `TextKeys`, `Toasts`,
`ToastStack`, `UiRules`. `Toasts` gains `install()` (one shared HUD element) and `avoid(rect)` in place of Architect's
placement-HUD dodge; README.md "What changed in 0.2.0" lists the seams.

## 0.1.0

First release: the Warm Studio kit, world-space UI and crash guard, moved out of AgentCraft Worlds.
