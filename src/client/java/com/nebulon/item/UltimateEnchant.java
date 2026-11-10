package com.nebulon.item;

import com.nebulon.client.NebulonClient;
import com.nebulon.config.ItemTweaksSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Map;

/**
 * Item Tweaks &gt; Show Ultimate Enchant: the ultimate enchant of an item (SkyBlock item data,
 * {@code enchantments.ultimate_*}) as a short tag in the top right corner of its slot, in the
 * colour SkyBlock uses for ultimate enchants (light purple, bold).
 */
public final class UltimateEnchant {
	/** SkyBlock ultimate enchant id -&gt; tag. Duplex is still called "reiterate" internally. */
	private static final Map<String, String> TAGS = Map.ofEntries(
			Map.entry("ultimate_combo", "CO"),
			Map.entry("ultimate_soul_eater", "SE"),
			Map.entry("ultimate_chimera", "CH"),
			Map.entry("ultimate_reiterate", "DU"),
			Map.entry("ultimate_duplex", "DU"),
			Map.entry("ultimate_fatal_tempo", "FT"),
			Map.entry("ultimate_inferno", "IF"),
			Map.entry("ultimate_one_for_all", "OFA"),
			Map.entry("ultimate_rend", "RE"),
			Map.entry("ultimate_swarm", "SW"),
			Map.entry("ultimate_jerry", "UJ"),
			Map.entry("ultimate_wise", "UW"));
	/** SkyBlock's ultimate enchant colour (§d). */
	private static final int COLOR = 0xFFFF55FF;
	private static final float SCALE = 0.6F;

	/** Drawn tags {in menus, on the hotbar}, for the client game tests. */
	private static final int[] drawCounts = new int[2];

	private UltimateEnchant() {
	}

	/** Test hook: tags drawn since the last call {in menus, on the hotbar}. */
	public static int[] takeDrawCountsForTest() {
		int[] counts = drawCounts.clone();
		drawCounts[0] = 0;
		drawCounts[1] = 0;
		return counts;
	}

	/** The tag of the item's ultimate enchant, or {@code null}. */
	public static String tag(ItemStack stack) {
		if (stack.isEmpty()) {
			return null;
		}
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data == null) {
			return null;
		}
		CompoundTag enchantments = data.copyTag().getCompound("enchantments").orElse(null);
		if (enchantments == null) {
			return null;
		}
		for (String key : enchantments.keySet()) {
			String tag = TAGS.get(key);
			if (tag != null) {
				return tag;
			}
		}
		return null;
	}

	/**
	 * Draws the tag over an item drawn at (x, y) (16 x 16), if the setting is on.
	 *
	 * @param hotbar whether the item is in the HUD hotbar (hidden with "Only In Inventory")
	 */
	public static void draw(GuiGraphicsExtractor g, ItemStack stack, int x, int y, boolean hotbar) {
		NebulonClient client = NebulonClient.get();
		if (client == null) {
			return;
		}
		ItemTweaksSettings s = client.configManager().get().itemTweaks;
		if (!s.enabled || !s.showUltimateEnchant || (hotbar && s.ultimateEnchantInventoryOnly)) {
			return;
		}
		String tag = tag(stack);
		if (tag == null) {
			return;
		}
		drawCounts[hotbar ? 1 : 0]++;
		Font font = Minecraft.getInstance().font;
		Component text = Component.literal(tag).withStyle(Style.EMPTY.withBold(true));
		float width = font.width(text) * SCALE;
		g.pose().pushMatrix();
		g.pose().translate(x + 16 - width, y);
		g.pose().scale(SCALE, SCALE);
		g.text(font, text, 0, 0, COLOR, true);
		g.pose().popMatrix();
	}
}
