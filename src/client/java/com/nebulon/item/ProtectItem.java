package com.nebulon.item;

import com.nebulon.client.NebulonClient;
import com.nebulon.config.ProtectItemSettings;
import com.nebulon.gui.Notifications;
import com.nebulon.render.DarkmodeMist;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

import java.util.Set;

/**
 * "Protect Item" (Settings), ported from NoammAddons 1.3.0 (CC0): prevents dropping or selling
 * important items.
 * <ul>
 *     <li>Protected: items whose SkyBlock UUID was protected with the Protect Key (hovering an item
 *     in a menu) or {@code /protectitem} (held item); optionally all starred and all recombobulated
 *     items.</li>
 *     <li>Blocked: throwing them out of a menu (drop key, clicking outside the menu), any click on
 *     them in a sell menu (shop with "Sell Item" / buyback), and the drop key outside menus (not
 *     during a dungeon run, as in NoammAddons). Quiver arrows can never be clicked away.</li>
 * </ul>
 * Only the player's own client actions are blocked; nothing is sent to the server.
 */
public final class ProtectItem {
	/** Why an item is protected (shown in its tooltip). */
	public enum Type {
		UUID, STARRED, RECOMBOBULATED, NONE, QUIVER
	}

	private ProtectItem() {
	}

	private static ProtectItemSettings settings() {
		return NebulonClient.get().configManager().get().protectItem;
	}

	private static boolean enabled() {
		NebulonClient client = NebulonClient.get();
		return client != null && settings().enabled;
	}

	// ---- Item data ---------------------------------------------------------------------------------

	private static CompoundTag customData(ItemStack stack) {
		return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
	}

	/** The item's SkyBlock UUID, or "". */
	public static String itemUuid(ItemStack stack) {
		return customData(stack).getString("uuid").orElse("");
	}

	public static Type typeOf(ItemStack stack) {
		if (stack.isEmpty()) {
			return Type.NONE;
		}
		ProtectItemSettings s = settings();
		if (s.protectUuid) {
			String uuid = itemUuid(stack);
			if (!uuid.isBlank() && s.uuids.contains(uuid)) {
				return Type.UUID;
			}
		}
		CompoundTag data = customData(stack);
		String name = stack.getHoverName().getString();
		if (s.protectStarred && (data.getIntOr("upgrade_level", 0) > 0 || name.contains("✪"))) {
			return Type.STARRED;
		}
		if (s.protectRecombobulated && data.getIntOr("rarity_upgrades", 0) > 0) {
			return Type.RECOMBOBULATED;
		}
		if (Boolean.parseBoolean(data.getString("quiver_arrow").orElse("false"))) {
			return Type.QUIVER;
		}
		return Type.NONE;
	}

	// ---- Blocking ----------------------------------------------------------------------------------

	private static void blocked() {
		if (settings().protectNotification) {
			Notifications.push("Action Blocked", "This item is protected!", Notifications.WHITE, 1500L);
		}
	}

	/** A click in a container menu; {@code true} = cancel it. */
	public static boolean onContainerClick(AbstractContainerMenu menu, int slotId, ContainerInput input) {
		if (!enabled()) {
			return false;
		}
		ItemStack stack;
		if (slotId == -999) {
			stack = menu.getCarried();
		} else if (slotId >= 0 && slotId < menu.slots.size()) {
			stack = menu.slots.get(slotId).getItem();
		} else {
			return false;
		}
		if (stack.isEmpty()) {
			return false;
		}
		Type type = typeOf(stack);
		if (type == Type.NONE) {
			return false;
		}
		boolean throwing = input == ContainerInput.THROW || slotId == -999;
		boolean selling = slotId >= 0 && isSellMenu(menu);
		if (throwing || selling || type == Type.QUIVER) {
			blocked();
			return true;
		}
		return false;
	}

	/** The drop key outside menus; {@code true} = cancel. Allowed during a dungeon run, as in NoammAddons. */
	public static boolean onDropKey() {
		Minecraft mc = Minecraft.getInstance();
		if (!enabled() || mc.player == null || mc.gui.screen() != null || inDungeon(mc)) {
			return false;
		}
		Type type = typeOf(mc.player.getInventory().getSelectedItem());
		if (type == Type.NONE) {
			return false;
		}
		blocked();
		return true;
	}

	private static boolean inDungeon(Minecraft mc) {
		String area = DarkmodeMist.currentArea(mc);
		return area != null && area.contains("Catacombs");
	}

	/** A shop that buys items: has a "Sell Item" hopper or "Click to buyback" items. */
	private static boolean isSellMenu(AbstractContainerMenu menu) {
		int count = Math.min(54, menu.slots.size());
		for (int i = 0; i < count; i++) {
			ItemStack stack = menu.slots.get(i).getItem();
			if (stack.isEmpty()) {
				continue;
			}
			if (stack.is(Items.HOPPER) && stack.getHoverName().getString().contains("Sell Item")) {
				return true;
			}
			ItemLore lore = stack.get(DataComponents.LORE);
			if (lore != null && lore.lines().stream().anyMatch(line -> line.getString().contains("Click to buyback"))) {
				return true;
			}
		}
		return false;
	}

	// ---- Protecting --------------------------------------------------------------------------------

	/** Protect Key over a slot in a menu; {@code true} if an item was there. */
	public static boolean protectSlot(Slot slot) {
		if (!enabled() || slot == null || slot.getItem().isEmpty()) {
			return false;
		}
		toggle(slot.getItem());
		return true;
	}

	/** {@code /protectitem}: the held item. */
	public static void protectHeld() {
		Minecraft mc = Minecraft.getInstance();
		ItemStack held = mc.player == null ? ItemStack.EMPTY : mc.player.getMainHandItem();
		if (held.isEmpty()) {
			Notifications.push("Error", "You need to be holding an item.", Notifications.RED, 2500L);
			return;
		}
		toggle(held);
	}

	/** Protects the item by its UUID, or removes the protection. */
	public static void toggle(ItemStack stack) {
		Set<String> list = settings().uuids;
		String label = ChatFormatting.stripFormatting(stack.getHoverName().getString());
		String key = itemUuid(stack);
		if (key.isBlank()) {
			Notifications.push("Error", "Item has no protectable UUID.", Notifications.RED, 2500L);
			return;
		}
		if (list.remove(key)) {
			Notifications.push("Protection Removed", "No longer protecting " + label + ".", Notifications.RED, 2500L);
		} else {
			list.add(key);
			Notifications.push("Protection Added", "Now protecting " + label + ".", Notifications.GREEN, 2500L);
		}
		NebulonClient.get().configManager().markDirty();
		NebulonClient.get().configManager().save();
	}

	// ---- Display -----------------------------------------------------------------------------------

	/** Tooltip line "Item Protected (TYPE)" below the name. */
	public static void addTooltip(ItemStack stack, java.util.List<Component> lines) {
		if (!enabled() || stack.isEmpty() || lines.size() < 2) {
			return;
		}
		Type type = typeOf(stack);
		if (type != Type.NONE) {
			lines.add(1, Component.literal("Item Protected ").withStyle(ChatFormatting.GREEN)
					.append(Component.literal("(" + type.name() + ")").withStyle(ChatFormatting.GRAY)));
		}
	}

	/** "Show Protected Items": whether the slot gets the small "P". */
	public static boolean showMarker(ItemStack stack) {
		if (!enabled() || !settings().showProtected || stack.isEmpty()) {
			return false;
		}
		Type type = typeOf(stack);
		return type != Type.NONE && type != Type.QUIVER;
	}
}
