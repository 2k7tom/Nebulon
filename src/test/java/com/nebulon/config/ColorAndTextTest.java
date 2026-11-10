package com.nebulon.config;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColorAndTextTest {
	@Test
	void parsesSupportedColorFormats() {
		assertEquals(0xFF3030, ColorUtil.parseRgb("#FF3030"));
		assertEquals(0xFF3030, ColorUtil.parseRgb("ff3030"));
		assertEquals(0xFF3030, ColorUtil.parseRgb("0xFF3030"));
		assertEquals(0xFF0000, ColorUtil.parseRgb("#f00"));
		assertEquals("#00A0FF", ColorUtil.format(0x00A0FF));
	}

	@Test
	void rejectsInvalidColors() {
		assertEquals(ColorUtil.INVALID, ColorUtil.parseRgb(null));
		assertEquals(ColorUtil.INVALID, ColorUtil.parseRgb(""));
		assertEquals(ColorUtil.INVALID, ColorUtil.parseRgb("#GG0000"));
		assertEquals(ColorUtil.INVALID, ColorUtil.parseRgb("#FF00"));
		assertEquals(ColorUtil.INVALID, ColorUtil.parseRgb("#FF0000FF"));
	}

	@Test
	void hsvRoundTrip() {
		for (int rgb : new int[]{0xFF3030, 0x30A0FF, 0x00FF00, 0x123456, 0xFFFFFF, 0x000000}) {
			float[] hsv = ColorUtil.rgbToHsv(rgb);
			int back = ColorUtil.hsvToRgb(hsv[0], hsv[1], hsv[2]);
			assertEquals(rgb, back, "round trip of " + ColorUtil.format(rgb));
		}
		assertEquals(0xFF0000, ColorUtil.hsvToRgb(0.0F, 1.0F, 1.0F));
		assertEquals(0x00FF00, ColorUtil.hsvToRgb(1.0F / 3.0F, 1.0F, 1.0F));
	}

	@Test
	void chromaCyclesHueAndKeepsBrightness() {
		int base = 0xFF3030;
		int atStart = ColorUtil.chroma(base, 0L);
		int halfway = ColorUtil.chroma(base, ColorUtil.CHROMA_CYCLE_MILLIS / 2);
		assertEquals(atStart, ColorUtil.chroma(base, ColorUtil.CHROMA_CYCLE_MILLIS), "one full cycle returns to the start");
		assertTrue(atStart != halfway, "the colour changes over time");
		assertEquals(ColorUtil.rgbToHsv(base)[2], ColorUtil.rgbToHsv(halfway)[2], 0.01F);
	}

	@Test
	void entityListRoundTrip() {
		List<String> parsed = ConfigText.parseEntityList("zombie, minecraft:husk;skeleton  zombie");
		assertEquals(List.of("minecraft:zombie", "minecraft:husk", "minecraft:skeleton"), parsed);
		assertEquals(parsed, ConfigText.parseEntityList(ConfigText.formatEntityList(parsed)));
		assertEquals(List.of(), ConfigText.parseEntityList("  "));
		assertNull(ConfigText.parseEntityList("zombie, Bad Id!"));
	}
}
