package com.nebulon.config;

/** Saved position and collapsed state of a category panel in the settings screen (design units). */
public class PanelPosition {
	public int x;
	public int y;
	/** Collapsed with a right-click on the category name: only the header is shown. */
	public boolean collapsed;

	public PanelPosition() {
	}

	public PanelPosition(int x, int y, boolean collapsed) {
		this.x = x;
		this.y = y;
		this.collapsed = collapsed;
	}
}
