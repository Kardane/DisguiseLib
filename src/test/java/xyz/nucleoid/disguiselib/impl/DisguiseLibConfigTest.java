package xyz.nucleoid.disguiselib.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisguiseLibConfigTest {
	private static final List<String> DEFAULT_EXCLUDED_ENTITIES = List.of(
			"minecraft:armor_stand",
			"minecraft:block_display",
			"minecraft:item_display",
			"minecraft:text_display");

	@TempDir
	Path tempDir;

	@Test
	void loadReturnsDefaultWhenFileIsMissing() {
		DisguiseLibConfig config = DisguiseLibConfig.load(this.tempDir);

		assertFalse(config.isPlayerDisguiseNameplate());
		assertFalse(config.isPlayerSneak());
		assertEquals(DEFAULT_EXCLUDED_ENTITIES, config.getPlayerDisguiseNameplateExcludedEntities());
	}

	@Test
	void saveAndLoadPersistsPlayerDisguiseNameplate() throws IOException {
		DisguiseLibConfig config = new DisguiseLibConfig();
		config.setPlayerDisguiseNameplate(true);
		config.setPlayerSneak(true);
		config.setPlayerDisguiseNameplateExcludedEntities(List.of("minecraft:pig"));

		config.save(this.tempDir);

		DisguiseLibConfig reloaded = DisguiseLibConfig.load(this.tempDir);
		assertTrue(reloaded.isPlayerDisguiseNameplate());
		assertTrue(reloaded.isPlayerSneak());
		assertEquals(List.of("minecraft:pig"), reloaded.getPlayerDisguiseNameplateExcludedEntities());
	}

	@Test
	void loadFillsDefaultExcludedEntitiesWhenFieldIsMissing() throws IOException {
		Files.writeString(DisguiseLibConfig.resolve(this.tempDir), """
				{
				  "playerDisguiseNameplate": true,
				  "playerSneak": true
				}
				""");

		DisguiseLibConfig config = DisguiseLibConfig.load(this.tempDir);

		assertTrue(config.isPlayerDisguiseNameplate());
		assertTrue(config.isPlayerSneak());
		assertEquals(DEFAULT_EXCLUDED_ENTITIES, config.getPlayerDisguiseNameplateExcludedEntities());
	}
}
