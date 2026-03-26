package xyz.nucleoid.disguiselib.impl;

import net.minecraft.world.entity.Pose;

public final class PlayerDisguiseSneakPolicy {
	private PlayerDisguiseSneakPolicy() {
	}

	public static SneakState resolve(boolean enabled, boolean sourceIsPlayer, boolean sneaking, Pose pose) {
		if (!sourceIsPlayer) {
			return new SneakState(sneaking, pose);
		}

		if (enabled) {
			return new SneakState(sneaking, pose);
		}

		Pose resolvedPose = pose == Pose.CROUCHING ? Pose.STANDING : pose;
		return new SneakState(false, resolvedPose);
	}

	public record SneakState(boolean sneaking, Pose pose) {
	}
}
