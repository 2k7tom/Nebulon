package com.nebulon.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nebulon.client.NebulonClient;
import com.nebulon.config.AuctionInputSettings;
import com.nebulon.gui.AuctionInputScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "Auction Input" (Render), ported from NoammAddons 1.3.0's Auction Price Input (CC0):
 * <ul>
 *     <li>Hypixel's "Your auction / starting bid" sign is replaced by a price box (accepts 10m, 5.5k,
 *     1,000,000 ...), with an Undercut mode that enters "lowest BIN minus the typed amount".</li>
 *     <li>Enter in "Create (BIN) Auction" / "Confirm (BIN) Auction" clicks the green create / confirm
 *     button.</li>
 * </ul>
 * Lowest BIN prices come from the public list NoammAddons uses (lb.tricked.dev), refreshed every
 * 10 minutes while the feature is on. Only that download is made; nothing about the player is sent.
 */
public final class AuctionInput {
	private static final String LOWEST_BINS_URL = "https://lb.tricked.dev/lowestbins";
	private static final long REFRESH_MS = 10L * 60L * 1000L;
	private static final long RETRY_MS = 60L * 1000L;

	private static final Set<String> GUI_NAMES = Set.of("Create BIN Auction", "Create Auction");
	private static final Set<String> CONFIRM_NAMES = Set.of("Confirm BIN Auction", "Confirm Auction");

	private static final Map<String, Long> lowestBins = new ConcurrentHashMap<>();
	private static volatile boolean fetching;
	private static long nextFetch;

	/** The item put up for auction (slot 13), remembered when its price button (slot 31) is clicked. */
	private static ItemStack item;
	/** Last mode and text of the price box ("Remember Mode" / "Remember Text"). */
	public static AuctionInputSettings.Mode lastMode;
	public static String lastInput = "";

	private AuctionInput() {
	}

	private static AuctionInputSettings settings() {
		return NebulonClient.get().configManager().get().auctionInput;
	}

	private static boolean enabled() {
		NebulonClient client = NebulonClient.get();
		return client != null && settings().enabled;
	}

	// ---- Lowest BIN prices -------------------------------------------------------------------------

	/** Client tick: downloads the lowest BIN list when the feature is on and the list is stale. */
	public static void tick() {
		if (enabled()) {
			requestLowestBins();
		}
	}

