package dev.larattalabs.labui.client.widget;

import dev.larattalabs.labui.ui.ChipFlow;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import org.jspecify.annotations.Nullable;

/**
 * Chips laid out in a width, at most {@code maxRows} rows ({@link ChipFlow}); what does not fit folds into one dashed
 * "+N" chip at the end. The caller hooks it with {@link #onMore} (open a picker, expand the row, …); its tooltip lists
 * the hidden labels.
 */
public final class ChipRow {
	public static final int GAP = 4;
	public static final int ROW_GAP = 3;
	private final List<Chip> chips = new ArrayList<>();
	private final Chip more = new Chip("+0").kind(Chip.Kind.MORE);
	private int maxRows = 1;
	private int hidden;
	private @Nullable IntConsumer onMore;

	public ChipRow() {
		more.onPress(() -> {
			if (onMore != null) {
				onMore.accept(hidden);
			}
		});
	}

	public ChipRow(List<Chip> chips) {
		this();
		this.chips.addAll(chips);
	}

	public List<Chip> chips() {
		return chips;
	}

	public ChipRow add(Chip c) {
		chips.add(c);
		return this;
	}

	public ChipRow clear() {
		chips.clear();
		return this;
	}

	public ChipRow maxRows(int n) {
		maxRows = Math.max(1, n);
		return this;
	}

	/** Called with the number of hidden chips when "+N" is pressed. */
	public ChipRow onMore(IntConsumer c) {
		onMore = c;
		return this;
	}

	/** The "+N" chip (for focus or a custom label). */
	public Chip moreChip() {
		return more;
	}

	public int hidden() {
		return hidden;
	}

	public static String moreLabel(int n) {
		return "+" + n;
	}

	/** Lays out and draws; returns the height used. */
	public int draw(GuiGraphicsExtractor g, Font font, int x, int y, int w, int mx, int my) {
		int[] widths = new int[chips.size()];
		for (int i = 0; i < widths.length; i++) {
			widths[i] = chips.get(i).width(font);
		}
		ChipFlow.Layout l = ChipFlow.layout(widths, w, GAP, maxRows, n -> more.label(moreLabel(n)).width(font));
		hidden = l.hidden();
		for (Chip c : chips) {
			c.hide();
		}
		for (ChipFlow.Placed p : l.placed()) {
			chips.get(p.index()).draw(g, font, x + p.x(), y + p.row() * (Chip.H + ROW_GAP), p.w(), mx, my);
		}
		if (l.more() != null) {
			StringBuilder tip = new StringBuilder();
			for (int i = l.placed().size(); i < chips.size(); i++) {
				tip.append(i > l.placed().size() ? ", " : "").append(chips.get(i).label());
			}
			more.label(moreLabel(l.hidden())).tooltip(tip.toString());
			more.draw(g, font, x + l.more().x(), y + l.more().row() * (Chip.H + ROW_GAP), l.more().w(), mx, my);
		} else {
			more.hide();
		}
		return l.rows() == 0 ? 0 : l.rows() * Chip.H + (l.rows() - 1) * ROW_GAP;
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
		for (Chip c : chips) {
			if (c.keyPressed(e)) {
				return true;
			}
		}
		return more.keyPressed(e);
	}

	/** The chip (or "+N") under the mouse, for tooltips. */
	public @Nullable Chip at(double mx, double my) {
		for (Chip c : chips) {
			if (c.contains(mx, my)) {
				return c;
			}
		}
		return more.contains(mx, my) ? more : null;
	}
}
