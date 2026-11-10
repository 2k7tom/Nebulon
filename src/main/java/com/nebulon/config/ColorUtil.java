package com.nebulon.config;

import java.util.Locale;

/** Parsing and formatting of {@code #RRGGBB} colour strings. */
public final class ColorUtil {
	/** Returned by {@link #parseRgb(String)} for invalid input. Real colours are always &gt;= 0. */
	public static final int INVALID = -1;

	private ColorUtil() {
	}

	/**
	 * Parses {@code #RRGGBB}, {@code RRGGBB}, {@code 0xRRGGBB} or the short form {@code #RGB}.
	 *
	 * @return the colour as {@code 0xRRGGBB}, or {@link #INVALID}
	 */
	public static int parseRgb(String value) {
		if (value == null) {
			return INVALID;
		}

		String hex = value.trim();
		if (hex.startsWith("#")) {
			hex = hex.substring(1);
		} else if (hex.startsWith("0x") || hex.startsWith("0X")) {
			hex = hex.substring(2);
		}

		if (hex.length() == 3) {
			StringBuilder expanded = new StringBuilder(6);
			for (int i = 0; i < 3; i++) {
				expanded.append(hex.charAt(i)).append(hex.charAt(i));
			}
			hex = expanded.toString();
		}

		if (hex.length() != 6) {
			return INVALID;
		}

		try {
			return Integer.parseInt(hex, 16);
		} catch (NumberFormatException e) {
			return INVALID;
		}
	}

	public static boolean isValid(String value) {
		return parseRgb(value) != INVALID;
	}

	/** Formats {@code 0xRRGGBB} as {@code #RRGGBB}. */
	public static String format(int rgb) {
		return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
	}

	/** Normalises a valid colour string to {@code #RRGGBB}, or returns {@code fallback} if invalid. */
	public static String normalize(String value, String fallback) {
		int rgb = parseRgb(value);
		return rgb == INVALID ? fallback : format(rgb);
	}

	/** Duration of one full chroma hue cycle. */
	public static final long CHROMA_CYCLE_MILLIS = 4000L;

	/**
	 * @param hue        0..1 (wraps)
	 * @param saturation 0..1
	 * @param value      0..1
	 * @return {@code 0xRRGGBB}
	 */
	public static int hsvToRgb(float hue, float saturation, float value) {
		float h = (hue - (float) Math.floor(hue)) * 6.0F;
		int sector = (int) h % 6;
		float f = h - (int) h;
		float p = value * (1.0F - saturation);
		float q = value * (1.0F - f * saturation);
		float t = value * (1.0F - (1.0F - f) * saturation);
		float r, g, b;
		switch (sector) {
			case 0 -> { r = value; g = t; b = p; }
			case 1 -> { r = q; g = value; b = p; }
			case 2 -> { r = p; g = value; b = t; }
			case 3 -> { r = p; g = q; b = value; }
			case 4 -> { r = t; g = p; b = value; }
			default -> { r = value; g = p; b = q; }
		}
		return (Math.round(r * 255.0F) << 16) | (Math.round(g * 255.0F) << 8) | Math.round(b * 255.0F);
	}

	/** @return {hue, saturation, value}, each 0..1 */
	public static float[] rgbToHsv(int rgb) {
		float r = ((rgb >> 16) & 0xFF) / 255.0F;
		float g = ((rgb >> 8) & 0xFF) / 255.0F;
		float b = (rgb & 0xFF) / 255.0F;
		float max = Math.max(r, Math.max(g, b));
		float min = Math.min(r, Math.min(g, b));
		float delta = max - min;
		float hue;
		if (delta == 0.0F) {
			hue = 0.0F;
		} else if (max == r) {
			hue = ((g - b) / delta) / 6.0F;
		} else if (max == g) {
			hue = ((b - r) / delta + 2.0F) / 6.0F;
		} else {
			hue = ((r - g) / delta + 4.0F) / 6.0F;
		}
		if (hue < 0.0F) {
			hue += 1.0F;
		}
		return new float[]{hue, max == 0.0F ? 0.0F : delta / max, max};
	}

	/**
	 * Chroma animation: rotates the hue with time while keeping the saturation and brightness of
	 * {@code baseRgb}. A grey base colour gets full saturation so the animation is always visible.
	 */
	public static int chroma(int baseRgb, long timeMillis) {
		float[] hsv = rgbToHsv(baseRgb);
		float hue = (timeMillis % CHROMA_CYCLE_MILLIS) / (float) CHROMA_CYCLE_MILLIS;
		float saturation = hsv[1] < 0.05F ? 1.0F : hsv[1];
		float value = hsv[2] < 0.05F ? 1.0F : hsv[2];
		return hsvToRgb(hue, saturation, value);
	}
}
