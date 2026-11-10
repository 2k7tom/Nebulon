package com.nebulon.gui;

import java.util.ArrayList;
import java.util.List;

/**
 * Alignment guides while dragging a box (category panel or display): edges and centres snap to
 * the edges and centres of the other boxes, to the screen centre and the screen border, and to a
 * position next to another box with the standard gap. The matching lines are drawn across the
 * screen in the menu colour.
 */
final class SnapGuides {
	/** Distance (design units) within which a dragged box snaps to a guide. */
	static final int SNAP = 4;
	/** Distance kept from the screen border when snapping to it. */
	static final int BORDER = 4;

	private final List<Double> lineX = new ArrayList<>();
	private final List<Double> lineY = new ArrayList<>();

	void clear() {
		lineX.clear();
		lineY.clear();
	}

	/**
	 * Snaps a box and records the guides it snapped to.
	 *
	 * @param others rectangles {x0, y0, x1, y1} of the other boxes (design units)
	 * @return the snapped {x, y}
	 */
	int[] snap(int x, int y, int w, int h, List<double[]> others, int designWidth, int designHeight) {
		clear();
		List<Double> xs = new ArrayList<>(List.of(designWidth / 2.0, (double) BORDER, (double) designWidth - BORDER));
		List<Double> ys = new ArrayList<>(List.of(designHeight / 2.0, (double) BORDER, (double) designHeight - BORDER));
		for (double[] r : others) {
			xs.add(r[0]);
			xs.add(r[2]);
			xs.add((r[0] + r[2]) / 2.0);
			// Side by side / stacked with the standard gap.
			xs.add(r[2] + Theme.PANEL_GAP);
			xs.add(r[0] - Theme.PANEL_GAP);
			ys.add(r[1]);
			ys.add(r[3]);
			ys.add(r[3] + Theme.PANEL_GAP);
			ys.add(r[1] - Theme.PANEL_GAP);
		}
		double dx = best(new double[]{x, x + w, x + w / 2.0}, xs, lineX);
		double dy = best(new double[]{y, y + h, y + h / 2.0}, ys, lineY);
		return new int[]{(int) Math.round(x + dx), (int) Math.round(y + dy)};
	}

	/** Smallest offset (within SNAP) that aligns one of the edges with a candidate; adds its guide. */
	private static double best(double[] edges, List<Double> candidates, List<Double> guides) {
		double best = Double.MAX_VALUE;
		double line = 0;
		for (double edge : edges) {
			for (double candidate : candidates) {
				double d = candidate - edge;
				if (Math.abs(d) <= SNAP && Math.abs(d) < Math.abs(best)) {
					best = d;
					line = candidate;
				}
			}
		}
		if (best == Double.MAX_VALUE) {
			return 0;
		}
		guides.add(line);
		return best;
	}

	/** Thin full-screen lines (one real pixel) at the current guides. */
	void render(Canvas c, int designWidth, int designHeight) {
		double px = 1.0 / c.scale;
		for (double gx : lineX) {
			c.fill(gx, 0, gx + px, designHeight, Theme.GUIDE);
		}
		for (double gy : lineY) {
			c.fill(0, gy, designWidth, gy + px, Theme.GUIDE);
		}
	}
}
