package com.nebulon.gui;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Category icons (8 x 8 design units), each built from simple shapes given as signed distance
 * functions. Coverage is computed per real screen pixel, so the edges are anti-aliased and match
 * the rounded rectangles of the rest of the screen at every resolution.
 */
public final class CategoryIcons {
	public static final double SIZE = 8.0;

	/** Signed distance in design units: negative inside, positive outside. */
	@FunctionalInterface
	private interface Shape {
		double distance(double x, double y);

		default Shape or(Shape other) {
			return (x, y) -> Math.min(distance(x, y), other.distance(x, y));
		}

		default Shape and(Shape other) {
			return (x, y) -> Math.max(distance(x, y), other.distance(x, y));
		}

		default Shape minus(Shape other) {
			return (x, y) -> Math.max(distance(x, y), -other.distance(x, y));
		}
	}

	/** Line from (ax, ay) to (bx, by) with round ends and the given half thickness. */
	private static Shape line(double ax, double ay, double bx, double by, double radius) {
		return (x, y) -> {
			double dx = bx - ax;
			double dy = by - ay;
			double t = Math.max(0.0, Math.min(1.0, ((x - ax) * dx + (y - ay) * dy) / (dx * dx + dy * dy)));
			return Math.hypot(x - ax - t * dx, y - ay - t * dy) - radius;
		};
	}

	private static Shape circle(double cx, double cy, double radius) {
		return (x, y) -> Math.hypot(x - cx, y - cy) - radius;
	}

	private static Shape ring(double cx, double cy, double radius, double halfWidth) {
		return (x, y) -> Math.abs(Math.hypot(x - cx, y - cy) - radius) - halfWidth;
	}

	/** Outline of an ellipse (approximate distance, fine for thin lines) with half axes a, b. */
	private static Shape ellipseRing(double cx, double cy, double a, double b, double halfWidth) {
		return (x, y) -> Math.abs(Math.hypot((x - cx) / a, (y - cy) / b) - 1.0) * Math.min(a, b) - halfWidth;
	}

	private static Shape roundBox(double x0, double y0, double x1, double y1, double radius) {
		double cx = (x0 + x1) / 2.0;
		double cy = (y0 + y1) / 2.0;
		double hx = (x1 - x0) / 2.0 - radius;
		double hy = (y1 - y0) / 2.0 - radius;
		return (x, y) -> {
			double qx = Math.abs(x - cx) - hx;
			double qy = Math.abs(y - cy) - hy;
			return Math.hypot(Math.max(qx, 0.0), Math.max(qy, 0.0)) + Math.min(Math.max(qx, qy), 0.0) - radius;
		};
	}

	/** The shape turned clockwise by {@code degrees} around the icon centre (designed upright, drawn tilted). */
	private static Shape rotated(Shape shape, double degrees) {
		double cos = Math.cos(Math.toRadians(degrees));
		double sin = Math.sin(Math.toRadians(degrees));
		return (x, y) -> {
			double dx = x - SIZE / 2.0;
			double dy = y - SIZE / 2.0;
			return shape.distance(SIZE / 2.0 + dx * cos + dy * sin, SIZE / 2.0 - dx * sin + dy * cos);
		};
	}

	/** Half plane on the side of the line through (ax, ay) and (bx, by) that lies to its left. */
	private static Shape side(double ax, double ay, double bx, double by) {
		double length = Math.hypot(bx - ax, by - ay);
		return (x, y) -> ((bx - ax) * (y - ay) - (by - ay) * (x - ax)) / length;
	}

	// ---- Icons --------------------------------------------------------------------------------

	/** Skull: dome with eye holes and a jaw. */
	private static final Shape DUNGEON = circle(4.0, 3.4, 3.4)
			.or(roundBox(1.9, 4.6, 6.1, 7.8, 0.9))
			.minus(circle(2.65, 3.75, 0.95))
			.minus(circle(5.35, 3.75, 0.95))
			.minus(line(4.0, 5.2, 4.0, 5.6, 0.35))
			.minus(line(3.0, 7.0, 3.0, 8.0, 0.28))
			.minus(line(5.0, 7.0, 5.0, 8.0, 0.28));

	/** Eye: almond outline with a pupil. */
	private static final Shape RENDER = circle(4.0, 5.3, 4.1).and(circle(4.0, 2.7, 4.1))
			.minus(circle(4.0, 4.0, 1.75))
			.or(circle(4.0, 4.0, 1.1));

	/** Globe: outline, a meridian, the equator and two circles of latitude. */
	private static final Shape QOL = ring(4.0, 4.0, 3.4, 0.45)
			.or(ellipseRing(4.0, 4.0, 1.55, 3.4, 0.4).and(circle(4.0, 4.0, 3.4)))
			.or(line(0.8, 4.0, 7.2, 4.0, 0.4))
			.or(line(1.0, 2.2, 7.0, 2.2, 0.35).or(line(1.0, 5.8, 7.0, 5.8, 0.35)).and(circle(4.0, 4.0, 3.2)));

	/** Sword pointing up-right. */
	private static final Shape COMBAT = line(2.9, 5.1, 7.0, 1.0, 0.75)
			.or(line(1.4, 4.2, 3.8, 6.6, 0.6))
			.or(line(2.4, 5.6, 1.0, 7.0, 0.55));

	/** Gear: teeth around a ring with a hole. */
	private static final Shape SETTINGS = circle(4.0, 4.0, 2.75)
			.or(line(4.0, 0.5, 4.0, 7.5, 0.8))
			.or(line(0.5, 4.0, 7.5, 4.0, 0.8))
			.or(line(1.53, 1.53, 6.47, 6.47, 0.8))
			.or(line(1.53, 6.47, 6.47, 1.53, 0.8))
			.minus(circle(4.0, 4.0, 1.2));

	/** Four squares, for unknown categories. */
	private static final Shape GRID = roundBox(0.5, 0.5, 3.5, 3.5, 0.8)
			.or(roundBox(4.5, 0.5, 7.5, 3.5, 0.8))
			.or(roundBox(0.5, 4.5, 3.5, 7.5, 0.8))
			.or(roundBox(4.5, 4.5, 7.5, 7.5, 0.8));

	private CategoryIcons() {
	}

	private static Shape forCategory(String id) {
		return switch (id) {
			case "dungeon" -> DUNGEON;
			case "render" -> RENDER;
			case "qol" -> QOL;
			case "combat" -> COMBAT;
			case "settings" -> SETTINGS;
			default -> GRID;
		};
	}

	/** Draws the icon of a category with its top-left corner at a design position. */
	public static void draw(Canvas c, String category, double x, double y, int color) {
		Shape shape = forCategory(category);
		int size = c.px(SIZE);
		int scale = c.scale;
		// Coverage per real pixel (anti-aliased), computed once per category and scale.
		Identifier icon = MaskTextures.get("icon:" + category + ":" + scale, size, size, (px, py) -> {
			double d = shape.distance((px + 0.5) / scale, (py + 0.5) / scale) * scale;
			return Math.max(0.0, Math.min(1.0, 0.5 - d));
		});
		c.g.blit(RenderPipelines.GUI_TEXTURED, icon, c.px(x), c.px(y), 0.0F, 0.0F, size, size, size, size, color);
	}}
