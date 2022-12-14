package genesis.team.addon.events;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.entity.MovementType;

public class PlayerMoveEvent extends Cancellable {
    private static final PlayerMoveEvent INSTANCE = new PlayerMoveEvent();
    private MovementType type;
    private double x;
    private double y;
    private double z;

    public static PlayerMoveEvent get(MovementType type, double x, double y, double z) {
        INSTANCE.setCancelled(false);
        INSTANCE.type = type;
        INSTANCE.x = x;
        INSTANCE.y = y;
        INSTANCE.z = z;
        return INSTANCE;
    }

    public MovementType  getType() {
        return type;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public void setType(MovementType  type) {
        this.type = type;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setZ(double z) {
        this.z = z;
    }
}
