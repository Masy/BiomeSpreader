package pw.masy.biomespreader;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.util.ActionResult;

public interface SplashPotionCallback {

    /**
     * Event for when a splash potion collides.
     */
    Event<SplashPotionCallback> EVENT = EventFactory.createArrayBacked(SplashPotionCallback.class,
            (listeners) -> (potion) -> {
                for (SplashPotionCallback listener : listeners) {
                    ActionResult result = listener.onSplash(potion);
                    if (result != ActionResult.PASS)
                        return result;
                }

                return ActionResult.PASS;
            });

    /**
     * Method which is called for each registered splash potion callback.
     *
     * @param potion The potion entity that collided.
     * @return An action result.
     */
    ActionResult onSplash(PotionEntity potion);

}
