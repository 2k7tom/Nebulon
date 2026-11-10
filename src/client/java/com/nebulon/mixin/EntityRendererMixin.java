package com.nebulon.mixin;

import com.nebulon.render.ParticleFilter;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Entity render-state tweaks at the end of
 * {@code EntityRenderer#extractRenderState(Entity, EntityRenderState, float)}:
 * <ul>
 *     <li>Inferno Demonlord: attunement colours as outline.</li>
 *     <li>Particles > Hide Fire on Entities: {@code displayFireAnimation = false}, so burning mobs
 *     are drawn without the fire.</li>
 * </ul>
 * <p>
 * Risks: the target method is version-specific. If a future Minecraft version renames it or changes
 * its parameters, the game will fail to start with a clear mixin error (injectors.defaultRequire = 1)
 * instead of silently doing nothing. Other mods that also overwrite {@code outlineColor} at TAIL may
 * win or lose depending on mixin order.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
	/** Inferno Demonlord > Hide Fire Particles and Fireballs: fireballs are not drawn while the boss is up. */
	@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
	private void nebulon$hideFireballs(Entity entity, net.minecraft.client.renderer.culling.Frustum culler, double camX, double camY,
			double camZ, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
		if (com.nebulon.slayer.InfernoDemonlord.hidesEntity(entity)) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void nebulon$applyRenderTweaks(Entity entity, EntityRenderState state, float partialTicks, CallbackInfo ci) {
		int attunement = com.nebulon.slayer.InfernoDemonlord.outlineColor(entity);
		if (attunement != 0) {
			state.outlineColor = attunement;
		}
		if (ParticleFilter.hideEntityFire()) {
			state.displayFireAnimation = false;
		}
	}
}
