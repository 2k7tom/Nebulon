package com.nebulon.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.util.Util;

import java.util.function.Predicate;

/**
 * Minimal single-line text field drawn in the settings style. Typing always appends at the end
 * (enough for ids, hex codes and the search); Backspace deletes, Ctrl+V pastes, Ctrl+Backspace clears,
 * Enter/Escape leave the field.
 */
public final class TextInput {
	private final int maxLength;
	/** Receives every edit; returns whether the text is valid (invalid text is shown in red). */
	private final Predicate<String> onChange;
	private String text;
	private boolean valid = true;
	private boolean focused;

	public TextInput(String initial, int maxLength, Predicate<String> onChange) {
		this.text = initial;
		this.maxLength = maxLength;
		this.onChange = onChange;
	}

	public String text() {
		return text;
	}

	/** Replaces the text without notifying the listener (e.g. after a wheel change). */
	public void setTextSilently(String value) {
		text = value;
		valid = true;
	}

	public boolean isFocused() {
		return focused;
	}

	public void setFocused(boolean focused) {
		this.focused = focused;
	}

	/** Draws the text and caret inside an area; the caller draws the box. */
	public void renderText(Canvas c, double x0, double y0, double x1, double y1, String placeholder) {
		double textY = y0 + (y1 - y0 - 7) / 2.0 - 0.5;
		int available = (int) (x1 - x0);
		if (text.isEmpty() && !focused) {
			c.text(c.fitStart(placeholder, available, false), x0, textY, Theme.TEXT_FAINT, false);
			return;
		}
		// Show the end of long texts, where typing happens.
		String visible = c.fitEnd(text, available - 2, false);
		c.text(visible, x0, textY, valid ? Theme.TEXT : Theme.TEXT_ERROR, false);
		if (focused && (Util.getMillis() / 500L) % 2L == 0L) {
			double caretX = x0 + c.width(visible, false) + 0.5;
			c.fill(caretX, textY - 0.5, caretX + 0.6, textY + 8.5, Theme.TEXT);
		}
	}

	/** Draws a bordered input box with the text inside. */
	public void render(Canvas c, double x0, double y0, double x1, double y1, String placeholder) {
		c.roundRectBordered(x0, y0, x1, y1, 3, Theme.INPUT_BG, focused ? Theme.INPUT_BORDER_FOCUSED : Theme.INPUT_BORDER);
		renderText(c, x0 + 4, y0, x1 - 4, y1, placeholder);
	}

	public boolean keyPressed(KeyEvent event) {
		if (!focused) {
			return false;
		}
		int key = event.key();
		if (key == InputConstants.KEY_ESCAPE || key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
			focused = false;
			return true;
		}
		if (key == InputConstants.KEY_BACKSPACE) {
			if (event.hasControlDownWithQuirk()) {
				set("");
			} else if (!text.isEmpty()) {
				set(text.substring(0, text.length() - 1));
			}
			return true;
		}
		if (event.isPaste()) {
			String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard().replace("\n", "").replace("\r", "");
			set(text + clipboard);
			return true;
		}
		// Swallow all other keys while typing so they do not trigger key mappings.
		return true;
	}

	public boolean charTyped(CharacterEvent event) {
		if (!focused || !event.isAllowedChatCharacter()) {
			return false;
		}
		set(text + event.codepointAsString());
		return true;
	}

	private void set(String value) {
		text = value.length() > maxLength ? value.substring(0, maxLength) : value;
		valid = onChange.test(text);
	}
}
