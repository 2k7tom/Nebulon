package com.nebulon.render;

import com.nebulon.client.NebulonClient;
import com.nebulon.config.ParticlesSettings;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;

import java.util.Set;

/**
 * "Particles" (Render): decides which particles and effects are hidden. Purely client-side; the
 * hooks are in the ParticleEngine, ClientLevel, LightningBoltRenderer and EntityRenderer mixins.
 */
public final class ParticleFilter {
	private static final Set<ParticleType<?>> EXPLOSIONS = Set.of(ParticleTypes.EXPLOSION, ParticleTypes.EXPLOSION_EMITTER);
	private static final Set<ParticleType<?>> SMOKE = Set.of(
			ParticleTypes.SMOKE, ParticleTypes.LARGE_SMOKE, ParticleTypes.WHITE_SMOKE,
			ParticleTypes.CAMPFIRE_COSY_SMOKE, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, ParticleTypes.POOF);

	private ParticleFilter() {
	}

	private static ParticlesSettings settings() {
		return NebulonClient.get().configManager().get().particles;
	}

	/** Whether a particle of this type should not be created. */
	public static boolean hides(ParticleType<?> type) {
		ParticlesSettings s = settings();
		if (!s.enabled) {
			return false;
		}
		return (s.hideExplosions && EXPLOSIONS.contains(type)) || (s.hideSmoke && SMOKE.contains(type));
	}

	public static boolean hideBlockBreaking() {
		ParticlesSettings s = settings();
		return s.enabled && s.hideBlockBreaking;
	}

	public static boolean hideLightning() {
		ParticlesSettings s = settings();
		return s.enabled && s.hideLightning;
	}

	public static boolean hideEntityFire() {
		ParticlesSettings s = settings();
		return s.enabled && s.hideEntityFire;
	}
}
