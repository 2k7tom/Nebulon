package com.nebulon.gui;

import com.nebulon.Nebulon;
import com.nebulon.config.GuiFont;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;

/**
 * Drawing surface of the settings screen with a <b>fixed resolution</b>, independent of Minecraft's
 * GUI scale.
 * <p>
 * Layout happens in "design units". One design unit is {@link #scale} real screen pixels, where the
 * scale only depends on the window height (1080p = 2, 1440p = 3, 2160p = 4). All drawing is done in
 * real pixels (the pose is reset with {@code 1 / guiScale}), so rounded corners are computed per
 * screen pixel with anti-aliased edges, and the Roboto font is rendered with an oversample equal to
 * the scale, which maps each glyph pixel 1:1 to a screen pixel (Minecraft samples font textures
 * with nearest filtering, so this is what keeps the text smooth).
 */
public final class Canvas {
	public final GuiGraphicsExtractor g;
	public final Font font;
	/** Real pixels per design unit (1..4). */
	public final int scale;
	private final Style regular;
	private final Style medium;

	public Canvas(GuiGraphicsExtractor g, Font font, int scale, GuiFont guiFont) {
		this(g, font, scale, guiFont, scale);
	}

	/**
	 * @param textResolution glyph resolution (font oversample). Use {@code scale} for 1:1 text; when
	 *                       the text is drawn enlarged (e.g. the resized Performance Display), pass
	 *                       the enlarged scale so glyphs stay sharp instead of showing big pixels.
	 */
	public Canvas(GuiGraphicsExtractor g, Font font, int scale, GuiFont guiFont, int textResolution) {
		this.g = g;
		this.font = font;
		this.scale = scale;
		if (guiFont.regular == null) {
			// Minecraft's (or the texture pack's) font.
			this.regular = Style.EMPTY;
			this.medium = Style.EMPTY;
			return;
		}
		// Supersampled fonts: glyphs at twice the pixel resolution, smoothed down by the linear
		// filter of their textures (see SmoothFonts), i.e. 2 x 2 samples per screen pixel.
		int wanted = guiFont.supersampled ? textResolution * 2 : textResolution;
		int resolution = Math.max(1, Math.min(guiFont.maxResolution, wanted));
		this.regular = Style.EMPTY.withFont(new FontDescription.Resource(fontId(guiFont.regular, resolution)));
		this.medium = Style.EMPTY.withFont(new FontDescription.Resource(fontId(guiFont.bold, resolution)));
	}

	/** The same surface with another font (a HUD element's own font, e.g. inside the Gui Editor). */
	public Canvas withFont(GuiFont guiFont) {
		return new Canvas(g, font, scale, guiFont, scale);
	}

	/** A canvas in Roboto, the font of the settings menu and the Gui Editor. */
	public Canvas(GuiGraphicsExtractor g, Font font, int scale) {
		this(g, font, scale, GuiFont.ROBOTO, scale);
	}

	/**
	 * Fixed scale for a window: one design unit per 540 px of window height (1080p = 2), limited so
	 * that the design space stays at least 960 units wide (all category panels fit side by side),
	 * between 1 and 4.
	 */
	public static int scaleFor(Window window) {
		int byHeight = (int) Math.round(window.getHeight() / 540.0);
		int byWidth = window.getWidth() / 960;
		return Math.max(1, Math.min(4, Math.min(byHeight, byWidth)));
	}

	private static Identifier fontId(String name, int scale) {
		return Identifier.fromNamespaceAndPath(Nebulon.MOD_ID, name + "_" + scale);
	}

	public int px(double designUnits) {
		return (int) Math.round(designUnits * scale);
	}

	// ---- Text ---------------------------------------------------------------------------------

	public MutableComponent styled(Component text, boolean bold) {
		return text.copy().withStyle(bold ? medium : regular);
	}

	public MutableComponent styled(String text, boolean bold) {
		return Component.literal(text).withStyle(bold ? medium : regular);
	}

	/** Width in design units. */
	public int width(Component text, boolean bold) {
		return font.width(styled(text, bold));
	}

	public int width(String text, boolean bold) {
		return font.width(styled(text, bold));
	}

	/** Draws text with its top-left corner at a design position (cap height is about 7 units). */
	public void text(Component text, double x, double y, int color, boolean bold) {
		text(text, x, y, color, bold, false);
	}

