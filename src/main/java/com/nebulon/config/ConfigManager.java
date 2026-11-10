package com.nebulon.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Loads, validates and saves {@link NebulonConfig} as pretty-printed JSON.
 * <p>
 * Default location: {@code <game directory>/config/nebulon.json}
 * (for the dev environment: {@code run/config/nebulon.json}).
 * <p>
 * Robustness rules:
 * <ul>
 *     <li>Missing file: defaults are written.</li>
 *     <li>Unreadable/corrupt JSON: the broken file is kept as {@code nebulon.json.broken} and defaults are used.</li>
 *     <li>Out-of-range values: repaired by {@link NebulonConfig#validate()} and written back.</li>
 *     <li>Saving writes a temporary file first and then replaces the old one, so a crash never leaves a half-written config.</li>
 * </ul>
 */
public final class ConfigManager {
	private static final Logger LOGGER = LoggerFactory.getLogger("nebulon");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private final Path path;
	private NebulonConfig config = new NebulonConfig();
	/** Incremented whenever the active config object changes; lets caches detect changes cheaply. */
	private int revision;

	public ConfigManager(Path path) {
		this.path = path;
	}

	public Path path() {
		return path;
	}

	public NebulonConfig get() {
		return config;
	}

	public int revision() {
		return revision;
	}

	/** Loads the file (creating it if needed). Never throws; falls back to defaults on any problem. */
	public NebulonConfig load() {
		NebulonConfig loaded = null;
		boolean needsSave = false;

		if (Files.isRegularFile(path)) {
			try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
				loaded = GSON.fromJson(reader, NebulonConfig.class);
				if (loaded == null) {
					LOGGER.warn("Config file {} is empty, using defaults.", path);
				}
			} catch (IOException | JsonParseException e) {
				LOGGER.error("Could not read config file {}, using defaults. The broken file is kept as {}.broken", path, path.getFileName(), e);
				backupBrokenFile();
			}
		} else {
			LOGGER.info("No config file found, creating {}", path);
		}

		if (loaded == null) {
			loaded = new NebulonConfig();
			needsSave = true;
		}

		List<String> problems = loaded.validate();
		if (!problems.isEmpty()) {
			for (String problem : problems) {
				LOGGER.warn("Config corrected: {}", problem);
			}
			needsSave = true;
		}

		config = loaded;
		revision++;

		if (needsSave) {
			save();
		}
		return config;
	}

	/**
	 * Call after mutating {@link #get()} directly (settings screen, key mappings) so that caches
	 * rebuild. Does not write the file; call {@link #save()} when the edit is finished.
	 */
	public void markDirty() {
		revision++;
	}

	public boolean save() {
		try {
			Path parent = path.toAbsolutePath().getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}
			Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
			try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
				GSON.toJson(config, writer);
			}
			try {
				Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
			}
			return true;
		} catch (IOException e) {
			LOGGER.error("Could not save config file {}", path, e);
			return false;
		}
	}

	private void backupBrokenFile() {
		try {
			Files.copy(path, path.resolveSibling(path.getFileName() + ".broken"), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			LOGGER.warn("Could not back up broken config file {}", path, e);
		}
	}
}
