package xyz.nucleoid.disguiselib.impl;

import net.minecraft.entity.EntityPose;

public final class DisguiseSwimmingPolicy {
	private DisguiseSwimmingPolicy() {
	}

	public static SwimState resolve(boolean sourceSwimming, EntityPose sourcePose, boolean disguiseIsDrowned) {
		if (disguiseIsDrowned) {
			return new SwimState(sourceSwimming, sourcePose);
		}

		EntityPose resolvedPose = sourcePose == EntityPose.SWIMMING ? EntityPose.STANDING : sourcePose;
		return new SwimState(false, resolvedPose);
	}

	public record SwimState(boolean swimming, EntityPose pose) {
	}
}
