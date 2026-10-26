package dev.larattalabs.labui.client.jobs;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import dev.larattalabs.labui.LabUi;
import dev.larattalabs.labui.client.hud.HudZones;
import dev.larattalabs.labui.client.hud.Toasts;
import dev.larattalabs.labui.ui.Guard;
import dev.larattalabs.labui.ui.HudZone;
import dev.larattalabs.labui.ui.JobBoardModel;
import dev.larattalabs.labui.ui.JobCost;
import dev.larattalabs.labui.ui.JobRow;
import java.time.ZoneId;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

/**
 * The shared Jobs &amp; spend panel (0.4.0): a client-side registry that every mod publishes its Claude work into, the
 * panel screen ({@link JobsScreen}) and the needs-you pill ({@link NeedsYouPill}) in {@link HudZone#TOP_LEFT}.
 *
 * <pre>
 * JobBoard.install();                                  // onInitializeClient; idempotent across mods
 * JobBoard.name("architect_mc", "Architect");
 * JobBoard.publish("architect_mc", rows);              // replaces this mod's rows, any thread
 * JobBoard.publishLedger("architect_mc", costs);       // replaces this mod's cost-ledger entries
 * Toasts.push(Toasts.Spec.of(NEED, "Market stall needs you", "…").ref("architect_mc:group/g12"));
 * </pre>
 *
 * <p>One lab-ui copy serves every mod, so there is one board, one key and one pill. {@link #install()} registers the panel
 * key ({@code key.lab_ui.jobs}, default K, rebindable under "Lab UI") once, however many mods call it; call it from your
 * client initializer, because key mappings registered after the options load are not saved. The key opens the panel
 * on the row of a toast that carries a {@code ref} while one shows, else on the list; it does nothing while a screen is open.
 */
public final class JobBoard {
	private static final JobBoardModel MODEL = new JobBoardModel();
	private static @Nullable KeyMapping key;
	private static volatile boolean installed;
	private static volatile boolean pillShown = true;

	private JobBoard() {
	}

	/**
	 * Registers the panel key, its tick handler and the needs-you pill once (later calls do nothing; client
	 * initializer). Also installs {@link Toasts} and {@link HudZones}.
	 */
	public static synchronized void install() {
		if (installed) {
			return;
		}
		KeyMapping.Category cat;
		try {
			cat = KeyMapping.Category.register(LabUi.id("lab_ui"));
		} catch (IllegalArgumentException alreadyThere) {
			cat = new KeyMapping.Category(LabUi.id("lab_ui"));
		}
		key = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.lab_ui.jobs", InputConstants.Type.KEYBOARD, InputConstants.KEY_K, cat));
		ClientTickEvents.END_CLIENT_TICK.register(mc -> Guard.run("lab_ui:jobs.key", () -> tick(mc)));
		Toasts.install();
		HudZones.add(LabUi.id("jobs_pill"), HudZone.TOP_LEFT, new NeedsYouPill()).priority(70);
		installed = true;
	}

	public static boolean installed() {
		return installed;
	}

	private static void tick(Minecraft mc) {
		KeyMapping k = key;
		if (k == null) {
			return;
		}
		while (k.consumeClick()) {
			if (mc.gui.screen() == null && mc.player != null) {
				openFromKey();
			}
		}
	}

	/** What the panel key does in game: the panel on the row of a toast with a ref that shows now, else the list. */
	public static void openFromKey() {
		open(Toasts.refShown());
	}

	/** Opens the panel (client thread), on {@code rowId} when it is published, else on the first row. */
	public static void open(@Nullable String rowId) {
		Minecraft mc = Minecraft.getInstance();
		Screen parent = mc.gui.screen();
		mc.gui.setScreen(new JobsScreen(parent, rowId));
	}

	/** The panel key as bound ("K"), for hints. */
	public static String keyLabel() {
		KeyMapping k = key;
		if (k == null || k.isUnbound()) {
			return "K";
		}
		String s = k.getTranslatedKeyMessage().getString();
		return s.length() > 9 ? s.substring(0, 9) : s;
	}

	/** The panel key mapping (null before {@link #install()}). */
	public static @Nullable KeyMapping key() {
		return key;
	}

	// ------------------------------------------------------------------ publishing

	/** Replaces {@code mod}'s rows (any thread). Ids are global: {@code <modid>:<thing>}. */
	public static void publish(String mod, List<JobRow> rows) {
		MODEL.publish(mod, rows);
	}

	/** Replaces {@code mod}'s cost-ledger entries (any thread): one per distinct Claude job. */
	public static void publishLedger(String mod, List<JobCost> entries) {
		MODEL.publishLedger(mod, entries);
	}

	/** {@code mod}'s display name ("Architect"); without one, the id before "_mc", capitalised. */
	public static void name(String mod, String displayName) {
		MODEL.name(mod, displayName);
	}

	/**
	 * {@code mod}'s Claude work runs in the host's game, not here (a LAN guest, a dedicated server's client): the panel's
	 * empty state says so.
	 */
	public static void hostOnly(String mod, boolean b) {
		MODEL.hostOnly(mod, b);
	}

	/** Shows or hides the needs-you pill (default shown). Hide it while your own HUD says the same thing. */
	public static void showPill(boolean b) {
		pillShown = b;
	}

	public static boolean pillShown() {
		return pillShown;
	}

	/** The registry (read-only use: the panel and the pill read it). */
	public static JobBoardModel model() {
		return MODEL;
	}

	/** The user's zone, for "today". */
	static ZoneId zone() {
		return ZoneId.systemDefault();
	}

	/** QA: rows, today's totals and the needs-you count. */
	public static JsonObject json() {
		JsonObject o = new JsonObject();
		List<JobRow> rows = MODEL.rows();
		JsonArray arr = new JsonArray();
		for (JobRow r : rows) {
			JsonObject j = new JsonObject();
			j.addProperty("id", r.id());
			j.addProperty("mod", r.mod());
			j.addProperty("state", r.state().name().toLowerCase(java.util.Locale.ROOT));
			j.addProperty("title", r.title());
			if (r.parentId() != null) {
				j.addProperty("parent", r.parentId());
			}
			arr.add(j);
		}
		o.add("rows", arr);
		JobBoardModel.Totals t = JobBoardModel.totals(MODEL.ledger(), System.currentTimeMillis(), zone());
		o.addProperty("todayUsd", t.total());
		JsonObject by = new JsonObject();
		t.byMod().forEach(by::addProperty);
		o.add("todayByMod", by);
		o.addProperty("needsYou", JobBoardModel.needsYouCount(rows));
		o.addProperty("pill", JobBoardModel.pillText(rows));
		o.addProperty("key", keyLabel());
		return o;
	}
}
