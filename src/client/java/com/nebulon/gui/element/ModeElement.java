package com.nebulon.gui.element;

import com.nebulon.gui.Canvas;
import com.nebulon.gui.Theme;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Single row: label on the left, the current value in a small pill directly next to it on the
 * right. Left-click switches to the next value, right-click to the previous one.
 */
public final class ModeElement<T> extends Element {
	private final List<T> options;
	private final Function<T, Component> names;
	private final Supplier<T> getter;
	private final Consumer<T> setter;

	public ModeElement(String translationKey, List<T> options, Function<T, Component> names, Supplier<T> getter, Consumer<T> setter) {
		super(translationKey);
		this.options = options;
		this.names = names;
		this.getter = getter;
		this.setter = setter;
	}

	@Override
	public int height() {
		return Theme.ROW_HEIGHT;
	}

	@Override
	public void render(Canvas c, double mouseX, double mouseY) {
		Component value = names.apply(getter.get());
		int pillWidth = c.width(value, true) + 12;
		double pillX = x + width - pillWidth;
		c.scrollingText(label, x, y + Theme.TEXT_OFFSET, pillX - x - 5, Theme.TEXT, false);
		boolean hovered = contains(mouseX, mouseY);
		c.roundRect(pillX, y + 1.5, x + width, y + Theme.ROW_HEIGHT - 1.5, 3, hovered ? Theme.VALUE_BG_HOVER : Theme.VALUE_BG);
		c.text(value, pillX + 6, y + Theme.TEXT_OFFSET, Theme.TEXT, true);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!contains(mouseX, mouseY) || (button != 0 && button != 1)) {
			return false;
		}
		int index = options.indexOf(getter.get());
		int step = button == 0 ? 1 : options.size() - 1;
		setter.accept(options.get((Math.max(0, index) + step) % options.size()));
		changed();
		return true;
	}
}
