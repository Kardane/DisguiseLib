package xyz.nucleoid.disguiselib.impl;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerDisguiseAnimationStateTest {
	@Test
	void manualAnimationStartReplaceAndExpire() {
		PlayerDisguiseAnimationState state = new PlayerDisguiseAnimationState();
		UUID playerId = UUID.randomUUID();

		state.start(playerId, PlayerDisguiseAnimationType.GHAST_CHARGE, 3);
		assertTrue(state.isActive(playerId, PlayerDisguiseAnimationType.GHAST_CHARGE));

		state.start(playerId, PlayerDisguiseAnimationType.EVOKER_CAST, 2);
		assertFalse(state.isActive(playerId, PlayerDisguiseAnimationType.GHAST_CHARGE));
		assertTrue(state.isActive(playerId, PlayerDisguiseAnimationType.EVOKER_CAST));

		assertTrue(state.tick().isEmpty());
		assertEquals(Set.of(playerId), state.tick());
		assertFalse(state.isActive(playerId, PlayerDisguiseAnimationType.EVOKER_CAST));
	}

	@Test
	void vindicatorAttackStartsAndExpires() {
		PlayerDisguiseAnimationState state = new PlayerDisguiseAnimationState();
		UUID playerId = UUID.randomUUID();

		state.startVindicatorAttack(playerId, 2);
		assertTrue(state.isVindicatorAttacking(playerId));
		assertTrue(state.tick().isEmpty());
		assertEquals(Set.of(playerId), state.tick());
		assertFalse(state.isVindicatorAttacking(playerId));
	}

	@Test
	void clearRemovesAllState() {
		PlayerDisguiseAnimationState state = new PlayerDisguiseAnimationState();
		UUID playerId = UUID.randomUUID();

		state.start(playerId, PlayerDisguiseAnimationType.ILLUSIONER_CAST, 5);
		state.startVindicatorAttack(playerId, 5);
		state.clear(playerId);

		assertFalse(state.isActive(playerId, PlayerDisguiseAnimationType.ILLUSIONER_CAST));
		assertFalse(state.isVindicatorAttacking(playerId));
		assertTrue(state.tick().isEmpty());
	}
}
