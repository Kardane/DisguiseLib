package xyz.nucleoid.disguiselib.impl.mixin;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nucleoid.disguiselib.api.DisguiseUtils;
import xyz.nucleoid.disguiselib.api.EntityDisguise;
import xyz.nucleoid.disguiselib.impl.DisguiseTracker;
import xyz.nucleoid.disguiselib.impl.mixin.accessor.*;
import xyz.nucleoid.disguiselib.impl.packets.ExtendedHandler;
import xyz.nucleoid.disguiselib.impl.packets.FakePackets;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static xyz.nucleoid.disguiselib.impl.DisguiseLib.DISGUISE_TEAM;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerPlayNetworkHandlerMixin_Disguiser extends ServerCommonPacketListenerImpl
		implements ExtendedHandler {
	@Shadow
	public ServerPlayer player;

	@Unique
	private boolean disguiselib$sentTeamPacket;

	public ServerPlayNetworkHandlerMixin_Disguiser(MinecraftServer server, Connection connection,
			CommonListenerCookie clientData) {
		super(server, connection, clientData);
	}

	public void disguiselib$transformPacket(Packet<? super ClientGamePacketListener> packet, Runnable remove,
			Consumer<Packet<ClientGamePacketListener>> add) {
		long startTime = System.nanoTime();

		try {
			Level world = this.player.level();
			if (packet instanceof ClientboundAddEntityPacket) {
				int entityId = ((EntitySpawnS2CPacketAccessor) packet).getEntityId();

				// 자기 자신의 스폰 패킷은 변환하지 않음
				if (entityId == this.player.getId()) {
					return;
				}

				var entity = world.getEntity(entityId);

				// entity가 null이면 UUID로 PlayerManager에서 플레이어 조회 (Carpet 봇 재스폰 시)
				if (entity == null && world instanceof ServerLevel serverWorld) {
					UUID uuid = ((EntitySpawnS2CPacketAccessor) packet).getUuid();
					if (uuid != null) {
						entity = serverWorld.getServer().getPlayerList().getPlayer(uuid);
					}
				}

				if (entity != null) {
					disguiselib$sendFakePacket(entity, remove, add);
				}
			} else if (packet instanceof ClientboundRemoveEntitiesPacket
					&& !((EntitiesDestroyS2CPacketAccessor) packet).getEntityIds().isEmpty()
					&& ((EntitiesDestroyS2CPacketAccessor) packet).getEntityIds().getInt(0) == this.player.getId()) {
				remove.run();
				return;
			} else if (packet instanceof ClientboundSetEntityDataPacket) {
				int entityId = ((EntityTrackerUpdateS2CPacketAccessor) packet).getEntityId();

				// 자기 자신의 DataTracker 패킷은 변환하지 않음
				if (entityId == this.player.getId()) {
					return;
				}

				// TrueSight가 있으면 변환하지 않음
				if (((EntityDisguise) this.player).hasTrueSight()) {
					return;
				}

				Entity original = world.getEntity(entityId);
				if (original == null) {
					return;
				}

				EntityDisguise disguise = (EntityDisguise) original;
				if (!disguise.isDisguised()) {
					return;
				}

				List<SynchedEntityData.DataValue<?>> trackedValues = ((EntityTrackerUpdateS2CPacketAccessor) packet)
						.getTrackedValues();
				boolean shouldRefreshDisguiseTracker = original instanceof Player
						|| this.disguiselib$hasSharedFlags(trackedValues);

				if (shouldRefreshDisguiseTracker) {
					((DisguiseUtils) disguise).updateTrackedData();
					Entity disguiseEntity = disguise.getDisguiseEntity();
					if (disguiseEntity != null) {
						var dataTracker = disguiseEntity.getEntityData();
						if (dataTracker != null) {
							// updateTrackedData에서 setSprinting 등을 호출하므로 값은 변경되었으나,
							// dirty check에서 걸러졌을 수 있음. getChangedEntries() 결과를 가져옴.
							var allEntries = dataTracker.getNonDefaultValues();
							if (allEntries == null) {
								allEntries = new ArrayList<>();
							} else {
								allEntries = new ArrayList<>(allEntries); // Ensure mutable
							}

							// Check if flags (Index 0) are present
							boolean flagsPresent = false;
							for (var entry : allEntries) {
								if (entry.id() == 0) {
									flagsPresent = true;
									break;
								}
							}

							// If not present, manually add current value
							if (!flagsPresent) {
								try {
									var flagsData = EntityAccessor.getFLAGS();
									byte currentFlags = dataTracker.get(flagsData);
									// DataTracker.SerializedEntry record: (int id, TrackedDataHandler<T> handler, T
									// value)
									// TrackedData has dataType() which returns the handler
									allEntries.add(new SynchedEntityData.DataValue<>(flagsData.id(),
											flagsData.serializer(), currentFlags));
								} catch (Exception e) {
									// Ignore
								}
							}

							if (!allEntries.isEmpty()) {
								add.accept(new ClientboundSetEntityDataPacket(entityId, allEntries));
							}
						}
					}
				} else {
					// No shared flags found. Dropping packet.
				}

				remove.run();
				return;
			} else if (packet instanceof ClientboundUpdateAttributesPacket && !((EntityDisguise) this.player).hasTrueSight()) {
				int entityId = ((EntityAttributesS2CPacketAccessor) packet).getEntityId();

				// 자기 자신의 속성 패킷은 변환하지 않음
				if (entityId == this.player.getId()) {
					return;
				}

				Entity original = world.getEntity(entityId);
				if (original != null) {
					EntityDisguise entityDisguise = (EntityDisguise) original;
					if (entityDisguise.isDisguised() && !((DisguiseUtils) original).disguiseAlive()) {
						remove.run();
						return;
					}
				}
			} else if (packet instanceof ClientboundTakeItemEntityPacket pickupPacket
					&& !((EntityDisguise) this.player).hasTrueSight()) {
				if (this.disguiselib$shouldDropForNonLivingDisguise(world, pickupPacket.getPlayerId())) {
					remove.run();
					return;
				}
			} else if (packet instanceof ClientboundSetEquipmentPacket equipmentPacket
					&& !((EntityDisguise) this.player).hasTrueSight()) {
				if (this.disguiselib$shouldDropForNonLivingDisguise(world, equipmentPacket.getEntity())) {
					remove.run();
					return;
				}
			} else if (packet instanceof ClientboundUpdateMobEffectPacket effectPacket
					&& !((EntityDisguise) this.player).hasTrueSight()) {
				if (this.disguiselib$shouldDropForNonLivingDisguise(world, effectPacket.getEntityId())) {
					remove.run();
					return;
				}
			} else if (packet instanceof ClientboundRemoveMobEffectPacket effectPacket
					&& !((EntityDisguise) this.player).hasTrueSight()) {
				if (this.disguiselib$shouldDropForNonLivingDisguise(world, effectPacket.entityId())) {
					remove.run();
					return;
				}
			} else if (packet instanceof ClientboundHurtAnimationPacket damageTiltPacket
					&& !((EntityDisguise) this.player).hasTrueSight()) {
				if (this.disguiselib$shouldDropForNonLivingDisguise(world, damageTiltPacket.id())) {
					remove.run();
					return;
				}
			} else if (packet instanceof ClientboundAnimatePacket animationPacket
					&& !((EntityDisguise) this.player).hasTrueSight()) {
				if (this.disguiselib$shouldDropForNonLivingDisguise(world, animationPacket.getId())) {
					remove.run();
					return;
				}
			} else if (packet instanceof ClientboundSetEntityMotionPacket velocityPacket) {
				int id = velocityPacket.id();
				if (id != this.player.getId()) {
					Entity entity1 = world.getEntity(id);
					if (entity1 != null && ((EntityDisguise) entity1).isDisguised()) {
						remove.run();
					}
				}
			}
		} finally {
			long duration = System.nanoTime() - startTime;
			DisguiseTracker.recordPacketTransform(duration);
		}
	}

	@Unique
	private boolean disguiselib$hasSharedFlags(List<SynchedEntityData.DataValue<?>> trackedValues) {
		if (trackedValues == null) {
			return false;
		}

		for (SynchedEntityData.DataValue<?> entry : trackedValues) {
			if (entry.id() == 0) {
				return true;
			}
		}

		return false;
	}

	@Unique
	private boolean disguiselib$shouldDropForNonLivingDisguise(Level world, int entityId) {
		if (entityId == this.player.getId()) {
			return false;
		}

		Entity entity = world.getEntity(entityId);
		if (entity == null) {
			return false;
		}

		EntityDisguise entityDisguise = (EntityDisguise) entity;
		return entityDisguise.isDisguised() && !((DisguiseUtils) entity).disguiseAlive();
	}

	/**
	 * Sends fake packet instead of the real one.
	 *
	 * @param entity the entity that is disguised and needs to have a custom packet
	 *               sent.
	 */
	@Unique
	private void disguiselib$sendFakePacket(Entity entity, Runnable remove,
			Consumer<Packet<ClientGamePacketListener>> add) {
		EntityDisguise disguise = (EntityDisguise) entity;

		// 자기 자신에게는 변장 패킷을 보내지 않음
		if (entity.getId() == this.player.getId()) {
			return;
		}

		// TrueSight가 있거나 변장되지 않은 경우 원본 패킷 유지
		if (((EntityDisguise) this.player).hasTrueSight() || !disguise.isDisguised()) {
			return;
		}

		Entity disguiseEntity = disguise.getDisguiseEntity();
		if (disguiseEntity == null) {
			return;
		}

		try {
			Packet<?> spawnPacket;
			var entry = new ServerEntity((ServerLevel) entity.level(), entity, 1, true,
					new ServerEntity.Synchronizer() {
						@Override
						public void sendToTrackingPlayers(Packet<? super ClientGamePacketListener> packet) {
						}

						@Override
						public void sendToTrackingPlayersAndSelf(Packet<? super ClientGamePacketListener> packet) {
						}

						@Override
						public void sendToTrackingPlayersFiltered(Packet<? super ClientGamePacketListener> packet,
								Predicate<ServerPlayer> predicate) {
						}
					});

			spawnPacket = FakePackets.universalSpawnPacket(entity, entry, true);
			add.accept((Packet<ClientGamePacketListener>) spawnPacket);

			// 변장 엔티티의 DataTracker 초기 값도 함께 전송 (NBT 태그 상태 반영)
			var dataTracker = disguiseEntity.getEntityData();
			if (dataTracker != null) {
				var allEntries = dataTracker.getNonDefaultValues();
				if (allEntries != null && !allEntries.isEmpty()) {
					add.accept(new ClientboundSetEntityDataPacket(entity.getId(), allEntries));
				}
			}

			remove.run();
		} catch (Exception e) {
			// 스폰 패킷 생성 중 예외 발생 시 원본 패킷 유지
		}
	}

	@Inject(method = "handleMovePlayer(Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V", shift = At.Shift.AFTER))
	private void disguiselib$moveDisguiseEntity(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
		// 자기 자신에게는 변장 엔티티 위치 업데이트를 보내지 않음
		// 다른 플레이어들에게만 전송됨
	}

	public void disguiselib$onClientBrand() {
		if (!this.disguiselib$sentTeamPacket) {
			ClientboundSetPlayerTeamPacket addTeamPacket = ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(DISGUISE_TEAM, true);
			this.disguiselib$sentTeamPacket = true;
			this.send(addTeamPacket);

			if (((EntityDisguise) this.player).isDisguised()) {
				ClientboundSetPlayerTeamPacket joinTeamPacket = ClientboundSetPlayerTeamPacket.createPlayerPacket(DISGUISE_TEAM,
						this.player.getGameProfile().name(), ClientboundSetPlayerTeamPacket.Action.ADD);
				this.send(joinTeamPacket);
			}
		}
	}
}
