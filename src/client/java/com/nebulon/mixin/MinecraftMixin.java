package com.nebulon.mixin;

import com.nebulon.render.VisualTweaks;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

/** Visual Tweaks > Dark Mode: regenerate the dark container textures after a resource pack change. */
@Mixin(Minecraft.class)
abstract class MinecraftMixin {
	@Inject(method = "reloadResourcePacks()Ljava/util/concurrent/CompletableFuture;", at = @At("HEAD"))
	private void nebulon$forgetDarkTextures(CallbackInfoReturnable<CompletableFuture<Void>> cir) {
		VisualTweaks.clearDarkTextures();
	}
}
