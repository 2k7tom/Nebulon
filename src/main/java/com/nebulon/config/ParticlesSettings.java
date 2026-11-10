package com.nebulon.config;

/** "Particles" (Render): hides selected particles and effects on the client. */
public class ParticlesSettings {
	public boolean enabled = false;
	/** Particles of breaking blocks (when a block breaks and while it is being mined). */
	public boolean hideBlockBreaking = true;
	/** Lightning bolts (e.g. Thunderlord / Thunderbolt) and their sky flash. */
	public boolean hideLightning = true;
	/** Explosion particles (creepers, TNT, ...). */
	public boolean hideExplosions = true;
	/** Fire drawn on burning mobs and players. */
	public boolean hideEntityFire = true;
	/** All kinds of smoke particles. */
	public boolean hideSmoke = true;
}
