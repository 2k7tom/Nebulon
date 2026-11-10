package com.nebulon.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Root of {@code config/nebulon.json}: the settings of every module plus the panel layout of the
 * settings screen. Pure Java (no Minecraft classes) so it can be unit-tested.
 */
public class NebulonConfig {
	/** Version 2: module-based layout (version 1 was a flat single-module file). */
	public static final int CURRENT_VERSION = 2;
	public static final String DEFAULT_MENU_COLOR = "#FFFFFF";

	/** Same character rules as Minecraft's {@code Identifier} ("namespace:path"). */
	private static final Pattern ENTITY_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

	public int configVersion = CURRENT_VERSION;
	/** Category name -&gt; panel position in the settings screen. Missing entries get a default layout. */
	public Map<String, PanelPosition> panels = new LinkedHashMap<>();
	/** Ids of modules whose settings are open in the settings screen (kept when the screen is reopened). */
	public List<String> expandedModules = new ArrayList<>();
	/** Darkmode Mist (Render): darker wool, glass and snow in the Dwarven Mines. */
	public boolean darkmodeMist = false;
	/** Whether the old F8 default of "Open settings" was removed once (the settings open with /nebulon). */
	public boolean openSettingsF8Removed = false;
	/** Anti Mouse Reset (Settings): switching between menus keeps the cursor where it was. */
	public boolean antiMouseReset = false;
	/** Legacy: No Equip Animation was its own module; it is now a Held Item setting (taken over once on load). */
	public Boolean noEquipAnimation;
	/** Fullbright (Render): the world is drawn at full brightness everywhere. */
	public boolean fullbright = false;
	/** Scrollable Tooltips (Render): tooltips larger than the screen can be scrolled with the mouse wheel. */
	public boolean scrollableTooltips = false;
	/** Menu Customisation (Settings): general colour of the menu; white = the original neutral look. */
	public ColorSetting menuColor = new ColorSetting(DEFAULT_MENU_COLOR, false);
	/** Performance Display (Render): FPS/TPS/ping/CPS box on the HUD. */
	public PerformanceDisplaySettings performanceDisplay = new PerformanceDisplaySettings();
	/** Visual Tweaks (Render): recipe book, offhand slot and dark containers. */
	public VisualTweaksSettings visualTweaks = new VisualTweaksSettings();
	/** Particles (Render): hide block breaking, lightning, explosions, entity fire, smoke. */
	public ParticlesSettings particles = new ParticlesSettings();
	/** Rarity (Render): a circle in the rarity colour behind items. */
	public RaritySettings rarity = new RaritySettings();
	/** Protect Item (Settings): prevents dropping or selling important items. */
	public ProtectItemSettings protectItem = new ProtectItemSettings();
	/** Slot Binding (Settings): inventory slots bound to hotbar slots, swapped with Shift + click. */
	public SlotBindingSettings slotBinding = new SlotBindingSettings();
	/** Item Tweaks (Render): e.g. compact master stars in item names. */
	public ItemTweaksSettings itemTweaks = new ItemTweaksSettings();
	/** Auction Input (Render): price box with undercut mode instead of the auction sign. */
	public AuctionInputSettings auctionInput = new AuctionInputSettings();
	/** Item Prices (Render): Coflnet prices in item tooltips. */
	public ItemPricesSettings itemPrices = new ItemPricesSettings();
	/** Inventory Display (Farming): the inventory as a movable HUD element. */
	public InventoryDisplaySettings inventoryDisplay = new InventoryDisplaySettings();
	/** Enchant Display (Render): missing enchants and level colours in tooltips. */
	public EnchantDisplaySettings enchantDisplay = new EnchantDisplaySettings();
	/** Inferno Demonlord (Combat): Blaze slayer helpers. */
	public InfernoDemonlordSettings infernoDemonlord = new InfernoDemonlordSettings();
	/** Command Keybinds (Combat): keys for /loadouts, /pets and /stats. */
	public CommandKeybindsSettings commandKeybinds = new CommandKeybindsSettings();
	/** Held Item (Render): position, size and swing of the item in first person. */
	public HeldItemSettings heldItem = new HeldItemSettings();
	/** HUD Tweaks (Render): smaller fire, no damage tilt, no nausea. */
	public HudTweaksSettings hudTweaks = new HudTweaksSettings();
	/**
	 * Legacy: lists of earlier versions (saved custom mobs, kill log, preset ids). The custom mob
	 * list now only lives for the current session (see the client), so these are dropped on load
	 * (Gson does not write {@code null} fields).
	 */
	public Object bestiaryMobs;
	public Object killedMobs;
	public Object installedPresets;

