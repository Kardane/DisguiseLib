package xyz.nucleoid.disguiselib.impl;

import net.minecraft.world.entity.Pose;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerDisguiseSneakPolicyTest {
	@Test
	void enabledPlayerKeepsSneakingAndCrouchingPose() {
		PlayerDisguiseSneakPolicy.SneakState state = PlayerDisguiseSneakPolicy.resolve(true, true, true,
				Pose.CROUCHING);

		assertTrue(state.sneaking());
		assertEquals(Pose.CROUCHING, state.pose());
	}

	@Test
	void enabledPlayerClearsSneakWhenStandingUp() {
		PlayerDisguiseSneakPolicy.SneakState state = PlayerDisguiseSneakPolicy.resolve(true, true, false,
				Pose.STANDING);

		assertFalse(state.sneaking());
		assertEquals(Pose.STANDING, state.pose());
	}

	@Test
	void disabledPlayerCrouchForcesStandingState() {
		PlayerDisguiseSneakPolicy.SneakState state = PlayerDisguiseSneakPolicy.resolve(false, true, true,
				Pose.CROUCHING);

		assertFalse(state.sneaking());
		assertEquals(Pose.STANDING, state.pose());
	}

	@Test
	void nonPlayerKeepsOriginalState() {
		PlayerDisguiseSneakPolicy.SneakState state = PlayerDisguiseSneakPolicy.resolve(false, false, true,
				Pose.CROUCHING);

		assertTrue(state.sneaking());
		assertEquals(Pose.CROUCHING, state.pose());
	}
}
