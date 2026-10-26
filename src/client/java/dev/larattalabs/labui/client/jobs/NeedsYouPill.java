package dev.larattalabs.labui.client.jobs;

import dev.larattalabs.labui.client.hud.HudZones;
import dev.larattalabs.labui.client.hud.UiBits;
import dev.larattalabs.labui.client.ui.Kit;
import dev.larattalabs.labui.client.ui.Panels;
import dev.larattalabs.labui.client.ui.TextUtil;
import dev.larattalabs.labui.client.ui.UiStyle;
import dev.larattalabs.labui.ui.JobBoardModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jspecify.annotations.Nullable;

/**
 * The shared needs-you pill (0.4.0), {@code lab_ui:jobs_pill} in {@link dev.larattalabs.labui.ui.HudZone#TOP_LEFT}: a
 * pulsing dot, "N jobs need you · &lt;first title&gt;" and the panel key, counting the waiting rows of every mod
 * ({@link JobBoardModel#pillText}). Hidden when nothing waits, under F1 and screens, with {@link JobBoard#showPill}
 * false, and (optional) when the zone has no room.
 */
final class NeedsYouPill implements HudZones.Element {
	static final int H = 16;
	private static final int PAD = 5;
	private String text = "";
	private String keyLabel = "K";

	@Override
	public HudZones.@Nullable Size measure(Font font, HudZones.Frame f, int maxW) {
		if (!JobBoard.pillShown() || Minecraft.getInstance().player == null) {
			return null;
		}
		String t = JobBoardModel.pillText(JobBoard.model().rows());
		if (t == null) {
			return null;
		}
		keyLabel = JobBoard.keyLabel();
		int fixed = PAD + 7 + 4 + 4 + UiBits.keycapWidth(font, keyLabel) + PAD + 2;
		int room = Math.max(40, maxW - fixed);
		text = TextUtil.ellipsize(font, t, room);
		return HudZones.Size.of(fixed + font.width(text), H).optional();
	}

	@Override
	public void draw(GuiGraphicsExtractor g, Font font, HudZones.Frame f, HudZones.Slot s) {
		int x = s.x();
		int y = s.y();
		Panels.sprite(g, Kit.TOOLTIP, x, y, s.w(), H);
		UiBits.pulsingDot(g, "waiting", x + PAD, y + (H - 7) / 2);
		int tx = x + PAD + 7 + 4;
		g.text(font, text, tx, y + 4, UiStyle.OnDark.text(), false);
		UiBits.keycap(g, font, keyLabel, tx + font.width(text) + 4, y + 2);
	}
}
