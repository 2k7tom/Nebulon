package com.nebulon.gui;

import com.nebulon.client.NebulonClient;
import com.nebulon.gui.element.BoolElement;
import com.nebulon.gui.element.ButtonElement;
import com.nebulon.gui.element.ColorElement;
import com.nebulon.gui.element.Element;
import com.nebulon.gui.element.ModeElement;
import com.nebulon.gui.element.SliderElement;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;

/**
 * A module row with the module name.
 * <ul>
 *     <li>Switchable modules: left-click on/off (bright card, bold name when on), right-click
 *     expands the settings.</li>
 *     <li>Settings-only modules (e.g. Font): left- or right-click expands.</li>
 * </ul>
 * Which modules are expanded is stored in the config, so it survives closing the screen.
 */
public final class ModuleCard {
	private static final int SETTING_GAP = 3;

	private final String id;
	private final Component name;
	private final Component description;
	/** {@code null} for modules without an on/off state. */
	private final BooleanSupplier enabled;
	private final Runnable toggle;
	private final List<Element> elements;
	/** Right-click action instead of expanding (e.g. opens a settings window), or {@code null}. */
	private Runnable rightClick;

	public int x;
	public int y;
	public int width;

	private ModuleCard(String id, BooleanSupplier enabled, Runnable toggle, List<Element> elements) {
		this.id = id;
		this.name = Component.translatable("module.nebulon." + id);
		this.description = Component.translatable("module.nebulon." + id + ".description");
		this.enabled = enabled;
		this.toggle = toggle;
		this.elements = elements;
	}

	/** Settings > Menu Customisation: general colour of the menu (colour wheel without chroma). */
	public static ModuleCard menuCustomisation() {
		List<Element> elements = List.of(
				new ColorElement("option.nebulon.menu_color", () -> NebulonClient.get().configManager().get().menuColor, false),
				new ButtonElement("option.nebulon.reset_menu_color", () -> NebulonClient.get().configManager().get().menuColor =
						new com.nebulon.config.ColorSetting(com.nebulon.config.NebulonConfig.DEFAULT_MENU_COLOR, false)));
		return new ModuleCard("menu_customisation", null, null, elements);
	}

	/** Render > Performance Display: on/off, plus which values are shown. */
	public static ModuleCard performanceDisplay() {
		java.util.function.Supplier<com.nebulon.config.PerformanceDisplaySettings> s =
				() -> NebulonClient.get().configManager().get().performanceDisplay;
		List<Element> elements = List.of(
				new ModeElement<>("option.nebulon.display_layout", List.of(com.nebulon.config.PerformanceDisplaySettings.Layout.values()),
						v -> Component.translatable("option.nebulon.display_layout." + v.name().toLowerCase(Locale.ROOT)),
						() -> s.get().layout, v -> s.get().layout = v),
				new ModeElement<>("option.nebulon.text_depth", List.of(com.nebulon.config.PerformanceDisplaySettings.TextDepth.values()),
						v -> Component.translatable("option.nebulon.text_depth." + v.name().toLowerCase(Locale.ROOT)),
						() -> s.get().textDepth, v -> s.get().textDepth = v),
				new SliderElement("option.nebulon.background_opacity", 0, 100, " %",
						() -> s.get().backgroundOpacity, v -> s.get().backgroundOpacity = v),
				new BoolElement("option.nebulon.show_fps", () -> s.get().showFps, v -> s.get().showFps = v),
				new BoolElement("option.nebulon.show_tps", () -> s.get().showTps, v -> s.get().showTps = v),
				new BoolElement("option.nebulon.show_ping", () -> s.get().showPing, v -> s.get().showPing = v),
				new BoolElement("option.nebulon.show_cps", () -> s.get().showCps, v -> s.get().showCps = v),
				new BoolElement("option.nebulon.use_mod_font", () -> s.get().useModFont, v -> s.get().useModFont = v),
				new ButtonElement("option.nebulon.reset_display", () -> {
					s.get().scale = 1.0F;
					s.get().x = -1;
					s.get().y = -1;
				}));
		return new ModuleCard("performance_display", () -> s.get().enabled, () -> {
			s.get().enabled = !s.get().enabled;
			NebulonClient.get().onSettingsChanged();
		}, elements);
	}

