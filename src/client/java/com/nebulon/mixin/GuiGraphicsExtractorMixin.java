package com.nebulon.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.nebulon.render.VisualTweaks;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Visual Tweaks > Dark Mode, while a container screen is open:
 * <ul>
 *     <li>container textures are swapped for their generated dark versions;</li>
 *     <li>the dark grey container labels ("Inventory", chest names ...) become light.</li>
 * </ul>
 */
@Mixin(GuiGraphicsExtractor.class)
abstract class GuiGraphicsExtractorMixin {
	/** Scrollable Tooltips: where the tooltip lies and how far it is scrolled. */
	@Inject(method = "tooltip", at = @At("HEAD"))
	private void nebulon$prepareTooltipScroll(net.minecraft.client.gui.Font font,
			java.util.List<net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent> lines, int xo, int yo,
			net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner positioner, Identifier style, CallbackInfo ci) {
		GuiGraphicsExtractor self = (GuiGraphicsExtractor) (Object) this;
		com.nebulon.render.ScrollableTooltips.prepare(font, lines, xo, yo, positioner, self.guiWidth(), self.guiHeight());
	}

	/** Scrollable Tooltips: draws the whole tooltip moved by the scroll offset. */
	@Inject(method = "tooltip", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix3x2fStack;pushMatrix()Lorg/joml/Matrix3x2fStack;",
			shift = At.Shift.AFTER))
	private void nebulon$scrollTooltip(net.minecraft.client.gui.Font font,
			java.util.List<net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent> lines, int xo, int yo,
			net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner positioner, Identifier style, CallbackInfo ci) {
		float dx = com.nebulon.render.ScrollableTooltips.offsetX();
		float dy = com.nebulon.render.ScrollableTooltips.offsetY();
		if (dx != 0 || dy != 0) {
			((GuiGraphicsExtractor) (Object) this).pose().translate(dx, dy);
		}
	}

	@ModifyVariable(
			method = "innerBlit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIIFFFFI)V",
			at = @At("HEAD"), argsOnly = true)
	private Identifier nebulon$darkContainerTexture(Identifier location) {
		return VisualTweaks.modStyle() ? VisualTweaks.styledTexture(location) : location;
	}

	/** Plain stretched container sprites (creative tabs, scroller, anvil name field ...): Mod Style copy. */
	@Inject(method = "blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V",
			at = @At("HEAD"), cancellable = true)
	private void nebulon$darkContainerSprite(RenderPipeline pipeline, Identifier location, int x, int y, int width, int height,
			int color, CallbackInfo ci) {
		// Menu widgets (buttons, sliders, text fields, tabs, scrollers) in every menu; rounded slot highlight.
		if (VisualTweaks.drawWidget((GuiGraphicsExtractor) (Object) this, location, x, y, width, height, color)
				|| VisualTweaks.drawSlotHighlight((GuiGraphicsExtractor) (Object) this, location, x, y)
				|| VisualTweaks.drawHotbar((GuiGraphicsExtractor) (Object) this, location, x, y)) {
			ci.cancel();
			return;
		}
		if (!VisualTweaks.styledContainerOpen()) {
			return;
		}
		VisualTweaks.DarkSprite sprite = VisualTweaks.styledSprite(location);
		if (sprite != null) {
			((GuiGraphicsExtractor) (Object) this).blit(pipeline, sprite.texture(), x, y, 0.0F, 0.0F, width, height,
					sprite.width(), sprite.height(), sprite.width(), sprite.height(), color);
			ci.cancel();
		}
	}

	/** The slot hover highlight (a nine-slice sprite) is toned down so it does not glare on dark slots. */
	@ModifyVariable(method = "blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V",
			at = @At("HEAD"), argsOnly = true, ordinal = 4)
	private int nebulon$dimSlotHighlight(int color, RenderPipeline pipeline, Identifier location) {
		if (location.getPath().startsWith("container/slot_highlight") && VisualTweaks.styledContainerOpen()) {
			return net.minecraft.util.ARGB.multiply(color, VisualTweaks.SLOT_HIGHLIGHT_TINT);
		}
		return color;
	}

	@ModifyVariable(
			method = "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V",
			at = @At("HEAD"), argsOnly = true, ordinal = 2)
	private int nebulon$lightContainerLabel(int color) {
		return color == VisualTweaks.LABEL_COLOR && VisualTweaks.styledContainerOpen() ? VisualTweaks.labelColor() : color;
	}
}
