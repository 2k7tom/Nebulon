package com.nebulon.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nebulon.client.NebulonClient;
import com.nebulon.config.HeldItemSettings;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Held Item (Render): moves and resizes the item in first person and changes its hit animation
 * (normal swing, a short forward tilt, or none). Only what is drawn changes.
 */
@Mixin(ItemInHandRenderer.class)
abstract class ItemInHandRendererMixin {
	private static HeldItemSettings nebulon$settings() {
		NebulonClient client = NebulonClient.get();
		if (client == null) {
			return null;
		}
		HeldItemSettings s = client.configManager().get().heldItem;
		return s.enabled ? s : null;
	}

	@org.spongepowered.asm.mixin.Shadow
	private ItemStack mainHandItem;
	@org.spongepowered.asm.mixin.Shadow
	private ItemStack offHandItem;
	@org.spongepowered.asm.mixin.Shadow
	private float mainHandHeight;
	@org.spongepowered.asm.mixin.Shadow
	private float oMainHandHeight;
	@org.spongepowered.asm.mixin.Shadow
	private float offHandHeight;
	@org.spongepowered.asm.mixin.Shadow
	private float oOffHandHeight;

	private static boolean nebulon$noEquipAnimation() {
		NebulonClient client = NebulonClient.get();
		if (client == null) {
			return false;
		}
		HeldItemSettings s = client.configManager().get().heldItem;
		return s.enabled && s.noEquipAnimation;
	}

	/** No Equip Animation: the held items are always shown fully raised, a new item appears at once. */
	@Inject(method = "tick", at = @At("TAIL"))
	private void nebulon$noEquip(CallbackInfo ci) {
		var player = net.minecraft.client.Minecraft.getInstance().player;
		if (player == null || player.isHandsBusy() || !nebulon$noEquipAnimation()) {
			return;
		}
		this.mainHandItem = player.getMainHandItem();
		this.offHandItem = player.getOffhandItem();
		this.mainHandHeight = 1.0F;
		this.oMainHandHeight = 1.0F;
		this.offHandHeight = 1.0F;
		this.oOffHandHeight = 1.0F;
	}

	@Inject(method = "itemUsed", at = @At("HEAD"), cancellable = true)
	private void nebulon$noEquipOnUse(InteractionHand hand, CallbackInfo ci) {
		if (nebulon$noEquipAnimation()) {
			ci.cancel();
		}
	}

	/** Position offset, in view space before the item is moved into the hand (mirrored for the left hand). */
	@Inject(method = "applyItemArmTransform", at = @At("HEAD"))
	private void nebulon$offset(PoseStack poseStack, HumanoidArm arm, float inverseArmHeight, CallbackInfo ci) {
		HeldItemSettings s = nebulon$settings();
		if (s != null) {
			int invert = arm == HumanoidArm.RIGHT ? 1 : -1;
			poseStack.translate(invert * s.offsetX / 100.0F, s.offsetY / 100.0F, -s.offsetZ / 100.0F);
		}
	}

	/** Size: the item is scaled right before it is drawn. */
	@Inject(method = "submitArmWithItem", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"))
	private void nebulon$scale(AbstractClientPlayer player, float frameInterp, float xRot, InteractionHand hand, float attack,
			ItemStack itemStack, float inverseArmHeight, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
			CallbackInfo ci) {
		HeldItemSettings s = nebulon$settings();
		if (s != null && s.scale != 100) {
			float k = s.scale / 100.0F;
			poseStack.scale(k, k, k);
		}
	}

	/** Swing Style: Tilt = the item only tips forward briefly; None = no hit animation. */
	@Inject(method = "swingArm", at = @At("HEAD"), cancellable = true)
	private void nebulon$swingStyle(float attack, PoseStack poseStack, int invert, HumanoidArm arm, CallbackInfo ci) {
		HeldItemSettings s = nebulon$settings();
		if (s == null || s.swingStyle == HeldItemSettings.SwingStyle.NORMAL) {
			return;
		}
		if (s.swingStyle == HeldItemSettings.SwingStyle.TILT) {
			float tilt = Mth.sin(attack * (float) Math.PI);
			poseStack.mulPose(Axis.XP.rotationDegrees(tilt * -50.0F));
		}
		ci.cancel();
	}
}
