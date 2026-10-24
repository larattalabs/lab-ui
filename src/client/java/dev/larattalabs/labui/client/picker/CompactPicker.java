package dev.larattalabs.labui.client.picker;

import dev.larattalabs.labui.client.widget.Chip;
import dev.larattalabs.labui.client.widget.NavKeys;
import dev.larattalabs.labui.ui.ListNav;
import dev.larattalabs.labui.ui.PickerModel;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import org.jspecify.annotations.Nullable;

/**
 * An inline picker row: at most {@link PickerSpec#compactChoices} chips (the current choice first, selected; then recents,
 * then recommended) and a dashed "more… N" chip that opens the full {@link PickerScreen}. Chips that do not fit the
 * width are dropped from the end (the current choice and "more…" always stay). While focused, Left / Right move between
 * the chips and Enter / Space picks.
 */
public final class CompactPicker {
	private final PickerSpec spec;
	private final List<Chip> chips = new ArrayList<>();
	private final List<String> chipIds = new ArrayList<>();
	private final Chip more = new Chip("more…").kind(Chip.Kind.MORE);
	private boolean focused;
	private int focus;

	public CompactPicker(PickerSpec spec) {
		this.spec = spec;
		more.onPress(this::openBrowser);
	}

	public PickerSpec spec() {
		return spec;
	}

	public CompactPicker focused(boolean f) {
		focused = f;
		return this;
	}

	public boolean isFocused() {
		return focused;
	}

	/** The ids the row shows (current, recents, recommended), before width trimming. */
	public List<String> choices() {
		return PickerModel.compact(spec.current(), PickerStore.memory().recents(spec.id()), spec.recommended(), spec.compactChoices(),
			id -> spec.byId(id) != null);
	}

	public void openBrowser() {
		Minecraft mc = Minecraft.getInstance();
		mc.gui.setScreen(new PickerScreen(spec, mc.gui.screen()));
	}

	private void rebuild() {
		List<String> ids = choices();
		if (ids.equals(chipIds)) {
			for (int i = 0; i < chips.size(); i++) {
				chips.get(i).selected(ids.get(i).equals(spec.current()));
			}
			return;
		}
		chipIds.clear();
		chipIds.addAll(ids);
		chips.clear();
		for (String id : ids) {
			PickerEntry e = spec.byId(id);
			if (e == null) {
				continue;
			}
			Chip c = new Chip(e.label()).swatch(e.swatch()).icon(e.swatch() == null ? e.icon() : null).selected(id.equals(spec.current()));
			c.tooltip(e.subtitle());
			c.onPress(() -> spec.pick(e));
			chips.add(c);
		}
	}

	/** Draws the row; returns the width used. */
	public int draw(GuiGraphicsExtractor g, Font font, int x, int y, int w, int mx, int my) {
		rebuild();
		more.label("more… " + spec.entries().size());
		int mw = more.width(font);
		int avail = w - mw - 4;
		int cx = x;
		int n = chips.size();
		focus = Math.max(0, Math.min(focus, n));
		for (int i = 0; i < n; i++) {
			Chip c = chips.get(i);
			int cw = c.width(font);
			if (i > 0 && cx + cw > x + avail) {
				c.hide();
				continue;
			}
			cw = Math.min(cw, x + avail - cx);
			c.focused(focused && focus == i);
			c.draw(g, font, cx, y, cw, mx, my);
			cx += cw + 4;
		}
		more.focused(focused && focus == n);
		more.draw(g, font, cx, y, mw, mx, my);
		return cx + mw - x;
	}

	public boolean mouseClicked(double mx, double my, int button) {
		for (Chip c : chips) {
			if (c.mouseClicked(mx, my, button)) {
				return true;
			}
		}
		return more.mouseClicked(mx, my, button);
	}

	public boolean keyPressed(KeyEvent e) {
		if (!focused) {
			return false;
		}
		ListNav.Key k = NavKeys.nav(e);
		int last = chips.size();
		if (k == ListNav.Key.LEFT && focus > 0) {
			focus--;
			return true;
		}
		if (k == ListNav.Key.RIGHT && focus < last) {
			focus++;
			return true;
		}
		if (NavKeys.activates(e)) {
			@Nullable Chip c = focus < last ? chips.get(focus) : more;
			if (c.shown()) {
				c.press();
			} else {
				openBrowser();
			}
			return true;
		}
		return false;
	}

	/** The chip under the mouse (for a tooltip). */
	public @Nullable Chip at(double mx, double my) {
		for (Chip c : chips) {
			if (c.contains(mx, my)) {
				return c;
			}
		}
		return more.contains(mx, my) ? more : null;
	}
}
