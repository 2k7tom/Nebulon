package com.nebulon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared constants. Nebulon is a purely client-side mod: there is no common/server entrypoint,
 * no networking and no server component. The only entrypoint is
 * {@code com.nebulon.client.NebulonClient}.
 */
public final class Nebulon {
	public static final String MOD_ID = "nebulon";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private Nebulon() {
	}
}
