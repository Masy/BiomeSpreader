package pw.masy.biomespreader.mixin;

import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pw.masy.biomespreader.SplashPotionCallback;

@Mixin(PotionEntity.class)
public class PotionEntityMixin {

    /**
     * Event handler mixin for when a splash potion collides with a block or an entity.
     * <p>
     * The callbacks will be called after the splash potions has been discarded.
     * </p>
     * @param hitResult The hit result of the collision test.
     * @param ci Callback information of the mixin.
     */
    @Inject(method = "onCollision", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/thrown/PotionEntity;discard()V", shift = At.Shift.AFTER), cancellable = true)
    private void onCollision(final HitResult hitResult, CallbackInfo ci) {
        PotionEntity potion = (PotionEntity) (Object) this;
        ActionResult result = SplashPotionCallback.EVENT.invoker().onSplash(potion);
        if (result != ActionResult.FAIL)
            ci.cancel();
    }

}
