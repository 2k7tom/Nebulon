package com.nebulon.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nebulon.render.HudTweaks;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** HUD Tweaks > No Damage Tilt: the camera does not tilt when you are hit (the death tilt stays). */
@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
	@Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
	private void nebulon$noDamageTilt(CameraRenderState cameraState, PoseStack poseStack, CallbackInfo ci) {
		if (HudTweaks.noDamageTilt() && !cameraState.entityRenderState.isDeadOrDying) {
			ci.cancel();
		}
	}
}
