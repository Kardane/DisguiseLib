package xyz.nucleoid.disguiselib.impl;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import xyz.nucleoid.disguiselib.api.DisguiseUtils;
import xyz.nucleoid.disguiselib.api.EntityDisguise;
import xyz.nucleoid.disguiselib.impl.mixin.accessor.ServerChunkLoadingManagerAccessor;

public final class DisguiseSync {
	private DisguiseSync() {
	}

	public static void refreshTracking(Entity entity) {
		if (!(entity.level() instanceof ServerLevel serverWorld)) {
			return;
		}

		var chunkLoadingManager = serverWorld.getChunkSource().chunkMap;
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
			tracker.getEntry().removePairing(listener.getPlayer());
			tracker.getEntry().addPairing(listener.getPlayer());
		}
	}

	public static void refreshDisguisedPlayers(MinecraftServer server) {
		var disguisedIds = DisguiseTracker.getDisguisedEntityIds();
		for (ServerLevel world : server.getAllLevels()) {
			for (int entityId : disguisedIds) {
				Entity entity = world.getEntity(entityId);
				if (!(entity instanceof ServerPlayer player)) {
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
