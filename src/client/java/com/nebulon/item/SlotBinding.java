package com.nebulon.item;

import com.nebulon.client.NebulonClient;
import com.nebulon.config.SlotBindingSettings;
import com.nebulon.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

import java.util.Map;

/**
 * "Slot Binding" (Settings), ported 1:1 from NoammAddons 1.3.0 (CC0): binds slots to hotbar slots
 * for quick item swaps, in the survival inventory.
 * <ul>
 *     <li>Binding key (R) over a slot, then over a second slot: links an inventory slot with a hotbar
 *     slot. The key over an already bound slot removes its binding.</li>
 *     <li>Shift + left click on a bound slot swaps the two items (one vanilla swap click, like
 *     pressing the hotbar number key over the slot).</li>
 *     <li>Bound slots get a border and are joined by a line; while binding, a line follows the mouse.</li>
 * </ul>
 */
public final class SlotBinding {
	private static Integer previousSlot;
	private static Object previousScreen;

	private SlotBinding() {
	}

	private static SlotBindingSettings settings() {
		return NebulonClient.get().configManager().get().slotBinding;
	}

	private static boolean enabled() {
		// Part of Inventory QOL: only while that feature is on, too.
		return NebulonClient.get() != null && settings().enabled && NebulonClient.get().configManager().get().visualTweaks.enabled;
	}

	private static boolean isHotbar(int slot) {
		return slot >= 36 && slot <= 44;
	}

	private static Integer hovered(AbstractContainerScreen<?> screen) {
		Slot slot = ((AbstractContainerScreenAccessor) screen).nebulon$hoveredSlot();
		return slot == null ? null : slot.index;
	}

	/** The binding key was pressed (keyboard or mouse); {@code true} = consumed. */
	public static boolean onBindKey(AbstractContainerScreen<?> screen) {
		if (!enabled() || !(screen instanceof InventoryScreen)) {
			return false;
		}
		Integer slot = hovered(screen);
		if (slot == null) {
			return false;
		}
		if (previousScreen != screen) {
			previousSlot = null;
			previousScreen = screen;
		}
		bindSlot(slot);
		return true;
	}

	/** Shift + left click on a bound slot swaps it with its partner; {@code true} = consumed. */
	public static boolean onShiftClick(AbstractContainerScreen<?> screen, int button, boolean shift) {
		if (!enabled() || !(screen instanceof InventoryScreen) || !shift || button != 0) {
			return false;
		}
		Integer slot = hovered(screen);
		if (slot == null) {
			return false;
		}
		Integer partner = partner(slot);
		if (partner == null) {
			return false;
		}
		int hotbarIndex = isHotbar(slot) ? slot - 36 : partner - 36;
		int inventorySlot = isHotbar(slot) ? partner : slot;
		Minecraft mc = Minecraft.getInstance();
		mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, inventorySlot, hotbarIndex, ContainerInput.SWAP, mc.player);
		return true;
	}

	private static Integer partner(int slot) {
		Map<Integer, Integer> binds = settings().bindings;
		Integer partner = binds.get(slot);
		if (partner != null) {
			return partner;
		}
		for (Map.Entry<Integer, Integer> e : binds.entrySet()) {
			if (e.getValue() == slot) {
				return e.getKey();
			}
		}
		return null;
	}

	private static void bindSlot(int slotId) {
		Map<Integer, Integer> binds = settings().bindings;
		Integer currentPrev = previousSlot;
		if (currentPrev != null) {
			previousSlot = null;
			if (currentPrev == slotId) {
				return;
			}
			boolean firstIsHotbar = isHotbar(currentPrev);
			boolean secondIsHotbar = isHotbar(slotId);
			if (firstIsHotbar != secondIsHotbar) {
				int inventory = firstIsHotbar ? slotId : currentPrev;
				int hotbar = firstIsHotbar ? currentPrev : slotId;
				binds.put(inventory, hotbar);
				saved();
			}
		} else if (partner(slotId) != null) {
			if (binds.containsKey(slotId)) {
				binds.remove(slotId);
			} else {
				binds.entrySet().removeIf(e -> e.getValue() == slotId);
			}
			saved();
		} else {
			previousSlot = slotId;
		}
	}

	private static void saved() {
		NebulonClient.get().configManager().markDirty();
		NebulonClient.get().configManager().save();
	}

	/** Draws the bindings over the inventory (after the slots and items). */
	public static void draw(GuiGraphicsExtractor g, int mouseX, int mouseY, AbstractContainerScreen<?> screen) {
		if (!enabled() || !(screen instanceof InventoryScreen)) {
			return;
		}
		if (previousScreen != screen) {
			previousSlot = null;
			previousScreen = screen;
		}
		SlotBindingSettings s = settings();
		Integer hovered = hovered(screen);
		// Lines use the border colour; bound slots are always shown.
		int borderColor = 0xFF000000 | s.borderColor.currentRgb(0L);
		int lineColor = borderColor;
		for (Map.Entry<Integer, Integer> e : s.bindings.entrySet()) {
			int inv = e.getKey();
			int hb = e.getValue();
			if (s.hoverOnly && (hovered == null || (hovered != inv && hovered != hb))) {
				continue;
			}
			int[] p1 = slotPos(screen, inv);
			int[] p2 = slotPos(screen, hb);
			if (p1 == null || p2 == null) {
				continue;
			}
			if (s.drawLine) {
				line(g, p1[0] + 8, p1[1] + 8, p2[0] + 8, p2[1] + 8, lineColor);
			}
			if (s.drawBorder) {
				border(g, p1[0], p1[1], borderColor);
				border(g, p2[0], p2[1], borderColor);
			}
		}
		if (previousSlot != null) {
			int[] p = slotPos(screen, previousSlot);
			if (p != null) {
				line(g, p[0] + 8, p[1] + 8, mouseX, mouseY, lineColor);
			}
		}
	}

	private static int[] slotPos(AbstractContainerScreen<?> screen, int index) {
		if (index < 0 || index >= screen.getMenu().slots.size()) {
			return null;
		}
		Slot slot = screen.getMenu().slots.get(index);
		AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) screen;
		return new int[]{acc.nebulon$leftPos() + slot.x, acc.nebulon$topPos() + slot.y};
	}

	/** 1-unit line between two points (a rotated, stretched fill, like NoammAddons' drawLine). */
	private static void line(GuiGraphicsExtractor g, float x1, float y1, float x2, float y2, int color) {
		float dx = x2 - x1;
		float dy = y2 - y1;
		float length = (float) Math.sqrt(dx * dx + dy * dy);
		if (length < 0.01F) {
			return;
		}
		g.pose().pushMatrix();
		g.pose().translate(x1, y1);
		g.pose().rotate((float) Math.atan2(dy, dx));
		g.pose().translate(0.0F, -0.5F);
		g.pose().scale(length, 1.0F);
		g.fill(0, 0, 1, 1, color);
		g.pose().popMatrix();
	}

	private static void border(GuiGraphicsExtractor g, int x, int y, int color) {
		g.fill(x, y, x + 16, y + 1, color);
		g.fill(x, y + 15, x + 16, y + 16, color);
		g.fill(x, y + 1, x + 1, y + 15, color);
		g.fill(x + 15, y + 1, x + 16, y + 15, color);
	}

	/** For the client game tests. */
	public static Integer pendingSlotForTest() {
		return previousSlot;
	}
}
