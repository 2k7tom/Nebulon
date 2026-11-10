package com.nebulon.mixin;

import com.nebulon.client.NebulonClient;
import com.nebulon.config.HeldItemSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Held Item > Swing Speed: the local player's swing animation lasts shorter / longer. Only the
 * animation on your screen changes; attacks and their timing are untouched.
 */
@Mixin(LivingEntity.class)
abstract class LivingEntitySwingMixin {
	/** HUD Tweaks > No Nausea: for your own player the nausea effect has no strength (no wobble, no overlay). */
	@Inject(method = "getEffectBlendFactor", at = @At("HEAD"), cancellable = true)
	private void nebulon$noNausea(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, float partialTicks,
			CallbackInfoReturnable<Float> cir) {
		if (effect.equals(net.minecraft.world.effect.MobEffects.NAUSEA) && (Object) this == Minecraft.getInstance().player
				&& com.nebulon.render.HudTweaks.noNausea()) {
			cir.setReturnValue(0.0F);
		}
	}

	@Inject(method = "getCurrentSwingDuration", at = @At("RETURN"), cancellable = true)
	private void nebulon$swingSpeed(CallbackInfoReturnable<Integer> cir) {
		if ((Object) this != Minecraft.getInstance().player) {
			return;
		}
		NebulonClient client = NebulonClient.get();
		if (client == null) {
			return;
		}
		HeldItemSettings s = client.configManager().get().heldItem;
		if (!s.enabled) {
			return;
		}
		int duration = cir.getReturnValue();
		if (s.ignoreHaste) {
			// The item's own swing duration, without the Haste / Mining Fatigue adjustment.
			LivingEntity self = (LivingEntity) (Object) this;
			net.minecraft.world.InteractionHand hand = self.swingingArm != null ? self.swingingArm : net.minecraft.world.InteractionHand.MAIN_HAND;
			duration = self.getItemInHand(hand).getSwingAnimation().duration();
		}
		if (s.swingSpeed != 100) {
			duration = Math.round(duration * 100.0F / s.swingSpeed);
		}
		cir.setReturnValue(Math.max(1, duration));
	}
}
