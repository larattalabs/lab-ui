package dev.larattalabs.labui.client.picker;

import dev.larattalabs.labui.ui.PickerModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * One choice in a picker. Only {@code id} and {@code label} are required; everything else is optional and the screens
 * draw what is there: a material swatch, an item / block icon, or a custom preview (a style's elevation sheet), a
 * subtitle (the block id), detail lines for the detail pane, and tags (searched, and shown on cards). {@link #attr} holds
 * caller data for later features (usage counts, compare keys) without changing this class.
 *
 * <pre>
 * PickerEntry.of("spruce_stairs", "Spruce").group("Wood stairs").swatch(0x6F4F2E)
 *     .icon(() -&gt; new ItemStack(Items.SPRUCE_STAIRS)).subtitle("minecraft:spruce_stairs")
 *     .details(() -&gt; List.of("Used in 4 of your designs")).build();
 * </pre>
 */
public final class PickerEntry {
	/** Draws a preview into a box (cells, cards and the detail pane call it at their own sizes). */
	@FunctionalInterface
	public interface Preview {
		void draw(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h);
	}

	private final String id;
	private final String label;
	private final String group;
	private final List<String> tags;
	private final @Nullable Integer swatch;
	private final @Nullable Supplier<ItemStack> icon;
	private final @Nullable Preview preview;
	private final @Nullable String subtitle;
	private final Supplier<List<String>> details;
	private final Map<String, Object> attrs;

	private PickerEntry(Builder b) {
		this.id = b.id;
		this.label = b.label;
		this.group = b.group;
		this.tags = List.copyOf(b.tags);
		this.swatch = b.swatch;
		this.icon = b.icon;
		this.preview = b.preview;
		this.subtitle = b.subtitle;
		this.details = b.details;
		this.attrs = Map.copyOf(b.attrs);
	}

	public static Builder of(String id, String label) {
		return new Builder(id, label);
	}

	public String id() {
		return id;
	}

	public String label() {
		return label;
	}

	public String group() {
		return group;
	}

	public List<String> tags() {
		return tags;
	}

	public @Nullable Integer swatch() {
		return swatch;
	}

	public @Nullable Supplier<ItemStack> icon() {
		return icon;
	}

	public @Nullable Preview preview() {
		return preview;
	}

	public @Nullable String subtitle() {
		return subtitle;
	}

	public List<String> details() {
		return details.get();
	}

	public @Nullable Object attr(String key) {
		return attrs.get(key);
	}

	public PickerModel.Key key() {
		return new PickerModel.Key(id, label, group, tags);
	}

	public static final class Builder {
		private final String id;
		private final String label;
		private String group = "";
		private final List<String> tags = new ArrayList<>();
		private @Nullable Integer swatch;
		private @Nullable Supplier<ItemStack> icon;
		private @Nullable Preview preview;
		private @Nullable String subtitle;
		private Supplier<List<String>> details = List::of;
		private final Map<String, Object> attrs = new HashMap<>();

		private Builder(String id, String label) {
			this.id = id;
			this.label = label;
		}

		/** The family shown in the rail and as a grid heading ("" = ungrouped). */
		public Builder group(String g) {
			group = g == null ? "" : g;
			return this;
		}

		public Builder tags(List<String> t) {
			tags.addAll(t);
			return this;
		}

		public Builder tag(String t) {
			tags.add(t);
			return this;
		}

		/** RGB of a material swatch. */
		public Builder swatch(int rgb) {
			swatch = rgb;
			return this;
		}

		public Builder icon(Supplier<ItemStack> stack) {
			icon = stack;
			return this;
		}

		/** A custom preview: wins over the swatch and icon in cells, cards and the detail pane. */
		public Builder preview(Preview p) {
			preview = p;
			return this;
		}

		public Builder subtitle(String s) {
			subtitle = s;
			return this;
		}

		/** Lines for the detail pane, read when it is drawn (so they may be live, e.g. usage counts). */
		public Builder details(Supplier<List<String>> d) {
			details = d;
			return this;
		}

		public Builder attr(String key, Object value) {
			attrs.put(key, value);
			return this;
		}

		public PickerEntry build() {
			return new PickerEntry(this);
		}
	}
}
