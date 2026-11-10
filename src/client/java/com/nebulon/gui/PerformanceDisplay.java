package com.nebulon.gui;

import com.nebulon.Nebulon;
import com.nebulon.client.NebulonClient;
import com.nebulon.config.NebulonConfig;
import com.nebulon.config.PerformanceDisplaySettings;
import com.nebulon.hud.PerformanceStats;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * "Performance Display" (Render): a box in the menu style showing FPS, TPS, ping and CPS
 * (left | right), drawn with the menu font at the menu's fixed resolution.
 * <ul>
 *     <li>Colours follow the menu colour: labels in {@link Theme#ACCENT}, background and border
 *     tinted like the menu.</li>
 *     <li>Background opacity 0-100 %; the border fades with it.</li>
 *     <li>Layout "Horizontal" (one line) or "Vertical" (one value per line).</li>
 *     <li>Optional subtle text depth: shadow, extrusion or glow.</li>
 *     <li>Size factor 0.5-3: hold the box with the left mouse button in the settings screen and scroll.</li>
 * </ul>
 */
public final class PerformanceDisplay {
	private static final int PAD_X = 6;
	private static final int PAD_Y = 4;
	private static final int LINE = 11;
	private static final int LABEL_GAP = 4;
	private static final int SEGMENT_GAP = 9;
	private static final int TEXT_HEIGHT = 9;

	private record Line(String label, String value) {
	}

	private PerformanceDisplay() {
	}

	public static void register() {
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
				Identifier.fromNamespaceAndPath(Nebulon.MOD_ID, "performance_display"), (g, delta) -> drawHud(g));
	}

	private static PerformanceDisplaySettings settings() {
		return NebulonClient.get().configManager().get().performanceDisplay;
	}

	private static List<Line> lines(Minecraft minecraft) {
		PerformanceDisplaySettings s = settings();
		List<Line> lines = new ArrayList<>(5);
		if (s.showFps) {
			lines.add(new Line("FPS", Integer.toString(minecraft.getFps())));
		}
		if (s.showTps) {
			double tps = PerformanceStats.tps();
			lines.add(new Line("TPS", tps < 0 ? "-" : String.format(Locale.ROOT, "%.1f", tps)));
		}
		if (s.showPing) {
			int ping = PerformanceStats.ping(minecraft);
			lines.add(new Line("Ping", ping < 0 ? "-" : ping + " ms"));
		}
		if (s.showCps) {
			lines.add(new Line("CPS", PerformanceStats.leftCps() + " | " + PerformanceStats.rightCps()));
		}
		return lines;
	}

	private static boolean horizontal() {
		return settings().layout == PerformanceDisplaySettings.Layout.HORIZONTAL;
	}

	/** "Use Mod Font": Roboto, otherwise Minecraft's (or the texture pack's) font. */
	private static com.nebulon.config.GuiFont font() {
		return com.nebulon.config.GuiFont.of(settings().useModFont);
	}

	/** Unscaled size of the box in design units ({width, height}); {0, 0} when nothing is shown. */
	private static int[] baseSize(Canvas canvas, List<Line> lines) {
		Canvas c = canvas.withFont(font());
		if (lines.isEmpty()) {
			return new int[]{0, 0};
		}
		if (horizontal()) {
			int width = 0;
			for (Line line : lines) {
				width += c.width(line.label(), true) + LABEL_GAP + c.width(line.value(), false) + SEGMENT_GAP;
			}
			return new int[]{PAD_X * 2 + width - SEGMENT_GAP, PAD_Y * 2 + TEXT_HEIGHT};
		}
		int labelWidth = labelWidth(c, lines);
		int valueWidth = 0;
		for (Line line : lines) {
			valueWidth = Math.max(valueWidth, c.width(line.value(), false));
		}
		return new int[]{PAD_X * 2 + labelWidth + LABEL_GAP + 2 + Math.max(valueWidth, 18), PAD_Y * 2 + lines.size() * LINE - 2};
	}

	private static int labelWidth(Canvas c, List<Line> lines) {
		int labelWidth = 0;
		for (Line line : lines) {
			labelWidth = Math.max(labelWidth, c.width(line.label(), true));
		}
		return labelWidth;
	}

	/** Size on screen in design units, including the size factor. */
	public static int[] size(Canvas c, Minecraft minecraft) {
		int[] base = baseSize(c, lines(minecraft));
		float scale = settings().scale;
		return new int[]{(int) Math.ceil(base[0] * scale), (int) Math.ceil(base[1] * scale)};
	}

	/** Position in design units, with the default (bottom left) applied and kept on screen. */
	public static int[] position(int[] size, int designWidth, int designHeight) {
		PerformanceDisplaySettings s = settings();
		int x = s.x < 0 ? 6 : s.x;
		int y = s.y < 0 ? designHeight - size[1] - 60 : s.y;
		x = Math.max(0, Math.min(designWidth - size[0], x));
		y = Math.max(0, Math.min(designHeight - size[1], y));
		return new int[]{x, y};
	}

	/** Draws the box with its top-left corner at a design position. */
	public static void render(Canvas c, Minecraft minecraft, double x, double y, boolean highlighted) {
		List<Line> lines = lines(minecraft);
		if (lines.isEmpty()) {
			return;
		}
		PerformanceDisplaySettings s = settings();
		int[] size = baseSize(c, lines);
		int alpha = Math.round(s.backgroundOpacity * 2.55F);
		int background = (alpha << 24) | (Theme.PANEL_BG & 0xFFFFFF);
		int border = highlighted ? Theme.ACCENT : (Math.round(alpha * (Theme.HEADER_BORDER >>> 24) / 255.0F) << 24) | (Theme.HEADER_BORDER & 0xFFFFFF);

		// Box: drawn directly at its final size, so corners and edges stay pixel-exact when enlarged.
		double w = size[0] * s.scale;
		double h = size[1] * s.scale;
		if (alpha > 0) {
			c.roundRect(x, y, x + w, y + h, 4 * s.scale, background);
		}
		if ((border >>> 24) > 0) {
			c.roundRectOutline(x, y, x + w, y + h, 4 * s.scale, border);
		}

		// Text: drawn in a pose scaled by the size factor, with glyphs rasterised at the final
		// resolution (canvas scale x size factor), so enlarged text stays smooth.
		int textResolution = Math.round(c.scale * s.scale);
		Canvas t = new Canvas(c.g, c.font, c.scale, font(), textResolution);
		c.g.pose().pushMatrix();
		c.g.pose().translate(c.px(x), c.px(y));
		c.g.pose().scale(s.scale, s.scale);
		if (horizontal()) {
			double cx = PAD_X;
			for (Line line : lines) {
				text(t, s.textDepth, line.label(), cx, PAD_Y, Theme.ACCENT, true);
				cx += t.width(line.label(), true) + LABEL_GAP;
				text(t, s.textDepth, line.value(), cx, PAD_Y, Theme.TEXT, false);
				cx += t.width(line.value(), false) + SEGMENT_GAP;
			}
		} else {
			int labelWidth = labelWidth(t, lines);
			for (int i = 0; i < lines.size(); i++) {
				Line line = lines.get(i);
				double rowY = PAD_Y + i * LINE;
				text(t, s.textDepth, line.label(), PAD_X, rowY, Theme.ACCENT, true);
				text(t, s.textDepth, line.value(), PAD_X + labelWidth + LABEL_GAP + 2, rowY, Theme.TEXT, false);
			}
		}
		c.g.pose().popMatrix();
	}

	/**
	 * Text with the chosen depth effect. The effects are layers drawn first, all within one design
	 * unit of the letters and low in contrast, so they add depth without drawing attention.
	 */
	private static void text(Canvas c, PerformanceDisplaySettings.TextDepth depth, String text, double x, double y, int color, boolean bold) {
		switch (depth) {
			case SHADOW -> {
				// Soft shadow: a faint wide layer and a slightly stronger close one.
				c.text(text, x + 1.0, y + 1.0, 0x22000000, bold);
				c.text(text, x + 0.5, y + 0.5, 0x55000000, bold);
			}
			case EXTRUDE -> {
				// Extrusion: the text colour, darkened, stacked down-right like a thin 3D edge.
				int dark = 0x80000000 | (ARGB.scaleRGB(color, 0.3F) & 0xFFFFFF);
				c.text(text, x + 1.0, y + 1.0, dark, bold);
				c.text(text, x + 0.5, y + 0.5, dark, bold);
			}
			case GLOW -> {
				// Halo: the menu colour, very transparent, around the letters.
				int halo = 0x30000000 | (Theme.ACCENT & 0xFFFFFF);
				c.text(text, x - 0.5, y, halo, bold);
				c.text(text, x + 0.5, y, halo, bold);
				c.text(text, x, y - 0.5, halo, bold);
				c.text(text, x, y + 0.5, halo, bold);
			}
			case OFF -> {
			}
		}
		c.text(text, x, y, color, bold);
	}

	/** The Performance Display for the Gui Editor. */
	static final HudDisplay DISPLAY = new HudDisplay() {
		@Override
		public boolean enabled() {
			return settings().enabled;
		}

		@Override
		public int[] size(Canvas c, Minecraft minecraft, boolean example) {
			return PerformanceDisplay.size(c, minecraft);
		}

		@Override
		public int[] position(int[] size, int designWidth, int designHeight) {
			return PerformanceDisplay.position(size, designWidth, designHeight);
		}

		@Override
		public void setPosition(int x, int y) {
			settings().x = x;
			settings().y = y;
		}

		@Override
		public float scale() {
			return settings().scale;
		}

		@Override
		public void setScale(float scale) {
			settings().scale = scale;
		}

		@Override
		public void render(Canvas c, Minecraft minecraft, double x, double y, boolean highlighted, boolean example) {
			PerformanceDisplay.render(c, minecraft, x, y, highlighted);
		}
	};

	private static void drawHud(GuiGraphicsExtractor g) {
		Minecraft minecraft = Minecraft.getInstance();
		NebulonConfig config = NebulonClient.get().configManager().get();
		if (!config.performanceDisplay.enabled || minecraft.gui.screen() instanceof GuiEditorScreen) {
			return; // the GUI editor draws its own, draggable copy
		}
		Theme.apply(config.menuColor.rgb());
		int scale = Canvas.scaleFor(minecraft.getWindow());
		int guiScale = minecraft.getWindow().getGuiScale();
		g.pose().pushMatrix();
		g.pose().scale(1.0F / guiScale, 1.0F / guiScale);
		Canvas c = new Canvas(g, minecraft.font, scale, font());
		int[] size = size(c, minecraft);
		int[] pos = position(size, minecraft.getWindow().getWidth() / scale, minecraft.getWindow().getHeight() / scale);
		render(c, minecraft, pos[0], pos[1], false);
		g.pose().popMatrix();
	}
}
