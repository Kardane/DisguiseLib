package xyz.nucleoid.disguiselib.impl;

import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import xyz.nucleoid.disguiselib.api.DisguiseUtils;
import xyz.nucleoid.disguiselib.api.EntityDisguise;

import java.util.UUID;

public final class PlayerDisguiseAnimationController {
	private static final PlayerDisguiseAnimationState STATE = new PlayerDisguiseAnimationState();

	private PlayerDisguiseAnimationController() {
	}

	public static void start(Entity entity, PlayerDisguiseAnimationType type, int ticks) {
		if (!(entity instanceof ServerPlayerEntity player)) {
			return;
		}

		EntityDisguise disguise = (EntityDisguise) player;
		if (!disguise.isDisguised()
				|| !PlayerDisguiseAnimationSupport.supports(
						Registries.ENTITY_TYPE.getId(disguise.getDisguiseType()).toString(),
						type)) {
			return;
		}

		STATE.start(player.getUuid(), type, ticks);
		refresh(player);
	}

	public static boolean isActive(Entity entity, PlayerDisguiseAnimationType type) {
		if (!(entity instanceof ServerPlayerEntity player)) {
			return false;
		}

		return STATE.isActive(player.getUuid(), type);
	}

	public static void clear(Entity entity) {
		if (!(entity instanceof ServerPlayerEntity player)) {
			return;
		}

		STATE.clear(player.getUuid());
	}

	public static void startVindicatorAttack(Entity entity, int ticks) {
		if (!(entity instanceof ServerPlayerEntity player)) {
			return;
		}

		STATE.startVindicatorAttack(player.getUuid(), ticks);
		refresh(player);
	}

	public static boolean isVindicatorAttacking(Entity entity) {
		if (!(entity instanceof ServerPlayerEntity player)) {
			return false;
		}

		return STATE.isVindicatorAttacking(player.getUuid());
	}

	public static void tick(MinecraftServer server) {
		for (UUID playerId : STATE.tick()) {
			ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
			if (player == null || !((EntityDisguise) player).isDisguised()) {
				continue;
			}

			refresh(player);
		}
	}

	private static void refresh(ServerPlayerEntity player) {
		((DisguiseUtils) player).updateTrackedData();
		DisguiseSync.refreshTracking(player);
	}
}
