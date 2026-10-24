package dev.larattalabs.labui.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Recents and favourites per picker id, and the open state of collapsible sections by key (pure, unit-tested in {@code PickerMemoryTest}); the client keeps one in
 * {@code config/lab_ui/pickers.json}. Picker ids are shared by every mod (one lab-ui copy at runtime), so prefix them with
 * your mod id: {@code architect.palette.roof}. Recents are newest first, without repeats, at most {@link #MAX_RECENTS}.
 */
public final class PickerMemory {
	public static final int MAX_RECENTS = 8;
	private final Map<String, List<String>> recents = new LinkedHashMap<>();
	private final Map<String, Set<String>> favourites = new LinkedHashMap<>();
	private final Map<String, Boolean> sections = new LinkedHashMap<>();

	/** A collapsible section's remembered open state, or null (never toggled: use the caller's default). */
	public Boolean section(String key) {
		return sections.get(key);
	}

	public void section(String key, boolean open) {
		sections.put(key, open);
	}

	public List<String> recents(String picker) {
		return Collections.unmodifiableList(recents.getOrDefault(picker, List.of()));
	}

	/** {@code id} was picked: it goes to the front of the recents. */
	public void used(String picker, String id) {
		List<String> r = recents.computeIfAbsent(picker, k -> new ArrayList<>());
		r.remove(id);
		r.add(0, id);
		while (r.size() > MAX_RECENTS) {
			r.remove(r.size() - 1);
		}
	}

	public void forget(String picker, String id) {
		List<String> r = recents.get(picker);
		if (r != null) {
			r.remove(id);
		}
	}

	public Set<String> favourites(String picker) {
		return Collections.unmodifiableSet(favourites.getOrDefault(picker, Set.of()));
	}

	public boolean favourite(String picker, String id) {
		return favourites.getOrDefault(picker, Set.of()).contains(id);
	}

	/** Toggle; returns the new state. */
	public boolean toggleFavourite(String picker, String id) {
		Set<String> f = favourites.computeIfAbsent(picker, k -> new LinkedHashSet<>());
		if (f.remove(id)) {
			return false;
		}
		f.add(id);
		return true;
	}

	/** {@code {"pickers": {id: {"recent": [...], "favourites": [...]}}}}. */
	public String toJson() {
		JsonObject pickers = new JsonObject();
		Set<String> ids = new LinkedHashSet<>(recents.keySet());
		ids.addAll(favourites.keySet());
		for (String id : ids) {
			JsonObject p = new JsonObject();
			JsonArray r = new JsonArray();
			recents.getOrDefault(id, List.of()).forEach(r::add);
			JsonArray f = new JsonArray();
			favourites.getOrDefault(id, Set.of()).forEach(f::add);
			p.add("recent", r);
			p.add("favourites", f);
			pickers.add(id, p);
		}
		JsonObject secs = new JsonObject();
		sections.forEach(secs::addProperty);
		JsonObject root = new JsonObject();
		root.addProperty("version", 1);
		root.add("pickers", pickers);
		root.add("sections", secs);
		return root.toString();
	}

	/** Reads {@link #toJson}'s form; anything unreadable gives an empty memory (never throws). */
	public static PickerMemory fromJson(String json) {
		PickerMemory m = new PickerMemory();
		try {
			JsonObject root = JsonParser.parseString(json).getAsJsonObject();
			JsonElement secs = root.get("sections");
			if (secs != null && secs.isJsonObject()) {
				for (var e : secs.getAsJsonObject().entrySet()) {
					if (e.getValue().isJsonPrimitive() && e.getValue().getAsJsonPrimitive().isBoolean()) {
						m.section(e.getKey(), e.getValue().getAsBoolean());
					}
				}
			}
			JsonElement pk = root.get("pickers");
			if (pk == null || !pk.isJsonObject()) {
				return m;
			}
			JsonObject pickers = pk.getAsJsonObject();
			for (var e : pickers.entrySet()) {
				if (!e.getValue().isJsonObject()) {
					continue;
				}
				JsonObject p = e.getValue().getAsJsonObject();
				List<String> r = strings(p.get("recent"));
				for (int i = r.size() - 1; i >= 0; i--) {
					m.used(e.getKey(), r.get(i));
				}
				for (String f : strings(p.get("favourites"))) {
					if (!m.favourite(e.getKey(), f)) {
						m.toggleFavourite(e.getKey(), f);
					}
				}
			}
		} catch (RuntimeException e) {
			return new PickerMemory();
		}
		return m;
	}

	private static List<String> strings(JsonElement el) {
		List<String> out = new ArrayList<>();
		if (el != null && el.isJsonArray()) {
			for (JsonElement x : el.getAsJsonArray()) {
				if (x.isJsonPrimitive() && x.getAsJsonPrimitive().isString()) {
					out.add(x.getAsString());
				}
			}
		}
		return out;
	}
}
