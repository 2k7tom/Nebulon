package com.nebulon.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

/** Container clicks for Protect Item and Auction Input. */
@Mixin(MultiPlayerGameMode.class)
abstract class MultiPlayerGameModeMixin {
	/** Protect Item: blocks throwing / selling protected items in container menus. */
	@Inject(method = "handleContainerInput", at = @At("HEAD"), cancellable = true)
	private void nebulon$protectItem(int containerId, int slotNum, int buttonNum, net.minecraft.world.inventory.ContainerInput input,
			net.minecraft.world.entity.player.Player player, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
		com.nebulon.item.AuctionInput.onContainerClick(net.minecraft.client.Minecraft.getInstance().gui.screen(), slotNum);
		if (net.minecraft.client.Minecraft.getInstance().gui.screen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>
				&& com.nebulon.item.ProtectItem.onContainerClick(player.containerMenu, slotNum, input)) {
			ci.cancel();
		}
	}
}
