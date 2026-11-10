package com.nebulon.client;

import com.nebulon.Nebulon;
import com.nebulon.config.ConfigManager;
import com.nebulon.gui.ClickGuiScreen;
import com.nebulon.gui.PerformanceDisplay;
import com.nebulon.hud.PerformanceStats;
import com.nebulon.keybind.NebulonKeybinds;
import com.nebulon.render.DarkmodeMist;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The only entrypoint of the mod (client). Wires configuration, the modules and the key mappings
 * together.
 * <p>
 * Fully client-side: the mod sends no packets of its own. The connection events below are only used
 * to clear local state when leaving a world or server.
 */
public class NebulonClient implements ClientModInitializer {
	private static NebulonClient instance;

	private final ConfigManager configManager =
			new ConfigManager(FabricLoader.getInstance().getConfigDir().resolve(Nebulon.MOD_ID + ".json"));
	private DarkmodeMist darkmodeMist;

	@Override
	public void onInitializeClient() {
		instance = this;
		migrateLegacyConfig();
		configManager.load();
		Nebulon.LOGGER.info("Nebulon config: {}", configManager.path().toAbsolutePath());

		NebulonKeybinds.register();
		darkmodeMist = new DarkmodeMist(configManager);
		DarkmodeMist.registerModels();
		PerformanceDisplay.register();
		com.nebulon.gui.Notifications.register();
		com.nebulon.gui.InventoryDisplay.register();
		// Protect Item: "/protectitem" toggles the held item, and the tooltip shows the protection.
		net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) ->
				dispatcher.register(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("protectitem").executes(ctx -> {
					if (!configManager.get().protectItem.enabled) {
						com.nebulon.gui.Notifications.push("Protect Item", "The feature is switched off.", com.nebulon.gui.Notifications.RED, 2500L);
						return 0;
					}
					com.nebulon.item.ProtectItem.protectHeld();
					return 1;
				})));
		// "/nebulon" opens the settings (client-side only; opened next tick, after the chat has closed).
		net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) ->
				dispatcher.register(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("nebulon").executes(ctx -> {
					openSettingsNextTick = true;
					return 1;
				})));
		net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register(
				(stack, tooltipContext, flag, lines) -> {
					com.nebulon.item.EnchantDisplay.addTooltip(stack, lines);
					com.nebulon.item.ProtectItem.addTooltip(stack, lines);
					com.nebulon.item.ItemPrices.addTooltip(stack, lines);
				});

		ClientTickEvents.END_CLIENT_TICK.register(this::onEndTick);

		// Never carry state over to another world, dimension or server.
		ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, level) -> resetAll());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> resetAll());
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> resetAll());

		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			NebulonKeybinds.migrateOpenSettingsKey(client, configManager.get());
			NebulonKeybinds.loadModOnlyKeys(client, configManager.get());
			configManager.markDirty();
			NebulonKeybinds.logConflicts(client);
		});
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> configManager.save());
	}

	/** The mod was called "MobScanner" before; take over its settings once ({@code mobscanner.json}). */
	private void migrateLegacyConfig() {
		Path legacy = FabricLoader.getInstance().getConfigDir().resolve("mobscanner.json");
		if (Files.isRegularFile(legacy) && !Files.exists(configManager.path())) {
			try {
				Files.copy(legacy, configManager.path());
				Nebulon.LOGGER.info("Took over the settings of the former MobScanner config {}", legacy);
			} catch (IOException e) {
				Nebulon.LOGGER.warn("Could not take over the former MobScanner config {}", legacy, e);
			}
		}
	}

	public static NebulonClient get() {
		return instance;
	}

	public ConfigManager configManager() {
		return configManager;
	}

	public DarkmodeMist darkmodeMist() {
		return darkmodeMist;
	}

	/** Call after any setting change (the config is saved soon after). */
	public void onSettingsChanged() {
		configManager.markDirty();
	}

	private void resetAll() {
		PerformanceStats.reset();
	}

	/** Set by "/nebulon": the settings screen opens on the next tick. */
	private static volatile boolean openSettingsNextTick;

	/** Command Keybinds: the key runs the command (only in game, not while a menu or the chat is open). */
	private void runCommandKey(Minecraft client, net.minecraft.client.KeyMapping key, String command) {
		while (key.consumeClick()) {
			if (configManager.get().commandKeybinds.enabled && client.gui.screen() == null && client.getConnection() != null) {
				client.getConnection().sendCommand(command);
			}
		}
	}

	private void onEndTick(Minecraft client) {
		com.nebulon.item.AuctionInput.tick();
		com.nebulon.slayer.InfernoDemonlord.tick(client);
		if (openSettingsNextTick) {
			openSettingsNextTick = false;
			client.gui.setScreen(new ClickGuiScreen());
		}
		while (NebulonKeybinds.openSettings.consumeClick()) {
			if (client.gui.screen() == null) {
				client.gui.setScreen(new ClickGuiScreen());
			}
		}
		runCommandKey(client, NebulonKeybinds.commandLoadouts, "loadouts");
		runCommandKey(client, NebulonKeybinds.commandPets, "pets");
		runCommandKey(client, NebulonKeybinds.commandStats, "stats");
		darkmodeMist.tick(client);
	}
}
