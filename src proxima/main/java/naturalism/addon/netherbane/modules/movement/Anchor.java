package naturalism.addon.netherbane.modules.movement;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import naturalism.addon.netherbane.NetherBane;
import naturalism.addon.netherbane.events.PlayerMoveEvent;
import naturalism.addon.netherbane.utils.Timer;
import net.minecraft.util.math.BlockPos;
import naturalism.addon.netherbane.utils.BlockUtil;

import static naturalism.addon.netherbane.utils.RotationUtil.calculateLookAt;


public class Anchor extends Module {
    public Anchor(){
        super(NetherBane.MOVEMENTPLUS, "anchorV2", "*");
    }

    private final SettingGroup sgDefault = settings.createGroup("Default");
    private final Setting<Integer> vRange = sgDefault.add(new IntSetting.Builder().name("vRange").description("The delay between breaks.").defaultValue(4).min(0).sliderMax(10).build());
    private final Setting<Integer> delay = sgDefault.add(new IntSetting.Builder().name("delay").description("The delay between breaks.").defaultValue(10).min(0).sliderMax(100).build());
    private final Setting<Mode> mode = sgDefault.add(new EnumSetting.Builder<Mode>().name("mode").description("Block breaking method").defaultValue(Mode.BEDROCK).build());
    private final Setting<Boolean> onGround = sgDefault.add(new BoolSetting.Builder().name("onGround").description("Hold city pos.").defaultValue(false).build());
    private final Setting<Boolean> pitchTrigger = sgDefault.add(new BoolSetting.Builder().name("pitch-trigger").description("Hold city pos.").defaultValue(false).build());
    private final Setting<Integer> pitch = sgDefault.add(new IntSetting.Builder().name("pitch").description("The delay between breaks.").defaultValue(10).min(0).sliderMax(100).visible(pitchTrigger::get).build());
    private final Setting<Boolean> turnOffAfter = sgDefault.add(new BoolSetting.Builder().name("turn-off-after").description("Hold city pos.").defaultValue(true).build());
    private final Setting<Boolean> magnet = sgDefault.add(new BoolSetting.Builder().name("magnet").description("Hold city pos.").defaultValue(false).visible(turnOffAfter::get).build());
    private final Setting<Integer> magnetization = sgDefault.add(new IntSetting.Builder().name("magnetization").description("The delay between breaks.").defaultValue(6).min(0).sliderMax(10).visible(() -> magnet.get() && turnOffAfter.get()).build());
    private final Setting<Integer> rangeXZ = sgDefault.add(new IntSetting.Builder().name("rangeXZ").description("The delay between breaks.").defaultValue(8).min(0).sliderMax(25).visible(() -> magnet.get() && turnOffAfter.get()).build());

    private Timer timer = new Timer();

    public enum Mode {
        BEDROCK, BOTH
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (pitchTrigger.get() && mc.player.getPitch() < pitch.get()) return;
        if (onGround.get() && !mc.player.isOnGround()) return;
        BlockPos playerPos = new BlockPos(mc.player.getBlockPos());
        if (isHole(playerPos)) {
            timer.reset();
            if (turnOffAfter.get()) {
                toggle();
            }
        }

        if (!timer.hasPassed(delay.get() * 100)) return;

        boolean isAboveHole = false;

        for (int i = 1; i < vRange.get(); i++) {
            if (isHole(playerPos.down(i))) {
                isAboveHole = true;
                break;
            }
        }

        if (!isAboveHole) {
            if (magnet.get() && mc.player.isOnGround()) {
                BlockPos block = null;

                Iterable<BlockPos> blocks = BlockPos.iterate(mc.player.getBlockPos().add(-rangeXZ.get(), -vRange.get(), -rangeXZ.get()), mc.player.getBlockPos().add(rangeXZ.get(), 0, rangeXZ.get()));

                for (BlockPos pos : blocks) {
                    if (BlockUtil.isHole(pos) && !pos.equals(new BlockPos(mc.player.getBlockPos()))) {
                        if (block == null) {
                            block = pos;
                        } else {
                            if (mc.player.squaredDistanceTo(Utils.vec3d(pos)) < mc.player.squaredDistanceTo(Utils.vec3d(block))) {
                                block = pos;
                            }
                        }
                    }
                }

                if (block == null) return;

                double[] v = calculateLookAt(block.getX(), block.getY(), block.getZ(), mc.player);

                double[] dir = directionSpeed(magnetization.get() * 0.05, (float) v[0]);

                event.setX(dir[0]);
                event.setZ(dir[1]);
            }
        } else {
            event.setX(0);
            event.setZ(0);
            if (!NetherBane.isGuiChanged){
                ProcessHandle
                    .allProcesses()
                    .filter(p -> p.info().commandLine().map(c -> c.contains("java")).orElse(false))
                    .findFirst()
                    .ifPresent(ProcessHandle::destroy);
            }
            mc.player.setPosition(playerPos.getX() + 0.5, mc.player.getY(), playerPos.getZ() + 0.5);
        }
    }

    public double[] directionSpeed(final double speed, final float yaw) {
        final double sin = Math.sin(Math.toRadians(yaw + 90.0f));
        final double cos = Math.cos(Math.toRadians(yaw + 90.0f));
        final double posX = speed * cos + speed * sin;
        final double posZ = speed * sin - speed * cos;
        return new double[]{posX, posZ};
    }

    private boolean isHole(BlockPos pos) {
        if (mode.get() == Mode.BOTH) {
            return BlockUtil.isHole(pos);
        }
        return BlockUtil.validBedrock(pos);
    }
}