	/**
	 * QOL > Inventory QOL: left-click on/off; right-click opens a window with the tabs Inventory
	 * (the inventory tweaks), Inventory Display and Slot Binding.
	 */
	public static ModuleCard inventoryQol() {
		java.util.function.Supplier<com.nebulon.config.VisualTweaksSettings> s =
				() -> NebulonClient.get().configManager().get().visualTweaks;
		ModuleCard card = new ModuleCard("visual_tweaks", () -> s.get().enabled, () -> {
			s.get().enabled = !s.get().enabled;
			NebulonClient.get().onSettingsChanged();
		}, List.of());
		card.rightClick = () -> {
			var mc = net.minecraft.client.Minecraft.getInstance();
			mc.gui.setScreen(new TabbedSettingsScreen(mc.gui.screen(), Component.translatable("module.nebulon.visual_tweaks"), List.of(
					new TabbedSettingsScreen.Tab(Component.translatable("screen.nebulon.inventory_qol.tab.inventory"),
							Component.translatable("screen.nebulon.inventory_qol.tab.inventory.desc"), inventoryElements()),
					new TabbedSettingsScreen.Tab(Component.translatable("module.nebulon.inventory_display"),
							Component.translatable("screen.nebulon.inventory_qol.tab.inventory_display.desc"), inventoryDisplayElements()),
					new TabbedSettingsScreen.Tab(Component.translatable("module.nebulon.slot_binding"),
							Component.translatable("screen.nebulon.inventory_qol.tab.slot_binding.desc"), slotBindingElements()))));
		};
		return card;
	}

	/** Inventory QOL > Inventory: recipe book, offhand, Custom Inventory and its slot colour, potion effects. */
	private static List<Element> inventoryElements() {
		java.util.function.Supplier<com.nebulon.config.VisualTweaksSettings> s =
				() -> NebulonClient.get().configManager().get().visualTweaks;
		return List.of(
				new BoolElement("option.nebulon.remove_recipe_book", () -> s.get().removeRecipeBook, v -> s.get().removeRecipeBook = v),
				new BoolElement("option.nebulon.remove_offhand", () -> s.get().removeOffhand, v -> s.get().removeOffhand = v),
				new BoolElement("option.nebulon.mod_style", () -> s.get().modStyle, v -> s.get().modStyle = v),
				new BoolElement("option.nebulon.hide_inventory_effects", () -> s.get().hideInventoryEffects, v -> s.get().hideInventoryEffects = v));
	}

	/** Settings > Protect Item: on/off, the Protect Key and NoammAddons' options. */
	public static ModuleCard protectItem() {
		java.util.function.Supplier<com.nebulon.config.ProtectItemSettings> s =
				() -> NebulonClient.get().configManager().get().protectItem;
		List<Element> elements = List.of(
				new com.nebulon.gui.element.KeybindElement("option.nebulon.protect.key", () -> com.nebulon.keybind.NebulonKeybinds.protectItem),
				new BoolElement("option.nebulon.protect.notification", () -> s.get().protectNotification, v -> s.get().protectNotification = v),
				new BoolElement("option.nebulon.protect.show_protected", () -> s.get().showProtected, v -> s.get().showProtected = v),
				new BoolElement("option.nebulon.protect.uuid", () -> s.get().protectUuid, v -> s.get().protectUuid = v),
				new BoolElement("option.nebulon.protect.starred", () -> s.get().protectStarred, v -> s.get().protectStarred = v),
				new BoolElement("option.nebulon.protect.recombobulated", () -> s.get().protectRecombobulated, v -> s.get().protectRecombobulated = v));
		return new ModuleCard("protect_item", () -> s.get().enabled, () -> {
			s.get().enabled = !s.get().enabled;
			NebulonClient.get().onSettingsChanged();
		}, elements);
	}

