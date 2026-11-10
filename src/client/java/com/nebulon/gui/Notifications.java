package com.nebulon.gui;

import com.nebulon.Nebulon;
import com.nebulon.client.NebulonClient;
import com.nebulon.config.NebulonConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Small notifications in the bottom right corner (like NoammAddons' NotificationManager), in the
 * menu style: a title in colour and a message, faded out after their time. Drawn on the HUD and
 * above open screens (container menus), so they are seen where the action happened.
 */
public final class Notifications {
	public static final int GREEN = 0xFF55FF55;
	public static final int RED = 0xFFFF5555;
	public static final int WHITE = 0xFFFFFFFF;

	private static final int PAD_X = 7;
	private static final int PAD_Y = 5;
	private static final int MARGIN = 6;
	private static final int GAP = 4;
	private static final long FADE_MS = 250L;

	private record Entry(String title, String message, int titleColor, long start, long duration) {
	}

	private static final List<Entry> ENTRIES = new ArrayList<>();

	private Notifications() {
	}

	public static void register() {
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(Nebulon.MOD_ID, "notifications"), (g, delta) -> {
			if (Minecraft.getInstance().gui.screen() == null) {
				draw(g);
			}
		});
	}

	public static void push(String title, String message, int titleColor, long durationMs) {
		ENTRIES.add(new Entry(title, message, titleColor, Util.getMillis(), durationMs));
		while (ENTRIES.size() > 5) {
			ENTRIES.removeFirst();
		}
	}

	/** For the client game tests. */
	public static List<String> activeTitles() {
		ENTRIES.removeIf(e -> Util.getMillis() - e.start() > e.duration());
		return ENTRIES.stream().map(Entry::title).toList();
	}

	/** Draws the active notifications (HUD, or after an open screen). */
	public static void draw(GuiGraphicsExtractor g) {
		long now = Util.getMillis();
		ENTRIES.removeIf(e -> now - e.start() > e.duration());
		if (ENTRIES.isEmpty()) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		NebulonConfig config = NebulonClient.get().configManager().get();
		Theme.apply(config.menuColor.rgb());
		int scale = Canvas.scaleFor(mc.getWindow());
		int guiScale = mc.getWindow().getGuiScale();
		g.nextStratum();
		g.pose().pushMatrix();
		g.pose().scale(1.0F / guiScale, 1.0F / guiScale);
		Canvas c = new Canvas(g, mc.font, scale);
		double right = mc.getWindow().getWidth() / (double) scale - MARGIN;
		double bottom = mc.getWindow().getHeight() / (double) scale - MARGIN;
		for (int i = ENTRIES.size() - 1; i >= 0; i--) {
			Entry e = ENTRIES.get(i);
			long age = now - e.start();
			float alpha = Math.min(1.0F, Math.min(age, e.duration() - age) / (float) FADE_MS);
			int width = Math.max(c.width(e.title(), true), c.width(e.message(), false)) + PAD_X * 2;
			int height = PAD_Y * 2 + 9 + 2 + 9;
			// Slides in from the right while fading in.
			double x = right - width + (1.0F - alpha) * 12;
			double y = bottom - height;
			c.roundRect(x, y, x + width, y + height, 4, fade(Theme.PANEL_BG, alpha));
			c.roundRectOutline(x, y, x + width, y + height, 4, fade(Theme.HEADER_BORDER, alpha));
			c.text(e.title(), x + PAD_X, y + PAD_Y, fade(e.titleColor(), alpha), true);
			c.text(e.message(), x + PAD_X, y + PAD_Y + 11, fade(Theme.TEXT_DIM, alpha), false);
			bottom = y - GAP;
		}
		g.pose().popMatrix();
	}

	private static int fade(int argb, float alpha) {
		int a = Math.round((argb >>> 24) * alpha);
		return (Math.max(4, a) << 24) | (argb & 0xFFFFFF);
	}
}
