package com.nebulon.config;

/** "Held Item" (Render): position, size and swing of the item in first person. */
public class HeldItemSettings {
	public enum SwingStyle {
		/** Vanilla swing. */
		NORMAL,
		/** No swing: the item only tilts forward once, briefly. */
		TILT,
		/** No hit animation at all. */
		NONE
	}

	public boolean enabled = false;
	/** Offsets in hundredths of a block (sideways, up, forward). */
	public int offsetX = 0;
	public int offsetY = 0;
	public int offsetZ = 0;
	/** Size in percent. */
	public int scale = 100;
	/** Swing speed in percent (200 = twice as fast). */
	public int swingSpeed = 100;
	public SwingStyle swingStyle = SwingStyle.NORMAL;
	/** "Ignore Haste": Haste / Mining Fatigue do not change the swing speed. */
	public boolean ignoreHaste = false;
	/** "No Equip Animation": a new item is in the hand at once instead of being lowered and raised. */
	public boolean noEquipAnimation = false;

	public void validate(java.util.List<String> problems) {
		if (swingStyle == null) {
			problems.add("heldItem.swingStyle invalid -> NORMAL");
			swingStyle = SwingStyle.NORMAL;
		}
		offsetX = Math.max(-100, Math.min(100, offsetX));
		offsetY = Math.max(-100, Math.min(100, offsetY));
		offsetZ = Math.max(-100, Math.min(100, offsetZ));
		scale = Math.max(10, Math.min(200, scale));
		swingSpeed = Math.max(10, Math.min(300, swingSpeed));
	}
}
