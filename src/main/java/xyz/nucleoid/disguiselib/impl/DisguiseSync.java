package xyz.nucleoid.disguiselib.impl;

import net.minecraft.entity.Entity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import xyz.nucleoid.disguiselib.api.DisguiseUtils;
import xyz.nucleoid.disguiselib.api.EntityDisguise;
import xyz.nucleoid.disguiselib.impl.mixin.accessor.EntityAccessor;
import xyz.nucleoid.disguiselib.impl.mixin.accessor.ServerChunkLoadingManagerAccessor;
import xyz.nucleoid.disguiselib.impl.packets.ExtendedHandler;

import java.util.List;

public final class DisguiseSync {
	private DisguiseSync() {
	}

	public static void refreshTracking(Entity entity) {
		if (!(((EntityAccessor) entity).getWorld() instanceof ServerWorld serverWorld)) {
			return;
		}

		var chunkLoadingManager = serverWorld.getChunkManager().chunkLoadingManager;
		if (chunkLoadingManager == null) {
			return;
		}

		var trackers = ((ServerChunkLoadingManagerAccessor) chunkLoadingManager).getEntityTrackers();
		if (trackers == null) {
			return;
		}

		var tracker = trackers.get(entity.getId());
		if (tracker == null) {
			return;
		}

		for (var listener : tracker.getListeners()) {
			tracker.getEntry().stopTracking(listener.getPlayer());
			tracker.getEntry().startTracking(listener.getPlayer());
		}
	}

	public static void refreshAnimationMetadata(Entity entity) {
		if (!(((EntityAccessor) entity).getWorld() instanceof ServerWorld serverWorld)) {
			return;
		}

		var chunkLoadingManager = serverWorld.getChunkManager().chunkLoadingManager;
		if (chunkLoadingManager == null) {
			return;
		}

		var trackers = ((ServerChunkLoadingManagerAccessor) chunkLoadingManager).getEntityTrackers();
		if (trackers == null) {
			return;
		}

		var tracker = trackers.get(entity.getId());
		if (tracker == null) {
			return;
		}

		Entity disguiseEntity = ((EntityDisguise) entity).getDisguiseEntity();
		if (disguiseEntity == null) {
			return;
		}

		List<DataTracker.SerializedEntry<?>> entries = getAnimationMetadataEntries(disguiseEntity.getDataTracker());
		if (entries.isEmpty()) {
			return;
		}

		sendAnimationMetadataRefreshToPlayers(
				tracker.getListeners().stream().map(listener -> listener.getPlayer()).toList(),
				entity.getId(),
				entries);
	}

	public static void sendDisguiseStatus(Entity entity, byte status) {
		if (!(((EntityAccessor) entity).getWorld() instanceof ServerWorld serverWorld)) {
			return;
		}

		var chunkLoadingManager = serverWorld.getChunkManager().chunkLoadingManager;
		if (chunkLoadingManager == null) {
			return;
		}

		var trackers = ((ServerChunkLoadingManagerAccessor) chunkLoadingManager).getEntityTrackers();
		if (trackers == null) {
			return;
		}

		var tracker = trackers.get(entity.getId());
		if (tracker == null) {
			return;
		}

		var packet = new EntityStatusS2CPacket(entity, status);
		for (var listener : tracker.getListeners()) {
			ServerPlayerEntity player = listener.getPlayer();
			if (player.getId() == entity.getId()) {
				continue;
			}

			player.networkHandler.sendPacket(packet);
		}
	}

	public static void refreshDisguisedPlayers(MinecraftServer server) {
		var disguisedIds = DisguiseTracker.getDisguisedEntityIds();
		for (ServerWorld world : server.getWorlds()) {
			for (int entityId : disguisedIds) {
				Entity entity = world.getEntityById(entityId);
				if (!(entity instanceof ServerPlayerEntity player)) {
					continue;
				}
				if (!((EntityDisguise) player).isDisguised()) {
					continue;
				}

				((DisguiseUtils) player).updateTrackedData();
				refreshTracking(player);
			}
		}
	}

	static EntityTrackerUpdateS2CPacket createAnimationMetadataRefreshPacket(int entityId,
			List<DataTracker.SerializedEntry<?>> entries) {
		return new EntityTrackerUpdateS2CPacket(entityId, entries);
	}

	static int sendAnimationMetadataRefreshToPlayers(Iterable<ServerPlayerEntity> players, int entityId,
			List<DataTracker.SerializedEntry<?>> entries) {
		int sent = 0;
		for (ServerPlayerEntity player : players) {
			if (player.getId() == entityId) {
				continue;
			}
			if (((EntityDisguise) player).hasTrueSight()) {
				continue;
			}

			var packet = createAnimationMetadataRefreshPacket(entityId, entries);
			((ExtendedHandler) player.networkHandler).disguiselib$sendPacketWithoutTransform(packet);
			sent++;
		}
		return sent;
	}

	static List<DataTracker.SerializedEntry<?>> getAnimationMetadataEntries(DataTracker dataTracker) {
		var dirtyEntries = dataTracker.getDirtyEntries();
		if (dirtyEntries != null && !dirtyEntries.isEmpty()) {
			return dirtyEntries;
		}

		var changedEntries = dataTracker.getChangedEntries();
		return changedEntries != null ? changedEntries : List.of();
	}
}