	/** Render > Item Tweaks: on/off, plus Compact Stars. */
	public static ModuleCard itemTweaks() {
		java.util.function.Supplier<com.nebulon.config.ItemTweaksSettings> s =
				() -> NebulonClient.get().configManager().get().itemTweaks;
		List<Element> elements = List.of(
				new BoolElement("option.nebulon.item_tweaks.compact_stars", () -> s.get().compactStars, v -> s.get().compactStars = v),
				new BoolElement("option.nebulon.item_tweaks.ultimate_enchant", () -> s.get().showUltimateEnchant, v -> s.get().showUltimateEnchant = v),
				new BoolElement("option.nebulon.item_tweaks.ultimate_inventory_only", () -> s.get().ultimateEnchantInventoryOnly,
						v -> s.get().ultimateEnchantInventoryOnly = v).visibleWhen(() -> s.get().showUltimateEnchant));
		return new ModuleCard("item_tweaks", () -> s.get().enabled, () -> {
			s.get().enabled = !s.get().enabled;
			NebulonClient.get().onSettingsChanged();
		}, elements);
	}

	/** Render > Auction Input: on/off, plus NoammAddons' Default Mode and Remember Input options. */
	public static ModuleCard auctionInput() {
		java.util.function.Supplier<com.nebulon.config.AuctionInputSettings> s =
				() -> NebulonClient.get().configManager().get().auctionInput;
		List<Element> elements = List.of(
				new ModeElement<>("option.nebulon.auction_input.default_mode", List.of(com.nebulon.config.AuctionInputSettings.Mode.values()),
						v -> Component.translatable("option.nebulon.auction_input.mode." + v.name().toLowerCase(Locale.ROOT)),
						() -> s.get().defaultMode, v -> s.get().defaultMode = v),
				new BoolElement("option.nebulon.auction_input.remember_text", () -> s.get().rememberText, v -> s.get().rememberText = v),
				new BoolElement("option.nebulon.auction_input.remember_mode", () -> s.get().rememberMode, v -> s.get().rememberMode = v),
				new BoolElement("option.nebulon.use_mod_font", () -> s.get().useModFont, v -> s.get().useModFont = v));
		return new ModuleCard("auction_input", () -> s.get().enabled, () -> {
			s.get().enabled = !s.get().enabled;
			NebulonClient.get().onSettingsChanged();
		}, elements);
	}

	/** Render > HUD Tweaks: smaller fire, no damage tilt, no nausea. */
	public static ModuleCard hudTweaks() {
		java.util.function.Supplier<com.nebulon.config.HudTweaksSettings> s =
				() -> NebulonClient.get().configManager().get().hudTweaks;
		List<Element> elements = List.of(
				new BoolElement("option.nebulon.hud_tweaks.low_fire", () -> s.get().lowFire, v -> s.get().lowFire = v),
				new BoolElement("option.nebulon.hud_tweaks.no_damage_tilt", () -> s.get().noDamageTilt, v -> s.get().noDamageTilt = v),
				new BoolElement("option.nebulon.hud_tweaks.no_nausea", () -> s.get().noNausea, v -> s.get().noNausea = v));
		return new ModuleCard("hud_tweaks", () -> s.get().enabled, () -> {
			s.get().enabled = !s.get().enabled;
			NebulonClient.get().onSettingsChanged();
		}, elements);
	}

	/** Render > Held Item: position, size, swing speed and swing style of the item in first person. */
	public static ModuleCard heldItem() {
		java.util.function.Supplier<com.nebulon.config.HeldItemSettings> s =
				() -> NebulonClient.get().configManager().get().heldItem;
		List<Element> elements = List.of(
				new SliderElement("option.nebulon.held_item.x", -100, 100, "", () -> s.get().offsetX, v -> s.get().offsetX = v),
				new SliderElement("option.nebulon.held_item.y", -100, 100, "", () -> s.get().offsetY, v -> s.get().offsetY = v),
				new SliderElement("option.nebulon.held_item.z", -100, 100, "", () -> s.get().offsetZ, v -> s.get().offsetZ = v),
				new SliderElement("option.nebulon.held_item.scale", 10, 200, " %", () -> s.get().scale, v -> s.get().scale = v),
				new SliderElement("option.nebulon.held_item.swing_speed", 10, 300, " %", () -> s.get().swingSpeed, v -> s.get().swingSpeed = v),
				new BoolElement("option.nebulon.held_item.ignore_haste", () -> s.get().ignoreHaste, v -> s.get().ignoreHaste = v),
				new BoolElement("option.nebulon.held_item.no_equip", () -> s.get().noEquipAnimation, v -> s.get().noEquipAnimation = v),
				new ModeElement<>("option.nebulon.held_item.swing_style", List.of(com.nebulon.config.HeldItemSettings.SwingStyle.values()),
						v -> Component.translatable("option.nebulon.held_item.swing_style." + v.name().toLowerCase(Locale.ROOT)),
						() -> s.get().swingStyle, v -> s.get().swingStyle = v),
				new ButtonElement("option.nebulon.held_item.reset", () -> {
					com.nebulon.config.HeldItemSettings d = new com.nebulon.config.HeldItemSettings();
					d.enabled = s.get().enabled;
					NebulonClient.get().configManager().get().heldItem = d;
				}));
		return new ModuleCard("held_item", () -> s.get().enabled, () -> {
			s.get().enabled = !s.get().enabled;
			NebulonClient.get().onSettingsChanged();
		}, elements);
	}

