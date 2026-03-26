package xyz.nucleoid.disguiselib.impl;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class PlayerDisguiseNameplatePolicy {
	private PlayerDisguiseNameplatePolicy() {
	}

	public static NameplateState resolve(boolean enabled, boolean sourceIsPlayer, boolean disguiseTypeExcluded,
			@Nullable Component playerDisplayName, @Nullable Component originalCustomName, boolean originalCustomNameVisible) {
		if (enabled && sourceIsPlayer && !disguiseTypeExcluded && playerDisplayName != null) {
			return new NameplateState(playerDisplayName.copy(), true);
		}

		return new NameplateState(originalCustomName == null ? null : originalCustomName.copy(),
				originalCustomNameVisible);
	}

	public record NameplateState(@Nullable Component customName, boolean visible) {
	}
}
