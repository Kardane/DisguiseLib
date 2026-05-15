package xyz.nucleoid.disguiselib.impl;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.command.argument.RegistryEntryReferenceArgumentType;
import net.minecraft.command.suggestion.SuggestionProviders;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import xyz.nucleoid.disguiselib.api.EntityDisguise;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import static net.minecraft.command.argument.EntityArgumentType.entities;
import static net.minecraft.command.suggestion.SuggestionProviders.SUMMONABLE_ENTITIES;
import static net.minecraft.server.command.CommandManager.argument;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.minecraft.server.command.CommandManager.literal;

public class DisguiseCommand {

	private static final Text NO_PERMISSION_ERROR = Text.translatable("commands.help.failed");

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
			CommandRegistryAccess commandRegistryAccess,
			CommandManager.RegistrationEnvironment registrationEnvironment) {
		dispatcher.register(literal("disguise")
				.requires(Permissions.require("disguiselib.disguise", 2))
				.then(argument("target", entities())
						.then(literal("animate")
								.then(literal("ghast-charge")
										.then(argument("ticks", integer(1))
												.executes(ctx -> animateDisguise(ctx,
														PlayerDisguiseAnimationType.GHAST_CHARGE))))
								.then(literal("evoker-cast")
										.then(argument("ticks", integer(1))
												.executes(ctx -> animateDisguise(ctx,
														PlayerDisguiseAnimationType.EVOKER_CAST))))
								.then(literal("illusioner-cast")
										.then(argument("ticks", integer(1))
												.executes(ctx -> animateDisguise(ctx,
														PlayerDisguiseAnimationType.ILLUSIONER_CAST))))
								.then(literal("armadillo-roll")
										.then(argument("ticks", integer(1))
												.executes(ctx -> animateDisguise(ctx,
														PlayerDisguiseAnimationType.ARMADILLO_ROLL))))
								.then(literal("polar-bear-attack")
										.then(argument("ticks", integer(1))
												.executes(ctx -> animateDisguise(ctx,
														PlayerDisguiseAnimationType.POLAR_BEAR_ATTACK))))
								.then(literal("bee-attack")
										.then(argument("ticks", integer(1))
												.executes(ctx -> animateDisguise(ctx,
														PlayerDisguiseAnimationType.BEE_ATTACK))))
								.then(literal("fox-pounce")
										.then(argument("ticks", integer(1))
												.executes(ctx -> animateDisguise(ctx,
														PlayerDisguiseAnimationType.FOX_POUNCE))))
								.then(literal("wolf-angry")
										.then(argument("ticks", integer(1))
												.executes(ctx -> animateDisguise(ctx,
														PlayerDisguiseAnimationType.WOLF_ANGRY))))
								.then(literal("frog-eat")
										.then(argument("ticks", integer(1))
												.then(argument("animationTarget", EntityArgumentType.entity())
														.executes(ctx -> animateDisguise(ctx,
																PlayerDisguiseAnimationType.FROG_EAT,
																EntityArgumentType.getEntity(ctx, "animationTarget"))))))
								.then(literal("goat-ram")
										.then(argument("ticks", integer(1))
												.executes(ctx -> animateDisguise(ctx,
														PlayerDisguiseAnimationType.GOAT_RAM))))
								.then(literal("ravager-attack")
										.then(argument("ticks", integer(1))
												.executes(ctx -> animateDisguise(ctx,
														PlayerDisguiseAnimationType.RAVAGER_ATTACK))))
								.then(literal("enderman-angry")
										.then(argument("ticks", integer(1))
												.executes(ctx -> animateDisguise(ctx,
														PlayerDisguiseAnimationType.ENDERMAN_ANGRY)))))
						.then(literal("as")
								.then(argument("disguise",
										new RegistryEntryReferenceArgumentType<>(commandRegistryAccess,
												RegistryKeys.ENTITY_TYPE))
										.suggests(SuggestionProviders.cast(SUMMONABLE_ENTITIES))
										.executes(DisguiseCommand::setDisguise)
										.then(argument("nbt", NbtCompoundArgumentType.nbtCompound())
												.executes(DisguiseCommand::setDisguise))))
						.then(literal("clear").executes(DisguiseCommand::clearDisguise)))
				.then(literal("option")
						.then(literal("player-nameplate")
								.executes(DisguiseCommand::queryPlayerNameplateOption)
								.then(literal("on").executes(ctx -> setPlayerNameplateOption(ctx, true)))
								.then(literal("off").executes(ctx -> setPlayerNameplateOption(ctx, false))))
						.then(literal("player-sneak")
								.executes(DisguiseCommand::queryPlayerSneakOption)
								.then(literal("on").executes(ctx -> setPlayerSneakOption(ctx, true)))
								.then(literal("off").executes(ctx -> setPlayerSneakOption(ctx, false))))));
	}

	private static int clearDisguise(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		Collection<? extends Entity> entities = EntityArgumentType.getEntities(ctx, "target");
		ServerCommandSource src = ctx.getSource();
		AtomicInteger successCount = new AtomicInteger(0);

		entities.forEach(entity -> {
			if (((EntityDisguise) entity).isDisguised()) {
				((EntityDisguise) entity).removeDisguise();
				successCount.incrementAndGet();
			}
		});

		int count = successCount.get();
		if (count > 0) {
			src.sendFeedback(() -> Text.literal("Cleared disguise from " + count + " entity(ies)")
					.formatted(Formatting.GREEN), true);
			return count;
		} else {
			src.sendError(Text.literal("No disguised entities found").formatted(Formatting.RED));
			return 0;
		}
	}

	private static int setDisguise(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		Collection<? extends Entity> entities = EntityArgumentType.getEntities(ctx, "target");
		ServerCommandSource src = ctx.getSource();
		var type = RegistryEntryReferenceArgumentType.getRegistryEntry(ctx, "disguise", RegistryKeys.ENTITY_TYPE);
		var disguiseId = Registries.ENTITY_TYPE.getId(type.value());

		// 플레이어 타입은 허용하지 않음
		if (type.value() == EntityType.PLAYER) {
			src.sendError(Text.literal("Disguising as player is not supported").formatted(Formatting.RED));
			return 0;
		}

		NbtCompound nbt;
		try {
			nbt = NbtCompoundArgumentType.getNbtCompound(ctx, "nbt").copy();
		} catch (IllegalArgumentException ignored) {
			nbt = new NbtCompound();
		}
		nbt.putString("id", disguiseId.toString());

		NbtCompound finalNbt = nbt;
		AtomicInteger successCount = new AtomicInteger(0);

		entities.forEach(entity -> EntityType.loadEntityWithPassengers(finalNbt, ctx.getSource().getWorld(),
				SpawnReason.LOAD, (entityx) -> {
					((EntityDisguise) entity).disguiseAs(entityx);
					successCount.incrementAndGet();
					return entityx;
				}));

		int count = successCount.get();
		if (count > 0) {
			String disguiseName = type.value().getName().getString();
			src.sendFeedback(() -> Text.literal("Disguised " + count + " entity(ies) as " + disguiseName)
					.formatted(Formatting.GREEN), true);
			return count;
		} else {
			src.sendError(Text.literal("Failed to disguise entities").formatted(Formatting.RED));
			return 0;
		}
	}

	private static int queryPlayerNameplateOption(CommandContext<ServerCommandSource> ctx) {
		boolean enabled = DisguiseLib.isPlayerDisguiseNameplateEnabled();
		ctx.getSource().sendFeedback(() -> Text.literal("플레이어 위장 이름표 옵션: " + (enabled ? "켜짐" : "꺼짐"))
				.formatted(enabled ? Formatting.GREEN : Formatting.YELLOW), false);
		return enabled ? 1 : 0;
	}

	private static int setPlayerNameplateOption(CommandContext<ServerCommandSource> ctx, boolean enabled) {
		boolean changed = DisguiseLib.setPlayerDisguiseNameplateEnabled(ctx.getSource().getServer(), enabled);
		if (changed) {
			ctx.getSource().sendFeedback(
					() -> Text.literal("플레이어 위장 이름표 옵션을 " + (enabled ? "켰음" : "껐음"))
							.formatted(Formatting.GREEN),
					true);
		} else {
			ctx.getSource().sendFeedback(
					() -> Text.literal("플레이어 위장 이름표 옵션이 이미 " + (enabled ? "켜져 있음" : "꺼져 있음"))
							.formatted(Formatting.YELLOW),
					false);
		}
		return Command.SINGLE_SUCCESS;
	}

	private static int queryPlayerSneakOption(CommandContext<ServerCommandSource> ctx) {
		boolean enabled = DisguiseLib.isPlayerSneakEnabled();
		ctx.getSource().sendFeedback(() -> Text.literal("플레이어 위장 웅크리기 옵션: " + (enabled ? "켜짐" : "꺼짐"))
				.formatted(enabled ? Formatting.GREEN : Formatting.YELLOW), false);
		return enabled ? 1 : 0;
	}

	private static int setPlayerSneakOption(CommandContext<ServerCommandSource> ctx, boolean enabled) {
		boolean changed = DisguiseLib.setPlayerSneakEnabled(ctx.getSource().getServer(), enabled);
		if (changed) {
			ctx.getSource().sendFeedback(
					() -> Text.literal("플레이어 위장 웅크리기 옵션을 " + (enabled ? "켰음" : "껐음"))
							.formatted(Formatting.GREEN),
					true);
		} else {
			ctx.getSource().sendFeedback(
					() -> Text.literal("플레이어 위장 웅크리기 옵션이 이미 " + (enabled ? "켜져 있음" : "꺼져 있음"))
							.formatted(Formatting.YELLOW),
					false);
		}
		return Command.SINGLE_SUCCESS;
	}

	private static int animateDisguise(CommandContext<ServerCommandSource> ctx, PlayerDisguiseAnimationType animationType)
			throws CommandSyntaxException {
		return animateDisguise(ctx, animationType, null);
	}

	private static int animateDisguise(CommandContext<ServerCommandSource> ctx, PlayerDisguiseAnimationType animationType,
			Entity animationTarget)
			throws CommandSyntaxException {
		Collection<? extends Entity> entities = EntityArgumentType.getEntities(ctx, "target");
		ServerCommandSource src = ctx.getSource();
		int ticks = IntegerArgumentType.getInteger(ctx, "ticks");
		AtomicInteger successCount = new AtomicInteger(0);
		AtomicInteger failureCount = new AtomicInteger(0);

		for (Entity entity : entities) {
			if (!(entity instanceof ServerPlayerEntity player)) {
				failureCount.incrementAndGet();
				continue;
			}

			EntityDisguise disguise = (EntityDisguise) player;
			if (!disguise.isDisguised()
					|| !PlayerDisguiseAnimationSupport.supports(
							Registries.ENTITY_TYPE.getId(disguise.getDisguiseType()).toString(),
							animationType)) {
				failureCount.incrementAndGet();
				continue;
			}

			PlayerDisguiseAnimationController.start(player, animationType, ticks, animationTarget);
			successCount.incrementAndGet();
		}

		int success = successCount.get();
		if (success > 0) {
			src.sendFeedback(
					() -> Text.literal(getAnimationName(animationType) + " 연출을 " + success + "명에게 적용했음")
							.formatted(Formatting.GREEN),
					true);
		}

		int failure = failureCount.get();
		if (failure > 0) {
			src.sendError(Text.literal(failure + "명은 현재 위장 타입과 맞지 않아서 적용하지 못했음")
					.formatted(Formatting.YELLOW));
		}

		if (success == 0) {
			return 0;
		}

		return success;
	}

	private static String getAnimationName(PlayerDisguiseAnimationType animationType) {
		return switch (animationType) {
			case GHAST_CHARGE -> "가스트 charging";
			case EVOKER_CAST -> "소환사 주문 시전";
			case ILLUSIONER_CAST -> "환술사 주문 시전";
			case ARMADILLO_ROLL -> "아르마딜로 웅크리기";
			case POLAR_BEAR_ATTACK -> "북극곰 공격";
			case BEE_ATTACK -> "꿀벌 공격";
			case FOX_POUNCE -> "여우 도약 공격";
			case WOLF_ANGRY -> "늑대 화난 얼굴";
			case FROG_EAT -> "개구리 잡아먹기";
			case GOAT_RAM -> "염소 돌진";
			case RAVAGER_ATTACK -> "파괴수 공격";
			case ENDERMAN_ANGRY -> "엔더맨 화난 상태";
		};
	}
}
