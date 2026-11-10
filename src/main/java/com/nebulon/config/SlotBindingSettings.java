package com.nebulon.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * "Slot Binding" (Settings): the settings of NoammAddons 1.3.0's Slot Binding (CC0) - binds
 * inventory slots to hotbar slots for quick item swaps (Shift + left click).
 */
public class SlotBindingSettings {
	public boolean enabled = false;
	/**
	 * The Binding key ({@code KeyMapping#saveString}, e.g. "key.keyboard.r"). Only set in the mod's
	 * settings, not in Minecraft's Controls; null = not stored yet (taken over from options.txt).
	 */
	public String key = null;
	// Rendering (bound slots are always shown)
	/** "Hover Only": bound slots are only shown while one of them is hovered. */
	public boolean hoverOnly = false;
	public boolean drawBorder = true;
	public boolean drawLine = true;
	/** Colour of the borders and of the lines between bound slots. */
	public ColorSetting borderColor = new ColorSetting("#FFAFAF", false);

	/** Inventory menu slot -> hotbar menu slot (36..44). */
	public Map<Integer, Integer> bindings = new LinkedHashMap<>();

	void validate(List<String> problems) {
		SlotBindingSettings d = new SlotBindingSettings();
		borderColor = ColorSetting.validateColor("slotBinding.borderColor", borderColor, d.borderColor, problems);
		if (bindings == null) {
			bindings = new LinkedHashMap<>();
		}
		bindings.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null || e.getValue() < 36 || e.getValue() > 44
				|| (e.getKey() >= 36 && e.getKey() <= 44) || e.getKey() < 0 || e.getKey() > 45);
	}
}
