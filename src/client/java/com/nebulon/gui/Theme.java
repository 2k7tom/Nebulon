package com.nebulon.gui;

import com.nebulon.config.ColorUtil;

/**
 * Colours and sizes of the settings screen: dark, mostly monochrome "glass" panels with rounded
 * cards. Sizes are design units (see {@link Canvas}).
 * <p>
 * The colours can be tinted with the "Menu Customisation" colour ({@link #apply}): every colour
 * keeps its brightness (HSL lightness) and its opacity, only hue and saturation come from the
 * chosen colour. White (the default) keeps the original neutral look. Text stays white.
 */
public final class Theme {
	public static final int PANEL_WIDTH = 128;
	public static final int PANEL_GAP = 8;
	public static final int HEADER_HEIGHT = 20;
	public static final int HEADER_GAP = 6;
	public static final int PADDING = 5;
	public static final int CARD_HEIGHT = 17;
	public static final int CARD_GAP = 4;
	public static final int SETTING_INSET = 7;
	public static final int ROW_HEIGHT = 14;
	/** Vertical offset that centres Roboto text inside a {@link #ROW_HEIGHT} row. */
	public static final int TEXT_OFFSET = 3;

	public static final int OVERLAY = 0x70000000;
	public static final int TEXT = 0xFFFFFFFF;
	public static final int TEXT_ERROR = 0xFFFF6B6B;

	// Neutral base colours; the public fields below are these, tinted with the menu colour.
	private static final int BASE_PANEL_BG = 0xB0121214;
	private static final int BASE_HEADER_BG = 0xFF26262A;
	private static final int BASE_HEADER_BORDER = 0xFF3C3C42;
	private static final int BASE_CARD_BG = 0xFF1B1B1E;
	private static final int BASE_CARD_BG_HOVER = 0xFF222226;
	private static final int BASE_CARD_BG_ENABLED = 0xFF2E2E33;
	private static final int BASE_CARD_BG_ENABLED_HOVER = 0xFF35353B;
	private static final int BASE_CARD_BORDER_ENABLED = 0xFF4A4A52;
	private static final int BASE_CARD_EXPANDED_BG = 0xFF19191C;
	private static final int BASE_SEPARATOR = 0xFF2C2C31;
	private static final int BASE_SEARCH_BG = 0xFF1C1C1F;
	private static final int BASE_TOOLTIP_BG = 0xF0141416;
	private static final int BASE_TEXT_DIM = 0xFF8E8E94;
	private static final int BASE_TEXT_FAINT = 0xFF5C5C62;
	private static final int BASE_CONTROL_OFF = 0xFF3A3A3F;
	private static final int BASE_CONTROL_ON = 0xFFE4E4E8;
	private static final int BASE_CONTROL_KNOB_ON = 0xFF1A1A1D;
	private static final int BASE_INPUT_BG = 0xFF141416;
	private static final int BASE_INPUT_BORDER = 0xFF3A3A40;
	private static final int BASE_INPUT_BORDER_FOCUSED = 0xFFB8B8C0;
	private static final int BASE_VALUE_BG = 0xFF26262B;
	private static final int BASE_VALUE_BG_HOVER = 0xFF303036;

	public static int PANEL_BG = BASE_PANEL_BG;
	public static int HEADER_BG = BASE_HEADER_BG;
	public static int HEADER_BORDER = BASE_HEADER_BORDER;
	public static int CARD_BG = BASE_CARD_BG;
	public static int CARD_BG_HOVER = BASE_CARD_BG_HOVER;
	public static int CARD_BG_ENABLED = BASE_CARD_BG_ENABLED;
	public static int CARD_BG_ENABLED_HOVER = BASE_CARD_BG_ENABLED_HOVER;
	public static int CARD_BORDER_ENABLED = BASE_CARD_BORDER_ENABLED;
	public static int CARD_EXPANDED_BG = BASE_CARD_EXPANDED_BG;
	public static int SEPARATOR = BASE_SEPARATOR;
	public static int SEARCH_BG = BASE_SEARCH_BG;
	public static int TOOLTIP_BG = BASE_TOOLTIP_BG;
	public static int TEXT_DIM = BASE_TEXT_DIM;
	public static int TEXT_FAINT = BASE_TEXT_FAINT;
	public static int CONTROL_OFF = BASE_CONTROL_OFF;
	public static int CONTROL_ON = BASE_CONTROL_ON;
	public static int CONTROL_KNOB_ON = BASE_CONTROL_KNOB_ON;
	public static int INPUT_BG = BASE_INPUT_BG;
	public static int INPUT_BORDER = BASE_INPUT_BORDER;
	public static int INPUT_BORDER_FOCUSED = BASE_INPUT_BORDER_FOCUSED;
	public static int VALUE_BG = BASE_VALUE_BG;
	public static int VALUE_BG_HOVER = BASE_VALUE_BG_HOVER;
	/** The menu colour itself (opaque), e.g. for the labels of the Performance Display. */
	public static int ACCENT = 0xFFFFFFFF;
	/** Alignment guides while dragging panels. */
	public static int GUIDE = 0xC0FFFFFF;

