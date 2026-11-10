package com.nebulon.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.nebulon.Nebulon;
import com.nebulon.client.NebulonClient;
import com.nebulon.config.VisualTweaksSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * "Visual Tweaks" (Render). All purely client-side and visual:
 * <ul>
 *     <li>Remove Recipebook: the recipe book button is not created and the book stays closed
 *     (see the RecipeBookComponent / AbstractRecipeBookScreen mixins).</li>
 *     <li>Remove Offhand: the offhand slot of the inventory is inactive (not drawn, not clickable)
 *     and its frame in the inventory texture is painted over.</li>
 *     <li>Mod Style: container textures ({@code textures/gui/container/...}) are replaced by recoloured
 *     versions generated at runtime from the textures in use (so they follow resource packs and no
 *     Mojang texture is shipped): flat like the mod's menu, in the chosen background / slot / border
 *     colours; labels get the text colour and the world behind is blurred.</li>
 * </ul>
 */
public final class VisualTweaks {
	private static final String CONTAINER_PREFIX = "textures/gui/container/";
	/** Background grey of vanilla container textures. */
	public static final int LIGHT_BACKGROUND = 0xFFC6C6C6;
	/** Vanilla container label colour (replaced by the Mod Style text colour). */
	public static final int LABEL_COLOR = 0xFF404040;
	/** Colour multiplier for the slot hover highlight in Mod Style (keeps it visible but calm). */
	public static final int SLOT_HIGHLIGHT_TINT = 0xFF6A6C72;

	/**
	 * Grey levels of the vanilla GUI palette: outline, slot shadow, panel shadow, slot fill,
	 * background, bevel highlight. Each is mapped to a Mod Style colour (see {@link #palette}); values
	 * in between are interpolated.
	 */
	private static final int[] GREYS = {0x00, 0x37, 0x55, 0x8B, 0xC6, 0xFF};

	private static final Map<Identifier, Identifier> DARK = new HashMap<>();
	/** The colours the cached textures were made with; a change regenerates them. */
	private static String paletteKey = "";

	private VisualTweaks() {
	}

	private static VisualTweaksSettings settings() {
		return NebulonClient.get().configManager().get().visualTweaks;
	}

	public static boolean removeRecipeBook() {
		VisualTweaksSettings s = settings();
		return s.enabled && s.removeRecipeBook;
	}

	public static boolean removeOffhand() {
		VisualTweaksSettings s = settings();
		return s.enabled && s.removeOffhand;
	}

	public static boolean hideInventoryEffects() {
		VisualTweaksSettings s = settings();
		return s.enabled && s.hideInventoryEffects;
	}

	public static boolean modStyle() {
		VisualTweaksSettings s = settings();
		return s.enabled && s.modStyle;
	}

	/** Mod Style applies while a container screen (inventory, chest, furnace ...) is open. */
	public static boolean styledContainerOpen() {
		return modStyle() && Minecraft.getInstance().gui.screen() instanceof AbstractContainerScreen<?>;
	}

	/** Background colour to paint over removed parts of a container texture. */
	public static int backgroundColor() {
		return styledContainerOpen() ? 0xFF000000 | BACKGROUND : LIGHT_BACKGROUND;
	}

	/** Slot colour of Custom Inventory ({@code 0xAARRGGBB}). */
	public static int slotColor() {
		return 0xFF000000 | SLOT;
	}

	/** Container label colour in Mod Style. */
	public static int labelColor() {
		return 0xFF000000 | TEXT;
	}

	/** Output colours ({@code 0xRRGGBB}) for {@link #GREYS}: flat panel and slots with a border. */
	private static int[] palette() {
		int background = BACKGROUND;
		int slot = SLOT;
		int border = BORDER;
		return new int[]{border, mix(slot, 0x000000, 0.25F), background, slot, background, background};
	}

	/** Regenerates the textures when the colours changed. */
	private static void checkPalette() {
		String key = "custom/" + removeOffhand();
		if (!key.equals(paletteKey)) {
			clearDarkTextures();
			paletteKey = key;
		}
	}

