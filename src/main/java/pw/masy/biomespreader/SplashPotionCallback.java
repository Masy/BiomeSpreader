package pw.masy.biomespreader;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;

public interface SplashPotionCallback {

    /**
     * Event for when a splash potion collides.
     */
    Event<SplashPotionCallback> EVENT = EventFactory.createArrayBacked(SplashPotionCallback.class,
            (listeners) -> (potion) -> {
                for (SplashPotionCallback listener : listeners) {
                    InteractionResult result = listener.onSplash(potion);
                    if (result != InteractionResult.PASS)
                        return result;
                }

                return InteractionResult.PASS;
            });

    /**
     * Method which is called for each registered splash potion callback.
     *
     * @param potion The potion entity that collided.
     * @return An interaction result.
     */
    InteractionResult onSplash(ThrownSplashPotion potion);

}
