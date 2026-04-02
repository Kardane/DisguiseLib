package xyz.nucleoid.disguiselib.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerDisguiseAnimationSupportTest {
	@Test
	void skeletonBowPoseRequiresMainHandBowWhileUsing() {
		assertTrue(PlayerDisguiseAnimationSupport.isSkeletonBowAiming(
				"minecraft:skeleton",
				true,
				true,
				true,
				true));
		assertTrue(PlayerDisguiseAnimationSupport.isSkeletonBowAiming(
				"minecraft:stray",
				true,
				true,
				true,
				true));
		assertTrue(PlayerDisguiseAnimationSupport.isSkeletonBowAiming(
				"minecraft:bogged",
				true,
				true,
				true,
				true));
	}

	@Test
	void skeletonBowPoseRejectsWrongUsageOrType() {
		assertFalse(PlayerDisguiseAnimationSupport.isSkeletonBowAiming(
				"minecraft:skeleton",
				true,
				false,
				true,
				true));
		assertFalse(PlayerDisguiseAnimationSupport.isSkeletonBowAiming(
				"minecraft:skeleton",
				true,
				true,
				false,
				true));
		assertFalse(PlayerDisguiseAnimationSupport.isSkeletonBowAiming(
				"minecraft:skeleton",
				true,
				true,
				true,
				false));
		assertFalse(PlayerDisguiseAnimationSupport.isSkeletonBowAiming(
				"minecraft:wither_skeleton",
				true,
				true,
				true,
				true));
	}

	@Test
	void manualAnimationSupportMatchesDisguiseType() {
		assertTrue(PlayerDisguiseAnimationSupport.supports("minecraft:ghast",
				PlayerDisguiseAnimationType.GHAST_CHARGE));
		assertTrue(PlayerDisguiseAnimationSupport.supports("minecraft:evoker",
				PlayerDisguiseAnimationType.EVOKER_CAST));
		assertTrue(PlayerDisguiseAnimationSupport.supports("minecraft:illusioner",
				PlayerDisguiseAnimationType.ILLUSIONER_CAST));

		assertFalse(PlayerDisguiseAnimationSupport.supports("minecraft:evoker",
				PlayerDisguiseAnimationType.GHAST_CHARGE));
		assertFalse(PlayerDisguiseAnimationSupport.supports("minecraft:ghast",
				PlayerDisguiseAnimationType.EVOKER_CAST));
		assertFalse(PlayerDisguiseAnimationSupport.supports("minecraft:vindicator",
				PlayerDisguiseAnimationType.ILLUSIONER_CAST));
	}
}