	/** The mod's base colours of Custom Inventory: background, border and text ({@code 0xRRGGBB}). */
	private static final int BACKGROUND = 0x1B1B1E;
	private static final int BORDER = 0x4A4A52;
	private static final int TEXT = 0xE4E4E8;
	/** Custom Inventory: only the background panel of inventories / containers is see-through (10 %). */
	private static final float INVENTORY_BACKGROUND_OPACITY = 0.10F;
	/** Buttons, sliders, text fields and tabs in menus: dark grey at 65 % opacity. */
	private static final int BUTTON = 0x2A2A30;
	/** Slot colour of Custom Inventory (inventory slots, hotbar). */
	private static final int SLOT = 0x1F1F22;
	private static final float WIDGET_OPACITY = 0.65F;
	/** The background of the mod's own screens (settings menu, Gui Editor ...): 10 %, so the world stays visible. */
	private static final float MOD_MENU_BACKGROUND_OPACITY = 0.10F;
	private static final Map<Identifier, Identifier> MOD_MENU = new HashMap<>();

	/** Opacity of menu backgrounds and the hotbar panel: opaque (only the inventory background is see-through). */
	private static float opacity() {
		return 1.0F;
	}

	/** Opacity of buttons, other widgets and slots: opaque. */
	private static float buttonOpacity() {
		return 1.0F;
	}

	/** Mod Style for every menu (pause menu, options, title screen ...): while any screen is open. */
	public static boolean styledMenuOpen() {
		return modStyle() && Minecraft.getInstance().gui.screen() != null;
	}

	/**
	 * The Mod Style version of a texture: container textures while a container is open, menu
	 * backgrounds and separators in every menu; otherwise the texture itself.
	 */
	public static Identifier styledTexture(Identifier texture) {
		if (!"minecraft".equals(texture.getNamespace())) {
			return texture;
		}
		String path = texture.getPath();
		if (path.startsWith(CONTAINER_PREFIX)) {
			if (!styledContainerOpen()) {
				return texture;
			}
			checkPalette();
			return DARK.computeIfAbsent(texture, VisualTweaks::createDark);
		}
		if (path.startsWith("textures/gui/") && styledMenuOpen()) {
			String name = path.substring("textures/gui/".length());
			// The mod's own screens (settings menu, Gui Editor ...) keep the world visible behind them.
			boolean modMenu = Minecraft.getInstance().gui.screen().getClass().getName().startsWith("com.nebulon.");
			if (modMenu && name.contains("menu_background")) {
				int c = (Math.round(MOD_MENU_BACKGROUND_OPACITY * 255) << 24)
						| (name.contains("list") ? mix(BACKGROUND, 0, 0.25F) : BACKGROUND);
				return MOD_MENU.computeIfAbsent(texture, t -> solid(t, c, "mod_menu/"));
			}
			// All of Minecraft's menus (title screen, worlds, servers, options, escape menu ...) keep
			// their vanilla background and separators.
		}
		return texture;
	}

	/** A small single-colour texture standing in for a menu background / separator. */
	private static Identifier solid(Identifier texture, int argb) {
		return solid(texture, argb, "dark/");
	}

