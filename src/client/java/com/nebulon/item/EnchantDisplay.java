package com.nebulon.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nebulon.Nebulon;
import com.nebulon.client.NebulonClient;
import com.nebulon.config.ColorUtil;
import com.nebulon.config.EnchantDisplaySettings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * "Enchant Display" (Render) for Hypixel SkyBlock items:
 * <ul>
 *     <li>Missing enchants: the normal (not ultimate) enchants the item type can get but does not have,
 *     in gray under the applied enchants. Enchants that conflict with an applied one are left out; of a
 *     conflict group with nothing applied only one is listed (e.g. Protection, not also Growth's
 *     rivals).</li>
 *     <li>Enchant colours: applied enchants at max level, one level below max and lower get their
 *     own colour (max level may be chroma). Ultimate enchants keep Hypixel's colour.</li>
 * </ul>
 * Data ({@code assets/nebulon/skyblock/enchants.json}): enchants per item type and conflict groups
 * from the NotEnoughUpdates repo, names and max levels from the SkyHanni repo.
 */
public final class EnchantDisplay {
	private static final Pattern RARITY_TYPE = Pattern.compile(
			"^(?:VERY SPECIAL|UNCOMMON|COMMON|RARE|EPIC|LEGENDARY|MYTHIC|DIVINE|SPECIAL|ULTIMATE|ADMIN)(?:\\s+(.+?))?\\s*$");
	private static final Pattern SEGMENT = Pattern.compile("^(.+?) ([IVXLCDM]+|\\d+)$");
	private static final Map<String, String> ALIASES = Map.of(
			"dragon_tracer", "aiming", "turbo_cocoa", "turbo_coco", "turbo_cacti", "turbo_cactus");
	/** Crop words in a farming tool's name -> its Turbo enchant. */
	private static final String[][] TURBO_BY_NAME = {
			{"wheat", "turbo_wheat"}, {"carrot", "turbo_carrot"}, {"potato", "turbo_potato"}, {"pumpkin", "turbo_pumpkin"},
			{"melon", "turbo_melon"}, {"cactus", "turbo_cactus"}, {"cane", "turbo_cane"}, {"wart", "turbo_warts"},
			{"coco", "turbo_coco"}, {"fungi", "turbo_mushrooms"}, {"mushroom", "turbo_mushrooms"}, {"rose", "turbo_rose"},
			{"sunflower", "turbo_sunflower"}, {"moonflower", "turbo_moonflower"}};
	private static final int NAMES_PER_LINE = 3;

	private record Enchant(String name, int max) {
	}

	private static Map<String, List<String>> categories;
	private static List<List<String>> pools;
	private static Map<String, Enchant> enchants;
	/** Lower-case lore name -> id (normal and stacking enchants). */
	private static Map<String, String> byLoreName;
	private static Set<String> ultimateNames;

	private EnchantDisplay() {
	}

	private static EnchantDisplaySettings settings() {
		return NebulonClient.get().configManager().get().enchantDisplay;
	}

	private static boolean enabled() {
		NebulonClient client = NebulonClient.get();
		return client != null && settings().enabled;
	}

	// ---- Data ---------------------------------------------------------------------------------------

