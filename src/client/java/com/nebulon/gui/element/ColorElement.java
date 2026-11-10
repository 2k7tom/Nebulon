package com.nebulon.gui.element;

import com.nebulon.config.ColorSetting;
import com.nebulon.config.ColorUtil;
import com.nebulon.gui.Canvas;
import com.nebulon.gui.ColorWheel;
import com.nebulon.gui.TextInput;
import com.nebulon.gui.Theme;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;

import java.util.function.Supplier;

/**
 * Colour setting: label and swatch; click to open a colour picker with an RGB wheel
 * (hue/saturation), a brightness bar, a hex code field and a chroma (rainbow animation) switch.
 */
public final class ColorElement extends Element {
	private static final int HEADER = Theme.ROW_HEIGHT;
	private static final int WHEEL = 64;
	private static final int BAR_WIDTH = 7;
	private static final int HEX_HEIGHT = 14;
	private static final int PICKER_BASE = 2 + WHEEL + 5 + HEX_HEIGHT;
	private static final int ALPHA_LABEL = 26;

	private final Supplier<ColorSetting> setting;
	/** Whether the chroma (rainbow) switch is offered; off for the menu colour. */
	private final boolean allowChroma;
	/** Whether an opacity slider is shown (puzzle solver colours). */
	private final boolean allowAlpha;
	private final TextInput hexInput;
	private boolean expanded;
	private float hue;
	private float saturation;
	private float value = 1.0F;
	private String syncedHex = "";
	private boolean draggingWheel;
	private boolean draggingBar;
	private boolean draggingAlpha;
	private final Toggle chromaToggle = new Toggle();

	public ColorElement(String translationKey, Supplier<ColorSetting> setting) {
		this(translationKey, setting, true);
	}

	public ColorElement(String translationKey, Supplier<ColorSetting> setting, boolean allowChroma) {
		this(translationKey, setting, allowChroma, false);
	}

	public ColorElement(String translationKey, Supplier<ColorSetting> setting, boolean allowChroma, boolean allowAlpha) {
		super(translationKey);
		this.setting = setting;
		this.allowChroma = allowChroma;
		this.allowAlpha = allowAlpha;
		this.hexInput = new TextInput(setting.get().hex, 9, this::onHexTyped);
	}

	@Override
	public int height() {
		int rows = (allowChroma ? 1 : 0) + (allowAlpha ? 1 : 0);
		return HEADER + (expanded ? PICKER_BASE + (rows == 0 ? 2 : rows * (4 + Theme.ROW_HEIGHT)) : 0);
	}

	private double wheelTop() {
		return y + HEADER + 2;
	}

	private double barX() {
		return x + WHEEL + 7;
	}

	private double hexY() {
		return wheelTop() + WHEEL + 5;
	}

	private double chromaY() {
		return hexY() + HEX_HEIGHT + 4;
	}

	private double alphaY() {
		return chromaY() + (allowChroma ? Theme.ROW_HEIGHT + 4 : 0);
	}

	private double alphaBarX0() {
		return x + ALPHA_LABEL;
	}

	/** Leaves room for the widest percentage ("100 %") right of the bar. */
	private double alphaBarX1() {
		return x + width - 30;
	}

