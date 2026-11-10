package com.nebulon.config;

/** "Inventory Display" (Inventory QOL): the player's inventory as a movable HUD element. */
public class InventoryDisplaySettings {
	public boolean enabled = false;
	/** "Show Hotbar": also shows the hotbar row under the inventory. */
	public boolean showHotbar = false;
	/** Position and size (moved / resized in the Gui Editor). */
	public HudPosition position = new HudPosition();

	public void validate(java.util.List<String> problems) {
		if (position == null) {
			problems.add("inventoryDisplay.position missing -> default");
			position = new HudPosition();
		}
		if (!(position.scale >= PerformanceDisplaySettings.MIN_SCALE && position.scale <= PerformanceDisplaySettings.MAX_SCALE)) {
			problems.add("inventoryDisplay.position.scale invalid -> 1.0");
			position.scale = 1.0F;
		}
	}
}
