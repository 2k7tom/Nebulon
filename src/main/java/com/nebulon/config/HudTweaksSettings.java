package com.nebulon.config;

/** "HUD Tweaks" (Render): less annoying screen effects. */
public class HudTweaksSettings {
	public boolean enabled = false;
	/** "Low Fire": the burning overlay sits lower, so it covers less of the screen. */
	public boolean lowFire = true;
	/** "No Damage Tilt": the camera does not tilt when you take damage. */
	public boolean noDamageTilt = true;
	/** "No Nausea": no wobbling / distortion from the nausea effect. */
	public boolean noNausea = true;
}
