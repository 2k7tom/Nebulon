package com.nebulon.config;

import java.util.ArrayList;
import java.util.List;

/** Converts the entity list setting to and from the single-line text used in the settings screen. */
public final class ConfigText {
	private ConfigText() {
	}

	public static String formatEntityList(List<String> ids) {
		return String.join(", ", ids);
	}

	/**
	 * Parses e.g. {@code minecraft:zombie, husk; skeleton}.
	 *
	 * @return the parsed ids, or {@code null} if at least one entry is not a valid identifier
	 */
	public static List<String> parseEntityList(String text) {
		List<String> result = new ArrayList<>();
		if (text == null || text.isBlank()) {
			return result;
		}
		for (String part : text.split("[,;\\s]+")) {
			if (part.isEmpty()) {
				continue;
			}
			String id = NebulonConfig.normalizeEntityId(part);
			if (id == null) {
				return null;
			}
			if (!result.contains(id)) {
				result.add(id);
			}
		}
		return result;
	}
}
