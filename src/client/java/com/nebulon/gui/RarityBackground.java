package com.nebulon.gui;

import com.nebulon.client.NebulonClient;
import com.nebulon.config.RaritySettings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * "Rarity" (Render): draws a soft circle in the colour of an item's rarity behind it, in container
 * slots and the hotbar.
 * <p>
 * The rarity comes from Hypixel SkyBlock's lore line (e.g. "LEGENDARY SWORD", also recombobulated
 * ones); items without lore use the vanilla rarity, except "common" (otherwise every block would get
 * a white circle).
 */
public final class RarityBackground {
	private static final Pattern RARITY = Pattern.compile(
			"\\b(VERY SPECIAL|UNCOMMON|COMMON|RARE|EPIC|LEGENDARY|MYTHIC|DIVINE|SPECIAL|ULTIMATE|ADMIN)\\b");
	/** How many lines from the end of the lore are searched (rarity is the last line). */
	private static final int LINES_TO_CHECK = 3;
	public static final int NONE = 0;

	private RarityBackground() {
	}

	/** Called before an item is drawn at (x, y) in GUI units (16 x 16 item area). */
	public static void draw(GuiGraphicsExtractor g, ItemStack stack, int x, int y) {
		NebulonClient client = NebulonClient.get();
		if (client == null || stack.isEmpty()) {
			return;
		}
		RaritySettings settings = client.configManager().get().rarity;
		if (!settings.enabled) {
			return;
		}
		int rgb = rarityColor(stack);
		if (rgb == NONE) {
			return;
		}
		int alpha = Math.round(settings.opacity * 2.55F);
		int color = (alpha << 24) | rgb;
		if (settings.shape == RaritySettings.Shape.FULL_SLOT) {
			g.blit(RenderPipelines.GUI_TEXTURED, slotMask(), x, y, 0.0F, 0.0F, 16, 16, 16, 16, 16, 16, color);
		} else if (settings.shape == RaritySettings.Shape.GRADIENT) {
			g.blit(RenderPipelines.GUI_TEXTURED, gradientMask(), x, y, 0.0F, 0.0F, 16, 16, 16, 16, 16, 16, color);
		} else {
			// Rendered at the real screen resolution of a slot, so the circle is smooth at every GUI scale.
			int size = 16 * guiScale();
			g.blit(RenderPipelines.GUI_TEXTURED, circleMask(size), x, y, 0.0F, 0.0F, 16, 16, size, size, size, size, color);
		}
	}

	private static int guiScale() {
		return Math.max(1, (int) Math.round(net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScale()));
	}

	/**
	 * Masks in Minecraft's own pixel grid (16 x 16 per slot, hard pixel edges like the item sprites):
	 * a lighter fill with a stronger 1-pixel rim, the way vanilla frames its slots and icons.
	 */
	private static final double FILL = 0.6;
	private static final double RIM = 1.0;

	/** Smooth (anti-aliased) circle of {@code size} real pixels in one even colour, without a rim. */
	private static Identifier circleMask(int size) {
		double radius = size / 2.0 - 0.25;
		double c = size / 2.0;
		return MaskTextures.get("rarity:circle-flat:" + size, size, size, (px, py) -> {
			double d = Math.hypot(px + 0.5 - c, py + 0.5 - c);
			return Math.max(0.0, Math.min(1.0, radius - d + 0.5)) * FILL;
		});
	}

	/** Fades in from the top of the slot in 2-pixel bands; strongest at the bottom, with a full bottom line. */
	private static Identifier gradientMask() {
		return MaskTextures.get("rarity:gradient", 16, 16, (px, py) -> py == 15 ? RIM : (py / 2) / 7.0 * 0.85);
	}

	/** The whole slot with a rim. */
	private static Identifier slotMask() {
		return MaskTextures.get("rarity:slot", 16, 16, (px, py) -> px == 0 || py == 0 || px == 15 || py == 15 ? RIM : FILL);
	}

