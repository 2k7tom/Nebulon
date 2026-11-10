package com.nebulon.gui.element;

import com.nebulon.client.NebulonClient;
import com.nebulon.gui.Canvas;
import com.nebulon.gui.Theme;

import java.util.List;

/**
 * A collapsible section header inside a module's settings (like Odin's dropdown settings): the
 * name with a chevron; left- or right-click opens or closes it. Settings of the section use
 * {@link #isOpen()} in their {@code visibleWhen}. The open state is saved.
 */
public final class DropdownElement extends Element {
	private static final String PREFIX = "dropdown:";
	private final String stateKey;

	public DropdownElement(String translationKey, String id) {
		super(translationKey);
		this.stateKey = PREFIX + id;
	}

	private static List<String> expanded() {
		return NebulonClient.get().configManager().get().expandedModules;
	}

	public boolean isOpen() {
		return expanded().contains(stateKey);
	}

	@Override
	public int height() {
		return Theme.ROW_HEIGHT;
	}

	@Override
	public void render(Canvas c, double mouseX, double mouseY) {
		boolean open = isOpen();
		c.scrollingText(label, x, y + Theme.TEXT_OFFSET, width - 12, open ? Theme.TEXT : Theme.TEXT_DIM, true);
		// Chevron: down when open, right when closed.
		double cx = x + width - 7;
		double cy = y + Theme.ROW_HEIGHT / 2.0;
		double step = 1.0 / c.scale;
		int color = open ? Theme.TEXT : Theme.TEXT_DIM;
		for (double i = 0; i < 2.5; i += step) {
			if (open) {
				c.fill(cx + i, cy - 1.2 + i, cx + 5 - i, cy - 1.2 + i + step, color);
			} else {
				c.fill(cx + 1 + i, cy - 2.5 + i, cx + 1 + i + step, cy + 2.5 - i, color);
			}
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!contains(mouseX, mouseY) || (button != 0 && button != 1)) {
			return false;
		}
		if (!expanded().remove(stateKey)) {
			expanded().add(stateKey);
		}
		return true;
	}
}