	@Override
	public void render(Canvas c, double mouseX, double mouseY) {
		ColorSetting color = setting.get();
		syncFrom(color);
		long now = Util.getMillis();
		int current = ARGB.opaque(color.currentRgb(now));
		int swatch = allowAlpha ? color.argb(now) : current;

		double swatchX = x + width - 16;
		c.roundRectBordered(swatchX, y + 2.5, x + width, y + 11.5, 2.5, swatch, Theme.INPUT_BORDER);
		double labelEnd = swatchX - 5;
		if (allowChroma && color.chroma) {
			Component tag = Component.translatable("option.nebulon.chroma");
			double tagX = swatchX - 4 - c.width(tag, false);
			c.text(tag, tagX, y + Theme.TEXT_OFFSET, Theme.TEXT_FAINT, false);
			labelEnd = tagX - 5;
		}
		c.scrollingText(label, x, y + Theme.TEXT_OFFSET, labelEnd - x, Theme.TEXT, false);
		if (!expanded) {
			return;
		}

		double top = wheelTop();
		c.colorWheel(x, top, WHEEL, value);
		double radius = WHEEL / 2.0;
		double markerX = x + radius + Math.cos(hue * 2.0 * Math.PI) * saturation * (radius - 1);
		double markerY = top + radius + Math.sin(hue * 2.0 * Math.PI) * saturation * (radius - 1);
		c.roundRectBordered(markerX - 2.5, markerY - 2.5, markerX + 2.5, markerY + 2.5, 2.5, 0xFFFFFFFF, 0xFF000000);

		// Brightness bar: full colour at the top, black at the bottom.
		double barX = barX();
		int full = ARGB.opaque(ColorUtil.hsvToRgb(hue, saturation, 1.0F));
		c.roundRect(barX - 0.5, top - 0.5, barX + BAR_WIDTH + 0.5, top + WHEEL + 0.5, 2, Theme.INPUT_BORDER);
		c.fillGradient(barX, top, barX + BAR_WIDTH, top + WHEEL, full, 0xFF000000);
		double barMarker = top + (1.0F - value) * (WHEEL - 1);
		c.roundRectBordered(barX - 1.5, barMarker - 1.5, barX + BAR_WIDTH + 1.5, barMarker + 1.5, 1.5, 0xFFFFFFFF, 0xFF000000);

		// Preview of the resulting colour.
		double previewX = barX + BAR_WIDTH + 7;
		if (previewX + 12 <= x + width) {
			c.roundRectBordered(previewX, top, x + width, top + WHEEL, 4, current, Theme.INPUT_BORDER);
		}

		double hexY = hexY();
		c.text("Hex", x, hexY + 3.5, Theme.TEXT_DIM, false);
		hexInput.render(c, x + 22, hexY, x + width, hexY + HEX_HEIGHT, "#RRGGBB");

		if (allowChroma) {
			double chromaY = chromaY();
			c.text(Component.translatable("option.nebulon.chroma"), x, chromaY + Theme.TEXT_OFFSET, color.chroma ? Theme.TEXT : Theme.TEXT_DIM, false);
			chromaToggle.render(c, x + width - Toggle.WIDTH, chromaY + (Theme.ROW_HEIGHT - Toggle.HEIGHT) / 2.0, color.chroma,
					mouseX >= x && mouseX < x + width && mouseY >= chromaY && mouseY < chromaY + Theme.ROW_HEIGHT);
		}
		if (allowAlpha) {
			renderAlpha(c, color, current);
		}
	}

