package com.nebulon.gui;

import com.nebulon.client.NebulonClient;
import com.nebulon.config.ConfigManager;
import com.nebulon.config.PanelPosition;
import com.nebulon.gui.element.Element;
import com.nebulon.gui.element.SliderElement;
import com.nebulon.keybind.NebulonKeybinds;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
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
 * The settings screen (command /nebulon, or a key set in Options &gt; Controls). Rendered at a fixed resolution in real screen pixels (see
 * {@link Canvas}), independent of Minecraft's GUI scale.
 * <ul>
 *     <li>Left-click a module: on/off. Right-click: expand its settings.</li>
 *     <li>Drag a category header to move the panel; scroll to move the content.</li>
 *     <li>Hover a module or setting to see what it does.</li>
 * </ul>
 * All changes apply immediately; the config file is written when the screen closes.
 */
public class ClickGuiScreen extends Screen {
	private static final int SEARCH_WIDTH = 160;
	private static final int SEARCH_HEIGHT = 18;
	private static final long TOOLTIP_DELAY_MS = 350L;
	private static final int TOOLTIP_WIDTH = 170;

	private final List<CategoryPanel> panels = new ArrayList<>();
	private final TextInput search = new TextInput("", 32, text -> true);
	private double scroll;
	private CategoryPanel draggedPanel;
	private double grabX;
	private double grabY;
	private Element activeElement;
	/** Alignment guides while a panel is dragged. */
	private final SnapGuides guides = new SnapGuides();
	/** Panels from back to front; a panel moves to the front when its header is clicked. */
	private final List<CategoryPanel> drawOrder = new ArrayList<>();
	private Object hoveredThing;
	private long hoverStart;

	/** Fixed scale (real pixels per design unit) and design-space screen size of the current frame. */
	private int scale = 2;
	private int designWidth;
	private int designHeight;

	public ClickGuiScreen() {
		super(Component.translatable("screen.nebulon.title"));
		NebulonClient client = NebulonClient.get();
		panels.add(new CategoryPanel("render", List.of(ModuleCard.performanceDisplay(), ModuleCard.particles(),
				ModuleCard.toggleOnly("fullbright", () -> client.configManager().get().fullbright, () -> {
					client.configManager().get().fullbright = !client.configManager().get().fullbright;
					client.onSettingsChanged();
				}), ModuleCard.rarity(), ModuleCard.heldItem())));
		panels.add(new CategoryPanel("qol", List.of(ModuleCard.toggleOnly("darkmode_mist",
				() -> client.configManager().get().darkmodeMist, client.darkmodeMist()::toggle), ModuleCard.inventoryQol(),
				ModuleCard.auctionInput(), ModuleCard.itemTweaks(),
				ModuleCard.toggleOnly("scrollable_tooltips", () -> client.configManager().get().scrollableTooltips, () -> {
					client.configManager().get().scrollableTooltips = !client.configManager().get().scrollableTooltips;
					client.onSettingsChanged();
				}), ModuleCard.enchantDisplay(), ModuleCard.hudTweaks(), ModuleCard.itemPrices(), ModuleCard.commandKeybinds(),
				ModuleCard.toggleOnly("anti_mouse_reset", () -> client.configManager().get().antiMouseReset, () -> {
					client.configManager().get().antiMouseReset = !client.configManager().get().antiMouseReset;
					client.onSettingsChanged();
				}))));
		// "combat" is shown as "Slayers" (the id keeps saved positions and the sword icon).
		panels.add(new CategoryPanel("combat", List.of(ModuleCard.infernoDemonlord())));
		panels.add(new CategoryPanel("settings", List.of(ModuleCard.menuCustomisation(),
				ModuleCard.action("gui_editor", () -> minecraft.gui.setScreen(new GuiEditorScreen(this))), ModuleCard.protectItem())));
		drawOrder.addAll(panels);
	}

