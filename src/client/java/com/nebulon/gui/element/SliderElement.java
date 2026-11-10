package com.nebulon.gui.element;

import com.nebulon.gui.Canvas;
import com.nebulon.gui.Theme;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Integer slider: label and value in the first line, a thin track below. Drag or scroll to change. */
public final class SliderElement extends Element {
	private final int min;
	private final int max;
	private final java.util.function.IntFunction<String> format;
	private final IntSupplier getter;
	private final IntConsumer setter;
	private boolean dragging;

	public SliderElement(String translationKey, int min, int max, String unit, IntSupplier getter, IntConsumer setter) {
		this(translationKey, min, max, value -> value + unit, getter, setter);
	}

	/** Slider whose value text is formatted freely, e.g. tenths shown as {@code 2.5}. */
	public SliderElement(String translationKey, int min, int max, java.util.function.IntFunction<String> format, IntSupplier getter, IntConsumer setter) {
		super(translationKey);
		this.format = format;
		this.min = min;
		this.max = max;
		this.getter = getter;
		this.setter = setter;
	}

	@Override
	public int height() {
		return 20;
	}

	@Override
	public void render(Canvas c, double mouseX, double mouseY) {
		int value = getter.getAsInt();
		String text = format.apply(value);
		int valueWidth = c.width(text, false);
		c.scrollingText(label, x, y + 2, width - valueWidth - 5, Theme.TEXT, false);
		c.text(text, x + width - valueWidth, y + 2, Theme.TEXT_DIM, false);
		double trackY = y + 14.5;
		c.roundRect(x, trackY, x + width, trackY + 1.5, 0.75, Theme.CONTROL_OFF);
		double fillX = x + (double) (value - min) / (max - min) * width;
		c.roundRect(x, trackY, Math.max(x + 1, fillX), trackY + 1.5, 0.75, Theme.CONTROL_ON);
		c.roundRect(fillX - 2.25, trackY - 1.5, fillX + 2.25, trackY + 3, 2.25, Theme.CONTROL_ON);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!contains(mouseX, mouseY) || button != 0) {
			return false;
		}
		dragging = true;
		update(mouseX);
		return true;
	}

	@Override
	public void mouseDragged(double mouseX, double mouseY) {
		if (dragging) {
			update(mouseX);
		}
	}

	@Override
	public void mouseReleased() {
		dragging = false;
	}

	/** Mouse wheel over the slider changes the value by one step. */
	public boolean scroll(double mouseX, double mouseY, double amount) {
		if (!contains(mouseX, mouseY)) {
			return false;
		}
		set(getter.getAsInt() + (amount > 0 ? 1 : -1));
		return true;
	}

	private void update(double mouseX) {
		double fraction = Math.max(0.0, Math.min(1.0, (mouseX - x) / width));
		set(min + (int) Math.round(fraction * (max - min)));
	}

	private void set(int value) {
		int clamped = Math.max(min, Math.min(max, value));
		if (clamped != getter.getAsInt()) {
			setter.accept(clamped);
			changed();
		}
	}
}
