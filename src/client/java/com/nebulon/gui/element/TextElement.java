package com.nebulon.gui.element;

import com.nebulon.gui.Canvas;
import com.nebulon.gui.TextInput;
import com.nebulon.gui.Theme;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Label with a text field below. Every edit is passed to the parser; only valid text is applied,
 * invalid text stays red and leaves the previous setting untouched.
 */
public final class TextElement extends Element {
	private final TextInput input;
	private final String placeholder;

	public TextElement(String translationKey, Supplier<String> initial, String placeholder, Predicate<String> parser) {
		super(translationKey);
		this.placeholder = placeholder;
		this.input = new TextInput(initial.get(), 2048, text -> {
			boolean valid = parser.test(text);
			if (valid) {
				changed();
			}
			return valid;
		});
	}

	@Override
	public int height() {
		return 28;
	}

	@Override
	public void render(Canvas c, double mouseX, double mouseY) {
		c.text(label, x, y + 2, Theme.TEXT, false);
		input.render(c, x, y + 13, x + width, y + 27, placeholder);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		boolean hit = contains(mouseX, mouseY) && mouseY >= y + 13;
		input.setFocused(hit);
		return hit;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		return input.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		return input.charTyped(event);
	}

	@Override
	public boolean capturesKeyboard() {
		return input.isFocused();
	}

	@Override
	public void releaseFocus() {
		input.setFocused(false);
	}
}
