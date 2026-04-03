package xyz.nucleoid.disguiselib.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IronGolemDisguiseHealthPolicyTest {
	@Test
	void keepsFullHealthWhenSourceIsFull() {
		assertEquals(100.0f, IronGolemDisguiseHealthPolicy.resolve(20.0f, 20.0f, 100.0f));
	}

	@Test
	void scalesDisguiseHealthBySourceHealthRatio() {
		assertEquals(37.5f, IronGolemDisguiseHealthPolicy.resolve(7.5f, 20.0f, 100.0f));
	}

	@Test
	void clampsBelowZeroAndAboveMax() {
		assertEquals(0.0f, IronGolemDisguiseHealthPolicy.resolve(-1.0f, 20.0f, 100.0f));
		assertEquals(100.0f, IronGolemDisguiseHealthPolicy.resolve(30.0f, 20.0f, 100.0f));
	}

	@Test
	void fallsBackToFullDisguiseHealthWhenSourceMaxIsInvalid() {
		assertEquals(100.0f, IronGolemDisguiseHealthPolicy.resolve(5.0f, 0.0f, 100.0f));
	}
}
