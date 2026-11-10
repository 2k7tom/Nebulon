package com.nebulon.mixin;

import com.nebulon.gui.SmoothFonts;
import net.minecraft.client.gui.font.FontTexture;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

/** Remembers the glyph textures of the supersampled Nebulon fonts (see {@link SmoothFonts}). */
@Mixin(FontTexture.class)
abstract class FontTextureMixin {
	@Inject(method = "<init>", at = @At("TAIL"))
	private void nebulon$rememberSmoothFont(Supplier<String> label, GlyphRenderTypes renderTypes, boolean colored, CallbackInfo ci) {
		SmoothFonts.onFontTexture(label.get(), ((AbstractTexture) (Object) this).getTextureView());
	}
}
