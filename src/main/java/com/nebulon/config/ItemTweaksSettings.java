package com.nebulon.config;

/** "Item Tweaks" (Render): small changes to how items are shown. */
public class ItemTweaksSettings {
	public boolean enabled = false;
	/**
	 * "Compact Stars": master stars (the red ➊-➎ after the five gold stars) are shown by turning that
	 * many gold stars red instead, e.g. ✪✪✪✪✪➎ becomes five red stars.
	 */
	public boolean compactStars = true;
	/** "Show Ultimate Enchant": the item's ultimate enchant as a short tag (e.g. "OFA") in the top right of its slot. */
	public boolean showUltimateEnchant = true;
	/** "Only In Inventory": the ultimate enchant tags only in open menus, not on the hotbar. */
	public boolean ultimateEnchantInventoryOnly = false;
}
