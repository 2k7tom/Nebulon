package com.nebulon.config;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * "Protect Item" (Settings): the settings of NoammAddons 1.3.0's Protect Item (CC0) - prevents
 * dropping or selling important items. Items are protected by their SkyBlock UUID (Protect Key /
 * {@code /protectitem}); starred and recombobulated items can be protected automatically.
 */
public class ProtectItemSettings {
	public boolean enabled = false;
	/** Shows a notification in the bottom right corner when an action was blocked. */
	public boolean protectNotification = true;
	/** Marks protected items in container menus with a small "P". */
	public boolean showProtected = false;
	public boolean protectUuid = true;
	public boolean protectStarred = true;
	public boolean protectRecombobulated = true;

	/** Protected item UUIDs (one specific item). */
	public Set<String> uuids = new LinkedHashSet<>();

	void validate(List<String> problems) {
		if (uuids == null) {
			uuids = new LinkedHashSet<>();
		}
		uuids.removeIf(s -> s == null || s.isBlank());
	}
}