	private static synchronized boolean load() {
		if (enchants != null) {
			return !enchants.isEmpty();
		}
		categories = new HashMap<>();
		pools = new ArrayList<>();
		enchants = new HashMap<>();
		byLoreName = new HashMap<>();
		ultimateNames = new HashSet<>();
		Identifier id = Identifier.fromNamespaceAndPath(Nebulon.MOD_ID, "skyblock/enchants.json");
		try (Reader reader = new InputStreamReader(Minecraft.getInstance().getResourceManager().open(id), StandardCharsets.UTF_8)) {
			JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
			for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("categories").entrySet()) {
				List<String> list = new ArrayList<>();
				e.getValue().getAsJsonArray().forEach(el -> list.add(el.getAsString()));
				categories.put(e.getKey(), list);
			}
			for (JsonElement pool : json.getAsJsonArray("pools")) {
				List<String> list = new ArrayList<>();
				pool.getAsJsonArray().forEach(el -> list.add(el.getAsString()));
				// On mining tools Fortune is the one to suggest of Silk Touch / Smelting Touch / Fortune.
				if (list.contains("fortune")) {
					list.remove("fortune");
					list.addFirst("fortune");
				}
				pools.add(list);
			}
			for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("enchants").entrySet()) {
				JsonObject o = e.getValue().getAsJsonObject();
				Enchant enchant = new Enchant(o.get("name").getAsString(), o.get("max").getAsInt());
				enchants.put(e.getKey(), enchant);
				byLoreName.put(enchant.name().toLowerCase(Locale.ROOT), e.getKey());
			}
			// Older names of renamed enchants (e.g. Syphon -> Drain) from their ids.
			for (String key : enchants.keySet()) {
				byLoreName.putIfAbsent(key.replace('_', ' '), key);
			}
			for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("ultimates").entrySet()) {
				ultimateNames.add(e.getValue().getAsString().toLowerCase(Locale.ROOT));
			}
		} catch (Exception e) {
			Nebulon.LOGGER.error("Enchant Display: could not load the enchant data", e);
			enchants.clear();
		}
		return !enchants.isEmpty();
	}

	public static int enchantCountForTest() {
		load();
		return enchants.size();
	}

	// ---- Tooltip ------------------------------------------------------------------------------------

	public static void addTooltip(ItemStack stack, List<Component> lines) {
		if (!enabled() || stack.isEmpty() || lines.size() < 2 || !stack.has(DataComponents.CUSTOM_DATA) || !load()) {
			return;
		}
		EnchantDisplaySettings s = settings();
		CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		CompoundTag enchantTag = data.getCompoundOrEmpty("enchantments");
		Set<String> applied = new HashSet<>();
		for (String key : enchantTag.keySet()) {
			applied.add(normalize(key));
		}

		int lastEnchantLine = -1;
		for (int i = 1; i < lines.size(); i++) {
			List<int[]> segments = enchantSegments(lines.get(i).getString());
			if (segments == null) {
				continue;
			}
			lastEnchantLine = i;
			lines.set(i, recolor(lines.get(i), segments, s));
		}

		// Only SkyBlock items (with an item id), not menu buttons.
		if (!s.showMissing || data.getString("id").orElse("").isEmpty()) {
			return;
		}
		String category = category(stack, lines, applied);
		if (category == null) {
			return;
		}
		List<String> missing = missing(category, applied, stack.getHoverName().getString().toLowerCase(Locale.ROOT));
		if (missing.isEmpty()) {
			return;
		}
		// The list itself only while Shift is held; otherwise a short hint.
		List<Component> extra = shiftDown() ? missingLines(missing)
				: List.of(Component.literal("Hold Shift for missing enchants").withStyle(ChatFormatting.DARK_GRAY));
		if (lastEnchantLine >= 0) {
			// After the enchant block (in the descriptive layout each enchant has description lines).
			int insert = lastEnchantLine + 1;
			while (insert < lines.size() && !lines.get(insert).getString().isBlank()) {
				insert++;
			}
			lines.addAll(insert, extra);
		} else {
			int rarityLine = rarityLine(lines);
			if (rarityLine < 0) {
				return;
			}
			List<Component> block = new ArrayList<>(extra);
			block.add(Component.empty());
			lines.addAll(rarityLine, block);
		}
	}

	private static String normalize(String id) {
		String lower = id.toLowerCase(Locale.ROOT);
		return ALIASES.getOrDefault(lower, lower);
	}

	/**
	 * If the line is a list of enchants ("Sharpness VII, Critical VI"), the character ranges of
	 * every segment as {start, end, level, max} (max = 0 for ultimate enchants); otherwise null.
	 */
	static List<int[]> enchantSegments(String text) {
		if (text.isBlank()) {
			return null;
		}
		List<int[]> result = new ArrayList<>();
		int start = 0;
		while (start < text.length()) {
			int comma = text.indexOf(", ", start);
			int end = comma < 0 ? text.length() : comma;
			String raw = text.substring(start, end);
			String segment = raw.trim();
			// Wrapped lists can end with a comma.
			if (segment.endsWith(",")) {
				segment = segment.substring(0, segment.length() - 1).trim();
			}
			Matcher m = SEGMENT.matcher(segment);
			if (!m.matches()) {
				return null;
			}
			String name = m.group(1).toLowerCase(Locale.ROOT);
			int level = parseLevel(m.group(2));
			int segStart = start + raw.indexOf(segment);
			String id = byLoreName.get(name);
			if (id != null) {
				result.add(new int[]{segStart, segStart + segment.length(), level, enchants.get(id).max()});
			} else if (ultimateNames.contains(name) || Character.isUpperCase(segment.charAt(0))) {
				// Ultimate or unknown (e.g. newly added) enchant: part of the list, colour unchanged.
				result.add(new int[]{segStart, segStart + segment.length(), level, 0, ultimateNames.contains(name) ? 1 : 0});
			} else {
				return null;
			}
			if (comma < 0) {
				break;
			}
			start = comma + 2;
		}
		// At least one known enchant (normal or ultimate), so ordinary lore lines are not taken for enchants.
		boolean known = result.stream().anyMatch(seg -> seg[3] > 0 || (seg.length > 4 && seg[4] == 1));
		return known ? result : null;
	}

	private static int parseLevel(String text) {
		if (Character.isDigit(text.charAt(0))) {
			return Integer.parseInt(text);
		}
		int total = 0;
		int previous = 0;
		for (int i = text.length() - 1; i >= 0; i--) {
			int value = switch (text.charAt(i)) {
				case 'I' -> 1;
				case 'V' -> 5;
				case 'X' -> 10;
				case 'L' -> 50;
				case 'C' -> 100;
				case 'D' -> 500;
				default -> 1000;
			};
			total += value < previous ? -value : value;
			previous = Math.max(previous, value);
		}
		return total;
	}

	/** The line with each normal enchant in its level colour; everything else keeps its style. */
	private static Component recolor(Component line, List<int[]> segments, EnchantDisplaySettings s) {
		List<String> chars = new ArrayList<>();
		List<Style> styles = new ArrayList<>();
		line.visit((style, text) -> {
			text.codePoints().forEach(cp -> {
				chars.add(new String(Character.toChars(cp)));
				styles.add(style);
			});
			return Optional.empty();
		}, Style.EMPTY);
		// Segment offsets are in chars of getString(); map them to code point indices.
		String plain = String.join("", chars);
		long now = Util.getMillis();
		for (int[] seg : segments) {
			if (seg[3] <= 0) {
				continue;
			}
			int from = plain.codePointCount(0, Math.min(plain.length(), seg[0]));
			int to = plain.codePointCount(0, Math.min(plain.length(), seg[1]));
			boolean max = seg[2] >= seg[3];
			boolean nearMax = !max && seg[2] == seg[3] - 1;
			boolean bold = max ? s.maxBold : nearMax ? s.nearMaxBold : s.normalBold;
			boolean italic = max ? s.maxItalic : nearMax ? s.nearMaxItalic : s.normalItalic;
			boolean strikethrough = max ? s.maxStrikethrough : nearMax ? s.nearMaxStrikethrough : s.normalStrikethrough;
			for (int i = from; i < to && i < styles.size(); i++) {
				int rgb;
				if (max) {
					rgb = s.maxColor.chroma ? ColorUtil.chroma(s.maxColor.rgb(), now - i * 80L) : s.maxColor.rgb();
				} else if (nearMax) {
					rgb = s.nearMaxColor.rgb();
				} else {
					rgb = s.normalColor.rgb();
				}
				styles.set(i, styles.get(i).withColor(rgb).withBold(bold).withItalic(italic).withStrikethrough(strikethrough));
			}
		}
		MutableComponent result = Component.empty();
		StringBuilder run = new StringBuilder();
		Style runStyle = null;
		for (int i = 0; i < chars.size(); i++) {
			if (runStyle != null && !styles.get(i).equals(runStyle)) {
				result.append(Component.literal(run.toString()).withStyle(runStyle));
				run.setLength(0);
			}
			runStyle = styles.get(i);
			run.append(chars.get(i));
		}
		if (runStyle != null) {
			result.append(Component.literal(run.toString()).withStyle(runStyle));
		}
		return result;
	}

	/**
	 * The rarity line without the obfuscated "a" Hypixel puts around recombobulated rarities
	 * ("a MYTHIC DUNGEON SWORD a" -> "MYTHIC DUNGEON SWORD"), or null if the line is no rarity line.
	 */
	private static String rarityText(Component line) {
		// Ignore whatever stands before / after the rarity (obfuscated recombobulator marks and similar).
		String text = line.getString().replaceAll("^[^A-Z]+", "").replaceAll("[^A-Z]+$", "");
		if (text.isEmpty() || !text.equals(text.toUpperCase(Locale.ROOT)) || !RARITY_TYPE.matcher(text).find()) {
			return null;
		}
		return text;
	}

	private static int rarityLine(List<Component> lines) {
		for (int i = lines.size() - 1; i >= 1; i--) {
			if (rarityText(lines.get(i)) != null) {
				return i;
			}
		}
		return -1;
	}

	/** Lore types that use another type's enchants. */
	private static final Map<String, String> TYPE_ALIASES = Map.of(
			"HOE", "FARMING TOOL", "SHORTBOW", "BOW", "LONGBOW", "BOW", "ROD", "FISHING ROD", "HATCHET", "AXE",
			"MASK", "HELMET", "HAT", "HELMET");

	/**
	 * The enchant category of the item, or null. SkyBlock items name their type in the rarity line
	 * ("SPECIAL DUNGEON HELMET"); only that counts, so player-head items such as Spirit Leap
	 * ("EPIC DUNGEON ITEM") are not taken for helmets. Only without a rarity line the Minecraft item
	 * decides (never for player heads).
	 */
	private static String category(ItemStack stack, List<Component> lines, Set<String> applied) {
		int index = rarityLine(lines);
		String type = null;
		if (index >= 0) {
			Matcher m = RARITY_TYPE.matcher(rarityText(lines.get(index)));
			if (m.find() && m.group(1) != null) {
				type = m.group(1).trim().replace("DUNGEON ", "").replace("SHINY ", "").trim();
				type = TYPE_ALIASES.getOrDefault(type, type);
			}
			if (type == null || !categories.containsKey(type)) {
				return null;
			}
		} else {
			if (stack.is(net.minecraft.world.item.Items.PLAYER_HEAD)) {
				return null;
			}
			type = vanillaType(stack);
			if (type == null) {
				return null;
			}
		}
		if (type.equals("AXE")) {
			boolean farming = applied.stream().anyMatch(id -> id.startsWith("turbo_") || id.equals("cultivating")
					|| id.equals("replenish") || id.equals("harvesting") || id.equals("dedication"));
			String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
			if (farming || name.contains("dicer") || name.contains("cocoa") || name.contains("coco ") || name.contains("fungi")) {
				type = "FARMING TOOL";
			}
		}
		return categories.containsKey(type) ? type : null;
	}

	/** Category from the Minecraft item itself (armor slot, sword, bow ...), for lore without a known type. */
	private static String vanillaType(ItemStack stack) {
		var equippable = stack.get(DataComponents.EQUIPPABLE);
		if (equippable != null) {
			switch (equippable.slot()) {
				case HEAD -> {
					return "HELMET";
				}
				case CHEST -> {
					return "CHESTPLATE";
				}
				case LEGS -> {
					return "LEGGINGS";
				}
				case FEET -> {
					return "BOOTS";
				}
				default -> {
				}
			}
		}
		if (stack.is(net.minecraft.tags.ItemTags.SWORDS)) {
			return "SWORD";
		}
		if (stack.is(net.minecraft.world.item.Items.BOW)) {
			return "BOW";
		}
		if (stack.is(net.minecraft.world.item.Items.FISHING_ROD)) {
			return "FISHING ROD";
		}
		if (stack.is(net.minecraft.tags.ItemTags.PICKAXES)) {
			return "PICKAXE";
		}
		if (stack.is(net.minecraft.tags.ItemTags.AXES)) {
			return "AXE";
		}
		if (stack.is(net.minecraft.tags.ItemTags.SHOVELS)) {
			return "SHOVEL";
		}
		if (stack.is(net.minecraft.tags.ItemTags.HOES)) {
			return "FARMING TOOL";
		}
		return null;
	}

	/** Test hook: behave as if Shift were held. */
	public static boolean forceShiftForTest;

	private static boolean shiftDown() {
		if (forceShiftForTest) {
			return true;
		}
		var window = Minecraft.getInstance().getWindow();
		return com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT)
				|| com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT);
	}

	/** Enchants of the category the item does not have, without conflicts and ultimates. */
	static List<String> missing(String category, Set<String> applied, String itemName) {
		Set<String> candidates = new LinkedHashSet<>();
		for (String id : categories.get(category)) {
			if (!applied.contains(id) && enchants.containsKey(id)) {
				candidates.add(id);
			}
		}
		for (List<String> pool : pools) {
			boolean anyApplied = pool.stream().anyMatch(applied::contains);
			if (anyApplied) {
				// Conflicts with an applied enchant: never suggest.
				pool.forEach(candidates::remove);
				continue;
			}
			List<String> inPool = pool.stream().filter(candidates::contains).toList();
			if (inPool.size() <= 1) {
				continue;
			}
			String keep = inPool.getFirst();
			if (inPool.getFirst().startsWith("turbo_")) {
				keep = null;
				for (String[] crop : TURBO_BY_NAME) {
					if (itemName.contains(crop[0]) && inPool.contains(crop[1])) {
						keep = crop[1];
						break;
					}
				}
			}
			for (String id : inPool) {
				if (!id.equals(keep)) {
					candidates.remove(id);
				}
			}
		}
		List<String> names = new ArrayList<>();
		for (String id : candidates) {
			names.add(enchants.get(id).name());
		}
		names.sort(String.CASE_INSENSITIVE_ORDER);
		return names;
	}

	private static List<Component> missingLines(List<String> names) {
		List<Component> result = new ArrayList<>();
		result.add(Component.literal("Missing Enchants:").withStyle(ChatFormatting.DARK_GRAY));
		for (int i = 0; i < names.size(); i += NAMES_PER_LINE) {
			List<String> part = names.subList(i, Math.min(names.size(), i + NAMES_PER_LINE));
			String text = String.join(", ", part) + (i + NAMES_PER_LINE < names.size() ? "," : "");
			result.add(Component.literal(text).withStyle(ChatFormatting.GRAY));
		}
		return result;
	}

	// ---- Preview (settings window) --------------------------------------------------------------------

	/** Every sword enchant (name, max level) without conflicts, alphabetical; Chimera V comes first. */
	private static final Object[][] PREVIEW_ENCHANTS = {
			{"Bane of Arthropods", 7}, {"Champion", 10}, {"Cleave", 6}, {"Critical", 7}, {"Cubism", 6}, {"Divine Gift", 3},
			{"Ender Slayer", 7}, {"Experience", 5}, {"Fire Aspect", 3}, {"First Strike", 5}, {"Gravity", 6}, {"Impaling", 5},
			{"Knockback", 2}, {"Lethality", 6}, {"Life Steal", 5}, {"Looting", 5}, {"Luck", 7}, {"Prosecute", 6},
			{"Pyroclasm", 6}, {"Scavenger", 6}, {"Sharpness", 7}, {"Smite", 7}, {"Smoldering", 5}, {"Tabasco", 3},
			{"Thunderlord", 7}, {"Titan Killer", 7}, {"Vampirism", 6}, {"Venomous", 7}, {"Vicious", 5}};

	private static String roman(int n) {
		int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
		String[] symbols = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < values.length; i++) {
			while (n >= values[i]) {
				sb.append(symbols[i]);
				n -= values[i];
			}
		}
		return sb.toString();
	}

	/**
	 * The example enchants for a tier: 7 = all at max, 6 = all one below max, 5 = two or three
	 * below max (alternating), never below level I; enchants that cannot be that low are left out.
	 */
	private static List<String> previewEnchants(int tier) {
		List<String> result = new ArrayList<>();
		for (int i = 0; i < PREVIEW_ENCHANTS.length; i++) {
			String name = (String) PREVIEW_ENCHANTS[i][0];
			int max = (Integer) PREVIEW_ENCHANTS[i][1];
			int below = tier >= 7 ? 0 : tier == 6 ? 1 : (i % 2 == 0 ? 2 : 3);
			int level = Math.max(1, max - below);
			if (max - level < Math.min(below, 2)) {
				continue;
			}
			result.add(name + " " + roman(level));
		}
		return result;
	}

	private static MutableComponent lit(String text, ChatFormatting... formats) {
		return Component.literal(text).withStyle(formats);
	}

	/**
	 * Tooltip of an example Withered Dark Claymore with every enchant at max level, styled with the
	 * current Enchant Parsing settings (as in game, also while the feature itself is off).
	 */
	public static List<Component> previewLines(int tier) {
		load();
		EnchantDisplaySettings s = settings();
		List<Component> lines = new ArrayList<>();
		lines.add(lit("Withered Dark Claymore ", ChatFormatting.LIGHT_PURPLE).append(lit("✪✪✪✪✪", ChatFormatting.GOLD))
				.append(lit("➎", ChatFormatting.RED)));
		lines.add(lit("Gear Score: ", ChatFormatting.GRAY).append(lit("1458 ", ChatFormatting.LIGHT_PURPLE)).append(lit("(5000)", ChatFormatting.DARK_GRAY)));
		lines.add(lit("Damage: ", ChatFormatting.GRAY).append(lit("+580 ", ChatFormatting.RED)).append(lit("(+30) ", ChatFormatting.YELLOW))
				.append(lit("(+3,392)", ChatFormatting.DARK_GRAY)));
		// Without the Hound pet's stats (Strength +37.2, Attack Speed, Ferocity, Speed).
		lines.add(lit("Strength: ", ChatFormatting.GRAY).append(lit("+360 ", ChatFormatting.RED)).append(lit("(+30) ", ChatFormatting.YELLOW))
				.append(lit("(+220) ", ChatFormatting.BLUE)).append(lit("(+2,420.42)", ChatFormatting.DARK_GRAY)));
		lines.add(lit("Crit Damage: ", ChatFormatting.GRAY).append(lit("+234% ", ChatFormatting.BLUE)).append(lit("(+24%) ", ChatFormatting.LIGHT_PURPLE))
				.append(lit("(+1,433.6%)", ChatFormatting.DARK_GRAY)));
		lines.add(lit("Swing Range: ", ChatFormatting.GRAY).append(lit("+2 ", ChatFormatting.YELLOW)).append(lit("(+2.1)", ChatFormatting.DARK_GRAY)));
		lines.add(lit("Gemstones: ", ChatFormatting.GRAY).append(lit("[", ChatFormatting.GOLD)).append(lit("⚔", ChatFormatting.DARK_GRAY))
				.append(lit("] [", ChatFormatting.GOLD)).append(lit("⚔", ChatFormatting.DARK_GRAY)).append(lit("]", ChatFormatting.GOLD)));
		lines.add(Component.empty());
		// Three per line like Hypixel; the first line starts with the ultimate enchant.
		List<String> names = previewEnchants(tier);
		List<List<String>> rows = new ArrayList<>();
		rows.add(new ArrayList<>(names.subList(0, Math.min(2, names.size()))));
		for (int i = 2; i < names.size(); i += NAMES_PER_LINE) {
			rows.add(names.subList(i, Math.min(names.size(), i + NAMES_PER_LINE)));
		}
		for (int i = 0; i < rows.size(); i++) {
			MutableComponent line = Component.empty();
			if (i == 0) {
				line.append(lit("Chimera V", ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)).append(lit(", ", ChatFormatting.BLUE));
			}
			String text = String.join(", ", rows.get(i)) + (i < rows.size() - 1 ? "," : "");
			line.append(lit(text, ChatFormatting.BLUE));
			List<int[]> segments = enchants == null || enchants.isEmpty() ? null : enchantSegments(line.getString());
			lines.add(segments != null ? recolor(line, segments, s) : line);
		}
		lines.add(Component.empty());
		lines.add(lit("That thing was too big to be called a", ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
		lines.add(lit("sword, it was more like a large hunk", ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
		lines.add(lit("of stone.", ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
		lines.add(Component.empty());
		lines.add(lit("Withered Bonus", ChatFormatting.BLUE));
		lines.add(lit("Grants ", ChatFormatting.GRAY).append(lit("+1❁ Strength ", ChatFormatting.RED)).append(lit("per ", ChatFormatting.GRAY))
				.append(lit("Catacombs", ChatFormatting.RED)));
		lines.add(lit("skill level you have.", ChatFormatting.GRAY));
		lines.add(Component.empty());
		lines.add(lit("* Co-op Soulbound *", ChatFormatting.DARK_GRAY));
		lines.add(Component.empty()
				.append(lit("a", ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD, ChatFormatting.OBFUSCATED))
				.append(lit(" MYTHIC DUNGEON LONGSWORD ", ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD))
				.append(lit("a", ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD, ChatFormatting.OBFUSCATED)));
		return lines;
	}

	public static List<String> missingForTest(String category, Set<String> applied, String itemName) {
		load();
		return missing(category, applied, itemName);
	}
}
