package com.nebulon.gui;

import com.nebulon.Nebulon;
import com.nebulon.client.NebulonClient;
import com.nebulon.config.InventoryDisplaySettings;
import com.nebulon.config.NebulonConfig;
import com.nebulon.config.HudPosition;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * "Inventory Display" (Render): the player's inventory (3 x 9, optionally with the hotbar) on the
 * HUD, in the menu style of the mod with its own colours and opacity. Moved and resized in the Gui
 * Editor.
 */
public final class InventoryDisplay {
	private static final int PAD = 5;
	private static final int SLOT = 18;
	private static final int HOTBAR_GAP = 4;

	/** Draw calls of the last frame (test hook). */
	private static int drawnItems;

	private InventoryDisplay() {
	}

	public static void register() {
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
				Identifier.fromNamespaceAndPath(Nebulon.MOD_ID, "inventory_display"), (g, delta) -> drawHud(g));
	}

	private static InventoryDisplaySettings settings() {
		return NebulonClient.get().configManager().get().inventoryDisplay;
	}

	/** Opacity of the panel and slot backgrounds (fixed), in percent. */
	private static final int OPACITY = 65;
	/** Fixed colours: panel background, border and slots ({@code 0xAARRGGBB}). */
	private static final int BACKGROUND = 0xFF121214;
	private static final int BORDER = 0xFF3C3C42;
	private static final int SLOT_COLOR = 0xFF1B1B1E;

	private static int[] baseSize() {
		int rows = settings().showHotbar ? 4 : 3;
		int height = PAD + rows * SLOT + (settings().showHotbar ? HOTBAR_GAP : 0) + PAD - 2;
		return new int[]{PAD * 2 + 9 * SLOT - 2, height};
	}

	/** Colour with its alpha scaled by the fixed opacity. */
	private static int withOpacity(int argb, int opacity) {
		int alpha = Math.round((argb >>> 24) * opacity / 100.0F);
		return (alpha << 24) | (argb & 0xFFFFFF);
	}

	private static void render(Canvas c, Minecraft mc, double x, double y, boolean highlighted) {
		InventoryDisplaySettings s = settings();
		HudPosition pos = s.position;
		float k = pos.scale;
		int[] base = baseSize();
		double w = base[0] * k;
		double h = base[1] * k;
		int background = withOpacity(BACKGROUND, OPACITY);
		int border = highlighted ? Theme.ACCENT : BORDER;
		int slotColor = withOpacity(SLOT_COLOR, OPACITY);
		if ((background >>> 24) > 0) {
			c.roundRect(x, y, x + w, y + h, 5 * k, background);
		}
		if ((border >>> 24) > 0) {
			c.roundRectOutline(x, y, x + w, y + h, 5 * k, border);
		}

		int drawn = 0;
		int rows = s.showHotbar ? 4 : 3;
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < 9; col++) {
				// Rows 0-2: inventory slots 9-35; row 3: hotbar 0-8.
				int index = row < 3 ? 9 + row * 9 + col : col;
				double sx = x + (PAD + col * SLOT) * k;
				double sy = y + (PAD + row * SLOT + (row == 3 ? HOTBAR_GAP : 0)) * k;
				if ((slotColor >>> 24) > 0) {
					c.roundRect(sx, sy, sx + 16 * k, sy + 16 * k, 2 * k, slotColor);
				}
				ItemStack stack = mc.player != null ? mc.player.getInventory().getItem(index) : ItemStack.EMPTY;
				if (stack.isEmpty()) {
					continue;
				}
				drawn++;
				c.g.pose().pushMatrix();
				c.g.pose().translate(c.px(sx), c.px(sy));
				c.g.pose().scale(c.scale * k, c.scale * k);
				RarityBackground.draw(c.g, stack, 0, 0);
				c.g.item(stack, 0, 0);
				c.g.itemDecorations(mc.font, stack, 0, 0);
				c.g.pose().popMatrix();
			}
		}
		drawnItems = drawn;
	}

	public static boolean enabledForTest() {
		return DISPLAY.enabled();
	}

	public static int drawnItemsForTest() {
		return drawnItems;
	}

	/** The Inventory Display for the Gui Editor. */
	static final HudDisplay DISPLAY = new HudDisplay() {
		@Override
		public boolean enabled() {
			// Part of Inventory QOL: only while that feature is on, too.
			return settings().enabled && NebulonClient.get().configManager().get().visualTweaks.enabled;
		}

		@Override
		public int[] size(Canvas c, Minecraft minecraft, boolean example) {
			int[] base = baseSize();
			float scale = settings().position.scale;
			return new int[]{(int) Math.ceil(base[0] * scale), (int) Math.ceil(base[1] * scale)};
		}

		@Override
		public int[] position(int[] size, int designWidth, int designHeight) {
			HudPosition pos = settings().position;
			int x = pos.x < 0 ? designWidth - size[0] - 8 : pos.x;
			int y = pos.y < 0 ? (designHeight - size[1]) / 2 : pos.y;
			return new int[]{Math.max(0, Math.min(designWidth - size[0], x)), Math.max(0, Math.min(designHeight - size[1], y))};
		}

		@Override
		public void setPosition(int x, int y) {
			settings().position.x = x;
			settings().position.y = y;
		}

		@Override
		public float scale() {
			return settings().position.scale;
		}

		@Override
		public void setScale(float scale) {
			settings().position.scale = scale;
		}

		@Override
		public void render(Canvas c, Minecraft minecraft, double x, double y, boolean highlighted, boolean example) {
			InventoryDisplay.render(c, minecraft, x, y, highlighted);
		}
	};

	private static void drawHud(GuiGraphicsExtractor g) {
		Minecraft minecraft = Minecraft.getInstance();
		NebulonConfig config = NebulonClient.get().configManager().get();
		if (!DISPLAY.enabled() || minecraft.player == null || minecraft.gui.screen() instanceof GuiEditorScreen) {
			return;
		}
		Theme.apply(config.menuColor.rgb());
		int scale = Canvas.scaleFor(minecraft.getWindow());
		int guiScale = minecraft.getWindow().getGuiScale();
		g.pose().pushMatrix();
		g.pose().scale(1.0F / guiScale, 1.0F / guiScale);
		Canvas c = new Canvas(g, minecraft.font, scale, com.nebulon.config.GuiFont.VANILLA);
		int[] size = DISPLAY.size(c, minecraft, false);
		int[] pos = DISPLAY.position(size, minecraft.getWindow().getWidth() / scale, minecraft.getWindow().getHeight() / scale);
		DISPLAY.render(c, minecraft, pos[0], pos[1], false, false);
		g.pose().popMatrix();
	}
}
