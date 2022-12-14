package genesis.team.addon.modules.misc;

import genesis.team.addon.Genesis;
import genesis.team.addon.util.ProximaUtil.BlockUtil.BlockUtil;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class InstaMineBypass extends Module {
    public InstaMineBypass(){
        super(Genesis.Misc, "packet-mine-proxima", "");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").description("Faces the blocks being mined server side.").defaultValue(true).build());
    public final Setting<Boolean> instaMineBypass = sgGeneral.add(new BoolSetting.Builder().name("insta-mine-bypass").description("Faces the blocks being mined server side.").defaultValue(true).build());
    public final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("delay").description("The delay between placing beds in ticks.").defaultValue(9).min(0).sliderMax(20).build());

    // Render
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").description("Renders a block overlay on the block being broken.").defaultValue(true).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The color of the sides of the blocks being rendered.").defaultValue(new SettingColor(204, 0, 0, 10)).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The color of the lines of the blocks being rendered.").defaultValue(new SettingColor(204, 0, 0, 255)).build());

    public BlockPos.Mutable blockPos = new BlockPos.Mutable(0, -1, 0);
    private Direction direction;

    private int timer;

    @EventHandler
    private void onTick(TickEvent.Pre event){
        if (instaMineBypass.get()){
            if (BlockUtil.distance(mc.player.getBlockPos(), blockPos) > mc.interactionManager.getReachDistance()) return;
            if(timer <= 0){
                mc.interactionManager.updateBlockBreakingProgress(blockPos, direction);
                mc.interactionManager.cancelBlockBreaking();
                timer = delay.get();
            }else timer--;
        }
    }

    @Override
    public void onActivate() {
        timer = delay.get();
        blockPos.set(0, -1, 0);
    }

    @EventHandler
    private void onStartBreakingBlock(StartBreakingBlockEvent event) {
        timer = delay.get();
        direction = event.direction;
        blockPos.set(event.blockPos);
        cum();
        event.cancel();
    }

    private void cum() {
        if (!wontMine()) {
            if (rotate.get()) {
                Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), () -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction)));
                Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), () -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction)));
            } else {
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction));
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction));
            }
        }
    }

    private boolean wontMine() {
        return blockPos.getY() == -1 || !BlockUtils.canBreak(blockPos);
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get() || wontMine()) return;
        event.renderer.box(blockPos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }
}