	private static Identifier solid(Identifier texture, int argb, String prefix) {
		NativeImage image = new NativeImage(16, 16, true);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				image.setPixel(x, y, argb);
			}
		}
		Identifier id = Identifier.fromNamespaceAndPath(Nebulon.MOD_ID, prefix + texture.getPath());
		Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "nebulon style " + texture, image));
		return id;
	}

	/**
	 * Draws a menu widget sprite (button, slider, text field, tab, scroller) in Mod Style.
	 *
	 * @return whether the sprite was drawn (vanilla must not draw it)
	 */
	public static boolean drawWidget(net.minecraft.client.gui.GuiGraphicsExtractor g, Identifier sprite, int x, int y, int w, int h, int tint) {
		if (!"minecraft".equals(sprite.getNamespace()) || !styledMenuOpen()) {
			return false;
		}
		String path = sprite.getPath();
		if (!path.startsWith("widget/")) {
			return false;
		}
		int background = BACKGROUND;
		// Buttons and tabs: the mod's dark grey (not the slot colour).
		int slot = BUTTON;
		int border = BORDER;
		int text = TEXT;
		int fill;
		int edge;
		switch (path.substring("widget/".length())) {
			case "button" -> {
				fill = slot;
				edge = border;
			}
			case "button_highlighted" -> {
				fill = mix(slot, border, 0.45F);
				edge = text;
			}
			case "button_disabled" -> {
				fill = background;
				edge = mix(border, background, 0.5F);
			}
			case "slider", "slider_highlighted", "scroller_background" -> {
				fill = mix(background, 0, 0.3F);
				edge = path.endsWith("highlighted") ? text : border;
			}
			case "slider_handle", "scroller" -> {
				fill = border;
				edge = border;
			}
			case "slider_handle_highlighted" -> {
				fill = text;
				edge = text;
			}
			case "text_field" -> {
				fill = mix(background, 0, 0.35F);
				edge = border;
			}
			case "text_field_highlighted" -> {
				fill = mix(background, 0, 0.35F);
				edge = text;
			}
			case "tab", "tab_highlighted" -> {
				fill = background;
				edge = path.endsWith("highlighted") ? text : border;
			}
			case "tab_selected", "tab_selected_highlighted" -> {
				fill = slot;
				edge = path.endsWith("highlighted") ? text : border;
			}
			default -> {
				return false;
			}
		}
		float tintAlpha = (tint >>> 24) / 255.0F;
		boolean opaqueFill = path.startsWith("widget/slider_handle") || path.equals("widget/scroller");
		int fillAlpha = Math.round(255 * tintAlpha * (opaqueFill ? 1 : WIDGET_OPACITY));
		int edgeAlpha = Math.round(255 * tintAlpha);
		double radius = Math.min(3.0, Math.min(w, h) / 2.0);
		roundedShape(g, x, y, w, h, radius, (fillAlpha << 24) | fill, edge != fill ? (edgeAlpha << 24) | edge : 0);
		return true;
	}

	/**
	 * A rounded rectangle at the real screen resolution (anti-aliased, like the mod's menu), in GUI
	 * coordinates; {@code border} 0 = none.
	 */
	private static void roundedShape(net.minecraft.client.gui.GuiGraphicsExtractor g, double x, double y, double w, double h,
			double radius, int fill, int border) {
		Minecraft mc = Minecraft.getInstance();
		int guiScale = Math.max(1, mc.getWindow().getGuiScale());
		g.pose().pushMatrix();
		g.pose().scale(1.0F / guiScale, 1.0F / guiScale);
		com.nebulon.gui.Canvas c = new com.nebulon.gui.Canvas(g, mc.font, guiScale);
		if ((fill >>> 24) != 0) {
			c.roundRect(x, y, x + w, y + h, radius, fill);
		}
		if (border != 0) {
			c.roundRectOutline(x, y, x + w, y + h, radius, border);
		}
		g.pose().popMatrix();
	}

	/**
	 * The hovered-slot highlight in containers: a soft rounded square instead of vanilla's square
	 * sprites (the back one is drawn, the front one over the item is left out).
	 */
	public static boolean drawSlotHighlight(net.minecraft.client.gui.GuiGraphicsExtractor g, Identifier sprite, int x, int y) {
		if (!"minecraft".equals(sprite.getNamespace()) || !styledContainerOpen()) {
			return false;
		}
		if (sprite.getPath().equals("container/slot_highlight_back")) {
			int text = TEXT;
			// The sprite is 24 x 24 around the 16 x 16 slot.
			roundedShape(g, x + 4, y + 4, 16, 16, SLOT_RADIUS, 0x40000000 | text, 0x90000000 | text);
			return true;
		}
		return sprite.getPath().equals("container/slot_highlight_front");
	}

	/**
	 * The hotbar in Mod Style: a rounded panel with rounded slots, a rounded selection and rounded
	 * offhand slot, instead of vanilla's sprites.
	 *
	 * @return whether the sprite was drawn
	 */
	public static boolean drawHotbar(net.minecraft.client.gui.GuiGraphicsExtractor g, Identifier sprite, int x, int y) {
		if (!"minecraft".equals(sprite.getNamespace()) || !modStyle() || !sprite.getPath().startsWith("hud/hotbar")) {
			return false;
		}
		int panelAlpha = Math.round(255 * opacity()) << 24;
		int slotAlpha = Math.round(255 * buttonOpacity()) << 24;
		int background = panelAlpha | BACKGROUND;
		int border = 0xFF000000 | BORDER;
		int slot = slotAlpha | SLOT;
		int text = TEXT;
		switch (sprite.getPath()) {
			case "hud/hotbar" -> {
				roundedShape(g, x, y, 182, 22, 5, background, border);
				for (int i = 0; i < 9; i++) {
					roundedShape(g, x + 3 + i * 20, y + 3, 16, 16, SLOT_RADIUS, slot, 0);
				}
			}
			case "hud/hotbar_selection" -> roundedShape(g, x + 3, y + 3, 18, 18, SLOT_RADIUS + 0.5, 0x38000000 | text, 0xE0000000 | text);
			case "hud/hotbar_offhand_left" -> {
				roundedShape(g, x, y + 1, 22, 22, 5, background, border);
				roundedShape(g, x + 3, y + 4, 16, 16, SLOT_RADIUS, slot, 0);
			}
			case "hud/hotbar_offhand_right" -> {
				roundedShape(g, x + 7, y + 1, 22, 22, 5, background, border);
				roundedShape(g, x + 10, y + 4, 16, 16, SLOT_RADIUS, slot, 0);
			}
			default -> {
				return false;
			}
		}
		return true;
	}

	/** A generated dark copy of a GUI sprite: texture id and size. */
	public record DarkSprite(Identifier texture, int width, int height) {
	}

	private static final Map<Identifier, java.util.Optional<DarkSprite>> DARK_SPRITES = new HashMap<>();

	/**
	 * The dark copy of a container GUI sprite (e.g. creative inventory tabs and scroller, anvil
	 * name field), or {@code null} if the sprite is not a plain stretched container sprite (sprites
	 * with an {@code .mcmeta} for nine-slice, tiling or animation are left as they are).
	 */
	public static DarkSprite styledSprite(Identifier sprite) {
		if (!"minecraft".equals(sprite.getNamespace()) || !sprite.getPath().startsWith("container/")) {
			return null;
		}
		checkPalette();
		return DARK_SPRITES.computeIfAbsent(sprite, VisualTweaks::createDarkSprite).orElse(null);
	}

	private static java.util.Optional<DarkSprite> createDarkSprite(Identifier sprite) {
		Identifier file = sprite.withPath("textures/gui/sprites/" + sprite.getPath() + ".png");
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.getResourceManager().getResource(file.withSuffix(".mcmeta")).isPresent()) {
			return java.util.Optional.empty();
		}
		NativeImage image = readDark(file);
		if (image == null) {
			return java.util.Optional.empty();
		}
		Identifier dark = Identifier.fromNamespaceAndPath(Nebulon.MOD_ID, "dark/" + file.getPath());
		DarkSprite result = new DarkSprite(dark, image.getWidth(), image.getHeight());
		minecraft.getTextureManager().register(dark, new StyledTexture("nebulon style " + sprite, image));
		return java.util.Optional.of(result);
	}

	/** Forget the generated textures (after a resource reload they are created again on use). */
	public static void clearDarkTextures() {
		Minecraft minecraft = Minecraft.getInstance();
		for (Identifier dark : DARK.values()) {
			if (dark.getNamespace().equals(Nebulon.MOD_ID)) {
				minecraft.getTextureManager().release(dark);
			}
		}
		DARK.clear();
		for (java.util.Optional<DarkSprite> sprite : DARK_SPRITES.values()) {
			sprite.ifPresent(s -> minecraft.getTextureManager().release(s.texture()));
		}
		DARK_SPRITES.clear();
	}

	/** Generated textures are this many times the resolution of the vanilla ones. */
	private static final int HI = 4;
	/** Corner radius of the redrawn slots, in GUI pixels. */
	private static final double SLOT_RADIUS = 2.25;

	/** Smooth (linear, clamped) sampling, so the high-resolution textures look clean at every GUI scale. */
	private static final class StyledTexture extends DynamicTexture {
		StyledTexture(String name, NativeImage image) {
			super(() -> name, image);
			this.sampler = com.mojang.blaze3d.systems.RenderSystem.getSamplerCache()
					.getClampToEdge(com.mojang.blaze3d.textures.FilterMode.LINEAR);
		}
	}

	/**
	 * Reads a texture and turns it into Mod Style at {@link #HI} times the resolution, or
	 * {@code null} if it cannot be read:
	 * <ol>
	 *     <li>every pixel is recoloured (palette, opacity);</li>
	 *     <li>vanilla slots (dark top-left edge, light bottom-right edge, grey fill) are found and
	 *     removed;</li>
	 *     <li>the image is upscaled with Scale2x twice, which rounds the stair-stepped corners;</li>
	 *     <li>the slots are drawn back as anti-aliased rounded squares.</li>
	 * </ol>
	 */
	private static NativeImage readDark(Identifier texture) {
		try (InputStream in = Minecraft.getInstance().getResourceManager().open(texture); NativeImage source = NativeImage.read(in)) {
			int[] palette = palette();
			int w = source.getWidth();
			int h = source.getHeight();
			int[] original = new int[w * h];
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					original[y * w + x] = source.getPixel(x, y);
				}
			}
			// Black areas (e.g. behind the player in the inventory) are not outlines: a dark background tone.
			int blackFill = mix(BACKGROUND, 0x000000, 0.45F);
			float opacity = INVENTORY_BACKGROUND_OPACITY;
			int[] mapped = new int[w * h];
			for (int i = 0; i < mapped.length; i++) {
				int argb = original[i];
				if ((argb >>> 24) == 0) {
					mapped[i] = argb;
					continue;
				}
				int rgb = argb & 0xFFFFFF;
				boolean blackArea = rgb == 0 && inBlackBlock(original, w, h, i % w, i / w);
				int color = blackArea ? blackFill : stylePixel(rgb, palette);
				int alpha = argb >>> 24;
				// The background panel gets the opacity (slots are drawn back opaque below); outlines and coloured details stay opaque.
				if (opacity < 1 && (blackArea || (isGreyish(rgb) && greyOf(rgb) >= GREYS[1]))) {
					alpha = Math.round(alpha * opacity);
				}
				mapped[i] = (alpha << 24) | color;
			}

			int panelAlpha = Math.round(255 * opacity);
			int panel = (panelAlpha << 24) | BACKGROUND;
			int slotColor = (Math.round(255 * buttonOpacity()) << 24) | SLOT;
			java.util.List<int[]> slots = findSlots(original, w, h);
			// Remove Offhand: the offhand slot of the inventory (frame at 76, 61) is not drawn back.
			java.util.List<int[]> hidden = new java.util.ArrayList<>();
			if (removeOffhand() && texture.getPath().endsWith("container/inventory.png")) {
				for (int[] s : slots) {
					if (s[0] == 76 && s[1] == 61) {
						hidden.add(s);
					}
				}
			}
			for (int[] s : slots) {
				for (int y = s[1]; y < s[1] + s[3] + 2; y++) {
					for (int x = s[0]; x < s[0] + s[2] + 2; x++) {
						if (!isSlotIcon(original, w, s, x, y)) {
							mapped[y * w + x] = panel;
						}
					}
				}
			}

			int[] hi = scale2x(scale2x(mapped, w, h), w * 2, h * 2);
			int hw = w * HI;
			slots.removeAll(hidden);
			for (int[] s : slots) {
				double x0 = (s[0] + 1) * HI;
				double y0 = (s[1] + 1) * HI;
				double x1 = x0 + s[2] * HI;
				double y1 = y0 + s[3] * HI;
				double r = SLOT_RADIUS * HI;
				for (int y = (int) y0; y < (int) y1; y++) {
					for (int x = (int) x0; x < (int) x1; x++) {
						double coverage = roundRectCoverage(x, y, x0, y0, x1, y1, r);
						if (coverage > 0) {
							hi[y * hw + x] = blend(hi[y * hw + x], slotColor, coverage);
						}
					}
				}
				// Icons printed into slots (e.g. bottles of the brewing stand) are kept.
				for (int y = s[1] + 1; y < s[1] + 1 + s[3]; y++) {
					for (int x = s[0] + 1; x < s[0] + 1 + s[2]; x++) {
						if (isSlotIcon(original, w, s, x, y)) {
							for (int dy = 0; dy < HI; dy++) {
								for (int dx = 0; dx < HI; dx++) {
									hi[(y * HI + dy) * hw + x * HI + dx] = mapped[y * w + x];
								}
							}
						}
					}
				}
			}

			NativeImage image = new NativeImage(hw, h * HI, true);
			for (int y = 0; y < h * HI; y++) {
				for (int x = 0; x < hw; x++) {
					image.setPixel(x, y, hi[y * hw + x]);
				}
			}
			return image;
		} catch (Exception e) {
			Nebulon.LOGGER.warn("Could not create the Mod Style version of {}", texture, e);
			return null;
		}
	}

	private static boolean isVanillaGrey(int argb, int value) {
		return (argb >>> 24) == 0xFF && (argb & 0xFFFFFF) == ((value << 16) | (value << 8) | value);
	}

	/** A non-fill pixel inside a slot (part of a printed icon). */
	private static boolean isSlotIcon(int[] original, int w, int[] slot, int x, int y) {
		boolean interior = x > slot[0] && x <= slot[0] + slot[2] && y > slot[1] && y <= slot[1] + slot[3];
		int argb = original[y * w + x];
		return interior && (argb >>> 24) != 0 && !isVanillaGrey(argb, 0x8B);
	}

	/**
	 * Vanilla slots in a texture: a dark (0x373737) top / left edge, a light (0xFFFFFF) bottom /
	 * right edge and a grey (0x8B8B8B) inside. Returns {x, y, innerWidth, innerHeight} with (x, y) the
	 * top-left corner of the frame.
	 */
	private static java.util.List<int[]> findSlots(int[] o, int w, int h) {
		java.util.List<int[]> result = new java.util.ArrayList<>();
		for (int y = 0; y + 3 < h; y++) {
			for (int x = 0; x + 3 < w; x++) {
				if (!isVanillaGrey(o[y * w + x], 0x37) || !isVanillaGrey(o[y * w + x + 1], 0x37)
						|| !isVanillaGrey(o[(y + 1) * w + x], 0x37) || !isVanillaGrey(o[(y + 1) * w + x + 1], 0x8B)) {
					continue;
				}
				int iw = 0;
				while (x + 1 + iw < w && isVanillaGrey(o[(y + 1) * w + x + 1 + iw], 0x8B)) {
					iw++;
				}
				int ih = 0;
				while (y + 1 + ih < h && isVanillaGrey(o[(y + 1 + ih) * w + x + 1], 0x8B)) {
					ih++;
				}
				if (iw < 8 || ih < 8 || x + iw + 1 >= w || y + ih + 1 >= h) {
					continue;
				}
				int top = 0;
				int bottom = 0;
				for (int i = 0; i < iw; i++) {
					top += isVanillaGrey(o[y * w + x + 1 + i], 0x37) ? 1 : 0;
					bottom += isVanillaGrey(o[(y + ih + 1) * w + x + 1 + i], 0xFF) ? 1 : 0;
				}
				int left = 0;
				int right = 0;
				for (int i = 0; i < ih; i++) {
					left += isVanillaGrey(o[(y + 1 + i) * w + x], 0x37) ? 1 : 0;
					right += isVanillaGrey(o[(y + 1 + i) * w + x + iw + 1], 0xFF) ? 1 : 0;
				}
				if (top >= iw * 0.8 && bottom >= iw * 0.8 && left >= ih * 0.8 && right >= ih * 0.8) {
					result.add(new int[]{x, y, iw, ih});
				}
			}
		}
		return result;
	}

	/** Scale2x (EPX): doubles the size and rounds diagonal stair steps without blurring. */
	private static int[] scale2x(int[] src, int w, int h) {
		int[] dst = new int[w * h * 4];
		int dw = w * 2;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int p = src[y * w + x];
				int a = y > 0 ? src[(y - 1) * w + x] : p;
				int b = x < w - 1 ? src[y * w + x + 1] : p;
				int c = x > 0 ? src[y * w + x - 1] : p;
				int d = y < h - 1 ? src[(y + 1) * w + x] : p;
				dst[(2 * y) * dw + 2 * x] = c == a && c != d && a != b ? a : p;
				dst[(2 * y) * dw + 2 * x + 1] = a == b && a != c && b != d ? b : p;
				dst[(2 * y + 1) * dw + 2 * x] = d == c && d != b && c != a ? c : p;
				dst[(2 * y + 1) * dw + 2 * x + 1] = b == d && b != a && d != c ? d : p;
			}
		}
		return dst;
	}

	/** Coverage of a pixel centre by a rounded rectangle (anti-aliased edge). */
	private static double roundRectCoverage(int px, int py, double x0, double y0, double x1, double y1, double r) {
		double x = px + 0.5;
		double y = py + 0.5;
		double cx = Math.max(x0 + r, Math.min(x1 - r, x));
		double cy = Math.max(y0 + r, Math.min(y1 - r, y));
		double d = Math.hypot(x - cx, y - cy);
		return Math.max(0.0, Math.min(1.0, r - d + 0.5));
	}

	/** {@code top} over {@code bottom} with coverage {@code t} (colour and alpha interpolated). */
	private static int blend(int bottom, int top, double t) {
		int a = (int) Math.round((bottom >>> 24) * (1 - t) + (top >>> 24) * t);
		return (a << 24) | mix(bottom & 0xFFFFFF, top & 0xFFFFFF, (float) t);
	}

	/** Test hook: alpha (0-255) of the background panel (texture pixel 5, 5) in the Custom Inventory version. */
	public static int backgroundAlphaForTest(Identifier texture) {
		try (NativeImage image = readDark(texture)) {
			return image == null ? -1 : image.getPixel(5 * HI + 1, 5 * HI + 1) >>> 24;
		}
	}

	private static Identifier createDark(Identifier texture) {
		NativeImage image = readDark(texture);
		if (image == null) {
			return texture;
		}
		Identifier dark = Identifier.fromNamespaceAndPath(Nebulon.MOD_ID, "dark/" + texture.getPath());
		Minecraft.getInstance().getTextureManager().register(dark, new StyledTexture("nebulon style " + texture, image));
		return dark;
	}

	/**
	 * Maps a pixel ({@code 0xRRGGBB}) to Mod Style: greys follow the palette by their brightness,
	 * coloured pixels (arrows, flames, bars) keep their colour, slightly dimmed.
	 */
	static int stylePixel(int rgb, int[] palette) {
		int r = (rgb >> 16) & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int b = rgb & 0xFF;
		int max = Math.max(r, Math.max(g, b));
		int min = Math.min(r, Math.min(g, b));
		// Strong colours (flames, XP, arrows) stay; greys and muted tones (e.g. the tan anvil name
		// field) follow the palette by their brightness.
		if (max - min > 12 && (max - min) > max * 0.38F) {
			return (scale(r, 0.9F) << 16) | (scale(g, 0.9F) << 8) | scale(b, 0.9F);
		}
		int grey = Math.round(0.299F * r + 0.587F * g + 0.114F * b);
		for (int i = 1; i < GREYS.length; i++) {
			if (grey <= GREYS[i]) {
				float t = (grey - GREYS[i - 1]) / (float) (GREYS[i] - GREYS[i - 1]);
				return mix(palette[i - 1], palette[i], t);
			}
		}
		return palette[palette.length - 1];
	}

	private static boolean isGreyish(int rgb) {
		int r = (rgb >> 16) & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int b = rgb & 0xFF;
		int max = Math.max(r, Math.max(g, b));
		int min = Math.min(r, Math.min(g, b));
		return !(max - min > 12 && (max - min) > max * 0.38F);
	}

	private static int greyOf(int rgb) {
		return Math.round(0.299F * ((rgb >> 16) & 0xFF) + 0.587F * ((rgb >> 8) & 0xFF) + 0.114F * (rgb & 0xFF));
	}

	/** Whether an opaque black pixel is part of a black area (some 2 x 2 block around it is all black). */
	private static boolean inBlackBlock(int[] pixels, int w, int h, int x, int y) {
		for (int dy = -1; dy <= 0; dy++) {
			for (int dx = -1; dx <= 0; dx++) {
				int x0 = x + dx;
				int y0 = y + dy;
				if (x0 < 0 || y0 < 0 || x0 + 1 >= w || y0 + 1 >= h) {
					continue;
				}
				if (isBlack(pixels[y0 * w + x0]) && isBlack(pixels[y0 * w + x0 + 1])
						&& isBlack(pixels[(y0 + 1) * w + x0]) && isBlack(pixels[(y0 + 1) * w + x0 + 1])) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean isBlack(int argb) {
		return (argb >>> 24) != 0 && (argb & 0xFFFFFF) == 0;
	}

	private static int mix(int c1, int c2, float t) {
		int r = Math.round(((c1 >> 16) & 0xFF) * (1 - t) + ((c2 >> 16) & 0xFF) * t);
		int g = Math.round(((c1 >> 8) & 0xFF) * (1 - t) + ((c2 >> 8) & 0xFF) * t);
		int b = Math.round((c1 & 0xFF) * (1 - t) + (c2 & 0xFF) * t);
		return (r << 16) | (g << 8) | b;
	}

	private static int scale(int channel, float factor) {
		return Math.round(channel * factor);
	}
}
