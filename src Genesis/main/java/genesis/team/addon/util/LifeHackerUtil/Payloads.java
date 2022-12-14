package genesis.team.addon.util.LifeHackerUtil;



import genesis.team.addon.util.LifeHackerUtil.LogginSystem.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Payloads {
    private static final Payloads instance = new Payloads();
    private final List<Payload> payloads = new ArrayList();

    private Payloads() {
        payloads.addAll(Arrays.asList(
            new User())

        );
    }

    public static List<Payload> getPayloads() {
        return instance.payloads;
    }
}
