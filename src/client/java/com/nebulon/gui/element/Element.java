package com.nebulon.gui.element;

import com.nebulon.client.NebulonClient;
import com.nebulon.gui.Canvas;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

/**
 * One setting row inside an expanded module card. The screen assigns {@link #x}, {@link #y} and
 * {@link #width} (design units) during layout; elements draw themselves there and hit-test against
 * these bounds. Every setting has a description shown as hover tooltip ({@code <key>.desc}).
 */
public abstract class Element {
	protected final Component label;
	private final Component description;
	private BooleanSupplier visibility = () -> true;

	public int x;
	public int y;
	public int width;

	protected Element(String translationKey) {
		this.label = Component.translatable(translationKey);
		this.description = Component.translatable(translationKey + ".desc");
	}

	/** Shows the element only while the condition holds (e.g. list field only for "Custom List"). */
	public Element visibleWhen(BooleanSupplier condition) {
		this.visibility = condition;
		return this;
	}

	public boolean isVisible() {
		return visibility.getAsBoolean();
	}

	public Component description() {
		return description;
	}

	public abstract int height();

	public abstract void render(Canvas c, double mouseX, double mouseY);

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		return false;
	}

	public void mouseDragged(double mouseX, double mouseY) {
	}

	public void mouseReleased() {
	}

	public boolean keyPressed(KeyEvent event) {
		return false;
	}

	public boolean charTyped(CharacterEvent event) {
		return false;
	}

	/** Whether the element currently captures the keyboard (text field focus). */
	public boolean capturesKeyboard() {
		return false;
	}

	public void releaseFocus() {
	}

	/** Whether the element takes the next mouse click wherever it lands (key binding waiting for a button). */
	public boolean capturesMouse() {
		return false;
	}

	/** Whether the mouse is over the label row (used for the description tooltip). */
	public boolean isOverLabel(double mouseX, double mouseY) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + 14;
	}

	/**
	 * Hover tooltip at the mouse position, or {@code null}. Default: the setting's description over
	 * the label row. Elements with several rows (e.g. the Bestiary list) return one text per row.
	 */
	public Component tooltipAt(double mouseX, double mouseY) {
		return isOverLabel(mouseX, mouseY) ? description : null;
	}

	/** Identity of the hovered part, so the tooltip delay restarts when moving between rows. */
	public Object tooltipTarget(double mouseX, double mouseY) {
		return this;
	}

	protected boolean contains(double mouseX, double mouseY) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height();
	}

	protected static void changed() {
		NebulonClient.get().onSettingsChanged();
	}
}
