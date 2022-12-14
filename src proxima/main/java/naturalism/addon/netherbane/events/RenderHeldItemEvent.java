package naturalism.addon.netherbane.events;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;

public class RenderHeldItemEvent extends Cancellable {
    private RenderHeldItemEvent() {
    }

    public static class Invoke extends RenderHeldItemEvent {
    }

    public static class Cancelled extends RenderHeldItemEvent {
        Hand hand;
        MatrixStack matrices;

        public Cancelled(Hand hand, MatrixStack matrices) {
            this.hand = hand;
            this.matrices = matrices;
        }

        public Hand getHand() {
            return hand;
        }

        public MatrixStack getMatrices() {
            return matrices;
        }
    }
}