	@Override
	protected void init() {
		ColorWheel.ensureRegistered(minecraft);
		updateMetrics();
		ConfigManager config = NebulonClient.get().configManager();
		// Positions saved by the old single-category layout would overlap the new categories.
		if (!config.get().panels.containsKey("render")) {
			config.get().panels.clear();
		}
		// Default layout: one row, centred; wraps into further rows on narrow windows.
		int step = Theme.PANEL_WIDTH + Theme.PANEL_GAP;
		int perRow = Math.max(1, Math.min(panels.size(), (designWidth - 8 + Theme.PANEL_GAP) / step));
		int rowWidth = perRow * step - Theme.PANEL_GAP;
		int startX = Math.max(4, (designWidth - rowWidth) / 2);
		for (int i = 0; i < panels.size(); i++) {
			CategoryPanel panel = panels.get(i);
			PanelPosition saved = config.get().panels.get(panel.id());
			panel.baseX = saved != null ? saved.x : startX + (i % perRow) * step;
			panel.baseY = saved != null ? saved.y : 24 + (i / perRow) * 150;
			panel.collapsed = saved != null && saved.collapsed;
		}
	}

	private void savePanel(CategoryPanel panel) {
		NebulonClient.get().configManager().get().panels
				.put(panel.id(), new PanelPosition(panel.baseX, panel.baseY, panel.collapsed));
	}

