package xyz.nucleoid.disguiselib.impl;

import net.minecraft.entity.EntityPose;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerDisguiseSneakPolicyTest {
	@Test
	void enabledPlayerKeepsSneakingAndCrouchingPose() {
		PlayerDisguiseSneakPolicy.SneakState state = PlayerDisguiseSneakPolicy.resolve(true, true, true,
				EntityPose.CROUCHING);

		assertTrue(state.sneaking());
		assertEquals(EntityPose.CROUCHING, state.pose());
	}

	@Test
	void enabledPlayerClearsSneakWhenStandingUp() {
		PlayerDisguiseSneakPolicy.SneakState state = PlayerDisguiseSneakPolicy.resolve(true, true, false,
				EntityPose.STANDING);

		assertFalse(state.sneaking());
		assertEquals(EntityPose.STANDING, state.pose());
	}

	@Test
	void disabledPlayerCrouchForcesStandingState() {
		PlayerDisguiseSneakPolicy.SneakState state = PlayerDisguiseSneakPolicy.resolve(false, true, true,
				EntityPose.CROUCHING);

		assertFalse(state.sneaking());
		assertEquals(EntityPose.STANDING, state.pose());
	}

	@Test
	void nonPlayerKeepsOriginalState() {
		PlayerDisguiseSneakPolicy.SneakState state = PlayerDisguiseSneakPolicy.resolve(false, false, true,
				EntityPose.CROUCHING);

		assertTrue(state.sneaking());
		assertEquals(EntityPose.CROUCHING, state.pose());
	}
}
