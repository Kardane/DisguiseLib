package xyz.nucleoid.disguiselib.impl.mixin;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nucleoid.disguiselib.api.DisguiseEvents;
import xyz.nucleoid.disguiselib.api.DisguiseUtils;
import xyz.nucleoid.disguiselib.api.EntityDisguise;
import xyz.nucleoid.disguiselib.impl.DisguiseLib;
import xyz.nucleoid.disguiselib.impl.DisguiseSync;
import xyz.nucleoid.disguiselib.impl.DisguiseTracker;
import xyz.nucleoid.disguiselib.impl.PlayerDisguiseNameplatePolicy;
import xyz.nucleoid.disguiselib.impl.PlayerDisguiseSneakPolicy;
import xyz.nucleoid.disguiselib.impl.mixin.accessor.EntityTrackerEntryAccessor;
import xyz.nucleoid.disguiselib.impl.mixin.accessor.ServerChunkLoadingManagerAccessor;

import java.util.*;
import java.util.stream.Collectors;

import static xyz.nucleoid.disguiselib.impl.DisguiseLib.DISGUISE_TEAM;

@Mixin(Entity.class)
public abstract class EntityMixin_Disguise implements EntityDisguise, DisguiseUtils {

	@Unique
	private final Entity disguiselib$entity = (Entity) (Object) this;
	@Shadow
	public Level level;
	@Shadow
	protected UUID uuid;
	@Unique
	private Entity disguiselib$disguiseEntity;
	@Unique
	private int disguiselib$ticks;
	@Unique
	private EntityType<?> disguiselib$disguiseType;
	@Unique
	private boolean disguiselib$trueSight = false;

	@Shadow
	public abstract EntityType<?> getType();

	@Shadow
	public abstract float getYHeadRot();

	@Shadow
	public abstract Component getName();

	@Shadow
	public abstract SynchedEntityData getEntityData();

	@Shadow
	@Nullable
	public abstract Component getCustomName();

	@Shadow
	public abstract boolean isCustomNameVisible();

	@Shadow
	public abstract boolean isSprinting();

	@Shadow
	public abstract boolean isShiftKeyDown();

	@Shadow
	public abstract boolean isSwimming();

	@Shadow
	public abstract boolean isCurrentlyGlowing();

	@Shadow
	public abstract boolean isSilent();

	@Shadow
	private int id;

	@Shadow
	public abstract Pose getPose();

	@Shadow
	public abstract int getId();

	@Shadow
	public abstract boolean isOnFire();

	@Shadow
	public abstract Component getDisplayName();

	@Shadow
	protected abstract void addPassenger(Entity passenger);

	@Shadow
	private boolean onGround;

	/**
	 * Tells you the disguised status.
	 *
	 * @return true if entity is disguised, otherwise false.
	 */
	@Override
	public boolean isDisguised() {
		return this.disguiselib$disguiseEntity != null;
	}

	/**
	 * Sets entity's disguise from {@link EntityType}
	 *
	 * @param entityType the type to disguise this entity into
	 */
	@Override
	public void disguiseAs(EntityType<?> entityType) {
		// 플레이어 타입은 지원하지 않음
		if (entityType == EntityType.PLAYER) {
			return;
		}

		// 이벤트 호출 - 취소 가능
		if (!DisguiseEvents.BEFORE_DISGUISE.invoker().beforeDisguise(this.disguiselib$entity, entityType)) {
			return;
		}

		this.disguiselib$disguiseType = entityType;

		if (this.disguiselib$disguiseEntity != null && this.disguiselib$entity instanceof ServerPlayer) {
			this.disguiselib$hideSelfView();
		}

		// 변장 엔티티가 없거나 다른 타입인 경우 새로 생성
		if (this.disguiselib$disguiseEntity == null || this.disguiselib$disguiseEntity.getType() != entityType) {
			this.disguiselib$disguiseEntity = entityType.create(level, EntitySpawnReason.LOAD);
		}

		// Fix some client predictions
		if (this.disguiselib$disguiseEntity instanceof Mob) {
			((Mob) this.disguiselib$disguiseEntity).setNoAi(true);
		}

		// Minor datatracker thingies
		this.updateTrackedData();

		DisguiseSync.refreshTracking(this.disguiselib$entity);

		// 트래커에 등록
		DisguiseTracker.onDisguise(this.disguiselib$entity);

		// 이벤트 호출
		DisguiseEvents.AFTER_DISGUISE.invoker().afterDisguise(this.disguiselib$entity, entityType);
	}

	/**
	 * Sets entity's disguise from {@link Entity}
	 *
	 * @param entity the entity to disguise into
	 */
	@Override
	public void disguiseAs(Entity entity) {
		// 플레이어로 변장 시도 시 무시
		if (entity instanceof Player) {
			return;
		}

		if (this.disguiselib$disguiseEntity != null && this.disguiselib$entity instanceof ServerPlayer) {
			this.disguiselib$hideSelfView();
		}

		this.disguiselib$disguiseEntity = entity;
		this.disguiseAs(entity.getType());
	}

