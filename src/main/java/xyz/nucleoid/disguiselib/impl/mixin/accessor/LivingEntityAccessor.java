package xyz.nucleoid.disguiselib.impl.mixin.accessor;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {

	@Invoker("getHurtSound")
	SoundEvent invokeGetHurtSound(DamageSource source);

	@Invoker("getDeathSound")
	SoundEvent invokeGetDeathSound();

	@Invoker("getSoundVolume")
	float invokeGetSoundVolume();

	@Invoker("getVoicePitch")
	float invokeGetSoundPitch();
}
