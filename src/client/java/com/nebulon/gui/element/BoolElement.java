package com.nebulon.gui.element;

import com.nebulon.gui.Canvas;
import com.nebulon.gui.Theme;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Label with an animated on/off switch on the right. */
public final class BoolElement extends Element {
	private final BooleanSupplier getter;
	private final Consumer<Boolean> setter;
	private final Toggle toggle = new Toggle();

	public BoolElement(String translationKey, BooleanSupplier getter, Consumer<Boolean> setter) {
		super(translationKey);
		this.getter = getter;
		this.setter = setter;
	}

	@Override
	public int height() {
		return Theme.ROW_HEIGHT;
	}

	@Override
	public void render(Canvas c, double mouseX, double mouseY) {
		boolean on = getter.getAsBoolean();
		// The label ends 5 units before the switch; longer labels scroll inside that space.
		c.scrollingText(label, x, y + Theme.TEXT_OFFSET, width - Toggle.WIDTH - 5, on ? Theme.TEXT : Theme.TEXT_DIM, false);
		toggle.render(c, x + width - Toggle.WIDTH, y + (height() - Toggle.HEIGHT) / 2.0, on, contains(mouseX, mouseY));
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!contains(mouseX, mouseY) || button != 0) {
			return false;
		}
		setter.accept(!getter.getAsBoolean());
		changed();
		return true;
	}
}