	/** Text with an optional drop shadow (e.g. on coloured map rooms). */
	public void text(Component text, double x, double y, int color, boolean bold, boolean shadow) {
		g.pose().pushMatrix();
		g.pose().translate(px(x), px(y));
		g.pose().scale(scale, scale);
		g.text(font, styled(text, bold), 0, 0, color, shadow);
		g.pose().popMatrix();
	}

	public void text(String text, double x, double y, int color, boolean bold) {
		text(Component.literal(text), x, y, color, bold);
	}

	public void textRight(Component text, double right, double y, int color, boolean bold) {
		text(text, right - width(text, bold), y, color, bold);
	}

	public void textCentered(Component text, double centerX, double y, int color, boolean bold) {
		text(text, centerX - width(text, bold) / 2.0, y, color, bold);
	}

	/** Pause (ms) at each end of a scrolling text, and its speed (design units per second). */
	private static final long SCROLL_PAUSE_MS = 1200L;
	private static final double SCROLL_SPEED = 22.0;

	/**
	 * Text limited to {@code maxWidth} design units. Text that fits is drawn normally; longer text
	 * is clipped at {@code x + maxWidth} and scrolls slowly to its end and back, pausing at both
	 * ends, so it never runs into the control next to it.
	 */
	public void scrollingText(Component text, double x, double y, double maxWidth, int color, boolean bold) {
		int textWidth = width(text, bold);
		if (textWidth <= maxWidth) {
			text(text, x, y, color, bold);
			return;
		}
		double overflow = textWidth - maxWidth;
		long travel = Math.max(1L, Math.round(overflow / SCROLL_SPEED * 1000.0));
		long cycle = 2 * SCROLL_PAUSE_MS + 2 * travel;
		long t = Util.getMillis() % cycle;
		double offset;
		if (t < SCROLL_PAUSE_MS) {
			offset = 0;
		} else if (t < SCROLL_PAUSE_MS + travel) {
			offset = overflow * (t - SCROLL_PAUSE_MS) / travel;
		} else if (t < 2 * SCROLL_PAUSE_MS + travel) {
			offset = overflow;
		} else {
			offset = overflow * (1.0 - (double) (t - 2 * SCROLL_PAUSE_MS - travel) / travel);
		}
		g.enableScissor(px(x), px(y - 2), px(x + maxWidth), px(y + 11));
		text(text, x - offset, y, color, bold);
		g.disableScissor();
	}

	/** Longest prefix of {@code text} that fits into {@code maxWidth} design units. */
	public String fitStart(String text, int maxWidth, boolean bold) {
		String result = text;
		while (!result.isEmpty() && width(result, bold) > maxWidth) {
			result = result.substring(0, result.length() - 1);
		}
		return result;
	}

	/** Longest suffix of {@code text} that fits into {@code maxWidth} design units. */
	public String fitEnd(String text, int maxWidth, boolean bold) {
		String result = text;
		while (!result.isEmpty() && width(result, bold) > maxWidth) {
			result = result.substring(1);
		}
		return result;
	}

	// ---- Shapes -------------------------------------------------------------------------------

	public void fill(double x0, double y0, double x1, double y1, int color) {
		g.fill(px(x0), px(y0), px(x1), px(y1), color);
	}

	public void fillGradient(double x0, double y0, double x1, double y1, int top, int bottom) {
		g.fillGradient(px(x0), px(y0), px(x1), px(y1), top, bottom);
	}

	/** Masks up to this many pixels are drawn as one quad; larger shapes use 4 cached corners. */
	private static final int SINGLE_MASK_PIXELS = 48 * 1024;

	/** Draws a cached mask texture 1:1 at real-pixel position (X, Y), tinted with {@code color}. */
	private void mask(Identifier texture, int X, int Y, float u, float v, int w, int h, int textureW, int textureH, int color) {
		g.blit(RenderPipelines.GUI_TEXTURED, texture, X, Y, u, v, w, h, textureW, textureH, color);
	}

