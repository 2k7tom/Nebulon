package com.nebulon.gui.element;

import com.mojang.blaze3d.platform.InputConstants;
import com.nebulon.gui.Canvas;
import com.nebulon.gui.Theme;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.function.Supplier;

/**
 * Key binding setting: label and the bound key on the right. Left-click starts listening; the next
 * key or mouse button (right, middle, side buttons) becomes the binding, Escape removes it, another
 * left click cancels. It changes the vanilla key mapping, so Options &gt; Controls shows the same.
 */
public final class KeybindElement extends Element {
	private static final int BOX_WIDTH = 46;

	private final Supplier<KeyMapping> mapping;
	private boolean listening;

	public KeybindElement(String translationKey, Supplier<KeyMapping> mapping) {
		super(translationKey);
		this.mapping = mapping;
	}

	@Override
	public int height() {
		return Theme.ROW_HEIGHT;
	}

	@Override
	public void render(Canvas c, double mouseX, double mouseY) {
		KeyMapping key = mapping.get();
		double boxX = x + width - BOX_WIDTH;
		c.scrollingText(label, x, y + Theme.TEXT_OFFSET, boxX - 5 - x, Theme.TEXT, false);
		boolean hover = Theme.inside(mouseX, mouseY, boxX, y + 1, x + width, y + Theme.ROW_HEIGHT - 1);
		c.roundRectBordered(boxX, y + 1.5, x + width, y + Theme.ROW_HEIGHT - 1.5, 2.5, Theme.INPUT_BG,
				listening ? Theme.ACCENT : hover ? Theme.TEXT_DIM : Theme.INPUT_BORDER);
		Component text = listening ? Component.literal("> ... <")
				: key.isUnbound() ? Component.literal("None") : key.getTranslatedKeyMessage();
		String shown = c.fitEnd(text.getString(), BOX_WIDTH - 6, false);
		c.textCentered(Component.literal(shown), boxX + BOX_WIDTH / 2.0, y + Theme.TEXT_OFFSET,
				listening ? Theme.ACCENT : key.isUnbound() ? Theme.TEXT_FAINT : Theme.TEXT, false);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (listening) {
			if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
				bind(InputConstants.Type.MOUSE.getOrCreate(button));
			}
			listening = false;
			return true;
		}
		if (!contains(mouseX, mouseY) || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			return false;
		}
		listening = true;
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (!listening) {
			return false;
		}
		bind(event.key() == GLFW.GLFW_KEY_ESCAPE ? InputConstants.UNKNOWN : InputConstants.getKey(event));
		listening = false;
		return true;
	}

	private void bind(InputConstants.Key key) {
		Minecraft mc = Minecraft.getInstance();
		KeyMapping keyMapping = mapping.get();
		keyMapping.setKey(key);
		KeyMapping.resetMapping();
		if (com.nebulon.keybind.NebulonKeybinds.isModOnly(keyMapping)) {
			// Not in Controls: stored in the mod's config.
			com.nebulon.keybind.NebulonKeybinds.storeModOnlyKey(keyMapping, com.nebulon.client.NebulonClient.get().configManager().get());
			changed();
		} else {
			mc.options.save();
		}
	}

	@Override
	public boolean capturesKeyboard() {
		return listening;
	}

	/** While listening, the next mouse button anywhere belongs to this element. */
	@Override
	public boolean capturesMouse() {
		return listening;
	}

	@Override
	public void releaseFocus() {
		listening = false;
	}
}
