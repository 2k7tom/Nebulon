package com.nebulon.keybind;

import com.nebulon.Nebulon;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Key mappings, registered through Fabric's key mapping API. They are changed in Options &gt;
 * Controls under "Nebulon" and stored in Minecraft's {@code options.txt}.
 * <p>
 * Defaults: settings unbound (opened with /nebulon), L Protect Item (in menus), command keys unbound.
 */
public final class NebulonKeybinds {
	public static final KeyMapping.Category CATEGORY =
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Nebulon.MOD_ID, "main"));

	public static KeyMapping openSettings;
	/** Protect Item: protect / unprotect the hovered item in a menu (L, as in NoammAddons). */
	public static KeyMapping protectItem;
	/** Slot Binding: link the hovered slot with another one in the inventory (R, as in NoammAddons). */
	public static KeyMapping slotBinding;
	/** Command Keybinds: /loadouts, /pets and /stats (unbound by default). */
	public static KeyMapping commandLoadouts;
	public static KeyMapping commandPets;
	public static KeyMapping commandStats;

	private NebulonKeybinds() {
	}

	public static void register() {
		// Unbound by default: the settings open with /nebulon (a key can still be set in Options > Controls).
		openSettings = register("key.nebulon.open_settings", InputConstants.UNKNOWN.getValue());
		// Only used inside container menus, so sharing L with vanilla "Advancements" (only outside menus) is fine.
		protectItem = register("key.nebulon.protect_item", GLFW.GLFW_KEY_L);
		// Not registered: only set in the mod's settings (Inventory QOL > Slot Binding), not in Controls.
		slotBinding = new KeyMapping("key.nebulon.slot_binding", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);
		commandLoadouts = register("key.nebulon.command.loadouts", InputConstants.UNKNOWN.getValue());
		commandPets = register("key.nebulon.command.pets", InputConstants.UNKNOWN.getValue());
		commandStats = register("key.nebulon.command.stats", InputConstants.UNKNOWN.getValue());
	}

	private static KeyMapping register(String name, int key) {
		return KeyMappingHelper.registerKeyMapping(new KeyMapping(name, InputConstants.Type.KEYSYM, key, CATEGORY));
	}

	/**
	 * Once: F8 was the old default of "Open settings" and is stored in options.txt, so it is removed
	 * (the settings now open with /nebulon). A key the player set themselves later stays.
	 */
	public static void migrateOpenSettingsKey(Minecraft minecraft, com.nebulon.config.NebulonConfig config) {
		if (config.openSettingsF8Removed) {
			return;
		}
		config.openSettingsF8Removed = true;
		if ("key.keyboard.f8".equals(openSettings.saveString())) {
			openSettings.setKey(InputConstants.UNKNOWN);
			KeyMapping.resetMapping();
			minecraft.options.save();
		}
	}

	/** Whether a key is only set in the mod (stored in the mod's config, not in options.txt). */
	public static boolean isModOnly(KeyMapping mapping) {
		return mapping == slotBinding;
	}

	/**
	 * Loads the mod-only keys from the mod's config. The first time, the Slot Binding key is taken
	 * over from options.txt, where it was stored while it was still in Controls.
	 */
	public static void loadModOnlyKeys(Minecraft minecraft, com.nebulon.config.NebulonConfig config) {
		String key = config.slotBinding.key;
		if (key == null) {
			key = "key.keyboard.r";
			java.nio.file.Path options = minecraft.gameDirectory.toPath().resolve("options.txt");
			try {
				for (String line : java.nio.file.Files.readAllLines(options)) {
					if (line.startsWith("key_key.nebulon.slot_binding:")) {
						key = line.substring(line.indexOf(':') + 1).trim();
					}
				}
			} catch (java.io.IOException | RuntimeException ignored) {
				// No options.txt yet: the default key.
			}
			config.slotBinding.key = key;
		}
		try {
			slotBinding.setKey(InputConstants.getKey(key));
		} catch (RuntimeException e) {
			slotBinding.setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_R));
		}
		KeyMapping.resetMapping();
	}

	/** Stores a changed mod-only key in the mod's config. */
	public static void storeModOnlyKey(KeyMapping mapping, com.nebulon.config.NebulonConfig config) {
		if (mapping == slotBinding) {
			config.slotBinding.key = mapping.saveString();
		}
	}

	/** Logs a warning for every other key mapping that uses the same key as one of ours. */
	public static void logConflicts(Minecraft minecraft) {
		KeyMapping[] own = {openSettings, protectItem, slotBinding,
				commandLoadouts, commandPets, commandStats};
		for (KeyMapping mine : own) {
			if (mine == null || mine.isUnbound()) {
				continue;
			}
			for (KeyMapping other : minecraft.options.keyMappings) {
				// L for Protect Item only works in menus, so sharing it with Advancements is intended.
				boolean intended = mine == protectItem && other == minecraft.options.keyAdvancements;
				if (other != mine && other.same(mine) && !intended) {
					Nebulon.LOGGER.warn("Key conflict: '{}' and '{}' both use {}. Rebind one of them under Options > Controls.",
							mine.getName(), other.getName(), mine.getTranslatedKeyMessage().getString());
				}
			}
		}
	}
}
