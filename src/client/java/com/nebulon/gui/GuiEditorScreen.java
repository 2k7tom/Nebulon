package com.nebulon.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.nebulon.client.NebulonClient;
import com.nebulon.config.PerformanceDisplaySettings;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * "Gui Editor" (Settings): a screen that only shows the displays (Performance Display, Quiz Timer)
 * over the game, so they can be placed freely.
 * <ul>
 *     <li>Drag a display with the left mouse button; it snaps to the screen centre and border, to
 *     the other displays (guides in the menu colour).</li>
 *     <li>Hold it and scroll to resize.</li>
 *     <li>Displays that are switched off are shown dimmed, so they can be placed beforehand; the
 *     Quiz Timer shows example values.</li>
 *     <li>Esc goes back to the settings menu (vanilla {@link Screen#onClose()} on Esc); positions are saved.</li>
 * </ul>
 */
public class GuiEditorScreen extends Screen {
	private static final float SCALE_STEP = 0.1F;
	private static final int DIM = 0x99000000;
	private static final List<HudDisplay> DISPLAYS = List.of(PerformanceDisplay.DISPLAY,
			InventoryDisplay.DISPLAY);

	private final Screen parent;
	private final SnapGuides guides = new SnapGuides();
	private int scale = 2;
	private int designWidth;
	private int designHeight;
	/** Index of the dragged display, or -1. */
	private int dragging = -1;
	private double grabX;
	private double grabY;
	/** Box per display of the last frame: x, y, width, height (width 0 = nothing shown). */
	private final int[][] boxes = new int[DISPLAYS.size()][4];

	public GuiEditorScreen(Screen parent) {
		super(Component.translatable("screen.nebulon.gui_editor"));
		this.parent = parent;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	/** In a world: no blur and no menu background, so displays can be placed against the sharp game view. */
	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		if (minecraft.level == null) {
			super.extractBackground(graphics, mouseX, mouseY, a);
		}
	}

	private void updateMetrics() {
		Window window = minecraft.getWindow();
		scale = Canvas.scaleFor(window);
		designWidth = window.getWidth() / scale;
		designHeight = window.getHeight() / scale;
	}

	private double toDesign(double guiCoordinate) {
		return guiCoordinate * minecraft.getWindow().getGuiScale() / scale;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		super.extractRenderState(g, mouseX, mouseY, a);
		updateMetrics();
		Theme.apply(NebulonClient.get().configManager().get().menuColor.rgb());
		g.fill(0, 0, width, height, 0x30000000);

		int guiScale = minecraft.getWindow().getGuiScale();
		g.pose().pushMatrix();
		g.pose().scale(1.0F / guiScale, 1.0F / guiScale);
		Canvas c = new Canvas(g, font, scale);
		double mx = toDesign(mouseX);
		double my = toDesign(mouseY);

		// Title and hint at the top.
		Component title = Component.translatable("screen.nebulon.gui_editor");
		Component hint = Component.translatable("screen.nebulon.gui_editor.hint");
		double hintWidth = Math.max(c.width(title, true), c.width(hint, false)) + 20;
		double hx = (designWidth - hintWidth) / 2.0;
		c.roundRect(hx, 8, hx + hintWidth, 36, 5, Theme.PANEL_BG);
		c.roundRectOutline(hx, 8, hx + hintWidth, 36, 5, Theme.HEADER_BORDER);
		c.textCentered(title, designWidth / 2.0, 12, Theme.ACCENT, true);
		c.textCentered(hint, designWidth / 2.0, 24, Theme.TEXT_DIM, false);

		if (dragging >= 0) {
			guides.render(c, designWidth, designHeight);
		}

		boolean any = false;
		int hoveredIndex = hovered(mx, my);
		for (int i = 0; i < DISPLAYS.size(); i++) {
			HudDisplay display = DISPLAYS.get(i);
			int[] size = display.size(c, minecraft, true);
			if (size[0] <= 0) {
				boxes[i] = new int[]{0, 0, 0, 0};
				continue;
			}
			any = true;
			int[] pos = display.position(size, designWidth, designHeight);
			boxes[i] = new int[]{pos[0], pos[1], size[0], size[1]};
			boolean highlighted = dragging == i || (dragging < 0 && hoveredIndex == i);
			display.render(c, minecraft, pos[0], pos[1], highlighted, true);
			if (!display.enabled()) {
				c.roundRect(pos[0], pos[1], pos[0] + size[0], pos[1] + size[1], 4 * display.scale(), DIM);
				Component off = Component.translatable("screen.nebulon.gui_editor.off");
				c.text(off, pos[0] + size[0] - c.width(off, true) - 3, pos[1] - 10, Theme.TEXT_DIM, true);
			}
			if (highlighted) {
				String percent = Math.round(display.scale() * 100) + " %";
				c.text(percent, pos[0], pos[1] + size[1] + 3, Theme.TEXT_DIM, false);
			}
		}
		if (!any) {
			Component empty = Component.translatable("screen.nebulon.gui_editor.empty");
			c.textCentered(empty, designWidth / 2.0, designHeight / 2.0, Theme.TEXT_DIM, false);
		}
		g.pose().popMatrix();
	}

	/** Topmost display under the mouse, or -1. */
	private int hovered(double mx, double my) {
		for (int i = DISPLAYS.size() - 1; i >= 0; i--) {
			int[] b = boxes[i];
			if (b[2] > 0 && Theme.inside(mx, my, b[0], b[1], b[0] + b[2], b[1] + b[3])) {
				return i;
			}
		}
		return -1;
	}

	/** Performance Display box of the last frame (x, y, width, height), for the client game tests. */
	public int[] box() {
		return boxes[0].clone();
	}

	/** Quiz Timer box of the last frame (x, y, width, height), for the client game tests. */
	public int[] quizTimerBox() {
		return boxes[1].clone();
	}

	/** Width of the screen in design units, for the client game tests. */
	public int designWidth() {
		return designWidth;
	}

	/** Real pixels per design unit, for the client game tests. */
	public int designScale() {
		return scale;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		updateMetrics();
		double mx = toDesign(event.x());
		double my = toDesign(event.y());
		int index = hovered(mx, my);
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && index >= 0) {
			dragging = index;
			grabX = mx - boxes[index][0];
			grabY = my - boxes[index][1];
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging < 0) {
			return false;
		}
		double mx = toDesign(event.x());
		double my = toDesign(event.y());
		int[] box = boxes[dragging];
		List<double[]> others = new ArrayList<>();
		for (int i = 0; i < boxes.length; i++) {
			if (i != dragging && boxes[i][2] > 0) {
				others.add(new double[]{boxes[i][0], boxes[i][1], boxes[i][0] + boxes[i][2], boxes[i][1] + boxes[i][3]});
			}
		}
		int[] snapped = guides.snap((int) Math.round(mx - grabX), (int) Math.round(my - grabY), box[2], box[3], others, designWidth, designHeight);
		box[0] = Math.max(0, Math.min(designWidth - box[2], snapped[0]));
		box[1] = Math.max(0, Math.min(designHeight - box[3], snapped[1]));
		DISPLAYS.get(dragging).setPosition(box[0], box[1]);
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		dragging = -1;
		guides.clear();
		return true;
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		if (dragging < 0) {
			return false;
		}
		// Hold the display with the left mouse button and scroll: resize it.
		HudDisplay display = DISPLAYS.get(dragging);
		display.setScale(Math.round(Math.max(PerformanceDisplaySettings.MIN_SCALE,
				Math.min(PerformanceDisplaySettings.MAX_SCALE, display.scale() + (float) scrollY * SCALE_STEP)) * 100.0F) / 100.0F);
		return true;
	}

	@Override
	public void removed() {
		NebulonClient.get().configManager().save();
		super.removed();
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}
}
