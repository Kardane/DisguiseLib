package xyz.nucleoid.disguiselib.impl.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.nucleoid.disguiselib.impl.DisguiseProjectileHitResolver;

@Mixin(PersistentProjectileEntity.class)
public abstract class PersistentProjectileEntityMixin_DisguiseProjectileHit {
	@Shadow
	protected abstract boolean canHit(Entity entity);

	@Inject(method = "getEntityCollision(Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/hit/EntityHitResult;", at = @At("RETURN"), cancellable = true)
	private void disguiselib$redirectDisguiseHit(Vec3d start, Vec3d end,
			CallbackInfoReturnable<EntityHitResult> cir) {
		ProjectileEntity projectile = (ProjectileEntity) (Object) this;
		Box searchBox = projectile.getBoundingBox().stretch(projectile.getVelocity()).expand(1.0D);
		cir.setReturnValue(DisguiseProjectileHitResolver.resolve(projectile, start, end, searchBox, this::canHit,
				cir.getReturnValue()));
	}
}
