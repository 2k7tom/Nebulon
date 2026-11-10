package com.nebulon.item;

import com.nebulon.client.NebulonClient;
import com.nebulon.config.ItemTweaksSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * Item Tweaks &gt; Compact Stars: Hypixel shows dungeon stars as five gold "✪" and master stars as
 * one red circled digit after them ("✪✪✪✪✪➌"). With Compact Stars the first N gold stars take the
 * colour of that digit and the digit is removed ("✪✪✪" red + "✪✪" gold). Only the displayed name
 * changes, on the client.
 */
public final class CompactStars {
	private static final char STAR = '✪';
	/** ➊ .. ➎ (master stars 1 to 5). */
	private static final char FIRST_DIGIT = '➊';
	private static final char LAST_DIGIT = '➎';

	/** Already converted names (keyed by the original component). */
	private static final Map<Component, Component> CACHE = new WeakHashMap<>();

	private CompactStars() {
	}

	private record Segment(Style style, String text) {
	}

	/** Applies Compact Stars to an item name when the feature is on (client thread only). */
	public static Component apply(Component name) {
		NebulonClient client = NebulonClient.get();
		if (client == null || name == null) {
			return name;
		}
		ItemTweaksSettings s = client.configManager().get().itemTweaks;
		if (!s.enabled || !s.compactStars || !Minecraft.getInstance().isSameThread()) {
			return name;
		}
		Component cached = CACHE.get(name);
		if (cached != null) {
			return cached;
		}
		Component result = convert(name);
		CACHE.put(name, result);
		return result;
	}

	/** The conversion itself (public for tests). */
	public static Component convert(Component name) {
		List<Segment> segments = new ArrayList<>();
		name.visit((style, text) -> {
			segments.add(new Segment(style, text));
			return Optional.empty();
		}, Style.EMPTY);

		// Pass 1: the master star digit and its colour.
		int master = 0;
		TextColor masterColor = TextColor.fromLegacyFormat(ChatFormatting.RED);
		for (Segment segment : segments) {
			TextColor legacy = null;
			String text = segment.text();
			for (int i = 0; i < text.length(); i++) {
				char ch = text.charAt(i);
				if (ch == '§' && i + 1 < text.length()) {
					ChatFormatting format = ChatFormatting.getByCode(text.charAt(++i));
					if (format != null && format.ordinal() <= ChatFormatting.WHITE.ordinal()) {
						legacy = TextColor.fromLegacyFormat(format);
					}
				} else if (ch >= FIRST_DIGIT && ch <= LAST_DIGIT) {
					master = ch - FIRST_DIGIT + 1;
					masterColor = legacy != null ? legacy : segment.style().getColor() != null ? segment.style().getColor() : masterColor;
				}
			}
		}
		if (master == 0) {
			return name;
		}

		// Pass 2: rebuild the name; the first `master` stars in the master colour, digit removed.
		MutableComponent out = Component.empty();
		int stars = 0;
		for (Segment segment : segments) {
			String text = segment.text();
			StringBuilder run = new StringBuilder();
			StringBuilder legacyPrefix = new StringBuilder();
			for (int i = 0; i < text.length(); i++) {
				char ch = text.charAt(i);
				if (ch == '§' && i + 1 < text.length()) {
					char code = text.charAt(i + 1);
					ChatFormatting format = ChatFormatting.getByCode(code);
					if (format != null && (format.ordinal() <= ChatFormatting.WHITE.ordinal() || format == ChatFormatting.RESET)) {
						legacyPrefix.setLength(0);
					}
					legacyPrefix.append('§').append(code);
					run.append('§').append(code);
					i++;
				} else if (ch >= FIRST_DIGIT && ch <= LAST_DIGIT) {
					// removed
				} else if (ch == STAR && stars < master) {
					stars++;
					flush(out, run, segment.style());
					out.append(Component.literal(String.valueOf(STAR)).withStyle(segment.style().withColor(masterColor)));
					// Legacy codes before the star still apply to the text after it.
					run.append(legacyPrefix);
				} else {
					if (ch == STAR) {
						stars++;
					}
					run.append(ch);
				}
			}
			flush(out, run, segment.style());
		}
		return out;
	}

	private static void flush(MutableComponent out, StringBuilder run, Style style) {
		if (!run.isEmpty()) {
			out.append(Component.literal(run.toString()).withStyle(style));
			run.setLength(0);
		}
	}

	/** Forget converted names (setting changed). */
	public static void clearCache() {
		CACHE.clear();
	}
}
