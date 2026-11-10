package com.nebulon.mixin;

import com.nebulon.item.ProtectItem;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Protect Item: the drop key (outside menus) does not drop a protected held item. */
@Mixin(LocalPlayer.class)
abstract class LocalPlayerMixin {
	@Inject(method = "drop", at = @At("HEAD"), cancellable = true)
	private void nebulon$protectDrop(boolean all, CallbackInfoReturnable<Boolean> cir) {
		if (ProtectItem.onDropKey()) {
			cir.setReturnValue(false);
		}
	}
}
