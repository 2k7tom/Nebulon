package com.nebulon.gui;

import com.mojang.blaze3d.platform.NativeImage;
import com.nebulon.Nebulon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cached anti-aliased shape masks (white, alpha = coverage) for the settings screen. A shape is
 * computed once per size and then drawn as a single tinted blit.
 * <p>
 * Why: Minecraft's GUI render state compares every new element with the elements already added
 * (to pick a layer), so drawing a rounded corner as dozens of one-pixel fills made the menu cost
 * 20-60 ms per frame. One textured quad per shape keeps the element count small.
 * <p>
 * Least recently used masks are released when the cache grows past its pixel budget.
 */
final class MaskTextures {
	/** Upper bound for all cached mask pixels (4 bytes each). */
	private static final long PIXEL_BUDGET = 8L * 1024 * 1024;

	@FunctionalInterface
	interface Coverage {
		/** Coverage 0..1 of the pixel (x, y) of a w x h mask. */
		double at(int x, int y);
	}

	private record Mask(Identifier id, int pixels) {
	}

	private static final LinkedHashMap<String, Mask> CACHE = new LinkedHashMap<>(256, 0.75F, true);
	private static long cachedPixels;
	private static int counter;

	private MaskTextures() {
	}

	/** A mask for {@code key}, computed with {@code coverage} on first use. */
	static Identifier get(String key, int width, int height, Coverage coverage) {
		Mask mask = CACHE.get(key);
		if (mask != null) {
			return mask.id();
		}
		NativeImage image = new NativeImage(width, height, true);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				double c = coverage.at(x, y);
				int alpha = c <= 0.0 ? 0 : (int) Math.round(Math.min(1.0, c) * 255.0);
				image.setPixel(x, y, (alpha << 24) | 0xFFFFFF);
			}
		}
		Identifier id = Identifier.fromNamespaceAndPath(Nebulon.MOD_ID, "mask/" + (counter++));
		Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "nebulon mask " + key, image));
		CACHE.put(key, new Mask(id, width * height));
		cachedPixels += (long) width * height;
		evict();
		return id;
	}

	private static void evict() {
		Iterator<Map.Entry<String, Mask>> it = CACHE.entrySet().iterator();
		while (cachedPixels > PIXEL_BUDGET && CACHE.size() > 1 && it.hasNext()) {
			Mask oldest = it.next().getValue();
			Minecraft.getInstance().getTextureManager().release(oldest.id());
			cachedPixels -= oldest.pixels();
			it.remove();
		}
	}

	/** Coverage of a pixel centre by a rounded rectangle (x0, y0)-(x1, y1) with corner radius r. */
	static double roundRect(int px, int py, double x0, double y0, double x1, double y1, double r) {
		double x = px + 0.5;
		double y = py + 0.5;
		if (r <= 0.0) {
			return x >= x0 && x < x1 && y >= y0 && y < y1 ? 1.0 : 0.0;
		}
		double cx = Math.max(x0 + r, Math.min(x1 - r, x));
		double cy = Math.max(y0 + r, Math.min(y1 - r, y));
		double d = Math.hypot(x - cx, y - cy);
		return Math.max(0.0, Math.min(1.0, r - d + 0.5));
	}

	/** Filled rounded rectangle of w x h real pixels with radius r. */
	static Identifier roundRect(int w, int h, int r) {
		return get("rr:" + w + "x" + h + ":" + r, w, h, (x, y) -> roundRect(x, y, 0, 0, w, h, r));
	}

	/** Quarter circles for the corners of large rounded rectangles: a 2r x 2r disc. */
	static Identifier disc(int r) {
		return get("disc:" + r, 2 * r, 2 * r, (x, y) -> roundRect(x, y, 0, 0, 2 * r, 2 * r, r));
	}

	/** Rounded border of thickness t: outer shape minus the inset inner shape. */
	static Identifier outline(int w, int h, int r, int t) {
		return get("ol:" + w + "x" + h + ":" + r + ":" + t, w, h,
				(x, y) -> Math.max(0.0, roundRect(x, y, 0, 0, w, h, r) - roundRect(x, y, t, t, w - t, h - t, Math.max(0, r - t))));
	}

	/** Corners of a large rounded border: a 2r x 2r ring of thickness t. */
	static Identifier ring(int r, int t) {
		return get("ring:" + r + ":" + t, 2 * r, 2 * r,
				(x, y) -> Math.max(0.0, roundRect(x, y, 0, 0, 2 * r, 2 * r, r) - roundRect(x, y, t, t, 2 * r - t, 2 * r - t, Math.max(0, r - t))));
	}

	/** Coverage of a pixel centre (in real pixels, {@code scale} pixels per unit) by a thick line segment in design units. */
	static double segment(int px, int py, int scale, double ax, double ay, double bx, double by, double radius) {
		double x = (px + 0.5) / scale;
		double y = (py + 0.5) / scale;
		double dx = bx - ax;
		double dy = by - ay;
		double t = Math.max(0.0, Math.min(1.0, ((x - ax) * dx + (y - ay) * dy) / (dx * dx + dy * dy)));
		double d = Math.hypot(x - ax - t * dx, y - ay - t * dy) - radius;
		return Math.max(0.0, Math.min(1.0, 0.5 - d * scale));
	}
}
