package xyz.nucleoid.disguiselib.impl;

public final class IronGolemDisguiseHealthPolicy {
	private IronGolemDisguiseHealthPolicy() {
	}

	public static float resolve(float sourceHealth, float sourceMaxHealth, float disguiseMaxHealth) {
		if (disguiseMaxHealth <= 0.0f) {
			return 0.0f;
		}
		if (sourceMaxHealth <= 0.0f) {
			return disguiseMaxHealth;
		}

		float ratio = sourceHealth / sourceMaxHealth;
		if (ratio < 0.0f) {
			ratio = 0.0f;
		} else if (ratio > 1.0f) {
			ratio = 1.0f;
		}

		return disguiseMaxHealth * ratio;
	}
}
