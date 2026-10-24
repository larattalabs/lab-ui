package dev.larattalabs.labui.client.picker;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jspecify.annotations.Nullable;

/**
 * What a picker shows and does: its id (recents and favourites are kept under it; prefix it with your mod id), title,
 * entries, the current choice, recommended ids for the compact row, and what picking does. Shared by
 * {@link CompactPicker} and {@link PickerScreen}.
 *
 * <pre>
 * PickerSpec roof = PickerSpec.of("architect.palette.roof", "Choose roof material", entries)
 *     .noun("roof materials").current(form.roof()).recommended(List.of("spruce", "deepslate_tile"))
 *     .onPick(e -&gt; form.setField("roof", e.id()));
 * </pre>
 */
public final class PickerSpec {
	/** How the browser lays entries out. */
	public enum Layout {
		/** Swatch / icon cells with a name (materials). */
		CELLS,
		/** Larger cards: a preview sheet, the name and tags with "+N" (styles). */
		CARDS
	}

	/** A preview of the hovered entry in context (e.g. a little house in the current palette), drawn in the detail pane. */
	@FunctionalInterface
	public interface DetailPreview {
		void draw(GuiGraphicsExtractor g, Font font, PickerEntry entry, int x, int y, int w, int h);
	}

	private final String id;
	private final String title;
	private final List<PickerEntry> entries;
	private @Nullable String current;
	private List<String> recommended = List.of();
	private List<String> groupOrder = List.of();
	private Consumer<PickerEntry> onPick = e -> {
	};
	private Layout layout = Layout.CELLS;
	private String noun = "entries";
	private String pickLabel = "Pick";
	private @Nullable DetailPreview detailPreview;
	private int compactChoices = 3;
	private @Nullable String subtitle;
	private @Nullable Consumer<PickerScreen> onTags;
	private @Nullable Consumer<PickerScreen> onCompare;

	private PickerSpec(String id, String title, List<PickerEntry> entries) {
		this.id = id;
		this.title = title;
		this.entries = List.copyOf(entries);
	}

	public static PickerSpec of(String id, String title, List<PickerEntry> entries) {
		return new PickerSpec(id, title, entries);
	}

	public PickerSpec current(@Nullable String id) {
		current = id;
		return this;
	}

	public PickerSpec recommended(List<String> ids) {
		recommended = List.copyOf(ids);
		return this;
	}

	/** Rail order; groups not listed follow in order of first appearance. */
	public PickerSpec groupOrder(List<String> groups) {
		groupOrder = List.copyOf(groups);
		return this;
	}

	public PickerSpec onPick(Consumer<PickerEntry> c) {
		onPick = c;
		return this;
	}

	public PickerSpec layout(Layout l) {
		layout = l;
		return this;
	}

	/** Plural noun for "Search 36 roof materials…" and counts. */
	public PickerSpec noun(String n) {
		noun = n;
		return this;
	}

	/** The detail pane's primary button ("Pick", "Use this style"). */
	public PickerSpec pickLabel(String l) {
		pickLabel = l;
		return this;
	}

	public PickerSpec detailPreview(@Nullable DetailPreview p) {
		detailPreview = p;
		return this;
	}

	/** Choice chips in the compact row before "more…" (default 3: current, then recents or recommended). */
	public PickerSpec compactChoices(int n) {
		compactChoices = Math.max(1, n);
		return this;
	}

	/** A muted line in the title bar after the title ("24 styles · 4 yours"); null = the key hints. */
	public PickerSpec subtitle(@Nullable String s) {
		subtitle = s;
		return this;
	}

	/** Hook for the Tags chip (enabled only when set): e.g. open a tag popup and filter with {@code screen.model()}. */
	public PickerSpec onTags(@Nullable Consumer<PickerScreen> hook) {
		onTags = hook;
		return this;
	}

	/** Hook for the Compare chip (enabled only when set). */
	public PickerSpec onCompare(@Nullable Consumer<PickerScreen> hook) {
		onCompare = hook;
		return this;
	}

	public @Nullable Consumer<PickerScreen> onTags() {
		return onTags;
	}

	public @Nullable Consumer<PickerScreen> onCompare() {
		return onCompare;
	}

	public String id() {
		return id;
	}

	public String title() {
		return title;
	}

	public List<PickerEntry> entries() {
		return entries;
	}

	public @Nullable String current() {
		return current;
	}

	public List<String> recommended() {
		return recommended;
	}

	public List<String> groupOrder() {
		return groupOrder;
	}

	public Layout layout() {
		return layout;
	}

	public String noun() {
		return noun;
	}

	public String pickLabel() {
		return pickLabel;
	}

	public @Nullable DetailPreview detailPreview() {
		return detailPreview;
	}

	public int compactChoices() {
		return compactChoices;
	}

	public @Nullable String subtitle() {
		return subtitle;
	}

	public @Nullable PickerEntry byId(@Nullable String id) {
		if (id == null) {
			return null;
		}
		for (PickerEntry e : entries) {
			if (e.id().equals(id)) {
				return e;
			}
		}
		return null;
	}

	/** Records the pick (recents), updates the current choice and calls the caller. */
	public void pick(PickerEntry e) {
		PickerStore.used(id, e.id());
		current = e.id();
		onPick.accept(e);
	}

	List<PickerEntry> resolve(List<String> ids) {
		List<PickerEntry> out = new ArrayList<>();
		for (String i : ids) {
			PickerEntry e = byId(i);
			if (e != null) {
				out.add(e);
			}
		}
		return out;
	}
}
