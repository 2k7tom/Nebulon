package com.nebulon.config;

/**
 * Font of the mod's own screens and HUD elements. The settings menu always uses Roboto; HUD / GUI
 * elements use it only with "Use Mod Font", otherwise Minecraft's font (or a texture pack's).
 * Nothing else in Minecraft is drawn with the mod's font.
 */
public enum GuiFont {
	/** Roboto (Regular/Medium), supersampled for smooth edges. */
	ROBOTO("roboto_regular", "roboto_medium", 24, true),
	/** Minecraft's default font, as the active texture pack defines it. */
	VANILLA(null, null, 1, false);

	/** Font definition base names in {@code assets/nebulon/font/}, suffixed with the resolution (null = vanilla). */
	public final String regular;
	public final String bold;
	/** Highest resolution suffix that exists (1 .. maxResolution). */
	public final int maxResolution;
	/**
	 * Whether glyphs are rasterised at twice the screen resolution and filtered down smoothly
	 * (2 x 2 supersampling). Only for outline (TrueType) fonts, not for bitmap fonts.
	 */
	public final boolean supersampled;

	GuiFont(String regular, String bold, int maxResolution, boolean supersampled) {
		this.regular = regular;
		this.bold = bold;
		this.maxResolution = maxResolution;
		this.supersampled = supersampled;
	}

	/** The font of a HUD / GUI element with a "Use Mod Font" setting. */
	public static GuiFont of(boolean useModFont) {
		return useModFont ? ROBOTO : VANILLA;
	}
}
