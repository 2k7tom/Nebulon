package com.nebulon.mixin;

import com.nebulon.render.VisualTweaks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Visual Tweaks > Dark Mode: blurs the world behind container screens (vanilla only darkens it). */
@Mixin(Screen.class)
abstract class ScreenMixin {
	@Shadow
	protected Minecraft minecraft;

	@Shadow
	protected abstract void extractBlurredBackground(GuiGraphicsExtractor graphics);

	@Shadow
	public abstract void extractTransparentBackground(GuiGraphicsExtractor graphics);

	@Shadow
	public int width;

	@Shadow
	public int height;

	@Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
	private void nebulon$blurBehindContainers(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		if ((Object) this instanceof AbstractContainerScreen<?> && this.minecraft.level != null && VisualTweaks.modStyle()) {
			this.extractBlurredBackground(graphics);
			this.extractTransparentBackground(graphics);
			this.minecraft.gui.hud.extractDeferredSubtitles();
			ci.cancel();
		}
	}
}
