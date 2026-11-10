package com.nebulon.render;

import com.nebulon.client.NebulonClient;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.util.Util;
import org.joml.Vector2ic;

import java.util.List;

/**
 * "Scrollable Tooltips" (Render): tooltips taller (or wider) than the screen can be scrolled with
 * the mouse wheel (Shift + wheel: sideways). Such a tooltip first shows its top; scrolling moves
 * it until its bottom is visible. Smaller tooltips and normal wheel use are unchanged.
 */
public final class ScrollableTooltips {
	/** GUI pixels per wheel notch. */
	private static final double STEP = 12.0;
	/** Space kept between the tooltip background and the screen edge. */
	private static final int MARGIN = 4;
	/** A tooltip not drawn for this long is gone; the next one starts at the top again. */
	private static final long FORGET_MS = 200L;

	private static double scrollY;
	private static double scrollX;
	/** Scroll ranges of the last drawn tooltip (0 = fits). */
	private static int rangeY;
	private static int rangeX;
	private static long signature;
	private static long lastDrawn;
	/** Offset applied to the tooltip being drawn. */
	private static float offsetX;
	private static float offsetY;

	private ScrollableTooltips() {
	}

	public static boolean enabled() {
		NebulonClient client = NebulonClient.get();
		return client != null && client.configManager().get().scrollableTooltips;
	}

	/**
	 * Before a tooltip is drawn: works out where vanilla places it and how far it can be scrolled,
	 * and the offset to draw it with ({@link #offsetX()}, {@link #offsetY()}).
	 */
	public static void prepare(Font font, List<ClientTooltipComponent> lines, int xo, int yo, ClientTooltipPositioner positioner,
			int guiWidth, int guiHeight) {
		offsetX = 0;
		offsetY = 0;
		if (!enabled()) {
			return;
		}
		int w = 0;
		int h = lines.size() == 1 ? -2 : 0;
		for (ClientTooltipComponent line : lines) {
			w = Math.max(w, line.getWidth(font));
			h += line.getHeight(font);
		}
		Vector2ic pos = positioner.positionTooltip(guiWidth, guiHeight, xo, yo, w, h);
		long now = Util.getMillis();
		long sig = ((long) lines.size() << 40) ^ ((long) w << 20) ^ h;
		if (sig != signature || now - lastDrawn > FORGET_MS) {
			signature = sig;
			scrollX = 0;
			scrollY = 0;
		}
		lastDrawn = now;

		// Background reaches about 4 px beyond the text area on every side.
		rangeY = Math.max(0, (h + 2 * MARGIN) - (guiHeight - 2 * MARGIN));
		scrollY = Math.max(0, Math.min(rangeY, scrollY));
		if (rangeY > 0) {
			// Top of the tooltip at the top of the screen, then moved up by the scroll distance.
			offsetY = (float) (2 * MARGIN - pos.y() - scrollY);
		}
		rangeX = Math.max(0, (w + 2 * MARGIN) - (guiWidth - 2 * MARGIN));
		scrollX = Math.max(0, Math.min(rangeX, scrollX));
		if (rangeX > 0) {
			offsetX = (float) (2 * MARGIN - pos.x() - scrollX);
		}
	}

	public static float offsetX() {
		return offsetX;
	}

	public static float offsetY() {
		return offsetY;
	}

	/**
	 * Mouse wheel while a screen is open: scrolls the visible tooltip if it does not fit.
	 *
	 * @return whether the wheel was used for the tooltip (the screen does not get it)
	 */
	public static boolean onScroll(double yOffset, boolean shift) {
		if (!enabled() || Util.getMillis() - lastDrawn > FORGET_MS || yOffset == 0) {
			return false;
		}
		if (shift && rangeX > 0) {
			scrollX = Math.max(0, Math.min(rangeX, scrollX - yOffset * STEP));
			return true;
		}
		if (rangeY > 0) {
			scrollY = Math.max(0, Math.min(rangeY, scrollY - yOffset * STEP));
			return true;
		}
		return false;
	}

	public static int rangeYForTest() {
		return rangeY;
	}

	public static double scrollYForTest() {
		return scrollY;
	}
}
