package com.nebulon.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.nebulon.client.NebulonClient;
import com.nebulon.gui.element.Element;
import com.nebulon.gui.element.SliderElement;
import com.nebulon.keybind.NebulonKeybinds;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * A small settings window in the middle of the screen with tabs on the left (e.g. Inventory QOL:
 * Inventory, Inventory Display, Slot Binding). Opened by right-clicking a feature in the settings
 * menu; Esc goes back to the menu. Uses the same setting rows as the menu, in the menu's style.
 */
public final class TabbedSettingsScreen extends Screen {
	/** One tab: its name, its hover tooltip and its settings. */
	public record Tab(Component name, Component description, List<Element> elements) {
	}

	// Layout in the style of the settings menu: a category header, below it a glass panel with the
	// tabs as feature cards and the settings in an expanded card.
	private static final int WIDTH = 340;
	private static final int HEIGHT = 236;
	private static final int TITLE_HEIGHT = Theme.HEADER_HEIGHT + Theme.HEADER_GAP;
	private static final int TAB_WIDTH = Theme.PANEL_WIDTH - 2 * Theme.PADDING;
	private static final int TAB_HEIGHT = Theme.CARD_HEIGHT;
	private static final int TAB_GAP = Theme.CARD_GAP;
	private static final int PAD = Theme.PADDING;
	private static final int ROW_GAP = 3;
	private static final long TOOLTIP_DELAY_MS = 450L;
	private static final int TOOLTIP_WIDTH = 170;

	/** Gap between the window and the preview tooltip, in GUI pixels. */
	private static final int PREVIEW_GAP = 10;

	private final Screen parent;
	private final List<Tab> tabs;
	/** Optional item tooltip shown right of the window (e.g. Enchant Parsing's example sword), rebuilt every frame. */
	private final java.util.function.@org.jspecify.annotations.Nullable IntFunction<List<Component>> preview;
	/** Width of the preview incl. gap and background, in GUI pixels (0 without preview). */
	private int previewWidth;
	private int selected;
	private double scroll;
	private Element activeElement;
	private Object hoveredThing;
	private long hoverStart;

	private int scale = 2;
	private int designWidth;
	private int designHeight;

	public TabbedSettingsScreen(Screen parent, Component title, List<Tab> tabs) {
		this(parent, title, tabs, null);
	}

	public TabbedSettingsScreen(Screen parent, Component title, List<Tab> tabs,
			java.util.function.@org.jspecify.annotations.Nullable IntFunction<List<Component>> preview) {
		super(title);
		this.parent = parent;
		this.tabs = tabs;
		this.preview = preview;
	}

	/** For the client game tests. */
	public int selectedTab() {
		return selected;
	}

	/** For the client game tests: the settings of the open tab (positions valid after a rendered frame). */
	public List<Element> elements() {
		return tabs.get(selected).elements();
	}

	public List<Tab> tabs() {
		return tabs;
	}

	public int designScale() {
		return scale;
	}

	/** Top-left corner of tab {@code index} in design units, for the client game tests. */
	public double[] tabPosition(int index) {
		return new double[]{left() + PAD, top() + TITLE_HEIGHT + PAD + index * (TAB_HEIGHT + TAB_GAP)};
	}

