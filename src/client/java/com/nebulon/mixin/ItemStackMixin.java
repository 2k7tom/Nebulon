package com.nebulon.mixin;

import com.nebulon.item.CompactStars;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Item Tweaks &gt; Compact Stars: the item name (tooltip, held item name above the hotbar, ...) shows
 * master stars as red stars. Only on the client thread; the item itself is not changed.
 */
@Mixin(ItemStack.class)
abstract class ItemStackMixin {
	@Inject(method = "getHoverName", at = @At("RETURN"), cancellable = true)
	private void nebulon$compactStars(CallbackInfoReturnable<Component> cir) {
		Component name = cir.getReturnValue();
		Component converted = CompactStars.apply(name);
		if (converted != name) {
			cir.setReturnValue(converted);
		}
	}
}
