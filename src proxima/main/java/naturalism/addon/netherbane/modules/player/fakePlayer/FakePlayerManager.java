package naturalism.addon.netherbane.modules.player.fakePlayer;


import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class FakePlayerManager {
    private static final List<FakePlayerEntity> fakePlayers = new ArrayList<>();

    private static List<PlayerProperties> array = new ArrayList<>();
    private static List<PlayerProperties> holdArray = new ArrayList<>();
    private static List<PlayerProperties> finalArray = new ArrayList<>();
    private static Vec3d startPos;

    public static void add(String name, float health, boolean copyInv) {
        FakePlayerEntity fakePlayer = new FakePlayerEntity(mc.player, name, health, copyInv);
        fakePlayer.spawn();
        fakePlayers.add(fakePlayer);
        startPos = fakePlayer.getPos();
    }

    public static void clear() {
        if (fakePlayers.isEmpty()) return;
        fakePlayers.forEach(FakePlayerEntity::despawn);
        finalArray.clear();
        array.clear();
        fakePlayers.clear();
    }

    public static void record(){
        double x = mc.player.getX() - startPos.x;
        double y = mc.player.getY() - startPos.y;
        double z = mc.player.getZ() - startPos.z;
        array.add(new PlayerProperties(mc.player.getYaw(), mc.player.getPitch(), mc.player.headYaw, mc.player.bodyYaw, startPos, x, y, z, mc.player.isSwimming(), mc.player.isSneaking()));
    }

    public static void stop(){
        finalArray = array;
        holdArray = array;
    }

    public static void load(){
        if (fakePlayers.isEmpty()) return;
        if (finalArray.isEmpty()) return;
        fakePlayers.forEach(fakePlayerEntity -> fakePlayerEntity.updatePosAndLook(finalArray.get(0).yaw, finalArray.get(0).pitch, finalArray.get(0).headYaw, finalArray.get(0).bodyYaw, finalArray.get(0).startPos, finalArray.get(0).x, finalArray.get(0).y, finalArray.get(0).z, finalArray.get(0).swimming, finalArray.get(0).sneaking));
        finalArray.remove(finalArray.get(0));
    }

    public static class PlayerProperties{
        public float yaw;
        public float pitch;
        public float headYaw;
        public float bodyYaw;
        public double x, y, z;
        public Vec3d startPos;
        public boolean swimming;
        public boolean sneaking;

        public PlayerProperties(float yaw, float pitch, float headYaw, float bodyYaw, Vec3d startPos, double x, double y, double z, boolean swimming, boolean sneaking){
            this.yaw = yaw;
            this.pitch = pitch;
            this.headYaw = headYaw;
            this.bodyYaw = bodyYaw;
            this.startPos = startPos;
            this.x = x;
            this.y = y;
             this.z = z;
            this.swimming = swimming;
            this.sneaking = sneaking;
        }
    }

    public static List<FakePlayerEntity> getPlayers() {
        return fakePlayers;
    }

    public static int size() {
        return fakePlayers.size();
    }
}
