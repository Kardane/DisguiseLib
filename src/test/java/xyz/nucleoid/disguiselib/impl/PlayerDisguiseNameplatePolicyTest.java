package xyz.nucleoid.disguiselib.impl;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerDisguiseNameplatePolicyTest {
	@Test
	void enabledPlayerUsesDisplayNameAndShowsNameplate() {
		Component displayName = Component.literal("ParkJ");
		Component customName = Component.literal("hidden");

		PlayerDisguiseNameplatePolicy.NameplateState state = PlayerDisguiseNameplatePolicy.resolve(true, true,
				false, displayName, customName, false);

		assertEquals("ParkJ", state.customName().getString());
		assertTrue(state.visible());
	}

	@Test
	void excludedDisguiseKeepsOriginalNameplateState() {
		Component customName = Component.literal("display");

		PlayerDisguiseNameplatePolicy.NameplateState state = PlayerDisguiseNameplatePolicy.resolve(true, true,
				true, Component.literal("ParkJ"), customName, false);

		assertEquals("display", state.customName().getString());
		assertEquals(false, state.visible());
	}

	@Test
	void disabledOptionKeepsOriginalNameplateState() {
		Component customName = Component.literal("custom");

		PlayerDisguiseNameplatePolicy.NameplateState state = PlayerDisguiseNameplatePolicy.resolve(false, true,
				false, Component.literal("ParkJ"), customName, false);

		assertEquals("custom", state.customName().getString());
		assertEquals(false, state.visible());
	}

	@Test
	void missingOriginalNameKeepsNameNull() {
		PlayerDisguiseNameplatePolicy.NameplateState state = PlayerDisguiseNameplatePolicy.resolve(false, false,
				false, Component.literal("ParkJ"), null, false);

		assertNull(state.customName());
		assertEquals(false, state.visible());
	}
}
