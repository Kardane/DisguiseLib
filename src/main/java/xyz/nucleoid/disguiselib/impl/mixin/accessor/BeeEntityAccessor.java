package xyz.nucleoid.disguiselib.impl.mixin.accessor;

import net.minecraft.entity.passive.BeeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BeeEntity.class)
public interface BeeEntityAccessor {
	@Invoker("setNearTarget")
	void callSetNearTarget(boolean nearTarget);
}
