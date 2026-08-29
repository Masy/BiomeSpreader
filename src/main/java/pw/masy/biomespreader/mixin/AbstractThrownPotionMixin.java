package pw.masy.biomespreader.mixin;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pw.masy.biomespreader.SplashPotionCallback;

@Mixin(AbstractThrownPotion.class)
public class AbstractThrownPotionMixin {

    /**
     * Event handler mixin for when a splash potion collides with a block or an entity.
     * <p>
     * The callbacks will be called after the splash potion has been discarded.
     * </p>
     * @param hitResult The hit result of the collision test.
     * @param ci Callback information of the mixin.
     */
    @Inject(method = "onHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/throwableitemprojectile/AbstractThrownPotion;discard()V", shift = At.Shift.AFTER), cancellable = true)
    private void onHit(final HitResult hitResult, CallbackInfo ci) {
        if (!(((Object) this) instanceof ThrownSplashPotion potion))
            return;

        InteractionResult result = SplashPotionCallback.EVENT.invoker().onSplash(potion);
        if (result != InteractionResult.FAIL)
            ci.cancel();
    }

}
