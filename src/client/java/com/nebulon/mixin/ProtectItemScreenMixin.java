package com.nebulon.mixin;

import com.nebulon.item.ProtectItem;
import com.nebulon.item.SlotBinding;
import com.nebulon.keybind.NebulonKeybinds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Protect Item and Slot Binding in container menus: the Protect Key (keyboard or mouse button)
 * protects the hovered item, "Show Protected Items" marks protected items with a small green "P";
 * the Binding key links slots and Shift + left click swaps bound slots.
 */
@Mixin(AbstractContainerScreen.class)
abstract class ProtectItemScreenMixin {
	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void nebulon$protectKey(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
		if (NebulonKeybinds.protectItem.matches(event) && ProtectItem.protectSlot(hovered())) {
			cir.setReturnValue(true);
		} else if (NebulonKeybinds.slotBinding.matches(event) && SlotBinding.onBindKey(self())) {
			cir.setReturnValue(true);
		} else if (com.nebulon.item.AuctionInput.onKey(self(), event.key())) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void nebulon$protectMouse(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
		if (NebulonKeybinds.protectItem.matchesMouse(event) && ProtectItem.protectSlot(hovered())) {
			cir.setReturnValue(true);
		} else if (NebulonKeybinds.slotBinding.matchesMouse(event) && SlotBinding.onBindKey(self())) {
			cir.setReturnValue(true);
		} else if (SlotBinding.onShiftClick(self(), event.button(), event.hasShiftDown() || shiftHeld())) {
			cir.setReturnValue(true);
		}
	}

	private static boolean shiftHeld() {
		var window = Minecraft.getInstance().getWindow();
		return com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT)
				|| com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT);
	}

	private AbstractContainerScreen<?> self() {
		return (AbstractContainerScreen<?>) (Object) this;
	}

	@Inject(method = "extractSlot", at = @At("TAIL"))
	private void nebulon$protectedMarker(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
		com.nebulon.item.UltimateEnchant.draw(graphics, slot.getItem(), slot.x, slot.y, false);		if (ProtectItem.showMarker(slot.getItem())) {
			graphics.pose().pushMatrix();
			graphics.pose().translate(slot.x + 1, slot.y + 1);
			graphics.pose().scale(0.75F, 0.75F);
			graphics.text(Minecraft.getInstance().font, Component.literal("P"), 0, 0, 0xFF55FF55, true);
			graphics.pose().popMatrix();
		}
	}

	private Slot hovered() {
		return ((AbstractContainerScreenAccessor) this).nebulon$hoveredSlot();
	}
}
