package xyz.nucleoid.disguiselib.impl.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nucleoid.disguiselib.api.EntityDisguise;
import xyz.nucleoid.disguiselib.impl.PlayerDisguiseAnimationController;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin_DisguiseAnimation {
	private static final int DISGUISELIB_VINDICATOR_ATTACK_TICKS = 6;

	@Inject(method = "swingHand(Lnet/minecraft/util/Hand;)V", at = @At("TAIL"))
	private void disguiselib$startVindicatorAttack(Hand hand, CallbackInfo ci) {
		if (hand != Hand.MAIN_HAND) {
			return;
		}

		ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
		EntityDisguise disguise = (EntityDisguise) player;
		if (!disguise.isDisguised() || disguise.getDisguiseType() != EntityType.VINDICATOR) {
			return;
		}

		PlayerDisguiseAnimationController.startVindicatorAttack(player, DISGUISELIB_VINDICATOR_ATTACK_TICKS);
	}
}