	/**
	 * Filled rounded rectangle with anti-aliased corners. Small shapes are one cached mask; large
	 * ones are three fills plus four cached corner quarters (see {@link MaskTextures}).
	 */
	public void roundRect(double x0, double y0, double x1, double y1, double radius, int color) {
		int X0 = px(x0);
		int Y0 = px(y0);
		int X1 = px(x1);
		int Y1 = px(y1);
		int w = X1 - X0;
		int h = Y1 - Y0;
		if (w <= 0 || h <= 0) {
			return;
		}
		int r = Math.max(0, Math.min(px(radius), Math.min(w / 2, h / 2)));
		if (r == 0) {
			g.fill(X0, Y0, X1, Y1, color);
			return;
		}
		if (w * h <= SINGLE_MASK_PIXELS) {
			mask(MaskTextures.roundRect(w, h, r), X0, Y0, 0, 0, w, h, w, h, color);
			return;
		}
		g.fill(X0 + r, Y0, X1 - r, Y1, color);
		g.fill(X0, Y0 + r, X0 + r, Y1 - r, color);
		g.fill(X1 - r, Y0 + r, X1, Y1 - r, color);
		Identifier disc = MaskTextures.disc(r);
		int d = 2 * r;
		mask(disc, X0, Y0, 0, 0, r, r, d, d, color);
		mask(disc, X1 - r, Y0, r, 0, r, r, d, d, color);
		mask(disc, X0, Y1 - r, 0, r, r, r, d, d, color);
		mask(disc, X1 - r, Y1 - r, r, r, r, r, d, d, color);
	}

	/** Rounded rectangle with a border one real pixel wide (scaled with the resolution). */
	public void roundRectBordered(double x0, double y0, double x1, double y1, double radius, int fill, int border) {
		roundRect(x0, y0, x1, y1, radius, border);
		double inset = Math.max(1.0, scale / 2.0) / scale;
		roundRect(x0 + inset, y0 + inset, x1 - inset, y1 - inset, Math.max(0.0, radius - inset), fill);
	}

	/**
	 * Rounded border only (one real pixel wide, scaled like {@link #roundRectBordered}), without
	 * anything underneath, so a translucent fill drawn inside it never shows the border colour.
	 */
	public void roundRectOutline(double x0, double y0, double x1, double y1, double radius, int color) {
		int X0 = px(x0);
		int Y0 = px(y0);
		int X1 = px(x1);
		int Y1 = px(y1);
		int w = X1 - X0;
		int h = Y1 - Y0;
		if (w <= 0 || h <= 0) {
			return;
		}
		int t = Math.max(1, scale / 2);
		int r = Math.max(t, Math.min(px(radius), Math.min(w / 2, h / 2)));
		if (w * h <= SINGLE_MASK_PIXELS) {
			mask(MaskTextures.outline(w, h, r, t), X0, Y0, 0, 0, w, h, w, h, color);
			return;
		}
		g.fill(X0 + r, Y0, X1 - r, Y0 + t, color);
		g.fill(X0 + r, Y1 - t, X1 - r, Y1, color);
		g.fill(X0, Y0 + r, X0 + t, Y1 - r, color);
		g.fill(X1 - t, Y0 + r, X1, Y1 - r, color);
		Identifier ring = MaskTextures.ring(r, t);
		int d = 2 * r;
		mask(ring, X0, Y0, 0, 0, r, r, d, d, color);
		mask(ring, X1 - r, Y0, r, 0, r, r, d, d, color);
		mask(ring, X0, Y1 - r, 0, r, r, r, d, d, color);
		mask(ring, X1 - r, Y1 - r, r, r, r, r, d, d, color);
	}

	/** Small magnifying glass (search bar). */
	public void magnifier(double x, double y, int color) {
		int size = px(8);
		int s = scale;
		Identifier glass = MaskTextures.get("magnifier:" + s, size, size, (px, py) -> {
			double cx = (px + 0.5) / s - 2.75;
			double cy = (py + 0.5) / s - 2.75;
			double ring = Math.max(0.0, Math.min(1.0, 0.5 - (Math.abs(Math.hypot(cx, cy) - 2.3) - 0.45) * s));
			return Math.max(ring, MaskTextures.segment(px, py, s, 4.6, 4.6, 7.0, 7.0, 0.55));
		});
		mask(glass, px(x), px(y), 0, 0, size, size, size, size, color);
	}
	/** Draws the colour wheel texture tinted with {@code brightness}. */
	public void colorWheel(double x, double y, double size, float brightness) {
		int shade = Math.round(brightness * 255.0F);
		g.blit(RenderPipelines.GUI_TEXTURED, ColorWheel.TEXTURE, px(x), px(y), 0.0F, 0.0F, px(size), px(size),
				ColorWheel.TEXTURE_SIZE, ColorWheel.TEXTURE_SIZE, ColorWheel.TEXTURE_SIZE, ColorWheel.TEXTURE_SIZE,
				ARGB.color(255, shade, shade, shade));
	}
}
