package xyz.nucleoid.disguiselib.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisguiseLibConfigTest {
	@TempDir
	Path tempDir;

	@Test
	void loadReturnsDefaultWhenFileIsMissing() {
		DisguiseLibConfig config = DisguiseLibConfig.load(this.tempDir);

		assertFalse(config.isPlayerDisguiseNameplate());
		assertFalse(config.isPlayerSneak());
	}

	@Test
	void saveAndLoadPersistsPlayerDisguiseNameplate() throws IOException {
		DisguiseLibConfig config = new DisguiseLibConfig();
		config.setPlayerDisguiseNameplate(true);
		config.setPlayerSneak(true);

		config.save(this.tempDir);

		DisguiseLibConfig reloaded = DisguiseLibConfig.load(this.tempDir);
		assertTrue(reloaded.isPlayerDisguiseNameplate());
		assertTrue(reloaded.isPlayerSneak());
	}
}
