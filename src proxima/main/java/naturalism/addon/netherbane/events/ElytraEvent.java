package naturalism.addon.netherbane.events;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.entity.Entity;

public class ElytraEvent extends Cancellable {
    private static ElytraEvent INSTANCE = new ElytraEvent();

    private Entity entity;

    public static ElytraEvent get(Entity entity) {
        INSTANCE.setCancelled(false);
        INSTANCE.entity = entity;
        return INSTANCE;
    }

    public Entity getEntity() {
        return entity;
    }
}