	private static int appliedRgb = 0xFFFFFF;

	private Theme() {
	}

	public static boolean inside(double mx, double my, double x0, double y0, double x1, double y1) {
		return mx >= x0 && mx < x1 && my >= y0 && my < y1;
	}

	/** Tints all colours with the menu colour ({@code 0xRRGGBB}); cheap no-op if unchanged. */
	public static void apply(int menuRgb) {
		if (menuRgb == appliedRgb) {
			return;
		}
		appliedRgb = menuRgb;
		float[] hsv = ColorUtil.rgbToHsv(menuRgb);
		float hue = hsv[0];
		float saturation = hsv[1];
		PANEL_BG = tint(BASE_PANEL_BG, hue, saturation);
		HEADER_BG = tint(BASE_HEADER_BG, hue, saturation);
		HEADER_BORDER = tint(BASE_HEADER_BORDER, hue, saturation);
		CARD_BG = tint(BASE_CARD_BG, hue, saturation);
		CARD_BG_HOVER = tint(BASE_CARD_BG_HOVER, hue, saturation);
		CARD_BG_ENABLED = tint(BASE_CARD_BG_ENABLED, hue, saturation);
		CARD_BG_ENABLED_HOVER = tint(BASE_CARD_BG_ENABLED_HOVER, hue, saturation);
		CARD_BORDER_ENABLED = tint(BASE_CARD_BORDER_ENABLED, hue, saturation);
		CARD_EXPANDED_BG = tint(BASE_CARD_EXPANDED_BG, hue, saturation);
		SEPARATOR = tint(BASE_SEPARATOR, hue, saturation);
		SEARCH_BG = tint(BASE_SEARCH_BG, hue, saturation);
		TOOLTIP_BG = tint(BASE_TOOLTIP_BG, hue, saturation);
		TEXT_DIM = tint(BASE_TEXT_DIM, hue, saturation * 0.6F);
		TEXT_FAINT = tint(BASE_TEXT_FAINT, hue, saturation * 0.6F);
		CONTROL_OFF = tint(BASE_CONTROL_OFF, hue, saturation);
		CONTROL_ON = tint(BASE_CONTROL_ON, hue, saturation);
		CONTROL_KNOB_ON = tint(BASE_CONTROL_KNOB_ON, hue, saturation);
		INPUT_BG = tint(BASE_INPUT_BG, hue, saturation);
		INPUT_BORDER = tint(BASE_INPUT_BORDER, hue, saturation);
		INPUT_BORDER_FOCUSED = tint(BASE_INPUT_BORDER_FOCUSED, hue, saturation);
		VALUE_BG = tint(BASE_VALUE_BG, hue, saturation);
		VALUE_BG_HOVER = tint(BASE_VALUE_BG_HOVER, hue, saturation);
		ACCENT = 0xFF000000 | menuRgb;
		GUIDE = 0xC0000000 | menuRgb;
	}

	/** Keeps alpha and HSL lightness of {@code argb}, takes hue and saturation from the menu colour. */
	static int tint(int argb, float hue, float saturation) {
		if (saturation <= 0.001F) {
			return argb;
		}
		float r = ((argb >> 16) & 0xFF) / 255.0F;
		float g = ((argb >> 8) & 0xFF) / 255.0F;
		float b = (argb & 0xFF) / 255.0F;
		float lightness = (Math.max(r, Math.max(g, b)) + Math.min(r, Math.min(g, b))) / 2.0F;
		float chroma = (1.0F - Math.abs(2.0F * lightness - 1.0F)) * saturation;
		float hPrime = hue * 6.0F;
		float x = chroma * (1.0F - Math.abs(hPrime % 2.0F - 1.0F));
		float r1;
		float g1;
		float b1;
		switch ((int) hPrime % 6) {
			case 0 -> { r1 = chroma; g1 = x; b1 = 0; }
			case 1 -> { r1 = x; g1 = chroma; b1 = 0; }
			case 2 -> { r1 = 0; g1 = chroma; b1 = x; }
			case 3 -> { r1 = 0; g1 = x; b1 = chroma; }
			case 4 -> { r1 = x; g1 = 0; b1 = chroma; }
			default -> { r1 = chroma; g1 = 0; b1 = x; }
		}
		float m = lightness - chroma / 2.0F;
		int ri = Math.round((r1 + m) * 255.0F);
		int gi = Math.round((g1 + m) * 255.0F);
		int bi = Math.round((b1 + m) * 255.0F);
		return (argb & 0xFF000000) | (clamp(ri) << 16) | (clamp(gi) << 8) | clamp(bi);
	}

	private static int clamp(int v) {
		return Math.max(0, Math.min(255, v));
	}
}
