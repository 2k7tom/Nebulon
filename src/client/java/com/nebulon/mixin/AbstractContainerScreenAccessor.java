package com.nebulon.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read access to the position of a container screen's texture. */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
	@Accessor("leftPos")
	int nebulon$leftPos();

	@Accessor("topPos")
	int nebulon$topPos();

	@Accessor("hoveredSlot")
	net.minecraft.world.inventory.Slot nebulon$hoveredSlot();
}
