package xyz.nucleoid.disguiselib.impl;

import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import xyz.nucleoid.disguiselib.api.DisguiseUtils;
import xyz.nucleoid.disguiselib.api.EntityDisguise;
import xyz.nucleoid.disguiselib.impl.mixin.accessor.EntityAccessor;
import xyz.nucleoid.disguiselib.impl.mixin.accessor.ServerChunkLoadingManagerAccessor;

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
}
