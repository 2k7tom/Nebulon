package com.nebulon.mixin;

import com.nebulon.render.VisualTweaks;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Visual Tweaks > Remove Offhand: paints over the offhand slot frame of the inventory texture. */
@Mixin(InventoryScreen.class)
abstract class InventoryScreenMixin {
	/** Offhand slot frame in the inventory texture (slot at 77, 62 plus its 1 px border). */
	private static final int SLOT_X = 76;
	private static final int SLOT_Y = 61;
	private static final int SLOT_SIZE = 18;

	@Inject(method = "extractBackground", at = @At("TAIL"))
	private void nebulon$hideOffhandFrame(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		// In Mod Style the generated inventory texture already leaves the offhand slot out.
		if (VisualTweaks.removeOffhand() && !VisualTweaks.styledContainerOpen()) {
			AbstractContainerScreenAccessor screen = (AbstractContainerScreenAccessor) (Object) this;
			int x = screen.nebulon$leftPos() + SLOT_X;
			int y = screen.nebulon$topPos() + SLOT_Y;
			graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, VisualTweaks.backgroundColor());
		}
	}
}
