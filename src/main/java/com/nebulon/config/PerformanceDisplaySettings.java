package com.nebulon.config;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** "Performance Display" (Render): which values the HUD box shows, its look and where it sits. */
public class PerformanceDisplaySettings {
	public static final int DEFAULT_OPACITY = 69;
	public static final float MIN_SCALE = 0.5F;
	public static final float MAX_SCALE = 3.0F;

	/** Horizontal: all values in one line. Vertical: one value per line. */
	public enum Layout {
		@SerializedName(value = "HORIZONTAL", alternate = "COMPACT")
		HORIZONTAL,
		@SerializedName(value = "VERTICAL", alternate = "LONG")
		VERTICAL
	}

	/** Subtle depth effects for the text; all stay close to the text and low in contrast. */
	public enum TextDepth {
		/** Plain text. */
		OFF,
		/** Soft drop shadow: two faint layers, fading out to the bottom right. */
		SHADOW,
		/** Small 3D extrusion in a darker shade of the text colour. */
		EXTRUDE,
		/** Faint halo in the menu colour around the letters. */
		GLOW
	}

	public boolean enabled = false;

	/** "Use Mod Font": Roboto (the mod's font) instead of Minecraft's / the texture pack's font. */

	public boolean useModFont = true;
	public boolean showFps = true;
	public boolean showTps = true;
	public boolean showPing = true;
	public boolean showCps = true;
	public Layout layout = Layout.VERTICAL;
	public TextDepth textDepth = TextDepth.OFF;
	/** Background opacity in percent (0 = fully transparent, 100 = fully opaque). */
	public int backgroundOpacity = DEFAULT_OPACITY;
	/** Size factor, changed by holding the box with the left mouse button and scrolling. */
	public float scale = 1.0F;
	/** Top-left corner in design units of the settings screen (see the client's Canvas), -1 = default. */
	public int x = -1;
	public int y = -1;

	void validate(List<String> problems) {
		if (layout == null) {
			problems.add("performanceDisplay.layout invalid -> VERTICAL");
			layout = Layout.VERTICAL;
		}
		if (textDepth == null) {
			problems.add("performanceDisplay.textDepth invalid -> OFF");
			textDepth = TextDepth.OFF;
		}
		if (backgroundOpacity < 0 || backgroundOpacity > 100) {
			int clamped = Math.max(0, Math.min(100, backgroundOpacity));
			problems.add("performanceDisplay.backgroundOpacity " + backgroundOpacity + " -> " + clamped);
			backgroundOpacity = clamped;
		}
		if (!(scale >= MIN_SCALE && scale <= MAX_SCALE)) { // also catches NaN
			float fixed = Float.isNaN(scale) ? 1.0F : Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
			problems.add("performanceDisplay.scale " + scale + " -> " + fixed);
			scale = fixed;
		}
	}
}
