package xyz.nucleoid.disguiselib.impl;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;
import xyz.nucleoid.disguiselib.api.EntityDisguise;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import static net.minecraft.commands.arguments.EntityArgument.entities;
import static net.minecraft.commands.synchronization.SuggestionProviders.SUMMONABLE_ENTITIES;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class DisguiseCommand {

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
			CommandBuildContext commandRegistryAccess,
			Commands.CommandSelection registrationEnvironment) {
		dispatcher.register(literal("disguise")
				.requires(DisguiseCommand::hasDisguisePermission)
				.then(argument("target", entities())
						.then(literal("as")
								.then(argument("disguise",
										new ResourceArgument<>(commandRegistryAccess,
												Registries.ENTITY_TYPE))
										.suggests(SuggestionProviders.cast(SUMMONABLE_ENTITIES))
										.executes(DisguiseCommand::setDisguise)
										.then(argument("nbt", CompoundTagArgument.compoundTag())
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

	private static int clearDisguise(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		Collection<? extends Entity> entities = EntityArgument.getEntities(ctx, "target");
		CommandSourceStack src = ctx.getSource();
		AtomicInteger successCount = new AtomicInteger(0);

		entities.forEach(entity -> {
			if (((EntityDisguise) entity).isDisguised()) {
				((EntityDisguise) entity).removeDisguise();
				successCount.incrementAndGet();
			}
		});

		int count = successCount.get();
		if (count > 0) {
			src.sendSuccess(() -> Component.literal("Cleared disguise from " + count + " entity(ies)")
					.withStyle(ChatFormatting.GREEN), true);
			return count;
		} else {
			src.sendFailure(Component.literal("No disguised entities found").withStyle(ChatFormatting.RED));
			return 0;
		}
	}

	private static int setDisguise(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		Collection<? extends Entity> entities = EntityArgument.getEntities(ctx, "target");
		CommandSourceStack src = ctx.getSource();
		var type = ResourceArgument.getResource(ctx, "disguise", Registries.ENTITY_TYPE);
		var disguiseId = BuiltInRegistries.ENTITY_TYPE.getKey(type.value());

		// 플레이어 타입은 허용하지 않음
		if (type.value() == EntityType.PLAYER) {
			src.sendFailure(Component.literal("Disguising as player is not supported").withStyle(ChatFormatting.RED));
			return 0;
		}

		CompoundTag nbt;
		try {
			nbt = CompoundTagArgument.getCompoundTag(ctx, "nbt").copy();
		} catch (IllegalArgumentException ignored) {
			nbt = new CompoundTag();
		}
		nbt.putString("id", disguiseId.toString());

		CompoundTag finalNbt = nbt;
		AtomicInteger successCount = new AtomicInteger(0);

		entities.forEach(entity -> EntityType.loadEntityRecursive(finalNbt, ctx.getSource().getLevel(),
				EntitySpawnReason.LOAD, (entityx) -> {
					((EntityDisguise) entity).disguiseAs(entityx);
					successCount.incrementAndGet();
					return entityx;
				}));

		int count = successCount.get();
		if (count > 0) {
			String disguiseName = type.value().getDescription().getString();
			src.sendSuccess(() -> Component.literal("Disguised " + count + " entity(ies) as " + disguiseName)
					.withStyle(ChatFormatting.GREEN), true);
			return count;
		} else {
			src.sendFailure(Component.literal("Failed to disguise entities").withStyle(ChatFormatting.RED));
			return 0;
		}
	}

	private static int queryPlayerNameplateOption(CommandContext<CommandSourceStack> ctx) {
		boolean enabled = DisguiseLib.isPlayerDisguiseNameplateEnabled();
		ctx.getSource().sendSuccess(() -> Component.literal("플레이어 위장 이름표 옵션: " + (enabled ? "켜짐" : "꺼짐"))
				.withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.YELLOW), false);
		return enabled ? 1 : 0;
	}

	private static int setPlayerNameplateOption(CommandContext<CommandSourceStack> ctx, boolean enabled) {
		boolean changed = DisguiseLib.setPlayerDisguiseNameplateEnabled(ctx.getSource().getServer(), enabled);
		if (changed) {
			ctx.getSource().sendSuccess(
					() -> Component.literal("플레이어 위장 이름표 옵션을 " + (enabled ? "켰음" : "껐음"))
							.withStyle(ChatFormatting.GREEN),
					true);
		} else {
			ctx.getSource().sendSuccess(
					() -> Component.literal("플레이어 위장 이름표 옵션이 이미 " + (enabled ? "켜져 있음" : "꺼져 있음"))
							.withStyle(ChatFormatting.YELLOW),
					false);
		}
		return Command.SINGLE_SUCCESS;
	}

	private static int queryPlayerSneakOption(CommandContext<CommandSourceStack> ctx) {
		boolean enabled = DisguiseLib.isPlayerSneakEnabled();
		ctx.getSource().sendSuccess(() -> Component.literal("플레이어 위장 웅크리기 옵션: " + (enabled ? "켜짐" : "꺼짐"))
				.withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.YELLOW), false);
		return enabled ? 1 : 0;
	}

	private static int setPlayerSneakOption(CommandContext<CommandSourceStack> ctx, boolean enabled) {
		boolean changed = DisguiseLib.setPlayerSneakEnabled(ctx.getSource().getServer(), enabled);
		if (changed) {
			ctx.getSource().sendSuccess(
					() -> Component.literal("플레이어 위장 웅크리기 옵션을 " + (enabled ? "켰음" : "껐음"))
							.withStyle(ChatFormatting.GREEN),
					true);
		} else {
			ctx.getSource().sendSuccess(
					() -> Component.literal("플레이어 위장 웅크리기 옵션이 이미 " + (enabled ? "켜져 있음" : "꺼져 있음"))
							.withStyle(ChatFormatting.YELLOW),
					false);
		}
		return Command.SINGLE_SUCCESS;
	}

	private static boolean hasDisguisePermission(CommandSourceStack source) {
		if (source.permissions() instanceof LevelBasedPermissionSet permissionSet) {
			return permissionSet.level().isEqualOrHigherThan(PermissionLevel.GAMEMASTERS);
		}

		return true;
	}
}
