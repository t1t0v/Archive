package genesis.team.addon.modules.bbc.event;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.util.math.BlockPos;

public class BreakBlockEvent extends Cancellable {
    private final BlockPos pos;

    public BreakBlockEvent(BlockPos pos) {
        this.pos = pos;
    }

    public BlockPos getPos() {
        return pos;
    }
}