	/** Combat > Command Keybinds: one key each for /loadouts, /pets and /stats. */
	public static ModuleCard commandKeybinds() {
		java.util.function.Supplier<com.nebulon.config.CommandKeybindsSettings> s =
				() -> NebulonClient.get().configManager().get().commandKeybinds;
		List<Element> elements = List.of(
				new com.nebulon.gui.element.KeybindElement("option.nebulon.command_keybinds.loadouts", () -> com.nebulon.keybind.NebulonKeybinds.commandLoadouts),
				new com.nebulon.gui.element.KeybindElement("option.nebulon.command_keybinds.pets", () -> com.nebulon.keybind.NebulonKeybinds.commandPets),
				new com.nebulon.gui.element.KeybindElement("option.nebulon.command_keybinds.stats", () -> com.nebulon.keybind.NebulonKeybinds.commandStats));
		return new ModuleCard("command_keybinds", () -> s.get().enabled, () -> {
			s.get().enabled = !s.get().enabled;
			NebulonClient.get().onSettingsChanged();
		}, elements);
	}

	/** Combat > Inferno Demonlord: Blaze slayer helpers. */
	public static ModuleCard infernoDemonlord() {
		java.util.function.Supplier<com.nebulon.config.InfernoDemonlordSettings> s =
				() -> NebulonClient.get().configManager().get().infernoDemonlord;
		List<Element> elements = List.of(
				new BoolElement("option.nebulon.inferno.hide_fire", () -> s.get().hideFire, v -> s.get().hideFire = v),
				new BoolElement("option.nebulon.inferno.hide_potion", () -> s.get().hidePotionParticles, v -> s.get().hidePotionParticles = v),
				new BoolElement("option.nebulon.inferno.attunement", () -> s.get().attunementHighlight, v -> s.get().attunementHighlight = v),
				new BoolElement("option.nebulon.inferno.pillar_alert", () -> s.get().firePillarAlert, v -> s.get().firePillarAlert = v));
		return new ModuleCard("inferno_demonlord", () -> s.get().enabled, () -> {
			s.get().enabled = !s.get().enabled;
			NebulonClient.get().onSettingsChanged();
		}, elements);
	}

