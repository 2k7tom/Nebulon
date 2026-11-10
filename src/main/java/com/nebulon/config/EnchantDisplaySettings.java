package com.nebulon.config;

/** "Enchant Display" (Render): missing enchants and level colours in SkyBlock item tooltips. */
public class EnchantDisplaySettings {
	public boolean enabled = false;
	/** "Show Missing Enchants": gray list of enchants the item can still get (no ultimates, no conflicting ones). */
	public boolean showMissing = true;
	/** "Color Enchants": applied enchants coloured by level (max / one below max / lower). */
	/** Max level (the only colour with chroma). */
	public ColorSetting maxColor = new ColorSetting("#FFAA00", false);
	/** One level below max. */
	public ColorSetting nearMaxColor = new ColorSetting("#AA00AA", false);
	/** Every lower level. */
	public ColorSetting normalColor = new ColorSetting("#5555FF", false);
	/** Text style per level group: bold, italic, strikethrough. */
	public boolean maxBold = false;
	public boolean maxItalic = false;
	public boolean maxStrikethrough = false;
	public boolean nearMaxBold = false;
	public boolean nearMaxItalic = false;
	public boolean nearMaxStrikethrough = false;
	public boolean normalBold = false;
	public boolean normalItalic = false;
	public boolean normalStrikethrough = false;

	public void validate(java.util.List<String> problems) {
		EnchantDisplaySettings d = new EnchantDisplaySettings();
		maxColor = ColorSetting.validateColor("enchantDisplay.maxColor", maxColor, d.maxColor, problems);
		nearMaxColor = ColorSetting.validateColor("enchantDisplay.nearMaxColor", nearMaxColor, d.nearMaxColor, problems);
		normalColor = ColorSetting.validateColor("enchantDisplay.normalColor", normalColor, d.normalColor, problems);
		nearMaxColor.chroma = false;
		normalColor.chroma = false;
	}
}
