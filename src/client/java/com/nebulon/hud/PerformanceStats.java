package com.nebulon.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.util.Util;

import java.util.ArrayDeque;

/**
 * Values of the Performance Display, all measured on the client from data it receives anyway:
 * <ul>
 *     <li>FPS: Minecraft's own counter.</li>
 *     <li>TPS: how fast the server's game time advances between its time updates (sent about once
 *     per second), smoothed and capped at 20.</li>
 *     <li>Ping: the latency the server reports for you in the tab list.</li>
 *     <li>CPS: left and right mouse clicks in the last second (in game, no screen open).</li>
 * </ul>
 * Nothing is sent to the server.
 */
public final class PerformanceStats {
	private static final long CPS_WINDOW_MS = 1000L;
	private static final long MIN_TPS_SAMPLE_MS = 500L;

	private static final ArrayDeque<Long> leftClicks = new ArrayDeque<>();
	private static final ArrayDeque<Long> rightClicks = new ArrayDeque<>();
	private static long lastGameTime = Long.MIN_VALUE;
	private static long lastTimeMillis;
	private static double tps = -1.0;

	private PerformanceStats() {
	}

	/** Mouse button pressed in game (0 = left, 1 = right). */
	public static void onClick(int button) {
		long now = Util.getMillis();
		if (button == 0) {
			leftClicks.addLast(now);
		} else if (button == 1) {
			rightClicks.addLast(now);
		}
	}

	public static int leftCps() {
		return count(leftClicks);
	}

	public static int rightCps() {
		return count(rightClicks);
	}

	private static int count(ArrayDeque<Long> clicks) {
		long cutoff = Util.getMillis() - CPS_WINDOW_MS;
		while (!clicks.isEmpty() && clicks.peekFirst() < cutoff) {
			clicks.removeFirst();
		}
		return clicks.size();
	}

	/** Server time update received (game time in ticks). */
	public static void onServerTime(long gameTime) {
		long now = Util.getMillis();
		if (lastGameTime == Long.MIN_VALUE || gameTime < lastGameTime) {
			lastGameTime = gameTime;
			lastTimeMillis = now;
			return;
		}
		long elapsed = now - lastTimeMillis;
		if (elapsed < MIN_TPS_SAMPLE_MS) {
			return;
		}
		double sample = Math.min(20.0, (gameTime - lastGameTime) * 1000.0 / elapsed);
		tps = tps < 0 ? sample : tps * 0.5 + sample * 0.5;
		lastGameTime = gameTime;
		lastTimeMillis = now;
	}

	/** Forget the TPS history (new world or server). */
	public static void reset() {
		lastGameTime = Long.MIN_VALUE;
		tps = -1.0;
	}

	/** @return the measured TPS, or -1 while there is no measurement yet */
	public static double tps() {
		return tps;
	}

	/** @return your latency in milliseconds as reported by the server, or -1 */
	public static int ping(Minecraft minecraft) {
		ClientPacketListener connection = minecraft.getConnection();
		if (connection == null || minecraft.player == null) {
			return -1;
		}
		PlayerInfo info = connection.getPlayerInfo(minecraft.player.getUUID());
		return info == null ? -1 : info.getLatency();
	}
}
