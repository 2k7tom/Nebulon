package com.nebulon.gui;

import com.mojang.blaze3d.textures.GpuTextureView;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Glyph textures of the supersampled Nebulon fonts (Roboto). Minecraft samples font textures with
 * nearest filtering; for these the GUI text pipeline uses linear filtering instead (see the
 * FontTexture and GlyphRenderState mixins). The glyphs are rasterised at twice the screen
 * resolution and text is drawn on whole pixels, so each screen pixel averages exactly 2 x 2
 * texels: smooth edges at the same size. Vanilla and other fonts are not affected.
 */
public final class SmoothFonts {
	private static final String PREFIX = "nebulon:roboto_";
	private static final Set<GpuTextureView> VIEWS = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

	private SmoothFonts() {
	}

	/** Called for every new font texture with its label (the texture id, e.g. {@code nebulon:roboto_regular_4/0}). */
	public static void onFontTexture(String label, GpuTextureView view) {
		if (label != null && label.startsWith(PREFIX)) {
			VIEWS.add(view);
		}
	}

	public static boolean isSmooth(GpuTextureView view) {
		return VIEWS.contains(view);
	}
}
