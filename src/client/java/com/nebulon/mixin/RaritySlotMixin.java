package com.nebulon.mixin;

import com.nebulon.gui.RarityBackground;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Rarity (Render): the rarity circle behind items in container slots, drawn before the item. */
@Mixin(AbstractContainerScreen.class)
abstract class RaritySlotMixin {
	@Inject(method = "extractSlot", at = @At("HEAD"))
	private void nebulon$rarityBackground(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
		RarityBackground.draw(graphics, slot.getItem(), slot.x, slot.y);
	}
}
