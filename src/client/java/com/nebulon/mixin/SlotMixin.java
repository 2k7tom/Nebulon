package com.nebulon.mixin;

import com.nebulon.render.VisualTweaks;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Visual Tweaks > Remove Offhand: the offhand slot (player inventory index 40) is inactive on the
 * client, so screens neither draw it nor let it be hovered or clicked. Only container screens ask
 * {@code isActive()}; the server-side logic is not affected.
 */
@Mixin(Slot.class)
abstract class SlotMixin {
	@Shadow
	@Final
	public Container container;

	@Shadow
	public abstract int getContainerSlot();

	@Inject(method = "isActive", at = @At("HEAD"), cancellable = true)
	private void nebulon$removeOffhand(CallbackInfoReturnable<Boolean> cir) {
		if (this.container instanceof Inventory inventory && getContainerSlot() == Inventory.SLOT_OFFHAND
				&& inventory.player.level().isClientSide() && VisualTweaks.removeOffhand()) {
			cir.setReturnValue(false);
		}
	}
}
