package com.nebulon.render;

import com.nebulon.client.NebulonClient;
import com.nebulon.config.HudTweaksSettings;

/** "HUD Tweaks" (Render): which screen effects are toned down (hooks in the mixins). */
public final class HudTweaks {
	private HudTweaks() {
	}

	private static HudTweaksSettings settings() {
		NebulonClient client = NebulonClient.get();
		if (client == null) {
			return null;
		}
		HudTweaksSettings s = client.configManager().get().hudTweaks;
		return s.enabled ? s : null;
	}

	public static boolean lowFire() {
		HudTweaksSettings s = settings();
		return s != null && s.lowFire;
	}

	public static boolean noDamageTilt() {
		HudTweaksSettings s = settings();
		return s != null && s.noDamageTilt;
	}

	public static boolean noNausea() {
		HudTweaksSettings s = settings();
		return s != null && s.noNausea;
	}

}
