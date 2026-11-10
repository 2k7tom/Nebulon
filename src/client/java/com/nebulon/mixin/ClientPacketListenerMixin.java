package com.nebulon.mixin;

import com.nebulon.hud.PerformanceStats;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Performance Display: reads the server's game time from the time updates it already sends (for TPS). */
@Mixin(ClientPacketListener.class)
abstract class ClientPacketListenerMixin {
	/** Auction Input: Hypixel's price sign opens the price box instead (after the switch to the main thread). */
	@Inject(method = "handleOpenSignEditor", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
			target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V"),
			cancellable = true)
	private void nebulon$auctionSign(net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket packet, CallbackInfo ci) {
		if (com.nebulon.item.AuctionInput.onOpenSignEditor(packet.getPos(), packet.isFrontText())) {
			ci.cancel();
		}
	}

	@Inject(method = "handleSetTime", at = @At("TAIL"))
	private void nebulon$measureTps(ClientboundSetTimePacket packet, CallbackInfo ci) {
		PerformanceStats.onServerTime(packet.gameTime());
	}
}
