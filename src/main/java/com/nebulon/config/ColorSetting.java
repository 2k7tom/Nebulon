package com.nebulon.config;

/**
 * A colour as edited by the colour wheel: a fixed {@code #RRGGBB} value, optionally with a chroma
 * animation that cycles the hue over time while keeping the chosen saturation and brightness, and
 * an opacity (used by the puzzle solver colours; 255 = opaque).
 */
public class ColorSetting {
	public String hex;
	public boolean chroma;
	public int alpha = 255;

	public ColorSetting() {
		this("#FFFFFF", false);
	}

	public ColorSetting(String hex, boolean chroma) {
		this.hex = hex;
		this.chroma = chroma;
	}

	/** Colour with opacity, e.g. {@code withAlpha("#55FF55", 0.5F)}. */
	public static ColorSetting withAlpha(String hex, float alpha) {
		ColorSetting setting = new ColorSetting(hex, false);
		setting.alpha = Math.round(alpha * 255.0F);
		return setting;
	}

	public ColorSetting copy() {
		ColorSetting copy = new ColorSetting(hex, chroma);
		copy.alpha = alpha;
		return copy;
	}

	public int rgb() {
		int rgb = ColorUtil.parseRgb(hex);
		return rgb == ColorUtil.INVALID ? 0xFFFFFF : rgb;
	}

	/** Colour to draw at the given time: the stored colour, or its chroma-animated variant. */
	public int currentRgb(long timeMillis) {
		return chroma ? ColorUtil.chroma(rgb(), timeMillis) : rgb();
	}

	/** {@code 0xAARRGGBB} with the stored opacity. */
	public int argb(long timeMillis) {
		return (Math.max(0, Math.min(255, alpha)) << 24) | currentRgb(timeMillis);
	}

	/** Checks a loaded colour: missing or not #RRGGBB -> fallback (reported in {@code problems}). */
	static ColorSetting validateColor(String name, ColorSetting value, ColorSetting fallback, java.util.List<String> problems) {
		if (value == null) {
			problems.add(name + " missing -> " + fallback.hex);
			return fallback.copy();
		}
		int rgb = ColorUtil.parseRgb(value.hex);
		if (rgb == ColorUtil.INVALID) {
			problems.add(name + " '" + value.hex + "' is not a #RRGGBB colour -> " + fallback.hex);
			return new ColorSetting(fallback.hex, value.chroma);
		}
		value.hex = ColorUtil.format(rgb);
		return value;
	}
}