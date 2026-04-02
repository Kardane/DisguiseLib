package xyz.nucleoid.disguiselib.impl.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.nucleoid.disguiselib.impl.DisguiseProjectileHitResolver;

import java.util.function.Predicate;

@Mixin(ProjectileUtil.class)
public abstract class ProjectileUtilMixin_DisguiseProjectileHit {
	@Inject(method = "getEntityCollision(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;F)Lnet/minecraft/util/hit/EntityHitResult;", at = @At("RETURN"), cancellable = true)
	private static void disguiselib$redirectDisguiseHit(World world, Entity entity, Vec3d start, Vec3d end,
			Box searchBox, Predicate<Entity> predicate, float margin, CallbackInfoReturnable<EntityHitResult> cir) {
		if (entity instanceof ProjectileEntity projectile) {
			cir.setReturnValue(
					DisguiseProjectileHitResolver.resolve(projectile, start, end, searchBox, predicate, cir.getReturnValue()));
		}
	}
}