	@Override
	protected void init() {
		ColorWheel.ensureRegistered(minecraft);
		updateMetrics();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
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

	private int left() {
		// With a preview, window and preview together are centred.
		int previewDesign = (int) Math.ceil(previewWidth * (double) minecraft.getWindow().getGuiScale() / scale);
		return Math.max(4, (designWidth - WIDTH - previewDesign) / 2);
	}

	private int top() {
		return Math.max(4, (designHeight - HEIGHT) / 2);
	}

	/** The expanded card that holds the settings of the open tab. */
	private int cardLeft() {
		return left() + PAD + TAB_WIDTH + PAD;
	}

	private int cardTop() {
		return top() + TITLE_HEIGHT + PAD;
	}

	private int cardRight() {
		return left() + WIDTH - PAD;
	}

	private int cardBottom() {
		return top() + HEIGHT - PAD;
	}

	/** Area of the setting rows inside the card (inset like the rows of an expanded feature card). */
	private int contentLeft() {
		return cardLeft() + Theme.SETTING_INSET;
	}

	private int contentTop() {
		return cardTop() + 5;
	}

	private int contentRight() {
		return cardRight() - Theme.SETTING_INSET;
	}

	private int contentBottom() {
		return cardBottom() - 4;
	}

	private List<Element> visibleElements() {
		List<Element> result = new ArrayList<>();
		for (Element element : tabs.get(selected).elements()) {
			if (element.isVisible()) {
				result.add(element);
			}
		}
		return result;
	}

	/** Positions the open tab's settings; returns the height of all of them. */
	private int layout() {
		updateMetrics();
		int y = contentTop() + (int) scroll;
		int total = 0;
		for (Element element : visibleElements()) {
			element.x = contentLeft();
			element.y = y;
			element.width = contentRight() - contentLeft();
			y += element.height() + ROW_GAP;
			total += element.height() + ROW_GAP;
		}
		return total;
	}

	private void clampScroll(int total) {
		double min = Math.min(0, (contentBottom() - contentTop()) - total);
		scroll = Math.max(min, Math.min(0, scroll));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		super.extractRenderState(g, mouseX, mouseY, a);
		g.fill(0, 0, width, height, Theme.OVERLAY);
		Theme.apply(NebulonClient.get().configManager().get().menuColor.rgb());
		List<net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent> previewLines = previewLines();
		clampScroll(layout());
		layout();
		renderPreview(g, previewLines);

		int guiScale = minecraft.getWindow().getGuiScale();
		g.pose().pushMatrix();
		g.pose().scale(1.0F / guiScale, 1.0F / guiScale);
		Canvas c = new Canvas(g, font, scale);
		double mx = toDesign(mouseX);
		double my = toDesign(mouseY);
		int x0 = left();
		int y0 = top();

		// Header like a category header in the settings menu, but only the feature name (no icon).
		// Exactly as wide as the glass panel below it (which reaches 2 units past the content on both sides).
		c.roundRectBordered(x0 - 2, y0, x0 + WIDTH + 2, y0 + Theme.HEADER_HEIGHT, 5, Theme.HEADER_BG, Theme.HEADER_BORDER);
		double titleX = x0 + (WIDTH - c.width(title, true)) / 2.0;
		c.text(title, titleX, y0 + (Theme.HEADER_HEIGHT - 8) / 2.0, Theme.TEXT, true);
		// Glass panel below it, as behind the feature cards.
		c.roundRect(x0 - 2, y0 + TITLE_HEIGHT - 2, x0 + WIDTH + 2, y0 + HEIGHT + 2, 7, Theme.PANEL_BG);
		// The settings sit in an expanded feature card.
		c.roundRectBordered(cardLeft(), cardTop(), cardRight(), cardBottom(), 4, Theme.CARD_EXPANDED_BG, Theme.CARD_BORDER_ENABLED);

		Object hovered = null;
		Component hoveredText = null;
		// Tabs on the left, drawn like feature cards (the open tab like an enabled one).
		for (int i = 0; i < tabs.size(); i++) {
			double[] p = tabPosition(i);
			boolean over = Theme.inside(mx, my, p[0], p[1], p[0] + TAB_WIDTH, p[1] + TAB_HEIGHT);
			if (i == selected) {
				c.roundRectBordered(p[0], p[1], p[0] + TAB_WIDTH, p[1] + TAB_HEIGHT, 4, Theme.CARD_BG_ENABLED, Theme.CARD_BORDER_ENABLED);
				if (over) {
					c.roundRect(p[0] + 0.5, p[1] + 0.5, p[0] + TAB_WIDTH - 0.5, p[1] + TAB_HEIGHT - 0.5, 3.5, Theme.CARD_BG_ENABLED_HOVER);
				}
			} else {
				c.roundRect(p[0], p[1], p[0] + TAB_WIDTH, p[1] + TAB_HEIGHT, 4, over ? Theme.CARD_BG_HOVER : Theme.CARD_BG);
			}
			c.scrollingText(tabs.get(i).name(), p[0] + 7, p[1] + TAB_HEIGHT / 2.0 - 4, TAB_WIDTH - 13,
					i == selected ? Theme.TEXT : Theme.TEXT_DIM, i == selected);
			if (over) {
				hovered = tabs.get(i);
				hoveredText = tabs.get(i).description();
			}
		}

		// Settings of the open tab, clipped to the content area.
		g.enableScissor(c.px(contentLeft() - 1), c.px(contentTop() - 1), c.px(contentRight() + 1), c.px(contentBottom() + 1));
		for (Element element : visibleElements()) {
			element.render(c, mx, my);
			boolean inside = Theme.inside(mx, my, contentLeft(), contentTop(), contentRight(), contentBottom());
			Component tooltip = inside ? element.tooltipAt(mx, my) : null;
			if (tooltip != null) {
				hovered = element.tooltipTarget(mx, my);
				hoveredText = tooltip;
			}
		}
		g.disableScissor();
		renderTooltip(c, hovered, hoveredText, mx, my);
		g.pose().popMatrix();
	}

	/** The preview's tooltip lines (vanilla font, like in game); also updates {@link #previewWidth}. */
	private List<net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent> previewLines() {
		if (preview == null) {
			previewWidth = 0;
			return List.of();
		}
		List<net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent> lines = new ArrayList<>();
		int w = 0;
		for (Component line : preview.apply(selected)) {
			var component = net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(line.getVisualOrderText());
			lines.add(component);
			w = Math.max(w, component.getWidth(font));
		}
		previewWidth = PREVIEW_GAP + w + 8;
		return lines;
	}

	/** Draws the preview tooltip right of the window, vertically centred (in GUI pixels). */
	private void renderPreview(GuiGraphicsExtractor g, List<net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent> lines) {
		if (lines.isEmpty()) {
			return;
		}
		int h = -2;
		for (var line : lines) {
			h += line.getHeight(font);
		}
		h += 2;
		double toGui = (double) scale / minecraft.getWindow().getGuiScale();
		int x = (int) Math.ceil((left() + WIDTH + 2) * toGui) + PREVIEW_GAP + 4;
		int y = Math.max(5, (height - h) / 2);
		g.tooltip(font, lines, x, y, (screenWidth, screenHeight, px, py, tw, th) -> new org.joml.Vector2i(px, py), null);
	}

	private void renderTooltip(Canvas c, Object hovered, Component text, double mx, double my) {
		if (hovered != hoveredThing) {
			hoveredThing = hovered;
			hoverStart = Util.getMillis();
		}
		if (hovered == null || text == null || Util.getMillis() - hoverStart < TOOLTIP_DELAY_MS) {
			return;
		}
		List<String> lines = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (String word : text.getString().split(" ")) {
			String candidate = line.isEmpty() ? word : line + " " + word;
			if (c.width(candidate, false) > TOOLTIP_WIDTH && !line.isEmpty()) {
				lines.add(line.toString());
				line = new StringBuilder(word);
			} else {
				line = new StringBuilder(candidate);
			}
		}
		if (!line.isEmpty()) {
			lines.add(line.toString());
		}
		int w = 0;
		for (String l : lines) {
			w = Math.max(w, c.width(l, false));
		}
		double h = lines.size() * 10 + 7;
		double x0 = Math.min(mx + 10, designWidth - w - 14);
		double y0 = Math.min(my + 10, designHeight - h - 2);
		c.roundRectBordered(x0, y0, x0 + w + 12, y0 + h, 4, Theme.TOOLTIP_BG, Theme.HEADER_BORDER);
		for (int i = 0; i < lines.size(); i++) {
			c.text(lines.get(i), x0 + 6, y0 + 4 + i * 10, Theme.TEXT_DIM, false);
		}
	}

	private void releaseAllFocus() {
		for (Tab tab : tabs) {
			for (Element element : tab.elements()) {
				element.releaseFocus();
			}
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		layout();
		double mx = toDesign(event.x());
		double my = toDesign(event.y());
		int button = event.button();
		for (Element element : visibleElements()) {
			if (element.capturesMouse()) {
				element.mouseClicked(mx, my, button);
				return true;
			}
		}
		for (int i = 0; i < tabs.size(); i++) {
			double[] p = tabPosition(i);
			if (Theme.inside(mx, my, p[0], p[1], p[0] + TAB_WIDTH, p[1] + TAB_HEIGHT)) {
				if (i != selected) {
					releaseAllFocus();
					selected = i;
					scroll = 0;
				}
				return true;
			}
		}
		// Clicks outside the window close it (back to the settings menu).
		if (!Theme.inside(mx, my, left(), top(), left() + WIDTH, top() + HEIGHT) && button == InputConstants.MOUSE_BUTTON_LEFT) {
			onClose();
			return true;
		}
		Element clicked = null;
		if (Theme.inside(mx, my, contentLeft(), contentTop(), contentRight(), contentBottom())) {
			for (Element element : visibleElements()) {
				if (clicked == null && element.mouseClicked(mx, my, button)) {
					clicked = element;
				}
			}
		}
		for (Element element : visibleElements()) {
			if (element != clicked) {
				element.releaseFocus();
			}
		}
		activeElement = clicked;
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (activeElement != null) {
			layout();
			activeElement.mouseDragged(toDesign(event.x()), toDesign(event.y()));
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (activeElement != null) {
			activeElement.mouseReleased();
		}
		return true;
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		int total = layout();
		double mx = toDesign(x);
		double my = toDesign(y);
		for (Element element : visibleElements()) {
			if (element instanceof SliderElement slider && slider.scroll(mx, my, scrollY)) {
				return true;
			}
		}
		scroll += scrollY * 18.0;
		clampScroll(total);
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		for (Element element : visibleElements()) {
			if (element.capturesKeyboard()) {
				element.keyPressed(event);
				return true;
			}
		}
		if (NebulonKeybinds.openSettings.matches(event)) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		for (Element element : visibleElements()) {
			if (element.capturesKeyboard()) {
				return element.charTyped(event);
			}
		}
		return false;
	}

	@Override
	public void removed() {
		releaseAllFocus();
		NebulonClient.get().configManager().save();
		super.removed();
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}
}