	/**
	 * Repairs every invalid value in place.
	 *
	 * @return human-readable descriptions of all corrections (empty if the config was valid)
	 */
	public List<String> validate() {
		List<String> problems = new ArrayList<>();
		if (configVersion != CURRENT_VERSION) {
			problems.add("configVersion " + configVersion + " -> " + CURRENT_VERSION
					+ (configVersion < CURRENT_VERSION ? " (old single-module settings are replaced by defaults)" : ""));
			configVersion = CURRENT_VERSION;
		}
		if (panels == null) {
			panels = new LinkedHashMap<>();
		}
		panels.values().removeIf(p -> p == null);
		if (expandedModules == null) {
			expandedModules = new ArrayList<>();
		}
		expandedModules.removeIf(id -> id == null || id.startsWith("bestiary_group:"));
		ColorSetting validMenuColor = ColorSetting.validateColor("menuColor", menuColor, new ColorSetting(DEFAULT_MENU_COLOR, false), problems);
		if (validMenuColor.chroma) {
			problems.add("menuColor: chroma is not available for the menu -> off");
			validMenuColor.chroma = false;
		}
		menuColor = validMenuColor;
		if (performanceDisplay == null) {
			problems.add("performanceDisplay missing -> defaults");
			performanceDisplay = new PerformanceDisplaySettings();
		}
		performanceDisplay.validate(problems);
		if (visualTweaks == null) {
			problems.add("visualTweaks missing -> defaults");
			visualTweaks = new VisualTweaksSettings();
		}
		visualTweaks.validate(problems);
		if (particles == null) {
			problems.add("particles missing -> defaults");
			particles = new ParticlesSettings();
		}
		if (rarity == null) {
			problems.add("rarity missing -> defaults");
			rarity = new RaritySettings();
		}
		rarity.validate(problems);
		if (protectItem == null) {
			problems.add("protectItem missing -> defaults");
			protectItem = new ProtectItemSettings();
		}
		protectItem.validate(problems);
		if (slotBinding == null) {
			problems.add("slotBinding missing -> defaults");
			slotBinding = new SlotBindingSettings();
		}
		slotBinding.validate(problems);
		if (itemTweaks == null) {
			problems.add("itemTweaks missing -> defaults");
			itemTweaks = new ItemTweaksSettings();
		}
		if (auctionInput == null) {
			problems.add("auctionInput missing -> defaults");
			auctionInput = new AuctionInputSettings();
		}
		auctionInput.validate(problems);
		if (itemPrices == null) {
			problems.add("itemPrices missing -> defaults");
			itemPrices = new ItemPricesSettings();
		}
		if (inventoryDisplay == null) {
			problems.add("inventoryDisplay missing -> defaults");
			inventoryDisplay = new InventoryDisplaySettings();
		}
		inventoryDisplay.validate(problems);
		if (enchantDisplay == null) {
			problems.add("enchantDisplay missing -> defaults");
			enchantDisplay = new EnchantDisplaySettings();
		}
		enchantDisplay.validate(problems);
		if (infernoDemonlord == null) {
			problems.add("infernoDemonlord missing -> defaults");
			infernoDemonlord = new InfernoDemonlordSettings();
		}
		if (commandKeybinds == null) {
			problems.add("commandKeybinds missing -> defaults");
			commandKeybinds = new CommandKeybindsSettings();
		}
		if (heldItem == null) {
			problems.add("heldItem missing -> defaults");
			heldItem = new HeldItemSettings();
		}
		heldItem.validate(problems);
		if (hudTweaks == null) {
			problems.add("hudTweaks missing -> defaults");
			hudTweaks = new HudTweaksSettings();
		}
		if (noEquipAnimation != null) {
			if (noEquipAnimation) {
				heldItem.enabled = true;
				heldItem.noEquipAnimation = true;
			}
			noEquipAnimation = null;
		}
		if (bestiaryMobs != null || killedMobs != null || installedPresets != null) {
			problems.add("saved custom mob list removed (marked mobs are now forgotten on server change)");
			bestiaryMobs = null;
			killedMobs = null;
			installedPresets = null;
		}
		return problems;
	}

	/**
	 * Normalises an entity id: trims, lower-cases and adds the {@code minecraft:} namespace if missing.
	 *
	 * @return the normalised id, or {@code null} if it can never be a valid identifier
	 */
	public static String normalizeEntityId(String raw) {
		if (raw == null) {
			return null;
		}
		String id = raw.trim().toLowerCase(Locale.ROOT);
		if (id.isEmpty()) {
			return null;
		}
		if (id.indexOf(':') < 0) {
			id = "minecraft:" + id;
		}
		return ENTITY_ID.matcher(id).matches() ? id : null;
	}
}
