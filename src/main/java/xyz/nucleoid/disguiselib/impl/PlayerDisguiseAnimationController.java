package xyz.nucleoid.disguiselib.impl;

import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.disguiselib.api.DisguiseUtils;
import xyz.nucleoid.disguiselib.api.EntityDisguise;

import java.util.UUID;

public final class PlayerDisguiseAnimationController {
	private static final PlayerDisguiseAnimationState STATE = new PlayerDisguiseAnimationState();

	private PlayerDisguiseAnimationController() {
	}

	public static void start(Entity entity, PlayerDisguiseAnimationType type, int ticks) {
		start(entity, type, ticks, null);
	}

	public static void start(Entity entity, PlayerDisguiseAnimationType type, int ticks, @Nullable Entity animationTarget) {
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

		STATE.start(player.getUuid(), type, ticks, animationTarget != null ? animationTarget.getId() : -1);
		sendStatus(player, type, true);
		refresh(player);
	}

	public static boolean isActive(Entity entity, PlayerDisguiseAnimationType type) {
		if (!(entity instanceof ServerPlayerEntity player)) {
			return false;
		}

		return STATE.isActive(player.getUuid(), type);
	}

	public static int getTargetEntityId(Entity entity) {
		if (!(entity instanceof ServerPlayerEntity player)) {
			return -1;
		}

		return STATE.getTargetEntityId(player.getUuid());
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
		for (PlayerDisguiseAnimationState.ExpiredAnimation expired : STATE.tick()) {
			ServerPlayerEntity player = server.getPlayerManager().getPlayer(expired.playerId());
			if (player == null || !((EntityDisguise) player).isDisguised()) {
				continue;
			}

			sendStatus(player, expired.type(), false);
			refresh(player);
		}
	}

	private static void sendStatus(ServerPlayerEntity player, @Nullable PlayerDisguiseAnimationType type, boolean starting) {
		if (type == PlayerDisguiseAnimationType.GOAT_RAM) {
			DisguiseSync.sendDisguiseStatus(player, starting ? (byte) 58 : (byte) 59);
		} else if (starting && type == PlayerDisguiseAnimationType.RAVAGER_ATTACK) {
			DisguiseSync.sendDisguiseStatus(player, (byte) 4);
		}
	}

	private static void refresh(ServerPlayerEntity player) {
		((DisguiseUtils) player).updateTrackedData();
		DisguiseSync.refreshAnimationMetadata(player);
	}
}