	/**
	 * Clears the disguise - sets the
	 * {@link EntityMixin_Disguise#disguiselib$disguiseType} back to original.
	 */
	@Override
	public void removeDisguise() {
		if (!this.isDisguised()) {
			return;
		}

		// 이벤트 호출 - 취소 가능
		if (!DisguiseEvents.BEFORE_REMOVE.invoker().beforeRemove(this.disguiselib$entity)) {
			return;
		}

		if (this.disguiselib$disguiseEntity != null && this.disguiselib$entity instanceof ServerPlayer) {
			this.disguiselib$hideSelfView();
		}

		// 트래커에서 제거
		DisguiseTracker.onRemoveDisguise(this.disguiselib$entity);

		// Setting as not-disguised
		this.disguiselib$disguiseEntity = null;
		this.disguiselib$disguiseType = null;

		DisguiseSync.refreshTracking(this.disguiselib$entity);

		// 이벤트 호출
		DisguiseEvents.AFTER_REMOVE.invoker().afterRemove(this.disguiselib$entity);
	}

	/**
	 * Gets the disguise entity type
	 *
	 * @return disguise entity type or real type if there's no disguise
	 */
	@Override
	public EntityType<?> getDisguiseType() {
		return this.disguiselib$disguiseType != null ? this.disguiselib$disguiseType : this.getType();
	}

	/**
	 * Gets the disguise entity.
	 *
	 * @return disguise entity or null if there's no disguise
	 */
	@Nullable
	@Override
	public Entity getDisguiseEntity() {
		return this.disguiselib$disguiseEntity;
	}

	/**
	 * Whether disguise type entity is an instance of {@link LivingEntity}.
	 *
	 * @return true if the disguise type is an instance of {@link LivingEntity},
	 *         otherwise false.
	 */
	@Override
	public boolean disguiseAlive() {
		return this.disguiselib$disguiseEntity instanceof LivingEntity;
	}

	/**
	 * Whether this entity can bypass the
	 * "disguises" and see entities normally
	 * Intended more for admins (to not get trolled themselves).
	 *
	 * @return if entity can be "fooled" by disguise
	 */
	@Override
	public boolean hasTrueSight() {
		return this.disguiselib$trueSight;
	}

	/**
	 * Toggles true sight - whether entity
	 * can see disguises or not.
	 * Intended more for admins (to not get trolled themselves).
	 *
	 * @param trueSight if entity should not see disguises
	 */
	@Override
	public void setTrueSight(boolean trueSight) {
		this.disguiselib$trueSight = trueSight;
	}

	/**
	 * Hides player's self-disguise-entity
	 */
	@Unique
	private void disguiselib$hideSelfView() {
		if (!(this.disguiselib$entity instanceof ServerPlayer player)) {
			return;
		}
		if (this.disguiselib$disguiseEntity == null) {
			return;
		}

		player.connection.send(new ClientboundRemoveEntitiesPacket(this.disguiselib$disguiseEntity.getId()));
		ClientboundSetPlayerTeamPacket removeTeamPacket = ClientboundSetPlayerTeamPacket.createPlayerPacket(DISGUISE_TEAM, player.getGameProfile().name(),
				ClientboundSetPlayerTeamPacket.Action.REMOVE);
		player.connection.send(removeTeamPacket);
	}

	/**
	 * Gets equipment as list of {@link Pair Pairs}.
	 * Requires entity to be an instanceof {@link LivingEntity}.
	 *
	 * @return equipment list of pairs.
	 */
	@Unique
	private List<Pair<EquipmentSlot, ItemStack>> disguiselib$getEquipment() {
		if (disguiselib$entity instanceof LivingEntity) {
			return Arrays.stream(EquipmentSlot.values())
					.map(slot -> new Pair<>(slot, ((LivingEntity) disguiselib$entity).getItemBySlot(slot)))
					.collect(Collectors.toList());
		}
		return Collections.emptyList();
	}

	/**
	 * Updates custom name and its visibility.
	 * Also sets no-gravity to true in order
	 * to prevent the client from predicting
	 * the entity position and velocity.
	 */
	@Override
	public void updateTrackedData() {
		if (this.disguiselib$disguiseEntity == null) {
			return;
		}

		this.disguiselib$disguiseEntity.setNoGravity(true);
		var nameplateState = PlayerDisguiseNameplatePolicy.resolve(
				DisguiseLib.isPlayerDisguiseNameplateEnabled(),
				this.disguiselib$entity instanceof Player,
				DisguiseLib.isPlayerDisguiseNameplateExcluded(this.disguiselib$disguiseEntity.getType()),
				this.disguiselib$entity instanceof Player ? this.getDisplayName() : null,
				this.getCustomName(),
				this.isCustomNameVisible());
		this.disguiselib$disguiseEntity.setCustomName(nameplateState.customName());
		this.disguiselib$disguiseEntity.setCustomNameVisible(nameplateState.visible());
		this.disguiselib$disguiseEntity.setSprinting(this.isSprinting());
		var sneakState = PlayerDisguiseSneakPolicy.resolve(
				DisguiseLib.isPlayerSneakEnabled(),
				this.disguiselib$entity instanceof Player,
				this.isShiftKeyDown(),
				this.getPose());
		this.disguiselib$disguiseEntity.setShiftKeyDown(sneakState.sneaking());
		this.disguiselib$disguiseEntity.setSwimming(this.isSwimming());
		this.disguiselib$disguiseEntity.setGlowingTag(this.isCurrentlyGlowing());
		this.disguiselib$disguiseEntity.setSharedFlagOnFire(this.isOnFire());
		this.disguiselib$disguiseEntity.setSilent(this.isSilent());
		this.disguiselib$disguiseEntity.setPose(sneakState.pose());

		if (this.disguiselib$disguiseEntity instanceof LivingEntity disguise
				&& ((Object) this) instanceof LivingEntity self) {
			disguise.getAttributes().assignAllValues(self.getAttributes());
		}
	}