	/** Downloads the lowest BIN list if it is stale (also used by other features, e.g. Croesus). */
	public static void requestLowestBins() {
		if (fetching || System.currentTimeMillis() < nextFetch) {
			return;
		}
		fetching = true;
		nextFetch = System.currentTimeMillis() + RETRY_MS;
		HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NORMAL).build();
		HttpRequest request = HttpRequest.newBuilder(URI.create(LOWEST_BINS_URL)).timeout(Duration.ofSeconds(20))
				.header("User-Agent", "Nebulon").GET().build();
		http.sendAsync(request, HttpResponse.BodyHandlers.ofString()).whenComplete((response, error) -> {
			try {
				if (error == null && response.statusCode() == 200) {
					JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
					Map<String, Long> prices = new HashMap<>();
					for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
						prices.put(entry.getKey(), (long) entry.getValue().getAsDouble());
					}
					lowestBins.putAll(prices);
					nextFetch = System.currentTimeMillis() + REFRESH_MS;
				}
			} catch (RuntimeException ignored) {
				// Keep the old prices; retried after RETRY_MS.
			} finally {
				fetching = false;
			}
		});
	}

	/** Lowest BIN of a SkyBlock item id, or 0 if unknown. */
	public static long lowestBin(String itemId) {
		Long price = lowestBins.get(itemId);
		if (price == null) {
			price = lowestBins.get(toLowestBinId(itemId));
		}
		return price != null ? price : 0L;
	}

	/** Test hook: a fixed price, and no download that could replace it during the test. */
	public static void setLowestBinForTest(String itemId, long price) {
		nextFetch = Long.MAX_VALUE;
		lowestBins.put(itemId, price);
	}

	static String toLowestBinId(String itemId) {
		if (!itemId.startsWith("ENCHANTMENT_")) {
			return itemId;
		}
		int separator = itemId.lastIndexOf('_');
		if (separator < "ENCHANTMENT_".length() || separator == itemId.length() - 1) {
			return itemId;
		}
		String level = itemId.substring(separator + 1);
		if (!level.chars().allMatch(Character::isDigit)) {
			return itemId;
		}
		return "ENCHANTED_BOOK-" + itemId.substring("ENCHANTMENT_".length(), separator) + "-" + level;
	}

	/** SkyBlock item id like NoammAddons' {@code skyblockId}: pets as PET-TYPE-TIER, books as ENCHANTMENT_NAME_LEVEL. */
	public static String skyblockId(ItemStack stack) {
		if (stack.isEmpty()) {
			return "";
		}
		CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		String id = data.getString("id").orElse("").replace(':', '-');
		if (id.equals("PET")) {
			String petInfo = data.getString("petInfo").orElse("");
			if (!petInfo.isEmpty()) {
				try {
					JsonObject pet = JsonParser.parseString(petInfo).getAsJsonObject();
					return "PET-" + pet.get("type").getAsString() + "-" + pet.get("tier").getAsString();
				} catch (RuntimeException ignored) {
					return id;
				}
			}
		} else if (id.equals("ENCHANTED_BOOK")) {
			CompoundTag enchantments = data.getCompoundOrEmpty("enchantments");
			if (enchantments.keySet().size() == 1) {
				String enchant = enchantments.keySet().iterator().next();
				int level = enchantments.getIntOr(enchant, 0);
				if (level > 0) {
					return "ENCHANTMENT_" + enchant.toUpperCase(Locale.ROOT) + "_" + level;
				}
			}
		}
		return id;
	}

	// ---- Menus ---------------------------------------------------------------------------------------

	/** A click in a menu: clicking the price button (slot 31) of the create menu remembers the item. */
	public static void onContainerClick(Screen screen, int slotNum) {
		if (!enabled() || slotNum != 31 || !(screen instanceof AbstractContainerScreen<?> container)
				|| !GUI_NAMES.contains(screen.getTitle().getString())) {
			return;
		}
		var slots = container.getMenu().slots;
		if (slots.size() > 13) {
			ItemStack stack = slots.get(13).getItem();
			item = skyblockId(stack).isEmpty() ? null : stack.copy();
		}
	}

	/** Enter in the create / confirm menu clicks the green button; returns whether the key was used. */
	public static boolean onKey(AbstractContainerScreen<?> screen, int key) {
		if (!enabled() || (key != InputConstants.KEY_RETURN && key != InputConstants.KEY_NUMPADENTER)) {
			return false;
		}
		String title = screen.getTitle().getString();
		int slotId;
		Set<String> validNames;
		if (GUI_NAMES.contains(title)) {
			slotId = 29;
			validNames = GUI_NAMES;
		} else if (CONFIRM_NAMES.contains(title)) {
			slotId = 11;
			validNames = CONFIRM_NAMES;
		} else {
			return false;
		}
		var slots = screen.getMenu().slots;
		if (slotId >= slots.size()) {
			return false;
		}
		Slot slot = slots.get(slotId);
		ItemStack stack = slot.getItem();
		if (!stack.is(Blocks.DYED_TERRACOTTA.pick(DyeColor.GREEN).asItem()) || !validNames.contains(stack.getHoverName().getString())) {
			return false;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.gameMode == null || mc.player == null) {
			return false;
		}
		mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, slotId, 0, ContainerInput.PICKUP, mc.player);
		return true;
	}

	/**
	 * Hypixel asks for the price with a sign: opens the price box instead of the sign editor.
	 *
	 * @return whether the sign editor was replaced
	 */
	public static boolean onOpenSignEditor(BlockPos pos, boolean frontText) {
		Minecraft mc = Minecraft.getInstance();
		if (!enabled() || item == null || mc.level == null || !(mc.level.getBlockEntity(pos) instanceof SignBlockEntity sign)) {
			return false;
		}
		SignText text = sign.getText(frontText);
		String[] lines = new String[4];
		for (int i = 0; i < 4; i++) {
			lines[i] = text.getMessage(i, false).getString();
		}
		if (!isAuctionSign(lines)) {
			return false;
		}
		mc.gui.setScreen(new AuctionInputScreen(pos, frontText, lines, item));
		return true;
	}

	public static boolean isAuctionSign(String[] lines) {
		return lines[1].equals("^^^^^^^^^^^^^^^") && lines[2].equals("Your auction") && lines[3].equals("starting bid");
	}

	public static void setItemForTest(ItemStack stack) {
		item = stack;
	}

	// ---- Numbers -------------------------------------------------------------------------------------

	private static final long[] SUFFIX_VALUES = {1_000_000_000_000_000_000L, 1_000_000_000_000_000L, 1_000_000_000_000L,
			1_000_000_000L, 1_000_000L, 1_000L};
	private static final char[] SUFFIXES = {'e', 'p', 't', 'b', 'm', 'k'};

	/** "10m" = 10000000, "5.5k" / "5,5k" = 5500, "250000" = 250000; null if invalid (like NoammAddons). */
	public static Long parseCompact(String value) {
		String clean = value.replace(',', '.').toLowerCase(Locale.ROOT).trim();
		if (clean.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(clean);
		} catch (NumberFormatException ignored) {
			// Suffix or decimal below.
		}
		char last = clean.charAt(clean.length() - 1);
		for (int i = 0; i < SUFFIXES.length; i++) {
			if (SUFFIXES[i] == last) {
				try {
					return (long) (Double.parseDouble(clean.substring(0, clean.length() - 1)) * SUFFIX_VALUES[i]);
				} catch (NumberFormatException e) {
					return null;
				}
			}
		}
		return null;
	}

	/** Short number like NoammAddons' format: 1.2m, 350k, 999. */
	public static String formatShort(long value) {
		if (value < 0) {
			return "-" + formatShort(-value);
		}
		if (value < 1000) {
			return Long.toString(value);
		}
		for (int i = 0; i < SUFFIX_VALUES.length; i++) {
			if (value >= SUFFIX_VALUES[i]) {
				long truncated = value / (SUFFIX_VALUES[i] / 10);
				boolean decimal = truncated < 100 && truncated % 10 != 0;
				return (decimal ? Double.toString(truncated / 10.0) : Long.toString(truncated / 10)) + SUFFIXES[i];
			}
		}
		return Long.toString(value);
	}

	public static String formatComma(long value) {
		return String.format(Locale.US, "%,d", value);
	}
}
