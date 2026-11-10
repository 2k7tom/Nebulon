package com.nebulon.gui.element;

import com.nebulon.gui.Canvas;
import com.nebulon.gui.Theme;

/** A full-width button with a centred label; left-click runs the action. */
public final class ButtonElement extends Element {
	private static final int HEIGHT = 16;

	private final Runnable action;

	public ButtonElement(String translationKey, Runnable action) {
		super(translationKey);
		this.action = action;
	}

	@Override
	public int height() {
		return HEIGHT;
	}

	@Override
	public void render(Canvas c, double mouseX, double mouseY) {
		boolean hovered = contains(mouseX, mouseY);
		c.roundRectBordered(x, y + 1, x + width, y + HEIGHT - 1, 3,
				hovered ? Theme.VALUE_BG_HOVER : Theme.VALUE_BG, hovered ? Theme.INPUT_BORDER_FOCUSED : Theme.INPUT_BORDER);
		c.textCentered(label, x + width / 2.0, y + 4, Theme.TEXT, false);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!contains(mouseX, mouseY) || button != 0) {
			return false;
		}
		action.run();
		changed();
		return true;
	}

	/** The description shows over the whole button. */
	@Override
	public boolean isOverLabel(double mouseX, double mouseY) {
		return contains(mouseX, mouseY);
	}
}
