package xyz.nucleoid.disguiselib.impl.mixin.accessor;

import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.mob.SpellcastingIllagerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SpellcastingIllagerEntity.class)
public interface SpellcastingIllagerEntityAccessor {
	@Accessor("SPELL")
	static TrackedData<Byte> getSpell() {
		throw new AssertionError();
	}
}
