package xyz.nucleoid.disguiselib.impl;

import net.minecraft.entity.EntityPose;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisguiseSwimmingPolicyTest {
	@Test
	void drownedKeepsSwimmingState() {
		DisguiseSwimmingPolicy.SwimState state = DisguiseSwimmingPolicy.resolve(true, EntityPose.SWIMMING, true);

		assertTrue(state.swimming());
		assertEquals(EntityPose.SWIMMING, state.pose());
	}

	@Test
	void nonDrownedClearsSwimmingState() {
		DisguiseSwimmingPolicy.SwimState state = DisguiseSwimmingPolicy.resolve(true, EntityPose.SWIMMING, false);

		assertFalse(state.swimming());
		assertEquals(EntityPose.STANDING, state.pose());
	}

	@Test
	void nonDrownedKeepsNonSwimmingPose() {
		DisguiseSwimmingPolicy.SwimState state = DisguiseSwimmingPolicy.resolve(true, EntityPose.CROUCHING, false);

		assertFalse(state.swimming());
		assertEquals(EntityPose.CROUCHING, state.pose());
	}
}
