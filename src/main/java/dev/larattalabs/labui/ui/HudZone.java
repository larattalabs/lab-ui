package dev.larattalabs.labui.ui;

/**
 * The shared HUD zones ({@code dev.larattalabs.labui.client.hud.HudZones}, laid out by {@link HudLayout}). Append-only.
 * Bottom left (chat) and bottom right (subtitles) are vanilla's and not zones.
 */
public enum HudZone {
	/** Top left, stacking down: one-line status (Steward's inbox line), progress strips. Ends at a known right edge. */
	TOP_LEFT,
	/** Top centre, stacking down below any boss bars: status lines (Architect's construction and region HUDs). */
	TOP_CENTER,
	/** Top right, stacking down below vanilla's toasts and effect icons: the shared toast column (lab-ui {@code Toasts}). */
	TOP_RIGHT_TOASTS,
	/** Left edge, centred vertically. Nothing uses it yet. */
	LEFT,
	/** Right edge, centred vertically. Nothing uses it yet. */
	RIGHT,
	/** Bottom centre, stacking up from above the hotbar, never over the crosshair: aimed panels (Architect's placement HUD). */
	BOTTOM_CENTER
}