	/** Opacity slider: the colour from transparent to opaque, with a marker and the percentage. */
	private void renderAlpha(Canvas c, ColorSetting color, int opaque) {
		double rowY = alphaY();
		c.text(Component.translatable("option.nebulon.alpha"), x, rowY + Theme.TEXT_OFFSET, Theme.TEXT_DIM, false);
		double x0 = alphaBarX0();
		double x1 = alphaBarX1();
		double barY0 = rowY + 4;
		double barY1 = rowY + Theme.ROW_HEIGHT - 4;
		c.roundRect(x0 - 0.5, barY0 - 0.5, x1 + 0.5, barY1 + 0.5, 1.5, Theme.INPUT_BORDER);
		c.fill(x0, barY0, x1, barY1, Theme.INPUT_BG);
		int segments = 12;
		double segment = (x1 - x0) / segments;
		for (int i = 0; i < segments; i++) {
			int a = Math.round(255.0F * (i + 0.5F) / segments);
			c.fill(x0 + i * segment, barY0, x0 + (i + 1) * segment, barY1, (a << 24) | (opaque & 0xFFFFFF));
		}
		double marker = x0 + color.alpha / 255.0 * (x1 - x0);
		c.roundRectBordered(marker - 1.5, barY0 - 1.5, marker + 1.5, barY1 + 1.5, 1.5, 0xFFFFFFFF, 0xFF000000);
		String percent = Math.round(color.alpha / 2.55F) + " %";
		c.text(percent, x + width - c.width(percent, false), rowY + Theme.TEXT_OFFSET, Theme.TEXT_DIM, false);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!contains(mouseX, mouseY) || button != 0) {
			return false;
		}
		if (mouseY < y + HEADER) {
			expanded = !expanded;
			hexInput.setFocused(false);
			return true;
		}
		double top = wheelTop();
		double barX = barX();
		double hexY = hexY();
		double chromaY = chromaY();
		hexInput.setFocused(mouseY >= hexY && mouseY < hexY + HEX_HEIGHT && mouseX >= x + 22);
		if (Theme.inside(mouseX, mouseY, x, top, x + WHEEL, top + WHEEL)) {
			draggingWheel = true;
			pickWheel(mouseX, mouseY);
		} else if (Theme.inside(mouseX, mouseY, barX - 2, top, barX + BAR_WIDTH + 2, top + WHEEL)) {
			draggingBar = true;
			pickBar(mouseY);
		} else if (allowAlpha && mouseY >= alphaY() && mouseY < alphaY() + Theme.ROW_HEIGHT
				&& mouseX >= alphaBarX0() - 2 && mouseX <= alphaBarX1() + 2) {
			draggingAlpha = true;
			pickAlpha(mouseX);
		} else if (allowChroma && mouseY >= chromaY && mouseY < chromaY + Theme.ROW_HEIGHT) {
			ColorSetting color = setting.get();
			color.chroma = !color.chroma;
			changed();
		}
		return true;
	}

	@Override
	public void mouseDragged(double mouseX, double mouseY) {
		if (draggingWheel) {
			pickWheel(mouseX, mouseY);
		} else if (draggingBar) {
			pickBar(mouseY);
		} else if (draggingAlpha) {
			pickAlpha(mouseX);
		}
	}

	@Override
	public void mouseReleased() {
		draggingWheel = false;
		draggingBar = false;
		draggingAlpha = false;
	}

	private void pickAlpha(double mouseX) {
		double fraction = Math.max(0.0, Math.min(1.0, (mouseX - alphaBarX0()) / (alphaBarX1() - alphaBarX0())));
		setting.get().alpha = (int) Math.round(fraction * 255.0);
		changed();
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		return hexInput.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		return hexInput.charTyped(event);
	}

	@Override
	public boolean capturesKeyboard() {
		return hexInput.isFocused();
	}

	@Override
	public void releaseFocus() {
		hexInput.setFocused(false);
	}

	private void pickWheel(double mouseX, double mouseY) {
		double radius = WHEEL / 2.0;
		float[] hs = ColorWheel.pick(mouseX - (x + radius), mouseY - (wheelTop() + radius), radius);
		hue = hs[0];
		saturation = hs[1];
		apply();
	}

	private void pickBar(double mouseY) {
		value = (float) Math.max(0.0, Math.min(1.0, 1.0 - (mouseY - wheelTop()) / (WHEEL - 1)));
		apply();
	}

	/** Writes the picked HSV colour into the setting and the hex field. */
	private void apply() {
		String hex = ColorUtil.format(ColorUtil.hsvToRgb(hue, saturation, value));
		setting.get().hex = hex;
		syncedHex = hex;
		hexInput.setTextSilently(hex);
		changed();
	}

	private boolean onHexTyped(String text) {
		int rgb = ColorUtil.parseRgb(text);
		if (rgb == ColorUtil.INVALID) {
			return false;
		}
		setting.get().hex = ColorUtil.format(rgb);
		changed();
		return true;
	}

	/** Re-reads the HSV values when the colour was changed elsewhere (hex field, config reload). */
	private void syncFrom(ColorSetting color) {
		if (color.hex.equals(syncedHex)) {
			return;
		}
		syncedHex = color.hex;
		float[] hsv = ColorUtil.rgbToHsv(color.rgb());
		hue = hsv[0];
		saturation = hsv[1];
		value = hsv[2];
		if (!hexInput.isFocused()) {
			hexInput.setTextSilently(color.hex);
		}
	}
}