	/**
	 * Sends additional move packets to the client if
	 * entity is disguised.
	 * Prevents client desync and fixes "blocky" movement.
	 */
	@Inject(method = "tick()V", at = @At("TAIL"))
	private void postTick(CallbackInfo ci) {
		if (!this.isDisguised()) {
			return;
		}

		if (this.level.getServer() != null && !(this.disguiselib$disguiseEntity instanceof LivingEntity)
				&& !(this.disguiselib$entity instanceof Player)) {
			this.level.getServer().getPlayerList().broadcastAll(
					new ClientboundTeleportEntityPacket(
							this.disguiselib$entity.getId(),
							new PositionMoveRotation(
									this.disguiselib$entity.trackingPosition(),
									this.disguiselib$entity.getDeltaMovement(),
									this.disguiselib$entity.getYRot(),
									this.disguiselib$entity.getXRot()),
							Set.of(), this.onGround),
					this.level.dimension());
		} else if (this.disguiselib$entity instanceof ServerPlayer && ++this.disguiselib$ticks % 40 == 0) {
			// MutableText msg = Text.literal("You are disguised as ")
			// .append(Text.translatable(this.disguiselib$disguiseEntity.getType().getTranslationKey()))
			// .formatted(Formatting.GREEN);

			// ((ServerPlayerEntity) this.disguiselib$entity).sendMessage(msg, true);
			this.disguiselib$ticks = 0;
		}
	}

	/**
	 * If entity is disguised, we need to clean up on discard.
	 */
	@Inject(method = "discard()V", at = @At("TAIL"))
	private void onRemove(CallbackInfo ci) {
		if (this.isDisguised()) {
			DisguiseTracker.onRemoveDisguise(this.disguiselib$entity);
		}
	}

	/**
	 * Takes care of loading the fake entity data from tag.
	 *
	 * @param tag tag to load data from.
	 */
	@Inject(method = "load", at = @At("TAIL"))
	private void fromTag(ValueInput tag, CallbackInfo ci) {
		var disguiseTag = tag.child("DisguiseLib");

		if (disguiseTag.isPresent()) {
			Identifier disguiseTypeId = Identifier.tryParse(disguiseTag.get().getStringOr("DisguiseType", ""));
			if (disguiseTypeId == null) {
				return;
			}

			this.disguiselib$disguiseType = BuiltInRegistries.ENTITY_TYPE.getValue(disguiseTypeId);

			// 플레이어 타입은 무시
			if (this.disguiselib$disguiseType == EntityType.PLAYER) {
				this.disguiselib$disguiseType = null;
				return;
			}

			var disguiseEntityTag = disguiseTag.get().child("DisguiseEntity");
			if (disguiseEntityTag.isPresent()) {
				this.disguiselib$disguiseEntity = EntityType.loadEntityRecursive(
						disguiseEntityTag.get(), this.level, EntitySpawnReason.LOAD, (entityx) -> entityx);
			}
		}
	}

	/**
	 * Takes care of saving the fake entity data to tag.
	 *
	 * @param tag tag to save data to.
	 */
	@Inject(method = "saveWithoutId", at = @At("TAIL"))
	private void toTag(ValueOutput tag, CallbackInfo ci) {
		if (this.isDisguised() && this.disguiselib$disguiseType != null) {
			var disguiseTag = tag.child("DisguiseLib");

			disguiseTag.putString("DisguiseType",
					BuiltInRegistries.ENTITY_TYPE.getKey(this.disguiselib$disguiseType).toString());

			if (this.disguiselib$disguiseEntity != null
					&& !this.disguiselib$entity.equals(this.disguiselib$disguiseEntity)) {
				var disguiseEntityTag = disguiseTag.child("DisguiseEntity");
				this.disguiselib$disguiseEntity.saveWithoutId(disguiseEntityTag);

				Identifier identifier = BuiltInRegistries.ENTITY_TYPE.getKey(this.disguiselib$disguiseEntity.getType());
				disguiseEntityTag.putString("id", identifier.toString());
			}
		}
	}
}
