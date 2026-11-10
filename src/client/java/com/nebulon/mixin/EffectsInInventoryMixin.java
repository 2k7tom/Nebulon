package com.nebulon.mixin;

import com.nebulon.render.VisualTweaks;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Visual Tweaks > Hide Effects in Inventory: no potion effect list next to the inventory. */
@Mixin(EffectsInInventory.class)
abstract class EffectsInInventoryMixin {
	@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
	private void nebulon$hideEffects(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
		if (VisualTweaks.hideInventoryEffects()) {
			ci.cancel();
		}
	}

	/** Without the list the screen does not count as showing effects (so the HUD keeps its effect icons). */
	@Inject(method = "canSeeEffects", at = @At("HEAD"), cancellable = true)
	private void nebulon$noEffectsShown(CallbackInfoReturnable<Boolean> cir) {
		if (VisualTweaks.hideInventoryEffects()) {
			cir.setReturnValue(false);
		}
	}
}