	/**
	 * QOL > Enchant Parsing: left-click on/off (applied enchants are then coloured by level); right-click
	 * opens a window with the tabs QOL (missing enchants), Tier 7 (max level, may be chroma), Tier 6
	 * (max - 1) and Tier 5 or sub, with a live example sword next to it.
	 */
	public static ModuleCard enchantDisplay() {
		java.util.function.Supplier<com.nebulon.config.EnchantDisplaySettings> s =
				() -> NebulonClient.get().configManager().get().enchantDisplay;
		ModuleCard card = new ModuleCard("enchant_display", () -> s.get().enabled, () -> {
			s.get().enabled = !s.get().enabled;
			NebulonClient.get().onSettingsChanged();
		}, List.of());
		card.rightClick = () -> {
			var mc = net.minecraft.client.Minecraft.getInstance();
			mc.gui.setScreen(new TabbedSettingsScreen(mc.gui.screen(), Component.translatable("module.nebulon.enchant_display"), List.of(
					tab("screen.nebulon.enchant_parsing.tab.qol", List.of(
							new BoolElement("option.nebulon.enchant_display.missing", () -> s.get().showMissing, v -> s.get().showMissing = v))),
					tab("screen.nebulon.enchant_parsing.tab.max", List.of(
							new ColorElement("option.nebulon.enchant_display.max_color", () -> s.get().maxColor, true),
							new BoolElement("option.nebulon.enchant_display.max_bold", () -> s.get().maxBold, v -> s.get().maxBold = v),
							new BoolElement("option.nebulon.enchant_display.max_italic", () -> s.get().maxItalic, v -> s.get().maxItalic = v),
							new BoolElement("option.nebulon.enchant_display.max_strikethrough", () -> s.get().maxStrikethrough,
									v -> s.get().maxStrikethrough = v))),
					tab("screen.nebulon.enchant_parsing.tab.near_max", List.of(
							new ColorElement("option.nebulon.enchant_display.near_max_color", () -> s.get().nearMaxColor, false),
							new BoolElement("option.nebulon.enchant_display.near_max_bold", () -> s.get().nearMaxBold, v -> s.get().nearMaxBold = v),
							new BoolElement("option.nebulon.enchant_display.near_max_italic", () -> s.get().nearMaxItalic, v -> s.get().nearMaxItalic = v),
							new BoolElement("option.nebulon.enchant_display.near_max_strikethrough", () -> s.get().nearMaxStrikethrough,
									v -> s.get().nearMaxStrikethrough = v))),
					tab("screen.nebulon.enchant_parsing.tab.normal", List.of(
							new ColorElement("option.nebulon.enchant_display.normal_color", () -> s.get().normalColor, false),
							new BoolElement("option.nebulon.enchant_display.normal_bold", () -> s.get().normalBold, v -> s.get().normalBold = v),
							new BoolElement("option.nebulon.enchant_display.normal_italic", () -> s.get().normalItalic, v -> s.get().normalItalic = v),
							new BoolElement("option.nebulon.enchant_display.normal_strikethrough", () -> s.get().normalStrikethrough,
									v -> s.get().normalStrikethrough = v)))),
					// Tabs QOL / Tier 7 show the max level sword, Tier 6 and Tier 5 or sub their levels.
					tab -> com.nebulon.item.EnchantDisplay.previewLines(tab == 2 ? 6 : tab == 3 ? 5 : 7)));
		};
		return card;
	}

	/** A tab named by {@code key}, with {@code key + ".desc"} as its tooltip. */
	private static TabbedSettingsScreen.Tab tab(String key, List<Element> elements) {
		return new TabbedSettingsScreen.Tab(Component.translatable(key), Component.translatable(key + ".desc"), elements);
	}

	/** Inventory QOL > Inventory Display: on/off, hotbar, Gui Editor button (fixed colours). */
	private static List<Element> inventoryDisplayElements() {
		java.util.function.Supplier<com.nebulon.config.InventoryDisplaySettings> s =
				() -> NebulonClient.get().configManager().get().inventoryDisplay;
		return List.of(
				new BoolElement("option.nebulon.inventory_display.enabled", () -> s.get().enabled, v -> s.get().enabled = v),
				new BoolElement("option.nebulon.inventory_display.hotbar", () -> s.get().showHotbar, v -> s.get().showHotbar = v)
						.visibleWhen(() -> s.get().enabled),
				new ButtonElement("option.nebulon.edit_gui", () -> {
					var mc = net.minecraft.client.Minecraft.getInstance();
					mc.gui.setScreen(new GuiEditorScreen(mc.gui.screen()));
				}).visibleWhen(() -> s.get().enabled));
	}

	/** Render > Item Prices: on/off, plus which Coflnet prices are shown in tooltips. */
	public static ModuleCard itemPrices() {
		java.util.function.Supplier<com.nebulon.config.ItemPricesSettings> s =
				() -> NebulonClient.get().configManager().get().itemPrices;
		List<Element> elements = List.of(
				new BoolElement("option.nebulon.item_prices.lowest_bin", () -> s.get().showLowestBin, v -> s.get().showLowestBin = v),
				new BoolElement("option.nebulon.item_prices.bazaar", () -> s.get().showBazaar, v -> s.get().showBazaar = v),
				new BoolElement("option.nebulon.item_prices.stack_total", () -> s.get().showStackTotal, v -> s.get().showStackTotal = v));
		return new ModuleCard("item_prices", () -> s.get().enabled, () -> {
			s.get().enabled = !s.get().enabled;
			NebulonClient.get().onSettingsChanged();
		}, elements);
	}

