package xyz.nucleoid.disguiselib.impl;

import net.minecraft.entity.data.DataTracked;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandler;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisguiseSyncTest {
	@Test
	void animationMetadataRefreshPacketUsesEntityIdAndTrackedValues() {
		List<DataTracker.SerializedEntry<?>> entries = List.of(new DataTracker.SerializedEntry<>(
				3,
				TrackedDataHandler.<Boolean>create(null),
				false));
		var packet = DisguiseSync.createAnimationMetadataRefreshPacket(37, entries);

		assertEquals(37, readField(packet, "id", Integer.class));
		assertEquals(entries, readField(packet, "trackedValues"));
	}

	@Test
	void animationMetadataRefreshNoOpsWhenNoTrackedPlayers() {
		assertEquals(0, DisguiseSync.sendAnimationMetadataRefreshToPlayers(
				List.of(),
				91,
				List.of()));
	}

	@Test
	void animationMetadataEntriesIncludeDirtyDefaultValues() {
		var booleanHandler = TrackedDataHandler.<Boolean>create(null);
		var flagsData = new TrackedData<>(0, TrackedDataHandler.<Byte>create(null));
		var fillerData1 = new TrackedData<>(1, booleanHandler);
		var fillerData2 = new TrackedData<>(2, booleanHandler);
		var animationData = new TrackedData<>(3, booleanHandler);
		var dataTracker = createDataTracker(new DataTracker.Entry<?>[] {
				new DataTracker.Entry<>(flagsData, (byte) 0),
				new DataTracker.Entry<>(fillerData1, false),
				new DataTracker.Entry<>(fillerData2, false),
				new DataTracker.Entry<>(animationData, false)
		});

		dataTracker.set(animationData, true);
		dataTracker.getDirtyEntries();
		dataTracker.set(animationData, false);

		List<DataTracker.SerializedEntry<?>> entries = DisguiseSync.getAnimationMetadataEntries(dataTracker);

		assertEquals(1, entries.size());
		assertTrue(entries.stream().anyMatch(entry -> entry.id() == 3 && Boolean.FALSE.equals(entry.value())));
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

	private static DataTracker createDataTracker(DataTracker.Entry<?>[] entries) {
		try {
			Constructor<DataTracker> constructor = DataTracker.class.getDeclaredConstructor(
					DataTracked.class,
					DataTracker.Entry[].class);
			constructor.setAccessible(true);
			return constructor.newInstance(new TestDataTracked(), entries);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("DataTracker 생성 실패", e);
		}
	}

	private static final class TestDataTracked implements DataTracked {
		@Override
		public void onTrackedDataSet(TrackedData<?> data) {
		}

		@Override
		public void onDataTrackerUpdate(List<DataTracker.SerializedEntry<?>> entries) {
		}
	}
}
