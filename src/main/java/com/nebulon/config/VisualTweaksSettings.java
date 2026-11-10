package com.nebulon.config;

/** "Visual Tweaks" (Render): small client-side changes to inventories and containers. */
public class VisualTweaksSettings {
	public boolean enabled = false;
	/** Hides the recipe book button and the recipe book in the inventory, crafting table and furnaces. */
	public boolean removeRecipeBook = true;
	/** Hides the offhand slot in the inventory; it can no longer be clicked. */
	public boolean removeOffhand = false;
	/**
	 * "Custom Inventory": all containers and menus in the flat look of the mod (the mod's base
	 * colours, blurred background); only the inventory background is see-through (10 %).
	 */
	public boolean modStyle = true;
	/** Hides the potion effect list next to the inventory (survival and creative). */
	public boolean hideInventoryEffects = false;

	public void validate(java.util.List<String> problems) {
	}
}
