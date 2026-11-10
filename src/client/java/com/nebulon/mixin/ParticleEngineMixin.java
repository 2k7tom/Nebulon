package com.nebulon.mixin;

import com.nebulon.render.ParticleFilter;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Particles > Hide Explosions / Hide Smoke: these particle types are never created. */
@Mixin(ParticleEngine.class)
abstract class ParticleEngineMixin {
	@Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
	private void nebulon$hideParticle(ParticleOptions options, double x, double y, double z, double xa, double ya, double za,
			CallbackInfoReturnable<Particle> cir) {
		if (ParticleFilter.hides(options.getType()) || com.nebulon.slayer.InfernoDemonlord.hidesParticle(options.getType())) {
			cir.setReturnValue(null);
		}
	}
}
