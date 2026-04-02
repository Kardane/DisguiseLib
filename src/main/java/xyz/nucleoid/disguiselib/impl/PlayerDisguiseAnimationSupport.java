package xyz.nucleoid.disguiselib.impl;

public final class PlayerDisguiseAnimationSupport {
	private PlayerDisguiseAnimationSupport() {
	}

	public static boolean isSkeletonBowAiming(String disguiseTypeId, boolean sourceIsPlayer, boolean usingItem,
			boolean activeItemIsBow, boolean mainHandBow) {
		return sourceIsPlayer
				&& isSkeletonBowType(disguiseTypeId)
				&& usingItem
				&& activeItemIsBow
				&& mainHandBow;
	}

	public static boolean supports(String disguiseTypeId, PlayerDisguiseAnimationType type) {
		if (disguiseTypeId == null) {
			return false;
		}

		return switch (type) {
			case GHAST_CHARGE -> disguiseTypeId.equals("minecraft:ghast");
			case EVOKER_CAST -> disguiseTypeId.equals("minecraft:evoker");
			case ILLUSIONER_CAST -> disguiseTypeId.equals("minecraft:illusioner");
		};
	}

	public static byte getSpellId(PlayerDisguiseAnimationType type) {
		return switch (type) {
			case GHAST_CHARGE -> 0;
			case EVOKER_CAST -> 2;
			case ILLUSIONER_CAST -> 4;
		};
	}

	private static boolean isSkeletonBowType(String disguiseTypeId) {
		return disguiseTypeId.equals("minecraft:skeleton")
				|| disguiseTypeId.equals("minecraft:stray")
				|| disguiseTypeId.equals("minecraft:bogged");
	}
}
