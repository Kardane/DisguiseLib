package xyz.nucleoid.disguiselib.impl;

import net.minecraft.entity.EntityPose;

public final class PlayerDisguiseSneakPolicy {
	private PlayerDisguiseSneakPolicy() {
	}

	public static SneakState resolve(boolean enabled, boolean sourceIsPlayer, boolean sneaking, EntityPose pose) {
		if (!sourceIsPlayer) {
			return new SneakState(sneaking, pose);
		}

		if (enabled) {
			return new SneakState(sneaking, pose);
		}

		EntityPose resolvedPose = pose == EntityPose.CROUCHING ? EntityPose.STANDING : pose;
		return new SneakState(false, resolvedPose);
	}

	public record SneakState(boolean sneaking, EntityPose pose) {
	}
}
