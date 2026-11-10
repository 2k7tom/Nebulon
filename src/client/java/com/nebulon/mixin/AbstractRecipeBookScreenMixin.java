package com.nebulon.mixin;

import com.nebulon.render.VisualTweaks;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Visual Tweaks > Remove Recipebook: the green recipe book button is not added at all. */
@Mixin(AbstractRecipeBookScreen.class)
abstract class AbstractRecipeBookScreenMixin {
	@Inject(method = "initButton", at = @At("HEAD"), cancellable = true)
	private void nebulon$removeRecipeBookButton(CallbackInfo ci) {
		if (VisualTweaks.removeRecipeBook()) {
			ci.cancel();
		}
	}

	/** Slot Binding: lines and borders over the inventory, after the slots and items (as in NoammAddons). */
	@Inject(method = "extractRenderState", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;nextStratum()V", ordinal = 0, shift = At.Shift.AFTER))
	private void nebulon$slotBinding(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		com.nebulon.item.SlotBinding.draw(graphics, mouseX, mouseY,
				(net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) (Object) this);
	}
}