	/** Read-only view for the client game tests (positions are valid after a rendered frame). */
	public List<CategoryPanel> panels() {
		return panels;
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
	public boolean isPauseScreen() {
		// Keep the world running so setting changes are visible immediately.
		return false;
	}

	private void updateMetrics() {
		Window window = minecraft.getWindow();
		scale = Canvas.scaleFor(window);
		designWidth = window.getWidth() / scale;
		designHeight = window.getHeight() / scale;
	}

	/** Converts Minecraft GUI coordinates (mouse events) into design units. */
	private double toDesign(double guiCoordinate) {
		return guiCoordinate * minecraft.getWindow().getGuiScale() / scale;
	}

	/** Positions everything for the current frame; returns the lowest panel edge (without scroll). */
	private int layout() {
		updateMetrics();
		int lowest = 0;
		for (CategoryPanel panel : panels) {
			panel.baseX = Math.max(0, Math.min(designWidth - Theme.PANEL_WIDTH, panel.baseX));
			panel.baseY = Math.max(0, Math.min(designHeight - Theme.HEADER_HEIGHT, panel.baseY));
			int bottom = panel.layout(panel.baseX, panel.baseY + (int) scroll, search.text());
			panel.height = Math.max(Theme.HEADER_HEIGHT, bottom - panel.y);
			lowest = Math.max(lowest, bottom - (int) scroll);
		}
		return lowest;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		super.extractRenderState(g, mouseX, mouseY, a);
		g.fill(0, 0, width, height, Theme.OVERLAY);
		Theme.apply(NebulonClient.get().configManager().get().menuColor.rgb());
		layout();

		// Switch to real-pixel space: everything below is drawn at the fixed resolution.
		int guiScale = minecraft.getWindow().getGuiScale();
		g.pose().pushMatrix();
		g.pose().scale(1.0F / guiScale, 1.0F / guiScale);
		Canvas c = new Canvas(g, font, scale);
		double mx = toDesign(mouseX);
		double my = toDesign(mouseY);

		Object hovered = null;
		Component hoveredText = null;
		// Back to front: the panel touched last is drawn on top and gets the hover.
		for (CategoryPanel panel : drawOrder) {
			int bottom = panel.layout(panel.x, panel.y, search.text());
			panel.render(c, mx, my, search.text(), bottom);
			if (panel.isOverHeader(mx, my)) {
				hovered = panel;
				hoveredText = Component.translatable("category.nebulon.hint");
			}
			for (ModuleCard card : panel.visibleCards(search.text())) {
				if (card.isOverHeader(mx, my)) {
					hovered = card;
					hoveredText = card.description();
				}
				if (card.isExpanded()) {
					for (Element element : card.elements()) {
						if (!element.isVisible()) {
							continue;
						}
						Component tooltip = element.tooltipAt(mx, my);
						if (tooltip != null) {
							hovered = element.tooltipTarget(mx, my);
							hoveredText = tooltip;
						}
					}
				}
			}
		}

		if (draggedPanel != null) {
			guides.render(c, designWidth, designHeight);
		}
		renderSearch(c);
		renderTooltip(c, hovered, hoveredText, mx, my);
		g.pose().popMatrix();
	}

	/** Snaps a dragged panel (screen coordinates) to the other panels and the screen; records the guides. */
	private int[] snap(int x, int y, int w, int h, CategoryPanel self) {
		List<double[]> others = new ArrayList<>();
		for (CategoryPanel panel : panels) {
			if (panel != self) {
				double top = panel.baseY + scroll;
				others.add(new double[]{panel.baseX, top, panel.baseX + Theme.PANEL_WIDTH, top + panel.height});
			}
		}
		return guides.snap(x, y, w, h, others, designWidth, designHeight);
	}

	/** Whether two panels (whole height) overlap or come closer than the standard gap allows. */
	private static boolean collides(int ax, int ay, int ah, CategoryPanel b) {
		int margin = Theme.PANEL_GAP / 2;
		return ax < b.baseX + Theme.PANEL_WIDTH + margin && ax + Theme.PANEL_WIDTH + margin > b.baseX
				&& ay < b.baseY + b.height + margin && ay + ah + margin > b.baseY;
	}

	private boolean freeSpot(CategoryPanel panel, int x, int y) {
		if (x < 0 || y < 0 || x + Theme.PANEL_WIDTH > designWidth || y + Theme.HEADER_HEIGHT > designHeight) {
			return false;
		}
		for (CategoryPanel other : panels) {
			if (other != panel && collides(x, y, panel.height, other)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * After a drop: if the panel overlaps another one, move it to the nearest free spot next to,
	 * below or above one of the panels it hits (with the standard gap). Leaves it if none is free.
	 */
	private void resolveOverlap(CategoryPanel panel) {
		if (freeSpot(panel, panel.baseX, panel.baseY)) {
			return;
		}
		int gap = Theme.PANEL_GAP;
		int bestX = panel.baseX;
		int bestY = panel.baseY;
		double bestDistance = Double.MAX_VALUE;
		for (CategoryPanel other : panels) {
			if (other == panel || !collides(panel.baseX, panel.baseY, panel.height, other)) {
				continue;
			}
			int[][] candidates = {
					{other.baseX + Theme.PANEL_WIDTH + gap, panel.baseY},
					{other.baseX - Theme.PANEL_WIDTH - gap, panel.baseY},
					{panel.baseX, other.baseY + other.height + gap},
					{panel.baseX, other.baseY - panel.height - gap},
					{other.baseX, other.baseY + other.height + gap}
			};
			for (int[] candidate : candidates) {
				double distance = Math.hypot(candidate[0] - panel.baseX, candidate[1] - panel.baseY);
				if (distance < bestDistance && freeSpot(panel, candidate[0], candidate[1])) {
					bestDistance = distance;
					bestX = candidate[0];
					bestY = candidate[1];
				}
			}
		}
		panel.baseX = bestX;
		panel.baseY = bestY;
	}

	private double searchX() {
		return (designWidth - SEARCH_WIDTH) / 2.0;
	}

	private double searchY() {
		return designHeight - SEARCH_HEIGHT - 10;
	}

	private void renderSearch(Canvas c) {
		double x0 = searchX();
		double y0 = searchY();
		c.roundRectBordered(x0, y0, x0 + SEARCH_WIDTH, y0 + SEARCH_HEIGHT, SEARCH_HEIGHT / 2.0, Theme.SEARCH_BG,
				search.isFocused() ? Theme.INPUT_BORDER_FOCUSED : Theme.HEADER_BORDER);
		search.renderText(c, x0 + 10, y0, x0 + SEARCH_WIDTH - 22, y0 + SEARCH_HEIGHT,
				Component.translatable("screen.nebulon.search").getString());
		c.magnifier(x0 + SEARCH_WIDTH - 17, y0 + 5.5, Theme.TEXT_DIM);
	}

	private void renderTooltip(Canvas c, Object hovered, Component text, double mx, double my) {
		if (hovered != hoveredThing) {
			hoveredThing = hovered;
			hoverStart = Util.getMillis();
		}
		if (hovered == null || text == null || draggedPanel != null || Util.getMillis() - hoverStart < TOOLTIP_DELAY_MS) {
			return;
		}
		List<String> lines = wrap(c, text.getString(), TOOLTIP_WIDTH);
		int w = 0;
		for (String line : lines) {
			w = Math.max(w, c.width(line, false));
		}
		double h = lines.size() * 10 + 7;
		double x0 = Math.min(mx + 10, designWidth - w - 14);
		double y0 = Math.min(my + 10, designHeight - h - 2);
		c.roundRectBordered(x0, y0, x0 + w + 12, y0 + h, 4, Theme.TOOLTIP_BG, Theme.HEADER_BORDER);
		for (int i = 0; i < lines.size(); i++) {
			c.text(lines.get(i), x0 + 6, y0 + 4 + i * 10, Theme.TEXT_DIM, false);
		}
	}

	/** Simple word wrap measured with the Roboto font. */
	private static List<String> wrap(Canvas c, String text, int maxWidth) {
		List<String> lines = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (String word : text.split(" ")) {
			String candidate = line.isEmpty() ? word : line + " " + word;
			if (c.width(candidate, false) > maxWidth && !line.isEmpty()) {
				lines.add(line.toString());
				line = new StringBuilder(word);
			} else {
				line = new StringBuilder(candidate);
			}
		}
		if (!line.isEmpty()) {
			lines.add(line.toString());
		}
		return lines;
	}

	private List<Element> visibleElements() {
		List<Element> result = new ArrayList<>();
		for (CategoryPanel panel : panels) {
			for (ModuleCard card : panel.visibleCards(search.text())) {
				if (card.isExpanded()) {
					for (Element element : card.elements()) {
						if (element.isVisible()) {
							result.add(element);
						}
					}
				}
			}
		}
		return result;
	}

	private void releaseAllFocus() {
		search.setFocused(false);
		for (CategoryPanel panel : panels) {
			for (ModuleCard card : panel.cards()) {
				for (Element element : card.elements()) {
					element.releaseFocus();
				}
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

		if (Theme.inside(mx, my, searchX(), searchY(), searchX() + SEARCH_WIDTH, searchY() + SEARCH_HEIGHT)) {
			releaseAllFocus();
			search.setFocused(true);
			if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
				search.setTextSilently("");
			}
			return true;
		}

		// Front to back, so the panel drawn on top gets the click.
		for (CategoryPanel panel : drawOrder.reversed()) {
			if (panel.isOverHeader(mx, my)) {
				releaseAllFocus();
				drawOrder.remove(panel);
				drawOrder.add(panel);
				if (button == InputConstants.MOUSE_BUTTON_LEFT) {
					draggedPanel = panel;
					grabX = mx - panel.baseX;
					grabY = my - panel.baseY;
				} else if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
					panel.collapsed = !panel.collapsed;
					savePanel(panel);
				}
				return true;
			}
			for (ModuleCard card : panel.visibleCards(search.text())) {
				if (card.headerClicked(mx, my, button)) {
					releaseAllFocus();
					return true;
				}
			}
		}

		Element clicked = null;
		List<Element> elements = visibleElements();
		for (Element element : elements) {
			if (clicked == null && element.mouseClicked(mx, my, button)) {
				clicked = element;
			}
		}
		for (Element element : elements) {
			if (element != clicked) {
				element.releaseFocus();
			}
		}
		search.setFocused(false);
		activeElement = clicked;
		return clicked != null;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		double mx = toDesign(event.x());
		double my = toDesign(event.y());
		if (draggedPanel != null) {
			int[] snapped = snap((int) Math.round(mx - grabX), (int) Math.round(my - grabY + scroll),
					Theme.PANEL_WIDTH, draggedPanel.height, draggedPanel);
			draggedPanel.baseX = snapped[0];
			draggedPanel.baseY = (int) Math.round(snapped[1] - scroll);
			return true;
		}
		if (activeElement != null) {
			layout();
			activeElement.mouseDragged(mx, my);
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (draggedPanel != null) {
			resolveOverlap(draggedPanel);
			savePanel(draggedPanel);
			draggedPanel = null;
		}
		guides.clear();
		if (activeElement != null) {
			activeElement.mouseReleased();
		}
		return true;
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		int lowest = layout();
		double mx = toDesign(x);
		double my = toDesign(y);
		for (Element element : visibleElements()) {
			if (element instanceof SliderElement slider && slider.scroll(mx, my, scrollY)) {
				return true;
			}
		}
		double minScroll = Math.min(0, designHeight - SEARCH_HEIGHT - 24 - lowest);
		scroll = Math.max(minScroll, Math.min(0, scroll + scrollY * 18.0));
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
		if (search.isFocused()) {
			search.keyPressed(event);
			return true;
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
		if (search.isFocused()) {
			return search.charTyped(event);
		}
		return false;
	}

	@Override
	public void removed() {
		ConfigManager config = NebulonClient.get().configManager();
		for (CategoryPanel panel : panels) {
			savePanel(panel);
		}
		config.save();
		super.removed();
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(null);
	}
}
