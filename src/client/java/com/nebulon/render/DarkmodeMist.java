package com.nebulon.render;

import com.nebulon.Nebulon;
import com.nebulon.client.NebulonClient;
import com.nebulon.config.ConfigManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * "Darkmode Mist" (QOL): while you are in the Dwarven Mines, white wool is drawn as gray wool,
 * white stained glass as connected gray glass, snow blocks and snow layers with a dark "Midnight"
 * snow texture (clay too) and ice with a dark "Graphite" ice texture. Mobs are not changed (e.g. the
 * Ghosts stay as Hypixel shows them).
 * <p>
 * Purely visual: only the block models draw differently (see {@link DarkMistModel}, works with
 * Fabric's renderer and with Sodium). The client world, collisions and everything the server sees
 * stay unchanged. The area is read from the tab list ("Area: Dwarven Mines", Hypixel SkyBlock) once a
 * second; when the effect turns on or off, the visible chunks are rebuilt once (like F3+A).
 */
public final class DarkmodeMist {
	public static final String AREA = "Dwarven Mines";
	private static final int CHECK_INTERVAL_TICKS = 20;

	/** Read by the chunk compile threads. */
	private static volatile boolean active;

	private final ConfigManager configManager;
	private int ticks;

	public DarkmodeMist(ConfigManager configManager) {
		this.configManager = configManager;
	}

	public static boolean active() {
		return active;
	}

	/** Wraps the affected block models; call once during client initialisation. */
	public static void registerModels() {
		DarkMistModel.register();
	}

	/** Visual replacement of a block state while the effect is active. */
	public static BlockState swap(BlockState state) {
		Block block = state.getBlock();
		if (block == Blocks.WOOL.pick(DyeColor.WHITE)) {
			return Blocks.WOOL.pick(DyeColor.GRAY).defaultBlockState();
		}
		if (block == Blocks.STAINED_GLASS.pick(DyeColor.WHITE)) {
			return Blocks.STAINED_GLASS.pick(DyeColor.GRAY).defaultBlockState();
		}
		return state;
	}

	/** White stained glass, drawn as connected gray glass while the effect is active. */
	public static boolean isMistGlass(BlockState state) {
		return state.is(Blocks.STAINED_GLASS.pick(DyeColor.WHITE));
	}

	/** Snow blocks and snow layers get the mod's dark "Midnight" snow texture (same shape). */
	public static boolean isSnow(BlockState state) {
		return state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.SNOW);
	}

	/** Clay blocks get the same dark "Midnight" snow texture as snow. */
	public static boolean isClay(BlockState state) {
		return state.is(Blocks.CLAY);
	}

	/** Ice gets the mod's dark "Graphite" ice texture (still see-through). */
	public static boolean isIce(BlockState state) {
		return state.is(Blocks.ICE);
	}

	/** Left-click on the module. */
	public void toggle() {
		configManager.get().darkmodeMist = !configManager.get().darkmodeMist;
		NebulonClient.get().onSettingsChanged();
		update(Minecraft.getInstance());
	}

	public void tick(Minecraft minecraft) {
		if (++ticks >= CHECK_INTERVAL_TICKS) {
			ticks = 0;
			update(minecraft);
		}
	}

	private void update(Minecraft minecraft) {
		boolean shouldBeActive = configManager.get().darkmodeMist && minecraft.level != null && AREA.equals(currentArea(minecraft));
		if (shouldBeActive != active) {
			active = shouldBeActive;
			Nebulon.LOGGER.info("Darkmode Mist {}", shouldBeActive ? "on (in the " + AREA + ")" : "off");
			if (minecraft.level != null) {
				minecraft.levelExtractor.allChanged(); // rebuild the chunk meshes once
			}
		}
	}

	/** The area from the tab list entry "Area: ..." or, in dungeons, "Dungeon: ..." (Hypixel SkyBlock), or {@code null}. */
	public static @Nullable String currentArea(Minecraft minecraft) {
		ClientPacketListener connection = minecraft.getConnection();
		if (connection == null) {
			return null;
		}
		for (PlayerInfo info : connection.getListedOnlinePlayers()) {
			Component name = info.getTabListDisplayName();
			if (name == null) {
				continue;
			}
			String text = ChatFormatting.stripFormatting(name.getString());
			if (text != null) {
				text = text.trim();
				if (text.startsWith("Area:")) {
					return text.substring("Area:".length()).trim();
				}
				if (text.startsWith("Dungeon:")) {
					return text.substring("Dungeon:".length()).trim();
				}
			}
		}
		return null;
	}
}
