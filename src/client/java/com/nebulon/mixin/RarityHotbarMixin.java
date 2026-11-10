package com.nebulon.mixin;

import com.nebulon.gui.RarityBackground;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Rarity (Render): the rarity circle behind items in the hotbar (and the offhand slot next to it). */
@Mixin(Hud.class)
abstract class RarityHotbarMixin {
	@Inject(method = "extractSlot", at = @At("HEAD"))
	private void nebulon$rarityBackground(GuiGraphicsExtractor graphics, int x, int y, DeltaTracker deltaTracker, Player player,
			ItemStack itemStack, int seed, CallbackInfo ci) {
		RarityBackground.draw(graphics, itemStack, x, y);
	}

	/** Item Tweaks > Show Ultimate Enchant: the tag over the hotbar item. */
	@Inject(method = "extractSlot", at = @At("TAIL"))
	private void nebulon$ultimateEnchant(GuiGraphicsExtractor graphics, int x, int y, DeltaTracker deltaTracker, Player player,
			ItemStack itemStack, int seed, CallbackInfo ci) {
		com.nebulon.item.UltimateEnchant.draw(graphics, itemStack, x, y, true);
	}
}
