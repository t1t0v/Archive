package genesis.team.addon.modules.bbc.event;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;

public class PlayerMoveEvent extends Cancellable {
    private static final PlayerMoveEvent INSTANCE = new PlayerMoveEvent();

    public MovementType type;
    public Vec3d movement;

    public static PlayerMoveEvent get(MovementType type, Vec3d movement) {
        INSTANCE.type = type;
        INSTANCE.movement = movement;
        return INSTANCE;
    }
}
