package xyz.nucleoid.disguiselib.impl.mixin;

import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.entity.mob.SpellcastingIllagerEntity;
import net.minecraft.entity.passive.ArmadilloEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.entity.passive.FrogEntity;
import net.minecraft.entity.passive.GoatEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.PolarBearEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerPosition;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;

import net.minecraft.util.Identifier;
import net.minecraft.world.World;
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
import xyz.nucleoid.disguiselib.impl.DisguiseSwimmingPolicy;
import xyz.nucleoid.disguiselib.impl.DisguiseSync;
import xyz.nucleoid.disguiselib.impl.DisguiseTracker;
import xyz.nucleoid.disguiselib.impl.IronGolemDisguiseHealthPolicy;
import xyz.nucleoid.disguiselib.impl.PlayerDisguiseAnimationController;
import xyz.nucleoid.disguiselib.impl.PlayerDisguiseAnimationSupport;
import xyz.nucleoid.disguiselib.impl.PlayerDisguiseAnimationType;
import xyz.nucleoid.disguiselib.impl.PlayerDisguiseNameplatePolicy;
import xyz.nucleoid.disguiselib.impl.PlayerDisguiseSneakPolicy;
import xyz.nucleoid.disguiselib.impl.mixin.accessor.BeeEntityAccessor;
import xyz.nucleoid.disguiselib.impl.mixin.accessor.EndermanEntityAccessor;
import xyz.nucleoid.disguiselib.impl.mixin.accessor.EntityTrackerEntryAccessor;
import xyz.nucleoid.disguiselib.impl.mixin.accessor.ServerChunkLoadingManagerAccessor;
import xyz.nucleoid.disguiselib.impl.mixin.accessor.SpellcastingIllagerEntityAccessor;

import java.util.*;
import java.util.stream.Collectors;

@Mixin(Entity.class)
public abstract class EntityMixin_Disguise implements EntityDisguise, DisguiseUtils {

	@Unique
	private final Entity disguiselib$entity = (Entity) (Object) this;
	@Shadow
	public World world;
	@Shadow
	protected UUID uuid;
	@Unique
	private Entity disguiselib$disguiseEntity;

	@Unique
	private EntityType<?> disguiselib$disguiseType;
	@Unique
	private boolean disguiselib$trueSight = false;

	@Shadow
	public abstract EntityType<?> getType();

	@Shadow
	public abstract float getHeadYaw();

	@Shadow
	public abstract Text getName();

	@Shadow
	public abstract DataTracker getDataTracker();

	@Shadow
	@Nullable
	public abstract Text getCustomName();

	@Shadow
	public abstract boolean isCustomNameVisible();

	@Shadow
	public abstract boolean isSprinting();

	@Shadow
	public abstract boolean isSneaking();

	@Shadow
	public abstract boolean isSwimming();

	@Shadow
	public abstract boolean isGlowing();

	@Shadow
	public abstract boolean isSilent();

	@Shadow
	private int id;

	@Shadow
	public abstract EntityPose getPose();

	@Shadow
	public abstract int getId();

	@Shadow
	public abstract boolean isOnFire();

	@Shadow
	public abstract Text getDisplayName();

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

		PlayerDisguiseAnimationController.clear(this.disguiselib$entity);
		this.disguiselib$disguiseType = entityType;

		if (this.disguiselib$disguiseEntity != null && this.disguiselib$entity instanceof ServerPlayerEntity) {
			this.disguiselib$hideSelfView();
		}

		// 변장 엔티티가 없거나 다른 타입인 경우 새로 생성
		if (this.disguiselib$disguiseEntity == null || this.disguiselib$disguiseEntity.getType() != entityType) {
			this.disguiselib$disguiseEntity = entityType.create(world, SpawnReason.LOAD);
		}

