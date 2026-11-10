package com.nebulon.gui;

import com.nebulon.Nebulon;
import com.nebulon.config.ColorUtil;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * The RGB colour wheel texture (hue = angle, saturation = distance from the centre, full
 * brightness). Generated once in memory and registered as a dynamic texture; brightness is applied
 * at draw time by tinting the blit, so the texture never has to be regenerated.
 */
public final class ColorWheel {
	public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Nebulon.MOD_ID, "color_wheel");
	/** Texture resolution; drawn at a smaller GUI size so it stays smooth on high GUI scales. */
	public static final int TEXTURE_SIZE = 128;

	private static boolean registered;

	private ColorWheel() {
	}

	public static void ensureRegistered(Minecraft minecraft) {
		if (registered) {
			return;
		}
		NativeImage image = new NativeImage(TEXTURE_SIZE, TEXTURE_SIZE, true);
		double radius = TEXTURE_SIZE / 2.0;
		for (int y = 0; y < TEXTURE_SIZE; y++) {
			for (int x = 0; x < TEXTURE_SIZE; x++) {
				double dx = x + 0.5 - radius;
				double dy = y + 0.5 - radius;
				double distance = Math.sqrt(dx * dx + dy * dy) / radius;
				if (distance > 1.0) {
					image.setPixel(x, y, 0);
					continue;
				}
				float hue = (float) ((Math.atan2(dy, dx) / (2.0 * Math.PI) + 1.0) % 1.0);
				int rgb = ColorUtil.hsvToRgb(hue, (float) distance, 1.0F);
				// Soft one-pixel edge.
				int alpha = (int) Math.round(255.0 * Math.min(1.0, (1.0 - distance) * radius));
				image.setPixel(x, y, (alpha << 24) | rgb);
			}
		}
		minecraft.getTextureManager().register(TEXTURE, new DynamicTexture(() -> "nebulon color wheel", image));
		registered = true;
	}

	/** @return {hue, saturation} for a point relative to the wheel centre, or {@code null} if far outside. */
	public static float[] pick(double dx, double dy, double radius) {
		double distance = Math.sqrt(dx * dx + dy * dy) / radius;
		float hue = (float) ((Math.atan2(dy, dx) / (2.0 * Math.PI) + 1.0) % 1.0);
		return new float[]{hue, (float) Math.min(1.0, distance)};
	}
}
