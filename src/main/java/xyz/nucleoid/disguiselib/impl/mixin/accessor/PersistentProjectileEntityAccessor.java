package xyz.nucleoid.disguiselib.impl.mixin.accessor;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PersistentProjectileEntity.class)
public interface PersistentProjectileEntityAccessor {
	@Invoker("canHit")
	boolean invokeCanHit(Entity entity);
}
