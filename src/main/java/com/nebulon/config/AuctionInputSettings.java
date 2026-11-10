package com.nebulon.config;

/** "Auction Input" (Render): NoammAddons' Auction Price Input - a price box instead of the auction sign. */
public class AuctionInputSettings {
	public enum Mode {
		NORMAL, UNDERCUT
	}

	public boolean enabled = false;

	/** "Use Mod Font": Roboto (the mod's font) instead of Minecraft's / the texture pack's font. */

	public boolean useModFont = true;
	/** "Default Mode": the input mode used when the price box opens. */
	public Mode defaultMode = Mode.NORMAL;
	/** "Remember Text": the last typed price is restored when the box opens again. */
	public boolean rememberText = false;
	/** "Remember Mode": the last used mode is restored instead of the default mode. */
	public boolean rememberMode = false;

	public void validate(java.util.List<String> problems) {
		if (defaultMode == null) {
			problems.add("auctionInput.defaultMode invalid -> NORMAL");
			defaultMode = Mode.NORMAL;
		}
	}
}
