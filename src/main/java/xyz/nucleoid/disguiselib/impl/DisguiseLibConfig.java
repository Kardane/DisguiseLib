package xyz.nucleoid.disguiselib.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class DisguiseLibConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String FILE_NAME = "disguiselib.json";
	private static final List<String> DEFAULT_PLAYER_DISGUISE_NAMEPLATE_EXCLUDED_ENTITIES = List.of(
			"minecraft:armor_stand",
			"minecraft:block_display",
			"minecraft:item_display",
			"minecraft:text_display");

	private boolean playerDisguiseNameplate;
	private boolean playerSneak;
	private List<String> playerDisguiseNameplateExcludedEntities = new ArrayList<>(
			DEFAULT_PLAYER_DISGUISE_NAMEPLATE_EXCLUDED_ENTITIES);

	public boolean isPlayerDisguiseNameplate() {
		return this.playerDisguiseNameplate;
	}

	public void setPlayerDisguiseNameplate(boolean playerDisguiseNameplate) {
		this.playerDisguiseNameplate = playerDisguiseNameplate;
	}

	public boolean isPlayerSneak() {
		return this.playerSneak;
	}

	public void setPlayerSneak(boolean playerSneak) {
		this.playerSneak = playerSneak;
	}

	public List<String> getPlayerDisguiseNameplateExcludedEntities() {
		return List.copyOf(this.playerDisguiseNameplateExcludedEntities);
	}

	public void setPlayerDisguiseNameplateExcludedEntities(List<String> excludedEntities) {
		if (excludedEntities == null) {
			this.playerDisguiseNameplateExcludedEntities = new ArrayList<>(
					DEFAULT_PLAYER_DISGUISE_NAMEPLATE_EXCLUDED_ENTITIES);
			return;
		}

		this.playerDisguiseNameplateExcludedEntities = new ArrayList<>(excludedEntities);
	}

	public static DisguiseLibConfig load(Path configDir) {
		Path path = resolve(configDir);
		if (Files.notExists(path)) {
			return new DisguiseLibConfig();
		}

		try (Reader reader = Files.newBufferedReader(path)) {
			DisguiseLibConfig config = GSON.fromJson(reader, DisguiseLibConfig.class);
			if (config == null) {
				return new DisguiseLibConfig();
			}

			config.fillDefaults();
			return config;
		} catch (IOException | JsonParseException ignored) {
			return new DisguiseLibConfig();
		}
	}

	public void save(Path configDir) throws IOException {
		Path path = resolve(configDir);
		Files.createDirectories(path.getParent());
		try (Writer writer = Files.newBufferedWriter(path)) {
			GSON.toJson(this, writer);
		}
	}

	public static Path resolve(Path configDir) {
		return configDir.resolve(FILE_NAME);
	}

	private void fillDefaults() {
		if (this.playerDisguiseNameplateExcludedEntities == null) {
			this.playerDisguiseNameplateExcludedEntities = new ArrayList<>(
					DEFAULT_PLAYER_DISGUISE_NAMEPLATE_EXCLUDED_ENTITIES);
		}
	}
}
