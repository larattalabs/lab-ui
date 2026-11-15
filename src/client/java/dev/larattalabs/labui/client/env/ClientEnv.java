package dev.larattalabs.labui.client.env;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;

/**
 * Client environment switches (0.5.0; moved from Architect's and Steward's {@code ClientEnv}, identical apart from the package and
 * the screenshot folder's name). A switch is read from the system property (the name lower-cased with '.' for '_', e.g.
 * {@code -Darchitect.mute=0}) or else the environment variable. Each mod keeps its own names and defaults; this holds the reading.
 */
public final class ClientEnv {
	/** True in {@code gradlew runClient}; false for a jar in a normal launcher. */
	public static final boolean DEV_RUN = FabricLoader.getInstance().isDevelopmentEnvironment();

	private ClientEnv() {
	}

	public static @Nullable String raw(String envName) {
		String prop = System.getProperty(envName.toLowerCase(Locale.ROOT).replace('_', '.'));
		if (prop != null && !prop.isBlank()) {
			return prop.trim();
		}
		String env = System.getenv(envName);
		return env == null || env.isBlank() ? null : env.trim();
	}

	public static boolean flag(String envName, boolean def) {
		String v = raw(envName);
		if (v == null) {
			return def;
		}
		return switch (v.toLowerCase(Locale.ROOT)) {
			case "1", "true", "yes", "on" -> true;
			case "0", "false", "no", "off" -> false;
			default -> def;
		};
	}

	public static int intValue(String envName, int def) {
		String v = raw(envName);
		if (v == null) {
			return def;
		}
		try {
			return Integer.parseInt(v);
		} catch (NumberFormatException e) {
			return def;
		}
	}

	/**
	 * Where screenshots go: {@code overrideEnv} when set; in the dev layout (the game dir is &lt;repo&gt;/mod/run)
	 * &lt;repo&gt;/artifacts/shots; else &lt;gameDir&gt;/{@code fallbackDir} ("architect-shots", "steward-shots").
	 */
	public static Path shotsDir(String overrideEnv, String fallbackDir) {
		String override = raw(overrideEnv);
		if (override != null) {
			return Path.of(override).toAbsolutePath().normalize();
		}
		Path gameDir = Minecraft.getInstance().gameDirectory.toPath().toAbsolutePath().normalize();
		Path repo = gameDir.getParent() != null ? gameDir.getParent().getParent() : null;
		if (repo != null && Files.isDirectory(repo.resolve("mod")) && Files.isDirectory(repo.resolve("docs"))) {
			return repo.resolve("artifacts").resolve("shots");
		}
		return gameDir.resolve(fallbackDir);
	}
}
