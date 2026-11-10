package com.nebulon.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nebulon.client.NebulonClient;
import com.nebulon.config.ItemPricesSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * "Item Prices" (Render): adds prices from Coflnet (sky.coflnet.com) to SkyBlock item tooltips.
 * <ul>
 *     <li>Bazaar items (Coflnet's list of bazaar tags): Insta-Buy and Insta-Sell price.</li>
 *     <li>All other items: the lowest BIN (cheapest Buy It Now auction); pets per rarity.</li>
 * </ul>
 * Prices are requested only for items whose tooltip is shown, and kept for 5 minutes. Only the
 * item id is sent to Coflnet, nothing about the player.
 */
public final class ItemPrices {
	private static final String API = "https://sky.coflnet.com/api/";
	private static final long PRICE_TTL_MS = 5L * 60L * 1000L;
	private static final long ERROR_RETRY_MS = 60L * 1000L;
	private static final long TAGS_TTL_MS = 60L * 60L * 1000L;
	private static final int MAX_IN_FLIGHT = 4;
	private static final long MIN_REQUEST_GAP_MS = 150L;

	private enum State {
		LOADING, OK, NONE, ERROR
	}

	/** A cached price: lowest BIN, or bazaar buy/sell. */
	private record Price(State state, boolean bazaar, double lowestBin, double buy, double sell, long time) {
	}

	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
			.followRedirects(HttpClient.Redirect.NORMAL).build();
	private static final Map<String, Price> cache = new ConcurrentHashMap<>();
	private static final AtomicInteger inFlight = new AtomicInteger();
	private static volatile long lastRequest;
	private static volatile Set<String> bazaarTags = Set.of();
	private static volatile long nextTagsFetch;
	private static volatile boolean fetchingTags;

	private ItemPrices() {
	}

	private static ItemPricesSettings settings() {
		return NebulonClient.get().configManager().get().itemPrices;
	}

	private static boolean enabled() {
		NebulonClient client = NebulonClient.get();
		return client != null && settings().enabled;
	}

	// ---- Tooltip -------------------------------------------------------------------------------------

	/** Item tooltip callback: appends the price lines. */
	public static void addTooltip(ItemStack stack, List<Component> lines) {
		if (!enabled() || stack.isEmpty() || lines.isEmpty()) {
			return;
		}
		ItemPricesSettings s = settings();
		String[] request = request(stack);
		if (request == null) {
			return;
		}
		refreshTags();
		boolean bazaar = bazaarTags.contains(request[0]);
		if ((bazaar && !s.showBazaar) || (!bazaar && !s.showLowestBin)) {
			return;
		}
		if (bazaarTags.isEmpty()) {
			// The bazaar list is still loading; without it, it is unknown which price to show.
			lines.add(label("Item Prices: ").append(Component.literal("Loading...").withStyle(ChatFormatting.DARK_GRAY)));
			return;
		}
		String key = (bazaar ? "bz:" : "bin:") + request[0] + request[1];
		Price price = cache.get(key);
		long now = System.currentTimeMillis();
		boolean stale = price == null || (price.state == State.ERROR ? now - price.time > ERROR_RETRY_MS
				: price.state != State.LOADING && now - price.time > PRICE_TTL_MS);
		if (stale) {
			fetch(key, request[0], request[1], bazaar, price);
		}
		price = cache.get(key);
		int count = stack.getCount();
		boolean total = s.showStackTotal && count > 1;
		if (price == null || price.state == State.LOADING) {
			lines.add(label(bazaar ? "Bazaar: " : "Lowest BIN: ").append(Component.literal("Loading...").withStyle(ChatFormatting.DARK_GRAY)));
		} else if (price.state == State.ERROR) {
			lines.add(label(bazaar ? "Bazaar: " : "Lowest BIN: ").append(Component.literal("unavailable").withStyle(ChatFormatting.DARK_GRAY)));
		} else if (price.state == State.NONE) {
			lines.add(label(bazaar ? "Bazaar: " : "Lowest BIN: ").append(Component.literal("no offers").withStyle(ChatFormatting.DARK_GRAY)));
		} else if (price.bazaar) {
			lines.add(priceLine("Bazaar Insta-Buy: ", price.buy, count, total));
			lines.add(priceLine("Bazaar Insta-Sell: ", price.sell, count, total));
		} else {
			lines.add(priceLine("Lowest BIN: ", price.lowestBin, count, total));
		}
	}

	private static net.minecraft.network.chat.MutableComponent label(String text) {
		return Component.literal(text).withStyle(ChatFormatting.GOLD);
	}

	private static Component priceLine(String label, double each, int count, boolean total) {
		var line = label(label).append(Component.literal(format(each)).withStyle(ChatFormatting.YELLOW));
		if (total) {
			line.append(Component.literal(" (" + count + "x: " + format(each * count) + ")").withStyle(ChatFormatting.GRAY));
		}
		return line;
	}

	/** Below 100,000 coins one decimal is kept (bazaar: 1,304.6); larger prices are whole coins: 485,000,000. */
	public static String format(double value) {
		if (value < 100_000 && Math.abs(value - Math.rint(value)) >= 0.05) {
			return String.format(Locale.US, "%,.1f", value);
		}
		return String.format(Locale.US, "%,d", Math.round(value));
	}

	// ---- Item ids --------------------------------------------------------------------------------

	/**
	 * Coflnet item tag and query of an item ({@code [tag, query]}), or null for items without a
	 * SkyBlock id. Pets are PET_TYPE with their rarity as a filter, enchanted books
	 * ENCHANTMENT_NAME_LEVEL.
	 */
	public static String[] request(ItemStack stack) {
		CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		String id = data.getString("id").orElse("");
		if (id.isEmpty()) {
			return null;
		}
		if (id.equals("PET")) {
			String petInfo = data.getString("petInfo").orElse("");
			try {
				JsonObject pet = JsonParser.parseString(petInfo).getAsJsonObject();
				return new String[]{"PET_" + pet.get("type").getAsString(), "?Rarity=" + pet.get("tier").getAsString()};
			} catch (RuntimeException e) {
				return null;
			}
		}
		if (id.equals("ENCHANTED_BOOK")) {
			CompoundTag enchantments = data.getCompoundOrEmpty("enchantments");
			if (enchantments.keySet().size() == 1) {
				String enchant = enchantments.keySet().iterator().next();
				int level = enchantments.getIntOr(enchant, 0);
				if (level > 0) {
					return new String[]{"ENCHANTMENT_" + enchant.toUpperCase(Locale.ROOT) + "_" + level, ""};
				}
			}
		}
		return new String[]{id, ""};
	}

	// ---- Requests --------------------------------------------------------------------------------

	private static void refreshTags() {
		if (fetchingTags || System.currentTimeMillis() < nextTagsFetch) {
			return;
		}
		fetchingTags = true;
		nextTagsFetch = System.currentTimeMillis() + ERROR_RETRY_MS;
		get(API + "items/bazaar/tags").whenComplete((body, error) -> {
			try {
				if (body != null) {
					JsonArray array = JsonParser.parseString(body).getAsJsonArray();
					Set<String> tags = new HashSet<>();
					for (JsonElement element : array) {
						tags.add(element.getAsString());
					}
					if (!tags.isEmpty()) {
						bazaarTags = Set.copyOf(tags);
						nextTagsFetch = System.currentTimeMillis() + TAGS_TTL_MS;
					}
				}
			} catch (RuntimeException ignored) {
				// Retried after ERROR_RETRY_MS.
			} finally {
				fetchingTags = false;
			}
		});
	}

	private static void fetch(String key, String tag, String query, boolean bazaar, Price old) {
		long now = System.currentTimeMillis();
		if (inFlight.get() >= MAX_IN_FLIGHT || now - lastRequest < MIN_REQUEST_GAP_MS) {
			return;
		}
		lastRequest = now;
		inFlight.incrementAndGet();
		// Keep showing the old price while it is refreshed.
		cache.put(key, old != null && old.state == State.OK ? new Price(State.OK, old.bazaar, old.lowestBin, old.buy, old.sell, now)
				: new Price(State.LOADING, bazaar, 0, 0, 0, now));
		String url = API + "item/price/" + URLEncoder.encode(tag, StandardCharsets.UTF_8) + (bazaar ? "/current" : "/bin") + query;
		get(url).whenComplete((body, error) -> {
			try {
				long time = System.currentTimeMillis();
				if (body == null) {
					cache.put(key, new Price(State.ERROR, bazaar, 0, 0, 0, time));
					return;
				}
				if (body.isEmpty()) {
					cache.put(key, new Price(State.NONE, bazaar, 0, 0, 0, time));
					return;
				}
				JsonObject json = JsonParser.parseString(body).getAsJsonObject();
				if (bazaar) {
					double buy = json.has("buy") ? json.get("buy").getAsDouble() : 0;
					double sell = json.has("sell") ? json.get("sell").getAsDouble() : 0;
					cache.put(key, new Price(buy <= 0 && sell <= 0 ? State.NONE : State.OK, true, 0, buy, sell, time));
				} else {
					double lowest = json.has("lowest") ? json.get("lowest").getAsDouble() : 0;
					cache.put(key, new Price(lowest <= 0 ? State.NONE : State.OK, false, lowest, 0, 0, time));
				}
			} catch (RuntimeException e) {
				cache.put(key, new Price(State.ERROR, bazaar, 0, 0, 0, System.currentTimeMillis()));
			} finally {
				inFlight.decrementAndGet();
			}
		});
	}

	/** GET; completes with the body, "" for 404 / 204 (no price), or null on any other failure. */
	private static java.util.concurrent.CompletableFuture<String> get(String url) {
		HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15))
				.header("User-Agent", "Nebulon").GET().build();
		return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString()).handle((response, error) -> {
			if (error != null || response == null) {
				return null;
			}
			int status = response.statusCode();
			if (status == 404 || status == 204) {
				return "";
			}
			return status == 200 ? response.body() : null;
		});
	}

	// ---- Test hooks ------------------------------------------------------------------------------

	public static void setBazaarTagsForTest(Set<String> tags) {
		bazaarTags = Set.copyOf(tags);
		nextTagsFetch = Long.MAX_VALUE;
	}

	public static void setLowestBinForTest(String tag, double lowest) {
		cache.put("bin:" + tag, new Price(State.OK, false, lowest, 0, 0, Long.MAX_VALUE / 2));
	}

	public static void setBazaarForTest(String tag, double buy, double sell) {
		cache.put("bz:" + tag, new Price(State.OK, true, 0, buy, sell, Long.MAX_VALUE / 2));
	}

	public static void clearForTest() {
		cache.clear();
		bazaarTags = Set.of();
		nextTagsFetch = 0;
	}
}
