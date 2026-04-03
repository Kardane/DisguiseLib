package xyz.nucleoid.disguiselib.impl;

import net.minecraft.entity.data.DataTracker;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DisguiseSyncTest {
	@Test
	void animationMetadataRefreshPacketUsesEntityIdAndEmptyTrackedValues() {
		var packet = DisguiseSync.createAnimationMetadataRefreshPacket(37);

		assertEquals(37, readField(packet, "id", Integer.class));
		assertEquals(List.<DataTracker.SerializedEntry<?>>of(), readField(packet, "trackedValues"));
	}

	@Test
	void animationMetadataRefreshNoOpsWhenNoTrackedPlayers() {
		assertEquals(0, DisguiseSync.sendAnimationMetadataRefreshToPlayers(List.of(), 91));
	}

	@SuppressWarnings("unchecked")
	private static <T> T readField(Object target, String name) {
		return readField(target, name, null);
	}

	@SuppressWarnings("unchecked")
	private static <T> T readField(Object target, String name, Class<T> type) {
		try {
			Field field = target.getClass().getDeclaredField(name);
			field.setAccessible(true);
			Object value = field.get(target);
			if (type != null) {
				return type.cast(value);
			}
			return (T) value;
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("필드 접근 실패: " + name, e);
		}
	}
}
