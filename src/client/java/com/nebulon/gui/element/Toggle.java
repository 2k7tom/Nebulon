package com.nebulon.gui.element;

import com.nebulon.gui.Canvas;
import com.nebulon.gui.Theme;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;

/**
 * The on/off switch of the settings menu (instead of a checkbox): a rounded track with a knob that
 * slides over with a small overshoot while the colours fade (about 0.2 s). Off: dark track, grey
 * knob on the left; on: light track, dark knob on the right. A thin outline shows on hover.
 */
public final class Toggle {
	/** Size in design units. */
	public static final double WIDTH = 17;
	public static final double HEIGHT = 9;
	private static final double INSET = 1.5;
	private static final double KNOB = HEIGHT - 2 * INSET;
	private static final float DURATION_MS = 200F;

	/** Shown position 0 (off) .. 1 (on), animated towards the real state. */
	private float progress = -1;
	private long lastFrame;

	/** Draws the switch with its top-left corner at (x, y). */
	public void render(Canvas c, double x, double y, boolean on, boolean hovered) {
		long now = Util.getMillis();
		float target = on ? 1F : 0F;
		if (progress < 0) {
			progress = target; // first frame: no animation
		} else {
			float step = Math.min(1F, (now - lastFrame) / DURATION_MS);
			progress = progress < target ? Math.min(target, progress + step) : Math.max(target, progress - step);
		}
		lastFrame = now;

		int track = ARGB.srgbLerp(progress, Theme.CONTROL_OFF, Theme.CONTROL_ON);
		int knob = ARGB.srgbLerp(progress, Theme.TEXT_DIM, Theme.CONTROL_KNOB_ON);
		if (hovered) {
			c.roundRect(x - 0.5, y - 0.5, x + WIDTH + 0.5, y + HEIGHT + 0.5, HEIGHT / 2 + 0.5, Theme.TEXT_FAINT);
		}
		c.roundRect(x, y, x + WIDTH, y + HEIGHT, HEIGHT / 2, track);
		// Ease out with a small overshoot towards the side it is moving to.
		double eased = on ? easeOutBack(progress) : 1 - easeOutBack(1 - progress);
		double travel = WIDTH - 2 * INSET - KNOB;
		double kx = x + INSET + travel * eased;
		c.roundRect(kx, y + INSET, kx + KNOB, y + INSET + KNOB, KNOB / 2, knob);
	}

	private static double easeOutBack(double t) {
		double s = 1.4;
		double u = t - 1;
		return 1 + (s + 1) * u * u * u + s * u * u;
	}
}
