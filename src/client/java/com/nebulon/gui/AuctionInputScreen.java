package com.nebulon.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.nebulon.client.NebulonClient;
import com.nebulon.config.AuctionInputSettings;
import com.nebulon.config.AuctionInputSettings.Mode;
import com.nebulon.item.AuctionInput;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.world.item.ItemStack;

/**
 * "Auction Input": the price box shown instead of Hypixel's auction sign, in the menu style of the
 * mod. Typing a price (10m, 5.5k ...) and pressing Enter or "Done" sends it as the sign's first
 * line, exactly as if it had been typed on the sign; Undercut mode sends "lowest BIN - amount".
 */
public class AuctionInputScreen extends Screen {
	private static final double WIDTH = 200;
	private static final double HEIGHT = 146;
	private static final int VALUE_GREEN = 0xFF55FF55;
	private static final int VALUE_YELLOW = 0xFFFFFF55;

	private final BlockPos signPos;
	private final boolean frontText;
	private final String[] originalText;
	private final ItemStack stack;
	private final TextInput input;
	private Mode mode;
	private Long parsedValue;
	private long lowestBin;

	private int scale = 2;
	private double designWidth;
	private double designHeight;

	public AuctionInputScreen(BlockPos signPos, boolean frontText, String[] originalText, ItemStack stack) {
		super(Component.translatable("screen.nebulon.auction_input"));
		this.signPos = signPos;
		this.frontText = frontText;
		this.originalText = originalText;
		this.stack = stack;
		AuctionInputSettings s = NebulonClient.get().configManager().get().auctionInput;
		if (!s.rememberText) {
			AuctionInput.lastInput = "";
		}
		mode = s.rememberMode && AuctionInput.lastMode != null ? AuctionInput.lastMode : s.defaultMode;
		AuctionInput.lastMode = mode;
		input = new TextInput(AuctionInput.lastInput, 32, text -> {
			AuctionInput.lastInput = text;
			recalculate();
			return text.isEmpty() || parsedValue != null;
		});
		input.setFocused(true);
		recalculate();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private void recalculate() {
		lowestBin = AuctionInput.lowestBin(AuctionInput.skyblockId(stack));
		Long value = AuctionInput.parseCompact(input.text());
		parsedValue = value == null ? null : mode == Mode.UNDERCUT ? Math.max(0L, lowestBin - value) : value;
	}

	public Long parsedValueForTest() {
		return parsedValue;
	}

	private void updateMetrics() {
		Window window = minecraft.getWindow();
		scale = Canvas.scaleFor(window);
		designWidth = window.getWidth() / (double) scale;
		designHeight = window.getHeight() / (double) scale;
	}

	private double toDesign(double guiCoordinate) {
		return guiCoordinate * minecraft.getWindow().getGuiScale() / scale;
	}

	private double left() {
		return Math.round((designWidth - WIDTH) / 2.0);
	}

	private double top() {
		return Math.round((designHeight - HEIGHT) / 2.0);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		super.extractRenderState(g, mouseX, mouseY, a);
		updateMetrics();
		recalculate();
		Theme.apply(NebulonClient.get().configManager().get().menuColor.rgb());
		g.fill(0, 0, width, height, Theme.OVERLAY);

		int guiScale = minecraft.getWindow().getGuiScale();
		g.pose().pushMatrix();
		g.pose().scale(1.0F / guiScale, 1.0F / guiScale);
		Canvas c = new Canvas(g, font, scale, com.nebulon.config.GuiFont.of(NebulonClient.get().configManager().get().auctionInput.useModFont));
		double mx = toDesign(mouseX);
		double my = toDesign(mouseY);
		double x0 = left();
		double y0 = top();
		double x1 = x0 + WIDTH;
		double cx = x0 + WIDTH / 2.0;

		c.roundRect(x0, y0, x1, y0 + HEIGHT, 6, Theme.PANEL_BG);
		c.roundRectOutline(x0, y0, x1, y0 + HEIGHT, 6, Theme.HEADER_BORDER);

		// The item, in a small rounded slot; hovering it shows its tooltip.
		double ix = cx - 8;
		double iy = y0 + 10;
		boolean itemHovered = Theme.inside(mx, my, ix - 3, iy - 3, ix + 19, iy + 19);
		c.roundRect(ix - 3, iy - 3, ix + 19, iy + 19, 4, itemHovered ? Theme.VALUE_BG_HOVER : Theme.VALUE_BG);
		g.pose().pushMatrix();
		g.pose().translate(c.px(ix), c.px(iy));
		g.pose().scale(scale, scale);
		g.item(stack, 0, 0);
		g.itemDecorations(font, stack, 0, 0);
		g.pose().popMatrix();

		Component header = Component.translatable(mode == Mode.UNDERCUT ? "screen.nebulon.auction_input.undercut" : "screen.nebulon.auction_input.title");
		c.textCentered(header, cx, y0 + 36, Theme.ACCENT, true);
		c.textCentered(Component.translatable("screen.nebulon.auction_input.lowest_bin", AuctionInput.formatShort(lowestBin)),
				cx, y0 + 48, Theme.TEXT_DIM, false);

		Component valueLine;
		int valueColor;
		if (parsedValue != null) {
			valueLine = Component.translatable("screen.nebulon.auction_input.value")
					.append(Component.literal(AuctionInput.formatComma(parsedValue)).withColor(VALUE_YELLOW));
			valueColor = VALUE_GREEN;
		} else if (input.text().isEmpty()) {
			valueLine = Component.translatable("screen.nebulon.auction_input.hint");
			valueColor = Theme.TEXT_FAINT;
		} else {
			valueLine = Component.translatable("screen.nebulon.auction_input.invalid");
			valueColor = Theme.TEXT_ERROR;
		}
		c.textCentered(valueLine, cx, y0 + 60, valueColor, false);

		double bx0 = x0 + 12;
		double bx1 = x1 - 12;
		input.render(c, bx0, y0 + 74, bx1, y0 + 92, Component.translatable("screen.nebulon.auction_input.placeholder").getString());
		button(c, bx0, y0 + 98, bx1, y0 + 114, Component.translatable("screen.nebulon.auction_input.done"), mx, my, true);
		button(c, bx0, y0 + 118, bx1, y0 + 134, Component.translatable("screen.nebulon.auction_input.mode",
				Component.translatable("option.nebulon.auction_input.mode." + mode.name().toLowerCase(java.util.Locale.ROOT))), mx, my, false);
		g.pose().popMatrix();

		if (itemHovered) {
			g.setTooltipForNextFrame(font, stack, mouseX, mouseY);
		}
	}

	private static void button(Canvas c, double x0, double y0, double x1, double y1, Component label, double mx, double my, boolean primary) {
		boolean hovered = Theme.inside(mx, my, x0, y0, x1, y1);
		int fill = primary ? (hovered ? Theme.CARD_BG_ENABLED_HOVER : Theme.CARD_BG_ENABLED) : (hovered ? Theme.CARD_BG_HOVER : Theme.CARD_BG);
		c.roundRectBordered(x0, y0, x1, y1, 3, fill, primary ? Theme.CARD_BORDER_ENABLED : Theme.INPUT_BORDER);
		c.textCentered(label, (x0 + x1) / 2.0, y0 + (y1 - y0 - 7) / 2.0 - 0.5, hovered ? Theme.ACCENT : Theme.TEXT, primary);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		updateMetrics();
		if (event.button() != 0) {
			return super.mouseClicked(event, doubleClick);
		}
		double mx = toDesign(event.x());
		double my = toDesign(event.y());
		double x0 = left() + 12;
		double x1 = left() + WIDTH - 12;
		double y0 = top();
		if (Theme.inside(mx, my, x0, y0 + 98, x1, y0 + 114)) {
			finish();
			return true;
		}
		if (Theme.inside(mx, my, x0, y0 + 118, x1, y0 + 134)) {
			toggleMode();
			return true;
		}
		input.setFocused(true);
		return true;
	}

	public void toggleMode() {
		mode = mode == Mode.NORMAL ? Mode.UNDERCUT : Mode.NORMAL;
		AuctionInput.lastMode = mode;
		recalculate();
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.key();
		if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
			finish();
			return true;
		}
		if (key == InputConstants.KEY_ESCAPE) {
			return super.keyPressed(event);
		}
		input.setFocused(true);
		return input.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		input.setFocused(true);
		return input.charTyped(event);
	}

	/** Sends the price as the sign's first line (the other lines unchanged) and closes the box. */
	public void finish() {
		String line0 = parsedValue != null ? Long.toString(parsedValue) : input.text();
		if (minecraft.getConnection() != null) {
			minecraft.getConnection().send(new ServerboundSignUpdatePacket(signPos, frontText, line0,
					originalText[1], originalText[2], originalText[3]));
		}
		onClose();
	}
}
