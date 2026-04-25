package xyz.nucleoid.disguiselib.impl;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import java.io.IOException;
import java.util.List;

import static org.apache.logging.log4j.LogManager.getLogger;

public class DisguiseLib {

	private static DisguiseLibConfig config = new DisguiseLibConfig();

	public static void init() {
		config = DisguiseLibConfig.load(FabricLoader.getInstance().getConfigDir());
		getLogger("DisguiseLib").info("DisguiseLib loaded.");

		CommandRegistrationCallback.EVENT.register(DisguiseCommand::register);
	}

	public static boolean isPlayerDisguiseNameplateEnabled() {
		return config.isPlayerDisguiseNameplate();
	}

	public static List<String> getPlayerDisguiseNameplateExcludedEntities() {
		return config.getPlayerDisguiseNameplateExcludedEntities();
	}

	public static boolean isPlayerDisguiseNameplateExcluded(EntityType<?> entityType) {
		String entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString();
		return config.getPlayerDisguiseNameplateExcludedEntities().stream().anyMatch(entityTypeId::equals);
	}

	public static boolean setPlayerDisguiseNameplateEnabled(MinecraftServer server, boolean enabled) {
		if (config.isPlayerDisguiseNameplate() == enabled) {
			return false;
		}

		config.setPlayerDisguiseNameplate(enabled);
		try {
			config.save(FabricLoader.getInstance().getConfigDir());
		} catch (IOException e) {
			getLogger("DisguiseLib").warn("플레이어 위장 이름표 설정 저장 실패", e);
		}

		DisguiseSync.refreshDisguisedPlayers(server);
		return true;
	}

	public static boolean isPlayerSneakEnabled() {
		return config.isPlayerSneak();
	}

	public static boolean setPlayerSneakEnabled(MinecraftServer server, boolean enabled) {
		if (config.isPlayerSneak() == enabled) {
			return false;
		}

		config.setPlayerSneak(enabled);
		try {
			config.save(FabricLoader.getInstance().getConfigDir());
		} catch (IOException e) {
			getLogger("DisguiseLib").warn("플레이어 위장 웅크리기 설정 저장 실패", e);
		}

		DisguiseSync.refreshDisguisedPlayers(server);
		return true;
	}
}
