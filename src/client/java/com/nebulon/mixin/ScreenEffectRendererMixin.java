package com.nebulon.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nebulon.render.HudTweaks;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** HUD Tweaks > Low Fire: the burning overlay is drawn lower, so it covers less of the screen. */
@Mixin(ScreenEffectRenderer.class)
abstract class ScreenEffectRendererMixin {
	private static final float NEBULON$FIRE_DROP = 0.3F;

	@Inject(method = "submitFire", at = @At("HEAD"))
	private static void nebulon$lowerFire(PoseStack poseStack, SubmitNodeCollector collector, TextureAtlasSprite sprite, CallbackInfo ci) {
		if (HudTweaks.lowFire()) {
			poseStack.translate(0.0F, -NEBULON$FIRE_DROP, 0.0F);
		}
	}

	@Inject(method = "submitFire", at = @At("RETURN"))
	private static void nebulon$restoreFire(PoseStack poseStack, SubmitNodeCollector collector, TextureAtlasSprite sprite, CallbackInfo ci) {
		if (HudTweaks.lowFire()) {
			poseStack.translate(0.0F, NEBULON$FIRE_DROP, 0.0F);
		}
	}
}
