package com.nebulon.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nebulon.render.ParticleFilter;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LightningBoltRenderer;
import net.minecraft.client.renderer.entity.state.LightningBoltRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Particles > Hide Lightning: lightning bolts (e.g. Thunderlord / Thunderbolt) are not drawn. */
@Mixin(LightningBoltRenderer.class)
abstract class LightningBoltRendererMixin {
	@Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LightningBoltRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
			at = @At("HEAD"), cancellable = true)
	private void nebulon$hideLightning(LightningBoltRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
			CameraRenderState camera, CallbackInfo ci) {
		if (ParticleFilter.hideLightning()) {
			ci.cancel();
		}
	}
}