	/** Inventory QOL > Slot Binding: on/off, the Binding key and NoammAddons' rendering options. */
	private static List<Element> slotBindingElements() {
		java.util.function.Supplier<com.nebulon.config.SlotBindingSettings> s =
				() -> NebulonClient.get().configManager().get().slotBinding;
		return List.of(
				new BoolElement("option.nebulon.slot_binding.enabled", () -> s.get().enabled, v -> s.get().enabled = v),
				new com.nebulon.gui.element.KeybindElement("option.nebulon.slot_binding.key", () -> com.nebulon.keybind.NebulonKeybinds.slotBinding)
						.visibleWhen(() -> s.get().enabled),
				new BoolElement("option.nebulon.slot_binding.hover_only", () -> s.get().hoverOnly, v -> s.get().hoverOnly = v)
						.visibleWhen(() -> s.get().enabled),
				new BoolElement("option.nebulon.slot_binding.draw_border", () -> s.get().drawBorder, v -> s.get().drawBorder = v)
						.visibleWhen(() -> s.get().enabled),
				new BoolElement("option.nebulon.slot_binding.draw_line", () -> s.get().drawLine, v -> s.get().drawLine = v)
						.visibleWhen(() -> s.get().enabled),
				// One colour for the borders and the lines.
				new ColorElement("option.nebulon.slot_binding.border_color", () -> s.get().borderColor, false)
						.visibleWhen(() -> s.get().enabled && (s.get().drawBorder || s.get().drawLine)));
	}

	/** Render > Particles: on/off, plus which particles and effects are hidden. */
	public static ModuleCard particles() {
		java.util.function.Supplier<com.nebulon.config.ParticlesSettings> s =
				() -> NebulonClient.get().configManager().get().particles;
		List<Element> elements = List.of(
				new BoolElement("option.nebulon.hide_block_breaking", () -> s.get().hideBlockBreaking, v -> s.get().hideBlockBreaking = v),
				new BoolElement("option.nebulon.hide_lightning", () -> s.get().hideLightning, v -> s.get().hideLightning = v),
				new BoolElement("option.nebulon.hide_explosions", () -> s.get().hideExplosions, v -> s.get().hideExplosions = v),
				new BoolElement("option.nebulon.hide_entity_fire", () -> s.get().hideEntityFire, v -> s.get().hideEntityFire = v),
				new BoolElement("option.nebulon.hide_smoke", () -> s.get().hideSmoke, v -> s.get().hideSmoke = v));
		return new ModuleCard("particles", () -> s.get().enabled, () -> {
			s.get().enabled = !s.get().enabled;
			NebulonClient.get().onSettingsChanged();
		}, elements);
	}

	/** Render > Rarity: on/off, plus the opacity of the rarity circle. */
	public static ModuleCard rarity() {
		java.util.function.Supplier<com.nebulon.config.RaritySettings> s =
				() -> NebulonClient.get().configManager().get().rarity;
		List<Element> elements = List.of(
				new ModeElement<>("option.nebulon.rarity_shape", List.of(com.nebulon.config.RaritySettings.Shape.values()),
						v -> Component.translatable("option.nebulon.rarity_shape." + v.name().toLowerCase(Locale.ROOT)),
						() -> s.get().shape, v -> s.get().shape = v),
				new SliderElement("option.nebulon.opacity", com.nebulon.config.RaritySettings.MIN_OPACITY,
						com.nebulon.config.RaritySettings.MAX_OPACITY, " %", () -> s.get().opacity, v -> s.get().opacity = v));
		return new ModuleCard("rarity", () -> s.get().enabled, () -> {
			s.get().enabled = !s.get().enabled;
			NebulonClient.get().onSettingsChanged();
		}, elements);
	}

	/**
	 * A button-like module: left-click runs the action (e.g. opens a screen). It has no on/off state
	 * and no settings, so right-click does nothing.
	 */
	public static ModuleCard action(String id, Runnable action) {
		return new ModuleCard(id, null, action, List.of());
	}

