package com.nebulon.config;

import java.util.List;

/** "Rarity" (Render): a circle in the colour of the item's rarity behind items in slots. */
public class RaritySettings {
	public static final int MIN_OPACITY = 5;
	public static final int MAX_OPACITY = 100;

	/** Shape of the rarity background. */
	public enum Shape {
		CIRCLE,
		FULL_SLOT,
		GRADIENT
	}

	public boolean enabled = false;
	/** Opacity of the background in percent. */
	public int opacity = 50;
	public Shape shape = Shape.CIRCLE;

	void validate(List<String> problems) {
		if (shape == null) {
			problems.add("rarity.shape invalid -> CIRCLE");
			shape = Shape.CIRCLE;
		}
		if (opacity < MIN_OPACITY || opacity > MAX_OPACITY) {
			int fixed = Math.max(MIN_OPACITY, Math.min(MAX_OPACITY, opacity));
			problems.add("rarity.opacity " + opacity + " out of range -> " + fixed);
			opacity = fixed;
		}
	}
}
