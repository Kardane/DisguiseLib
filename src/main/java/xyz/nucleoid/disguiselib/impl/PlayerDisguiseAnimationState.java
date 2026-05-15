package xyz.nucleoid.disguiselib.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class PlayerDisguiseAnimationState {
	private final Map<UUID, TimedAnimation> manualAnimations = new HashMap<>();
	private final Map<UUID, Integer> vindicatorAttackTicks = new HashMap<>();

	void start(UUID playerId, PlayerDisguiseAnimationType type, int ticks) {
		this.start(playerId, type, ticks, -1);
	}

	void start(UUID playerId, PlayerDisguiseAnimationType type, int ticks, int targetEntityId) {
		this.manualAnimations.put(playerId, new TimedAnimation(type, ticks, targetEntityId));
	}

	boolean isActive(UUID playerId, PlayerDisguiseAnimationType type) {
		TimedAnimation animation = this.manualAnimations.get(playerId);
		return animation != null && animation.type() == type;
	}

	int getTargetEntityId(UUID playerId) {
		TimedAnimation animation = this.manualAnimations.get(playerId);
		return animation != null ? animation.targetEntityId() : -1;
	}

	void startVindicatorAttack(UUID playerId, int ticks) {
		this.vindicatorAttackTicks.put(playerId, ticks);
	}

	boolean isVindicatorAttacking(UUID playerId) {
		Integer ticks = this.vindicatorAttackTicks.get(playerId);
		return ticks != null && ticks > 0;
	}

	void clear(UUID playerId) {
		this.manualAnimations.remove(playerId);
		this.vindicatorAttackTicks.remove(playerId);
	}

	Set<ExpiredAnimation> tick() {
		Set<ExpiredAnimation> refreshTargets = new HashSet<>();
		this.tickManualAnimations(refreshTargets);
		this.tickVindicatorAttacks(refreshTargets);
		return refreshTargets;
	}

	private void tickManualAnimations(Set<ExpiredAnimation> refreshTargets) {
		var iterator = this.manualAnimations.entrySet().iterator();
		while (iterator.hasNext()) {
			var entry = iterator.next();
			int remainingTicks = entry.getValue().remainingTicks() - 1;
			if (remainingTicks <= 0) {
				iterator.remove();
				refreshTargets.add(new ExpiredAnimation(entry.getKey(), entry.getValue().type()));
				continue;
			}

			entry.setValue(new TimedAnimation(entry.getValue().type(), remainingTicks, entry.getValue().targetEntityId()));
		}
	}

	private void tickVindicatorAttacks(Set<ExpiredAnimation> refreshTargets) {
		var iterator = this.vindicatorAttackTicks.entrySet().iterator();
		while (iterator.hasNext()) {
			var entry = iterator.next();
			int remainingTicks = entry.getValue() - 1;
			if (remainingTicks <= 0) {
				iterator.remove();
				refreshTargets.add(new ExpiredAnimation(entry.getKey(), null));
				continue;
			}

			entry.setValue(remainingTicks);
		}
	}

	record ExpiredAnimation(UUID playerId, PlayerDisguiseAnimationType type) {
	}

	private record TimedAnimation(PlayerDisguiseAnimationType type, int remainingTicks, int targetEntityId) {
	}
}