	/** A module that is only switched on and off (left-click), without settings. */
	public static ModuleCard toggleOnly(String id, BooleanSupplier enabled, Runnable toggle) {
		return new ModuleCard(id, enabled, toggle, List.of());
	}

	public String id() {
		return id;
	}

	public Component description() {
		return description;
	}

	public List<Element> elements() {
		return elements;
	}

	private static List<String> expandedIds() {
		return NebulonClient.get().configManager().get().expandedModules;
	}

	public boolean isExpanded() {
		return !elements.isEmpty() && expandedIds().contains(id);
	}

	public void toggleExpanded() {
		if (isExpanded()) {
			expandedIds().remove(id);
			for (Element element : elements) {
				element.releaseFocus();
			}
		} else {
			expandedIds().add(id);
		}
	}

	private boolean isOn() {
		return enabled == null || enabled.getAsBoolean();
	}

	public boolean matches(String search) {
		return search.isEmpty() || name.getString().toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT));
	}

	public int height() {
		if (!isExpanded()) {
			return Theme.CARD_HEIGHT;
		}
		int h = Theme.CARD_HEIGHT + 5;
		for (Element element : elements) {
			if (element.isVisible()) {
				h += element.height() + SETTING_GAP;
			}
		}
		return h + 2;
	}

	/** Assigns positions to the card and its visible settings. */
	public void layout(int x, int y, int width) {
		this.x = x;
		this.y = y;
		this.width = width;
		int elementY = y + Theme.CARD_HEIGHT + 5;
		for (Element element : elements) {
			if (!element.isVisible()) {
				continue;
			}
			element.x = x + Theme.SETTING_INSET;
			element.y = elementY;
			element.width = width - 2 * Theme.SETTING_INSET;
			elementY += element.height() + SETTING_GAP;
		}
	}

	public boolean isOverHeader(double mouseX, double mouseY) {
		return Theme.inside(mouseX, mouseY, x, y, x + width, y + Theme.CARD_HEIGHT);
	}

	public void render(Canvas c, double mouseX, double mouseY) {
		boolean on = isOn();
		boolean expanded = isExpanded();
		boolean hovered = isOverHeader(mouseX, mouseY);
		int bottom = y + height();
		if (on) {
			c.roundRectBordered(x, y, x + width, bottom, 4, expanded ? Theme.CARD_EXPANDED_BG : Theme.CARD_BG_ENABLED, Theme.CARD_BORDER_ENABLED);
			if (expanded) {
				c.roundRect(x + 0.5, y + 0.5, x + width - 0.5, y + Theme.CARD_HEIGHT, 3.5,
						hovered ? Theme.CARD_BG_ENABLED_HOVER : Theme.CARD_BG_ENABLED);
			} else if (hovered) {
				c.roundRect(x + 0.5, y + 0.5, x + width - 0.5, bottom - 0.5, 3.5, Theme.CARD_BG_ENABLED_HOVER);
			}
		} else {
			c.roundRect(x, y, x + width, bottom, 4, hovered ? Theme.CARD_BG_HOVER : Theme.CARD_BG);
		}

		double centerY = y + Theme.CARD_HEIGHT / 2.0;
		int available = width - 7 - 6;
		// Names that do not fit scroll from left to right and back, like long setting labels.
		c.scrollingText(name, x + 7, centerY - 4, available, on ? Theme.TEXT : Theme.TEXT_DIM, on);

		if (!expanded) {
			return;
		}
		c.fill(x + 4, y + Theme.CARD_HEIGHT + 1, x + width - 4, y + Theme.CARD_HEIGHT + 1.5, Theme.SEPARATOR);
		for (Element element : elements) {
			if (element.isVisible()) {
				element.render(c, mouseX, mouseY);
			}
		}
	}

	/**
	 * Left-click: module on/off (settings-only modules: expand). Right-click: expand/collapse.
	 *
	 * @return whether the click hit the card header
	 */
	public boolean headerClicked(double mouseX, double mouseY, int button) {
		if (!isOverHeader(mouseX, mouseY)) {
			return false;
		}
		if (button == 0 && toggle != null) {
			toggle.run();
		} else if (button == 1 && rightClick != null) {
			rightClick.run();
		} else if ((button == 0 || button == 1) && !elements.isEmpty()) {
			toggleExpanded();
		}
		return true;
	}
}