		// Fix some client predictions
		if (this.disguiselib$disguiseEntity instanceof MobEntity) {
			((MobEntity) this.disguiselib$disguiseEntity).setAiDisabled(true);
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
		if (entity instanceof PlayerEntity) {
			return;
		}

		if (this.disguiselib$disguiseEntity != null && this.disguiselib$entity instanceof ServerPlayerEntity) {
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

		PlayerDisguiseAnimationController.clear(this.disguiselib$entity);
		if (this.disguiselib$disguiseEntity != null && this.disguiselib$entity instanceof ServerPlayerEntity) {
			this.disguiselib$hideSelfView();
		}

		// 트래커에서 제거
		DisguiseTracker.onRemoveDisguise(this.disguiselib$entity);
		this.disguiselib$removeTeamEntry();

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

	@Override
	public boolean setDisguiseScale(double scale) {
		if (!(this.disguiselib$disguiseEntity instanceof LivingEntity livingDisguise)) {
			return false;
		}

		EntityAttributeInstance scaleAttribute = livingDisguise.getAttributes().getCustomInstance(EntityAttributes.SCALE);
		if (scaleAttribute == null) {
			return false;
		}

		scaleAttribute.setBaseValue(scale);
		return true;
	}

	@Override
	public double getDisguiseScale() {
		if (!(this.disguiselib$disguiseEntity instanceof LivingEntity livingDisguise)) {
			return Double.NaN;
		}

		EntityAttributeInstance scaleAttribute = livingDisguise.getAttributes().getCustomInstance(EntityAttributes.SCALE);
		return scaleAttribute != null ? scaleAttribute.getBaseValue() : Double.NaN;
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
		if (!(this.disguiselib$entity instanceof ServerPlayerEntity player)) {
			return;
		}
		if (this.disguiselib$disguiseEntity == null) {
			return;
		}

		player.networkHandler.sendPacket(new EntitiesDestroyS2CPacket(this.disguiselib$disguiseEntity.getId()));
	}

	@Unique
	private void disguiselib$removeTeamEntry() {
		if (!(this.disguiselib$entity instanceof PlayerEntity)) {
			return;
		}
		Team team = this.disguiselib$entity.getScoreboardTeam();
		if (team == null) {
			return;
		}
		if (this.world.getServer() == null) {
			return;
		}
		if (!(this.world instanceof ServerWorld serverWorld)) {
			return;
		}

		TeamS2CPacket packet = TeamS2CPacket.changePlayerTeam(
				team,
				this.disguiselib$entity.getUuidAsString(),
				TeamS2CPacket.Operation.REMOVE);
		var chunkLoadingManager = serverWorld.getChunkManager().chunkLoadingManager;
		if (chunkLoadingManager == null) {
			return;
		}
		var trackers = ((ServerChunkLoadingManagerAccessor) chunkLoadingManager).getEntityTrackers();
		if (trackers == null) {
			return;
		}
		var tracker = trackers.get(this.disguiselib$entity.getId());
		if (tracker == null) {
			return;
		}

		for (var listener : tracker.getListeners()) {
			ServerPlayerEntity player = listener.getPlayer();
			if (player.getId() == this.disguiselib$entity.getId()) {
				continue;
			}
			if (((EntityDisguise) player).hasTrueSight()) {
				continue;
			}
			player.networkHandler.sendPacket(packet);
		}
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
					.map(slot -> new Pair<>(slot, ((LivingEntity) disguiselib$entity).getEquippedStack(slot)))
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
				this.disguiselib$entity instanceof PlayerEntity,
				DisguiseLib.isPlayerDisguiseNameplateExcluded(this.disguiselib$disguiseEntity.getType()),
				this.disguiselib$entity instanceof PlayerEntity ? this.getDisplayName() : null,
				this.getCustomName(),
				this.isCustomNameVisible());
		this.disguiselib$disguiseEntity.setCustomName(nameplateState.customName());
		this.disguiselib$disguiseEntity.setCustomNameVisible(nameplateState.visible());
		this.disguiselib$disguiseEntity.setSprinting(this.isSprinting());
		var sneakState = PlayerDisguiseSneakPolicy.resolve(
				DisguiseLib.isPlayerSneakEnabled(),
				this.disguiselib$entity instanceof PlayerEntity,
				this.isSneaking(),
				this.getPose());
		var swimState = DisguiseSwimmingPolicy.resolve(
				this.isSwimming(),
				sneakState.pose(),
				this.disguiselib$disguiseEntity.getType() == EntityType.DROWNED);
		this.disguiselib$disguiseEntity.setSneaking(sneakState.sneaking());
		this.disguiselib$disguiseEntity.setSwimming(swimState.swimming());
		this.disguiselib$disguiseEntity.setGlowing(this.isGlowing());
		this.disguiselib$disguiseEntity.setOnFire(this.isOnFire());
		this.disguiselib$disguiseEntity.setSilent(this.isSilent());
		this.disguiselib$disguiseEntity.setPose(swimState.pose());
		this.disguiselib$applyMobAnimations();

		if (this.disguiselib$disguiseEntity instanceof LivingEntity disguise
				&& ((Object) this) instanceof LivingEntity self) {
			if (disguise instanceof IronGolemEntity) {
				disguise.setHealth(IronGolemDisguiseHealthPolicy.resolve(
						self.getHealth(),
						self.getMaxHealth(),
						disguise.getMaxHealth()));
			}
			EntityAttributeInstance.Packed disguiseScale = null;
			EntityAttributeInstance scale = disguise.getAttributes().getCustomInstance(EntityAttributes.SCALE);
			if (scale != null) {
				disguiseScale = scale.pack();
			}
			disguise.getAttributes().setFrom(self.getAttributes());
			if (disguiseScale != null) {
				scale = disguise.getAttributes().getCustomInstance(EntityAttributes.SCALE);
				if (scale != null) {
					scale.unpack(disguiseScale);
				}
			}
		}
	}

	@Unique
	private void disguiselib$applyMobAnimations() {
		if (!(this.disguiselib$disguiseEntity instanceof MobEntity mobEntity)) {
			return;
		}

		if (!(this.disguiselib$entity instanceof PlayerEntity player)) {
			return;
		}

		if (mobEntity instanceof SpellcastingIllagerEntity spellcaster) {
			spellcaster.getDataTracker().set(SpellcastingIllagerEntityAccessor.getSpell(), (byte) 0);
		}
		if (mobEntity instanceof ArmadilloEntity armadilloEntity) {
			armadilloEntity.unroll();
		}
		if (mobEntity instanceof PolarBearEntity polarBearEntity) {
			polarBearEntity.setWarning(false);
			polarBearEntity.setAngerTime(0);
			polarBearEntity.setAngryAt(null);
		}
		if (mobEntity instanceof BeeEntity beeEntity) {
			beeEntity.setAngerTime(0);
			beeEntity.setAngryAt(null);
			((BeeEntityAccessor) beeEntity).callSetNearTarget(false);
		}
		if (mobEntity instanceof FoxEntity foxEntity) {
			foxEntity.setCrouching(false);
			foxEntity.setChasing(false);
		}
		if (mobEntity instanceof WolfEntity wolfEntity) {
			wolfEntity.setAngerTime(0);
			wolfEntity.setAngryAt(null);
		}
		if (mobEntity instanceof FrogEntity frogEntity) {
			frogEntity.clearFrogTarget();
		}
		if (mobEntity instanceof EndermanEntity endermanEntity) {
			endermanEntity.setAngerTime(0);
			endermanEntity.setAngryAt(null);
			endermanEntity.getDataTracker().set(EndermanEntityAccessor.getAngry(), false);
			endermanEntity.getDataTracker().set(EndermanEntityAccessor.getProvoked(), false);
		}

		boolean skeletonBowAiming = PlayerDisguiseAnimationSupport.isSkeletonBowAiming(
				Registries.ENTITY_TYPE.getId(this.disguiselib$disguiseEntity.getType()).toString(),
				true,
				player.isUsingItem(),
				player.getActiveItem().getItem() instanceof BowItem,
				player.getMainHandStack().isOf(Items.BOW));

		if (PlayerDisguiseAnimationController.isVindicatorAttacking(this.disguiselib$entity)) {
			mobEntity.setAttacking(true);
		} else {
			mobEntity.setAttacking(skeletonBowAiming);
		}

		if (mobEntity instanceof GhastEntity ghastEntity) {
			ghastEntity.setShooting(
					PlayerDisguiseAnimationController.isActive(this.disguiselib$entity,
							PlayerDisguiseAnimationType.GHAST_CHARGE));
		}
		if (mobEntity instanceof ArmadilloEntity armadilloEntity
				&& PlayerDisguiseAnimationController.isActive(this.disguiselib$entity,
						PlayerDisguiseAnimationType.ARMADILLO_ROLL)) {
			armadilloEntity.setState(ArmadilloEntity.State.SCARED);
		}
		if (mobEntity instanceof PolarBearEntity polarBearEntity
				&& PlayerDisguiseAnimationController.isActive(this.disguiselib$entity,
						PlayerDisguiseAnimationType.POLAR_BEAR_ATTACK)) {
			polarBearEntity.setWarning(true);
			polarBearEntity.setAngerTime(100);
			polarBearEntity.setAngryAt(player.getUuid());
		}
		if (mobEntity instanceof BeeEntity beeEntity
				&& PlayerDisguiseAnimationController.isActive(this.disguiselib$entity,
						PlayerDisguiseAnimationType.BEE_ATTACK)) {
			beeEntity.setAngerTime(100);
			beeEntity.setAngryAt(player.getUuid());
			((BeeEntityAccessor) beeEntity).callSetNearTarget(true);
		}
		if (mobEntity instanceof FoxEntity foxEntity
				&& PlayerDisguiseAnimationController.isActive(this.disguiselib$entity,
						PlayerDisguiseAnimationType.FOX_POUNCE)) {
			foxEntity.setCrouching(true);
			foxEntity.setChasing(true);
		}
		if (mobEntity instanceof WolfEntity wolfEntity
				&& PlayerDisguiseAnimationController.isActive(this.disguiselib$entity,
						PlayerDisguiseAnimationType.WOLF_ANGRY)) {
			wolfEntity.setAngerTime(100);
			wolfEntity.setAngryAt(player.getUuid());
		}
		if (mobEntity instanceof FrogEntity frogEntity
				&& PlayerDisguiseAnimationController.isActive(this.disguiselib$entity,
						PlayerDisguiseAnimationType.FROG_EAT)) {
			int targetEntityId = PlayerDisguiseAnimationController.getTargetEntityId(this.disguiselib$entity);
			Entity target = targetEntityId != -1 ? this.world.getEntityById(targetEntityId) : null;
			if (target != null) {
				frogEntity.setFrogTarget(target);
				frogEntity.setPose(EntityPose.USING_TONGUE);
			}
		}
		if (mobEntity instanceof GoatEntity
				&& PlayerDisguiseAnimationController.isActive(this.disguiselib$entity,
						PlayerDisguiseAnimationType.GOAT_RAM)) {
			mobEntity.setAttacking(true);
		}
		if (mobEntity instanceof RavagerEntity
				&& PlayerDisguiseAnimationController.isActive(this.disguiselib$entity,
						PlayerDisguiseAnimationType.RAVAGER_ATTACK)) {
			mobEntity.setAttacking(true);
		}
		if (mobEntity instanceof EndermanEntity endermanEntity
				&& PlayerDisguiseAnimationController.isActive(this.disguiselib$entity,
						PlayerDisguiseAnimationType.ENDERMAN_ANGRY)) {
			endermanEntity.setAngerTime(100);
			endermanEntity.setAngryAt(player.getUuid());
			endermanEntity.getDataTracker().set(EndermanEntityAccessor.getAngry(), true);
			endermanEntity.getDataTracker().set(EndermanEntityAccessor.getProvoked(), true);
		}

		if (mobEntity instanceof SpellcastingIllagerEntity spellcaster) {
			if (PlayerDisguiseAnimationController.isActive(this.disguiselib$entity,
					PlayerDisguiseAnimationType.EVOKER_CAST)) {
				spellcaster.getDataTracker().set(SpellcastingIllagerEntityAccessor.getSpell(),
						PlayerDisguiseAnimationSupport.getSpellId(PlayerDisguiseAnimationType.EVOKER_CAST));
			} else if (PlayerDisguiseAnimationController.isActive(this.disguiselib$entity,
					PlayerDisguiseAnimationType.ILLUSIONER_CAST)) {
				spellcaster.getDataTracker().set(SpellcastingIllagerEntityAccessor.getSpell(),
						PlayerDisguiseAnimationSupport.getSpellId(PlayerDisguiseAnimationType.ILLUSIONER_CAST));
			}
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

		if (this.world.getServer() != null && !(this.disguiselib$disguiseEntity instanceof LivingEntity)
				&& !(this.disguiselib$entity instanceof PlayerEntity)) {
			this.world.getServer().getPlayerManager().sendToDimension(
					new EntityPositionS2CPacket(
							this.disguiselib$entity.getId(),
							new PlayerPosition(
									this.disguiselib$entity.getSyncedPos(),
									this.disguiselib$entity.getVelocity(),
									this.disguiselib$entity.getYaw(),
									this.disguiselib$entity.getPitch()),
							Set.of(), this.onGround),
					this.world.getRegistryKey());
		}
	}

	/**
	 * If entity is disguised, we need to clean up on discard.
	 */
	@Inject(method = "discard()V", at = @At("TAIL"))
	private void onRemove(CallbackInfo ci) {
		if (this.isDisguised()) {
			PlayerDisguiseAnimationController.clear(this.disguiselib$entity);
			DisguiseTracker.onRemoveDisguise(this.disguiselib$entity);
		}
	}

	/**
	 * Takes care of loading the fake entity data from tag.
	 *
	 * @param tag tag to load data from.
	 */
	@Inject(method = "readData", at = @At("TAIL"))
	private void fromTag(ReadView tag, CallbackInfo ci) {
		var disguiseTag = tag.getOptionalReadView("DisguiseLib");

		if (disguiseTag.isPresent()) {
			Identifier disguiseTypeId = Identifier.tryParse(disguiseTag.get().getString("DisguiseType", ""));
			if (disguiseTypeId == null) {
				return;
			}

			this.disguiselib$disguiseType = Registries.ENTITY_TYPE.get(disguiseTypeId);

			// 플레이어 타입은 무시
			if (this.disguiselib$disguiseType == EntityType.PLAYER) {
				this.disguiselib$disguiseType = null;
				return;
			}

			var disguiseEntityTag = disguiseTag.get().getOptionalReadView("DisguiseEntity");
			if (disguiseEntityTag.isPresent()) {
				this.disguiselib$disguiseEntity = EntityType.loadEntityWithPassengers(
						disguiseEntityTag.get(), this.world, SpawnReason.LOAD, (entityx) -> entityx);
			}
		}
	}

	/**
	 * Takes care of saving the fake entity data to tag.
	 *
	 * @param tag tag to save data to.
	 */
	@Inject(method = "writeData", at = @At("TAIL"))
	private void toTag(WriteView tag, CallbackInfo ci) {
		if (this.isDisguised() && this.disguiselib$disguiseType != null) {
			var disguiseTag = tag.get("DisguiseLib");

			disguiseTag.putString("DisguiseType",
					Registries.ENTITY_TYPE.getId(this.disguiselib$disguiseType).toString());

			if (this.disguiselib$disguiseEntity != null
					&& !this.disguiselib$entity.equals(this.disguiselib$disguiseEntity)) {
				var disguiseEntityTag = disguiseTag.get("DisguiseEntity");
				this.disguiselib$disguiseEntity.writeData(disguiseEntityTag);

				Identifier identifier = Registries.ENTITY_TYPE.getId(this.disguiselib$disguiseEntity.getType());
				disguiseEntityTag.putString("id", identifier.toString());
			}
		}
	}
}
