package xyz.nucleoid.disguiselib.impl;

import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerDisguiseNameplatePolicyTest {
	@Test
	void enabledPlayerUsesDisplayNameAndShowsNameplate() {
		Text displayName = Text.literal("ParkJ");
		Text customName = Text.literal("hidden");

		PlayerDisguiseNameplatePolicy.NameplateState state = PlayerDisguiseNameplatePolicy.resolve(true, true,
				false, displayName, customName, false);

		assertEquals("ParkJ", state.customName().getString());
		assertTrue(state.visible());
	}

	@Test
	void excludedDisguiseKeepsOriginalNameplateState() {
		Text customName = Text.literal("display");

		PlayerDisguiseNameplatePolicy.NameplateState state = PlayerDisguiseNameplatePolicy.resolve(true, true,
				true, Text.literal("ParkJ"), customName, false);

		assertEquals("display", state.customName().getString());
		assertEquals(false, state.visible());
	}

	@Test
	void disabledOptionKeepsOriginalNameplateState() {
		Text customName = Text.literal("custom");

		PlayerDisguiseNameplatePolicy.NameplateState state = PlayerDisguiseNameplatePolicy.resolve(false, true,
				false, Text.literal("ParkJ"), customName, false);

		assertEquals("custom", state.customName().getString());
		assertEquals(false, state.visible());
	}

	@Test
	void missingOriginalNameKeepsNameNull() {
		PlayerDisguiseNameplatePolicy.NameplateState state = PlayerDisguiseNameplatePolicy.resolve(false, false,
				false, Text.literal("ParkJ"), null, false);

		assertNull(state.customName());
		assertEquals(false, state.visible());
	}
}
