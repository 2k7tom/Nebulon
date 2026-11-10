package com.nebulon.mixin;

import com.nebulon.client.NebulonClient;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fullbright (Render): at the end of {@code LightmapRenderStateExtractor#extract} the lightmap gets
 * full night vision, maximum brightness and no darkness pulse, so every block is lit fully. Only the
 * local lightmap changes; no potion effect is added.
 */
@Mixin(LightmapRenderStateExtractor.class)
public abstract class LightmapRenderStateExtractorMixin {
	@Inject(method = "extract", at = @At("TAIL"))
	private void nebulon$fullbright(LightmapRenderState state, float partialTicks, CallbackInfo ci) {
		NebulonClient client = NebulonClient.get();
		if (state.needsUpdate && client != null && client.configManager().get().fullbright) {
			state.nightVisionEffectIntensity = 1.0F;
			state.nightVisionColor = LightmapRenderStateExtractor.WHITE;
			state.brightness = Math.max(state.brightness, 1.0F);
			state.darknessEffectScale = 0.0F;
		}
	}
}
