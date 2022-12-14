package genesis.team.addon.events;


import genesis.team.addon.util.ProximaUtil.KeyUtil;

public class KeyEvent {
    private static final KeyEvent INSTANCE = new KeyEvent();

    public int key;
    public KeyUtil.Action action;

    public static KeyEvent getInstance(int key, KeyUtil.Action action) {
        INSTANCE.key = key;
        INSTANCE.action = action;
        return INSTANCE;
    }
}
