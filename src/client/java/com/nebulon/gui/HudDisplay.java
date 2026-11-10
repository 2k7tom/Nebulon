package com.nebulon.gui;

import net.minecraft.client.Minecraft;

/**
 * A display on the HUD that the Gui Editor can move and resize (Performance Display, Quiz Timer).
 * Sizes and positions are design units of the menu's fixed resolution (see {@link Canvas}).
 */
interface HudDisplay {
	/** Whether the display's feature is on (off: shown dimmed in the editor, hidden in game). */
	boolean enabled();

	/** Size on screen including the size factor; {0, 0} if there is nothing to show. */
	int[] size(Canvas c, Minecraft minecraft, boolean example);

	/** Top-left corner with the default applied and kept on screen. */
	int[] position(int[] size, int designWidth, int designHeight);

	void setPosition(int x, int y);

	float scale();

	void setScale(float scale);

	/** Draws the display; {@code example} = editor preview with sample values. */
	void render(Canvas c, Minecraft minecraft, double x, double y, boolean highlighted, boolean example);
}
