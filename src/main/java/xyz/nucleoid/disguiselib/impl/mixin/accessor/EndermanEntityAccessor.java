package xyz.nucleoid.disguiselib.impl.mixin.accessor;

import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.mob.EndermanEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EndermanEntity.class)
public interface EndermanEntityAccessor {
	@Accessor("ANGRY")
	static TrackedData<Boolean> getAngry() {
		throw new AssertionError();
	}

	@Accessor("PROVOKED")
	static TrackedData<Boolean> getProvoked() {
		throw new AssertionError();
	}
}
