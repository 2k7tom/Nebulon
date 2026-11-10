package com.nebulon.mixin;

import com.nebulon.render.ParticleFilter;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Particles:
 * <ul>
 *     <li>Hide Block breaking Particles: no particles when a block breaks or is being mined.</li>
 *     <li>Hide Lightning: no sky flash (like the vanilla "Hide Lightning Flashes" option).</li>
 * </ul>
 */
@Mixin(ClientLevel.class)
abstract class ClientLevelMixin {
	@Inject(method = "addDestroyBlockEffect", at = @At("HEAD"), cancellable = true)
	private void nebulon$hideDestroyParticles(BlockPos pos, BlockState blockState, CallbackInfo ci) {
		if (ParticleFilter.hideBlockBreaking()) {
			ci.cancel();
		}
	}

	@Inject(method = "addBreakingBlockEffect", at = @At("HEAD"), cancellable = true)
	private void nebulon$hideCrackParticles(BlockPos pos, Direction direction, CallbackInfo ci) {
		if (ParticleFilter.hideBlockBreaking()) {
			ci.cancel();
		}
	}

	@Inject(method = "getSkyFlashTime", at = @At("HEAD"), cancellable = true)
	private void nebulon$hideLightningFlash(CallbackInfoReturnable<Integer> cir) {
		if (ParticleFilter.hideLightning()) {
			cir.setReturnValue(0);
		}
	}
}
