package xyz.nucleoid.disguiselib.impl;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class DisguiseProjectileHitResolverTest {
	@Test
	void returnsDisguiseHitWhenVanillaMisses() {
		Vec3d start = new Vec3d(0.0, 0.0, 0.0);
		Vec3d end = new Vec3d(10.0, 0.0, 0.0);

		DisguiseProjectileHitResolver.CandidateHit<String> resolved = DisguiseProjectileHitResolver.resolveHit(
				start,
				end,
				null,
				Set.of("ghast"),
				List.of(new DisguiseProjectileHitResolver.CandidateBounds<>("ghast",
						new Box(4.0, -1.0, -1.0, 6.0, 1.0, 1.0))));

		assertNotNull(resolved);
		assertEquals("ghast", resolved.target());
		assertEquals(new Vec3d(4.0, 0.0, 0.0), resolved.hitPos());
	}

	@Test
	void removesVanillaHitWhenDisguisedTargetIsOutsideDisguiseBounds() {
		Vec3d start = new Vec3d(-2.0, 0.6, 0.0);
		Vec3d end = new Vec3d(2.0, 0.6, 0.0);
		DisguiseProjectileHitResolver.CandidateHit<String> vanillaHit = new DisguiseProjectileHitResolver.CandidateHit<>(
				"small",
				new Vec3d(0.9, 0.6, 0.0),
				start.squaredDistanceTo(0.9, 0.6, 0.0));

		DisguiseProjectileHitResolver.CandidateHit<String> resolved = DisguiseProjectileHitResolver.resolveHit(
				start,
				end,
				vanillaHit,
				Set.of("small"),
				List.of(new DisguiseProjectileHitResolver.CandidateBounds<>("small",
						new Box(-0.25, -0.5, -0.25, 0.25, 0.5, 0.25))));

		assertNull(resolved);
	}

	@Test
	void prefersCloserDisguiseHitOverFartherVanillaHit() {
		Vec3d start = new Vec3d(0.0, 0.0, 0.0);
		Vec3d end = new Vec3d(10.0, 0.0, 0.0);
		DisguiseProjectileHitResolver.CandidateHit<String> vanillaHit = new DisguiseProjectileHitResolver.CandidateHit<>(
				"real-target",
				new Vec3d(8.0, 0.0, 0.0),
				start.squaredDistanceTo(8.0, 0.0, 0.0));

		DisguiseProjectileHitResolver.CandidateHit<String> resolved = DisguiseProjectileHitResolver.resolveHit(
				start,
				end,
				vanillaHit,
				Set.of("ghast"),
				List.of(new DisguiseProjectileHitResolver.CandidateBounds<>("ghast",
						new Box(4.0, -1.0, -1.0, 6.0, 1.0, 1.0))));

		assertNotNull(resolved);
		assertEquals("ghast", resolved.target());
		assertEquals(new Vec3d(4.0, 0.0, 0.0), resolved.hitPos());
	}

	@Test
	void keepsVanillaHitWhenDisguiseBoundsAreFartherAway() {
		Vec3d start = new Vec3d(0.0, 0.0, 0.0);
		Vec3d end = new Vec3d(10.0, 0.0, 0.0);
		DisguiseProjectileHitResolver.CandidateHit<String> vanillaHit = new DisguiseProjectileHitResolver.CandidateHit<>(
				"real-target",
				new Vec3d(3.0, 0.0, 0.0),
				start.squaredDistanceTo(3.0, 0.0, 0.0));

		DisguiseProjectileHitResolver.CandidateHit<String> resolved = DisguiseProjectileHitResolver.resolveHit(
				start,
				end,
				vanillaHit,
				Set.of("ghast"),
				List.of(new DisguiseProjectileHitResolver.CandidateBounds<>("ghast",
						new Box(6.0, -1.0, -1.0, 8.0, 1.0, 1.0))));

		assertNotNull(resolved);
		assertEquals("real-target", resolved.target());
		assertEquals(new Vec3d(3.0, 0.0, 0.0), resolved.hitPos());
	}

	@Test
	void selectsClosestDisguiseHitAmongMultipleCandidates() {
		Vec3d start = new Vec3d(0.0, 0.0, 0.0);
		Vec3d end = new Vec3d(10.0, 0.0, 0.0);

		DisguiseProjectileHitResolver.CandidateHit<String> resolved = DisguiseProjectileHitResolver.resolveHit(
				start,
				end,
				null,
				Set.of("first", "second"),
				List.of(
						new DisguiseProjectileHitResolver.CandidateBounds<>("first",
								new Box(6.0, -1.0, -1.0, 8.0, 1.0, 1.0)),
						new DisguiseProjectileHitResolver.CandidateBounds<>("second",
								new Box(2.0, -1.0, -1.0, 4.0, 1.0, 1.0))));

		assertNotNull(resolved);
		assertEquals("second", resolved.target());
		assertEquals(new Vec3d(2.0, 0.0, 0.0), resolved.hitPos());
	}
}
