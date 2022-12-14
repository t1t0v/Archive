package naturalism.addon.netherbane.modules.render;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.DamageUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import naturalism.addon.netherbane.NetherBane;
import naturalism.addon.netherbane.utils.BlockUtil;
import naturalism.addon.netherbane.utils.EntityUtil;

import java.util.*;
import java.util.stream.Collectors;

public class DamageRender extends Module {
    public DamageRender(){
        super(NetherBane.RENDERPLUS, "damage-render", "");
    }

    public enum TargetMode{
        Name,
        Nearest
    }

    public enum DamageMode{
        Crystal,
        Bed,
        Anchor
    }


    private final SettingGroup sgDefault = settings.createGroup("Default");
    private final Setting<TargetMode> targetMode = sgDefault.add(new EnumSetting.Builder<TargetMode>().name("target-mode").description("Which way to swap.").defaultValue(TargetMode.Nearest).build());
    private final Setting<String> name = sgDefault.add(new StringSetting.Builder().name("name").description("Name to be replaced with.").defaultValue("WomanAreObjects").visible(() -> targetMode.get() == TargetMode.Name).build());
    private final Setting<DamageMode> damageMode = sgDefault.add(new EnumSetting.Builder<DamageMode>().name("damage-mode").description("Which way to swap.").defaultValue(DamageMode.Crystal).build());
    private final Setting<Double> enemyRange = sgDefault.add(new DoubleSetting.Builder().name("enemyRange").description("The range at which players can be targeted.").defaultValue(8).min(0.0).sliderMax(15).build());
    private final Setting<Double> placeRange = sgDefault.add(new DoubleSetting.Builder().name("placeRange").description("The range at which players can be targeted.").defaultValue(5).min(0.0).sliderMax(15).build());

    public final Setting<Double> minDamage = sgDefault.add(new DoubleSetting.Builder().name("min-damage").description("The range at which players can be targeted.").defaultValue(4.5).min(0.0).sliderMax(36).build());
    public final Setting<Double> maxDamage = sgDefault.add(new DoubleSetting.Builder().name("max-damage").description("The range at which players can be targeted.").defaultValue(6).min(0.0).sliderMax(36).build());

    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The side color.").defaultValue(new SettingColor(15, 255, 211,75)).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The line color.").defaultValue(new SettingColor(15, 255, 211)).build());

    private List<Pair<BlockPos, Double>> posAndDamage = new ArrayList<>();

    private List<BlockPos> renderArray = new ArrayList<>();

    @EventHandler
    private void onTick(TickEvent.Pre event){
        try {
            if (!posAndDamage.isEmpty()){
                posAndDamage.forEach(blockPosDoublePair -> {
                    if (BlockUtil.distance(new BlockPos(mc.player.getEyePos()), blockPosDoublePair.getLeft()) > placeRange.get()) posAndDamage.remove(blockPosDoublePair);
                });
            }
            if (!renderArray.isEmpty()){
                renderArray.forEach(blockPos -> {
                    if (BlockUtil.distance(new BlockPos(mc.player.getEyePos()), blockPos) > placeRange.get()) renderArray.remove(blockPos);
                });
            }
        }catch (ConcurrentModificationException ignored){}


        List<PlayerEntity> playerArray = EntityUtil.getTargetsInRange(enemyRange.get());
        if (targetMode.get() == TargetMode.Nearest){
            playerArray.sort(Comparator.comparing(player -> BlockUtil.distance(new BlockPos(mc.player.getEyePos()), player.getBlockPos())));
        }
        if (targetMode.get() == TargetMode.Name){
            playerArray.stream().filter(player -> Objects.equals(player.getEntityName(), name.get()));
        }
        if (playerArray.isEmpty()) return;

        PlayerEntity target = playerArray.get(0);

        Runnable task = () -> {
            try {
                renderArray = getRenderArray(findPos(getPosAround(), target));
            }catch (IndexOutOfBoundsException ignored){
            }
        };

        Thread thread = new Thread(task);
        if (thread.getThreadGroup().activeCount() > 1 || thread.getState() == Thread.State.TERMINATED){
            thread.start();
        }else {
            thread.interrupt();
        }
    }

    private List<BlockPos> getRenderArray(List<Pair<BlockPos, Double>> pair){
        List<BlockPos> array = new ArrayList<>();

        pair.forEach(blockPosDoublePair -> {
            if (blockPosDoublePair.getRight() >= minDamage.get() && blockPosDoublePair.getRight() <= maxDamage.get()){
                array.add(blockPosDoublePair.getLeft());
            }
        });

        return array;
    }

    private List<Pair<BlockPos, Double>> findPos(List<BlockPos> array, PlayerEntity player){
        List<Pair<BlockPos, Double>> pair = new ArrayList<>();

        array.forEach(blockPos -> {
            double damage = 0;
            switch (damageMode.get()){
                case Crystal -> damage = DamageUtils.crystalDamage(player, Utils.vec3d(blockPos));
                case Bed, Anchor -> damage = DamageUtils.bedDamage(player, Utils.vec3d(blockPos));
            }
            pair.add(new Pair<>(blockPos, damage));
        });

        return pair;
    }

    private List<BlockPos> getPosAround(){
        return BlockUtil.getSphere(new BlockPos(new BlockPos(mc.player.getEyePos())), placeRange.get().floatValue(), placeRange.get().intValue(), false, true, 0).stream().filter(this::filter).collect(Collectors.toList());
    }

    private boolean filter(BlockPos pos){
        if (BlockUtil.getBlock(pos) != Blocks.AIR && !(BlockUtil.getBlock(pos) instanceof BedBlock) && BlockUtil.getBlock(pos) != Blocks.RESPAWN_ANCHOR) return false;
        switch (damageMode.get()){
            case Crystal -> {
                if (!(mc.world.getBlockState(pos.down()).getBlock() == Blocks.BEDROCK
                    || mc.world.getBlockState(pos.down()).getBlock() == Blocks.OBSIDIAN)) return false;
            }
            case Bed -> {
                if (!BlockUtil.isAir(pos) && BlockUtil.getBlock(pos) != Blocks.LAVA && BlockUtil.getBlock(pos) != Blocks.FIRE) return false;
                if ((!BlockUtil.isAir(pos.west()) && BlockUtil.getBlock(pos.west()) != Blocks.LAVA && BlockUtil.getBlock(pos.west()) != Blocks.FIRE) &&
                    (!BlockUtil.isAir(pos.east()) && BlockUtil.getBlock(pos.east()) != Blocks.LAVA && BlockUtil.getBlock(pos.east()) != Blocks.FIRE) &&
                    (!BlockUtil.isAir(pos.south()) && BlockUtil.getBlock(pos.south()) != Blocks.LAVA && BlockUtil.getBlock(pos.south()) != Blocks.FIRE) &&
                    (!BlockUtil.isAir(pos.north()) && BlockUtil.getBlock(pos.north()) != Blocks.LAVA && BlockUtil.getBlock(pos.north()) != Blocks.FIRE)
                ) return false;
            }
        }
        return true;
    }

    @EventHandler
    private void onRender(Render3DEvent event){
        if (renderArray.isEmpty()) return;
        renderArray.forEach(blockPos -> {
            if (damageMode.get() == DamageMode.Crystal){
                event.renderer.box(blockPos.down(), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            }
            else event.renderer.box(blockPos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        });
    }

}