	/** The {@code 0xRRGGBB} colour of the item's rarity, or {@link #NONE}. */
	public static int rarityColor(ItemStack stack) {
		ItemLore lore = stack.get(DataComponents.LORE);
		if (lore != null && !lore.lines().isEmpty()) {
			int fromLore = skyBlockRarity(lore.lines());
			// Pets in the Pets menu have no rarity line; their name is coloured by rarity instead.
			return fromLore != NONE ? fromLore : petRarity(stack.getHoverName());
		}
		// Server items (custom name or custom data, e.g. SkyBlock menu buttons like the "Close" barrier)
		// only get a colour from their lore; vanilla would call a barrier "epic".
		if (stack.has(DataComponents.CUSTOM_NAME) || stack.has(DataComponents.CUSTOM_DATA)) {
			return NONE;
		}
		Rarity rarity = stack.getRarity();
		if (rarity == Rarity.COMMON) {
			return NONE;
		}
		return switch (rarity) {
			case UNCOMMON -> 0xFFFF55;
			case RARE -> 0x55FFFF;
			case EPIC -> 0xFF55FF;
			default -> NONE;
		};
	}

	/** Rarity from the last lore lines, e.g. "LEGENDARY SWORD" or "a MYTHIC DUNGEON BOOTS a". */
	static int skyBlockRarity(List<Component> lines) {
		int checked = 0;
		for (int i = lines.size() - 1; i >= 0 && checked < LINES_TO_CHECK; i--) {
			String text = ChatFormatting.stripFormatting(lines.get(i).getString());
			if (text == null || text.isBlank()) {
				continue;
			}
			checked++;
			String trimmed = text.trim();
			// The rarity line is written in capitals; normal lore sentences are not.
			if (!trimmed.equals(trimmed.toUpperCase(Locale.ROOT)) && !trimmed.matches("^\\S \\p{Lu}.*")) {
				continue;
			}
			Matcher m = RARITY.matcher(trimmed);
			if (m.find()) {
				return colorOf(m.group(1));
			}
		}
		return NONE;
	}

	/** "[Lvl 100] ...", optionally after a few symbols such as the favourite star "★ ". */
	private static final Pattern PET_NAME = Pattern.compile("^[^\\p{L}\\p{N}\\[]{0,4}\\[Lvl \\d+]");
	/** Colours of the legacy codes 0-9, a-f. */
	private static final int[] CODE_COLORS = {0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
			0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};
	/** Name colours that stand for a rarity (common white ... special red). */
	private static final java.util.Set<Integer> RARITY_COLORS = java.util.Set.of(
			0xFFFFFF, 0x55FF55, 0x5555FF, 0xAA00AA, 0xFFAA00, 0xFF55FF, 0x55FFFF, 0xFF5555);

	/**
	 * A pet's rarity from its name, e.g. "[Lvl 100] Ender Dragon" with "Ender Dragon" in gold =
	 * legendary: the colour of the last letters in the name (after the level and an optional
	 * "[108✦]" golden dragon level). Works with styled text and legacy colour codes.
	 */
	static int petRarity(Component name) {
		// Favourite pets start with "★ " before the level.
		if (!PET_NAME.matcher(ChatFormatting.stripFormatting(name.getString())).find()) {
			return NONE;
		}
		int[] last = {NONE};
		name.visit((style, text) -> {
			int color = style.getColor() != null ? style.getColor().getValue() : 0xFFFFFF;
			for (int i = 0; i < text.length(); i++) {
				char ch = text.charAt(i);
				if (ch == '§' && i + 1 < text.length()) {
					int code = Character.digit(text.charAt(i + 1), 16);
					if (code >= 0) {
						color = CODE_COLORS[code];
					} else if (Character.toLowerCase(text.charAt(i + 1)) == 'r') {
						color = 0xFFFFFF;
					}
					i++;
				} else if (Character.isLetter(ch)) {
					last[0] = color;
				}
			}
			return java.util.Optional.empty();
		}, net.minecraft.network.chat.Style.EMPTY);
		return RARITY_COLORS.contains(last[0]) ? last[0] : NONE;
	}

	static int colorOf(String rarity) {
		return switch (rarity) {
			case "COMMON" -> 0xFFFFFF;
			case "UNCOMMON" -> 0x55FF55;
			case "RARE" -> 0x5555FF;
			case "EPIC" -> 0xAA00AA;
			case "LEGENDARY" -> 0xFFAA00;
			case "MYTHIC" -> 0xFF55FF;
			case "DIVINE" -> 0x55FFFF;
			case "SPECIAL", "VERY SPECIAL" -> 0xFF5555;
			case "ULTIMATE", "ADMIN" -> 0xAA0000;
			default -> NONE;
		};
	}
}
