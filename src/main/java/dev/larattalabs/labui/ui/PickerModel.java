package dev.larattalabs.labui.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

/**
 * The logic of a picker (pure, unit-tested in {@code PickerModelTest}): entries with an id, label, group and tags; a
 * search query; a group (rail) filter; a favourites-only toggle; sections in group order for a grouped grid; and a
 * selection that survives refiltering (it is kept by id). The client's {@code PickerScreen} and {@code CompactPicker}
 * draw it. {@code T} is the caller's entry type; {@code key} reads what the model needs from it.
 */
public final class PickerModel<T> {
	/** What the model reads from an entry. Tags (and the id and group) are searched as well as the label. */
	public record Key(String id, String label, String group, List<String> tags) {
	}

	/** A run of entries of one group, for a grouped grid. */
	public record Section<T>(String group, List<T> items) {
	}

	private final List<T> all;
	private final Function<T, Key> keyFn;
	private final List<Key> keys;
	private final List<String> groups;
	private @Nullable String group;
	private String query = "";
	private boolean favouritesOnly;
	private Set<String> favourites = Set.of();
	private @Nullable String selectedId;
	private List<Integer> filtered = List.of();

	public PickerModel(List<T> entries, Function<T, Key> key) {
		this(entries, key, List.of());
	}

	/** {@code groupOrder} first (those that have entries), then any other group in order of first appearance. */
	public PickerModel(List<T> entries, Function<T, Key> key, List<String> groupOrder) {
		this.all = List.copyOf(entries);
		this.keyFn = key;
		this.keys = new ArrayList<>(all.size());
		for (T t : all) {
			keys.add(key.apply(t));
		}
		Set<String> seen = new LinkedHashSet<>();
		Set<String> present = new LinkedHashSet<>();
		for (Key k : keys) {
			present.add(k.group());
		}
		for (String g : groupOrder) {
			if (present.contains(g)) {
				seen.add(g);
			}
		}
		seen.addAll(present);
		this.groups = List.copyOf(seen);
		refilter();
	}

	public List<T> all() {
		return all;
	}

	public List<String> groups() {
		return groups;
	}

	public @Nullable String group() {
		return group;
	}

	/** Show one group (null = all). */
	public void setGroup(@Nullable String group) {
		this.group = group;
	}

	public String query() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query == null ? "" : query;
		refilter();
	}

	public boolean favouritesOnly() {
		return favouritesOnly;
	}

	public void setFavouritesOnly(boolean on) {
		favouritesOnly = on;
		refilter();
	}

	public void setFavourites(Collection<String> ids) {
		favourites = Set.copyOf(ids);
		refilter();
	}

	public boolean isFavourite(String id) {
		return favourites.contains(id);
	}

	private void refilter() {
		List<Integer> idx = new ArrayList<>();
		for (int i = 0; i < all.size(); i++) {
			if (!favouritesOnly || favourites.contains(keys.get(i).id())) {
				idx.add(i);
			}
		}
		filtered = TextMatch.rank(idx, query, i -> keys.get(i).label(), i -> {
			Key k = keys.get(i);
			List<String> extra = new ArrayList<>(k.tags());
			extra.add(k.id());
			extra.add(k.group());
			return extra;
		});
	}

	/** Entries that pass the query and favourites filter in {@code group} (null = all): the rail counts. */
	public int count(@Nullable String group) {
		int n = 0;
		for (int i : filtered) {
			if (group == null || keys.get(i).group().equals(group)) {
				n++;
			}
		}
		return n;
	}

	/** The visible entries as sections, in group order; within a group, best match first. */
	public List<Section<T>> sections() {
		List<Section<T>> out = new ArrayList<>();
		for (String g : groups) {
			if (group != null && !group.equals(g)) {
				continue;
			}
			List<T> items = new ArrayList<>();
			for (int i : filtered) {
				if (keys.get(i).group().equals(g)) {
					items.add(all.get(i));
				}
			}
			if (!items.isEmpty()) {
				out.add(new Section<>(g, List.copyOf(items)));
			}
		}
		return out;
	}

	/** The visible entries in grid order (the sections concatenated). */
	public List<T> visible() {
		List<T> out = new ArrayList<>();
		for (Section<T> s : sections()) {
			out.addAll(s.items());
		}
		return out;
	}

	public SectionGrid grid(int cols, boolean headings) {
		List<Section<T>> s = sections();
		int[] sizes = new int[s.size()];
		for (int i = 0; i < sizes.length; i++) {
			sizes[i] = s.get(i).items().size();
		}
		return new SectionGrid(sizes, cols, headings);
	}

	public @Nullable String selectedId() {
		return selectedId;
	}

	public void select(@Nullable String id) {
		selectedId = id;
	}

	/** Index of the selection among {@link #visible}, or -1 (none, or filtered out). */
	public int selectedIndex() {
		if (selectedId == null) {
			return -1;
		}
		List<T> v = visible();
		for (int i = 0; i < v.size(); i++) {
			if (key(v.get(i)).id().equals(selectedId)) {
				return i;
			}
		}
		return -1;
	}

	public @Nullable T selected() {
		int i = selectedIndex();
		return i < 0 ? null : visible().get(i);
	}

	/** Move the selection in a grid of {@code cols}; false at an edge (the caller may move focus, e.g. Up to search). */
	public boolean move(ListNav.Key k, int cols, boolean headings, int pageRows) {
		List<T> v = visible();
		int next = grid(cols, headings).move(selectedIndex(), k, pageRows);
		if (next < 0) {
			return false;
		}
		selectedId = key(v.get(next)).id();
		return true;
	}

	public Key key(T t) {
		return keyFn.apply(t);
	}

	public @Nullable T byId(String id) {
		for (int i = 0; i < all.size(); i++) {
			if (keys.get(i).id().equals(id)) {
				return all.get(i);
			}
		}
		return null;
	}

	/** The entries for {@code ids} that exist here, in that order (e.g. the recents strip). */
	public List<T> resolve(List<String> ids) {
		List<T> out = new ArrayList<>();
		for (String id : ids) {
			T t = byId(id);
			if (t != null) {
				out.add(t);
			}
		}
		return out;
	}

	/**
	 * The chips of a compact picker: the current choice first, then recents, then recommended, without repeats or
	 * unknown ids, at most {@code max}.
	 */
	public static List<String> compact(@Nullable String current, List<String> recents, List<String> recommended, int max, Predicate<String> known) {
		Set<String> out = new LinkedHashSet<>();
		if (current != null && known.test(current)) {
			out.add(current);
		}
		for (List<String> src : List.of(recents, recommended)) {
			for (String id : src) {
				if (out.size() >= max) {
					break;
				}
				if (id != null && known.test(id)) {
					out.add(id);
				}
			}
		}
		List<String> l = new ArrayList<>(out);
		return l.subList(0, Math.min(l.size(), Math.max(0, max)));
	}
}
