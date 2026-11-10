package com.nebulon.mixin;

import com.nebulon.hud.PerformanceStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Performance Display: counts mouse presses in game for the CPS value (input is not changed). */
@Mixin(MouseHandler.class)
abstract class MouseHandlerMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	/** Scrollable Tooltips: the wheel scrolls a tooltip that does not fit the screen. */
	@Inject(method = "onScroll", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screens/Screen;mouseScrolled(DDDD)Z"), cancellable = true)
	private void nebulon$scrollTooltip(long handle, double xoffset, double yoffset, CallbackInfo ci) {
		var window = minecraft.getWindow();
		boolean shift = com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT)
				|| com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT);
		double amount = yoffset != 0 ? Math.signum(yoffset) : Math.signum(xoffset);
		if (com.nebulon.render.ScrollableTooltips.onScroll(amount, shift)) {
			ci.cancel();
		}
	}

	@Shadow
	private boolean mouseGrabbed;

	@Shadow
	private double xpos;

	@Shadow
	private double ypos;

	/** Anti Mouse Reset: cursor position when the last menu closed, and when. */
	private double nebulon$savedX;
	private double nebulon$savedY;
	private long nebulon$grabbedAt;

	@Inject(method = "grabMouse", at = @At("HEAD"))
	private void nebulon$rememberCursor(CallbackInfo ci) {
		if (!this.mouseGrabbed && this.minecraft.isWindowActive()) {
			this.nebulon$savedX = this.xpos;
			this.nebulon$savedY = this.ypos;
			this.nebulon$grabbedAt = net.minecraft.util.Util.getMillis();
		}
	}

	/** A menu that opens right after another one closed keeps the cursor where it was (not the centre). */
	@Inject(method = "releaseMouse", at = @At("HEAD"), cancellable = true)
	private void nebulon$keepCursor(CallbackInfo ci) {
		if (this.mouseGrabbed && com.nebulon.client.NebulonClient.get() != null
				&& com.nebulon.client.NebulonClient.get().configManager().get().antiMouseReset
				&& net.minecraft.util.Util.getMillis() - this.nebulon$grabbedAt < 1000L) {
			this.mouseGrabbed = false;
			this.xpos = this.nebulon$savedX;
			this.ypos = this.nebulon$savedY;
			com.mojang.blaze3d.platform.InputConstants.grabOrReleaseMouse(this.minecraft.getWindow(), 212993, this.xpos, this.ypos);
			// Re-enabling the cursor makes GLFW put the real cursor back where it was when it was
			// captured (the centre); move it to the remembered position afterwards.
			org.lwjgl.glfw.GLFW.glfwSetCursorPos(this.minecraft.getWindow().handle(), this.xpos, this.ypos);
			ci.cancel();
		}
	}

	@Inject(method = "onButton", at = @At("HEAD"))
	private void nebulon$countClicks(long handle, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
		// action 1 = press; only clicks in the world count, not clicks in menus
		if (action == 1 && handle == minecraft.getWindow().handle() && minecraft.gui.screen() == null) {
			PerformanceStats.onClick(buttonInfo.button());
		}
	}
}
