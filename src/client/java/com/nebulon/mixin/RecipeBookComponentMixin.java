package com.nebulon.mixin;

import com.nebulon.render.VisualTweaks;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Visual Tweaks > Remove Recipebook: the book always counts as closed (it never opens). */
@Mixin(RecipeBookComponent.class)
abstract class RecipeBookComponentMixin {
	@Inject(method = "isVisibleAccordingToBookData", at = @At("HEAD"), cancellable = true)
	private void nebulon$removeRecipeBook(CallbackInfoReturnable<Boolean> cir) {
		if (VisualTweaks.removeRecipeBook()) {
			cir.setReturnValue(false);
		}
	}
}
