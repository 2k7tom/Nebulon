package com.nebulon.gui;

import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * A category column: a header card (icon + name; drag to move, right-click to collapse) and below
 * it a translucent container holding the module cards.
 */
public final class CategoryPanel {
	private static final int EMPTY_HINT_HEIGHT = 16;

	private final String id;
	private final Component title;
	private final List<ModuleCard> cards;

	/** Saved position in design units (without scroll). */
	public int baseX;
	public int baseY;
	/** Collapsed: only the header is shown (right-click on the category name). */
	public boolean collapsed;
	/** Position on screen for the current frame. */
	public int x;
	public int y;
	/** Height of the whole panel (header and cards) in the current frame, design units. */
	public int height = Theme.HEADER_HEIGHT;

	public CategoryPanel(String id, List<ModuleCard> cards) {
		this.id = id;
		this.title = Component.translatable("category.nebulon." + id);
		this.cards = cards;
	}

	public String id() {
		return id;
	}

	public List<ModuleCard> cards() {
		return cards;
	}

	/** Cards that are currently shown (not collapsed, matching the search). */
	public List<ModuleCard> visibleCards(String search) {
		if (collapsed) {
			return List.of();
		}
		return cards.stream().filter(card -> card.matches(search)).toList();
	}

	public boolean isOverHeader(double mouseX, double mouseY) {
		return Theme.inside(mouseX, mouseY, x - 2, y, x + Theme.PANEL_WIDTH + 2, y + Theme.HEADER_HEIGHT);
	}

	/** Lays out the visible cards; returns the bottom edge of the panel. */
	public int layout(int x, int y, String search) {
		this.x = x;
		this.y = y;
		if (collapsed) {
			return y + Theme.HEADER_HEIGHT;
		}
		if (cards.isEmpty()) {
			return search.isEmpty() ? containerTop() + Theme.PADDING * 2 + EMPTY_HINT_HEIGHT : y + Theme.HEADER_HEIGHT;
		}
		int cardY = containerTop() + Theme.PADDING;
		boolean any = false;
		for (ModuleCard card : cards) {
			if (!card.matches(search)) {
				continue;
			}
			card.layout(x + Theme.PADDING, cardY, Theme.PANEL_WIDTH - 2 * Theme.PADDING);
			cardY += card.height() + Theme.CARD_GAP;
			any = true;
		}
		return any ? cardY - Theme.CARD_GAP + Theme.PADDING : y + Theme.HEADER_HEIGHT;
	}

	private int containerTop() {
		return y + Theme.HEADER_HEIGHT + Theme.HEADER_GAP;
	}

	public void render(Canvas c, double mouseX, double mouseY, String search, int bottom) {
		boolean hovered = isOverHeader(mouseX, mouseY);
		// Exactly as wide as the glass panel below it (which reaches 2 units past the cards on both sides).
		c.roundRectBordered(x - 2, y, x + Theme.PANEL_WIDTH + 2, y + Theme.HEADER_HEIGHT, 5,
				hovered ? Theme.CARD_BG_ENABLED : Theme.HEADER_BG, Theme.HEADER_BORDER);
		int textWidth = c.width(title, true);
		double contentX = x + (Theme.PANEL_WIDTH - (textWidth + 12)) / 2.0;
		CategoryIcons.draw(c, id, contentX, y + (Theme.HEADER_HEIGHT - CategoryIcons.SIZE) / 2.0, collapsed ? Theme.TEXT_DIM : Theme.TEXT);
		c.text(title, contentX + 12, y + (Theme.HEADER_HEIGHT - 8) / 2.0, collapsed ? Theme.TEXT_DIM : Theme.TEXT, true);

		if (bottom <= y + Theme.HEADER_HEIGHT) {
			return; // collapsed or everything filtered out by the search
		}
		c.roundRect(x - 2, containerTop() - 2, x + Theme.PANEL_WIDTH + 2, bottom + 2, 7, Theme.PANEL_BG);
		if (cards.isEmpty()) {
			Component hint = Component.translatable("category.nebulon.empty");
			c.textCentered(hint, x + Theme.PANEL_WIDTH / 2.0, containerTop() + Theme.PADDING + 4, Theme.TEXT_FAINT, false);
			return;
		}
		for (ModuleCard card : cards) {
			if (card.matches(search)) {
				card.render(c, mouseX, mouseY);
			}
		}
	}
}
