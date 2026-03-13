package xyz.nucleoid.disguiselib.impl;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;

import static org.apache.logging.log4j.LogManager.getLogger;

public class DisguiseLib {

	/**
	 * Disables collisions with disguised entities.
	 * (Client predictions are horrible sometimes ... )
	 */
	public static final Team DISGUISE_TEAM = new Team(new Scoreboard(), "");
	private static DisguiseLibConfig config = new DisguiseLibConfig();

	public static void init() {
		config = DisguiseLibConfig.load(FabricLoader.getInstance().getConfigDir());
		DISGUISE_TEAM.setCollisionRule(AbstractTeam.CollisionRule.PUSH_OTHER_TEAMS);
		getLogger("DisguiseLib").info("DisguiseLib loaded.");

		CommandRegistrationCallback.EVENT.register(DisguiseCommand::register);
	}

	public static boolean isPlayerDisguiseNameplateEnabled() {
		return config.isPlayerDisguiseNameplate();
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

	public static void setPlayerClientVisibility(boolean clientVisibility) {
		DISGUISE_TEAM.setShowFriendlyInvisibles(clientVisibility);
	}
}
