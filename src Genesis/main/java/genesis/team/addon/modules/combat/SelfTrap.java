package genesis.team.addon.modules.combat;

import genesis.team.addon.Genesis;
import genesis.team.addon.modules.info.Notifications;
import genesis.team.addon.util.CombatUtil.PacketUtils;
import genesis.team.addon.util.InfoUtil.BlockInfo;
import genesis.team.addon.util.InfoUtil.EntityInfo;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SelfTrap extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgMisc = settings.createGroup("Misc");
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgNone = settings.createGroup("");
    private final Setting<Notifications.Mode> notifications = sgNone.add(new EnumSetting.Builder<Notifications.Mode>().name("notifications").defaultValue(Notifications.Mode.Toast).build());

    // General
    private final Setting<Integer> bpt = sgGeneral.add(new IntSetting.Builder().name("blocks-per-tick").description("How many blocks can be placed per one tick.").defaultValue(3).sliderMin(1).sliderMax(5).build());
    private final Setting<TopMode> modes = sgGeneral.add(new EnumSetting.Builder<TopMode>().name("mode").description("Which positions to place.").defaultValue(TopMode.Head).build());
    private final Setting<Boolean> packet = sgGeneral.add(new BoolSetting.Builder().name("packet").description("Packet block placing method.").defaultValue(true).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").description("Sends rotation packets to the server when placing.").defaultValue(false).build());
    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder().name("block").description("Which blocks used for placing.").defaultValue(Collections.singletonList(Blocks.OBSIDIAN)).filter(this::blockFilter).build());

    //Misc
    private final Setting<Boolean> onlyOnGround = sgMisc.add(new BoolSetting.Builder().name("only-on-ground").description("Works only when you standing on blocks.").defaultValue(true).build());
    private final Setting<Boolean> disableOnTp = sgMisc.add(new BoolSetting.Builder().name("disable-on-tp").description("Automatically disables when you teleport (chorus or pearl).").defaultValue(true).build());
    private final Setting<Boolean> onlyHole = sgMisc.add(new BoolSetting.Builder().name("only-hole").description("Turning off self trap if player isnt in hole.").defaultValue(true).build());
    private final Setting<Boolean> pauseOnUse = sgMisc.add(new BoolSetting.Builder().name("pause-on-use").description("Pauses selftrap if players is using item(eating etc).").defaultValue(false).build());

    // Render
    private final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder().name("swing").description("Client side hand-swing").defaultValue(true).build());
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").description("Renders a block overlay where the obsidian will be placed.").defaultValue(true).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The color of the sides of the blocks being rendered.").defaultValue(new SettingColor(204, 0, 0, 10)).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The color of the lines of the blocks being rendered.").defaultValue(new SettingColor(204, 0, 0, 255)).build());

    private static final List<BlockPos> placePositions = new ArrayList<>();
    private int places;

    public SelfTrap() {
        super(Genesis.Combat, "self-trap-plus", "Places obsidian above your head.");
    }

    @Override
    public void onActivate() {
        if (!placePositions.isEmpty()) placePositions.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (onlyOnGround.get() && !mc.player.isOnGround()) return;
        if (onlyHole.get() && !EntityInfo.isInHole(mc.player)) {
            Notifications.send("Players isnt surrounded! disabling...", notifications);
            toggle();
            return;
        }
        if (pauseOnUse.get() && mc.player.isUsingItem()) return;
        FindItemResult block = InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem())));

        if (!block.found()) {
            placePositions.clear();
            return;
        }

        if (!placePositions.isEmpty()) placePositions.clear();

        findPlacePos();
        places = 0;

        if (placePositions.isEmpty()) return;

        for (BlockPos b : placePositions) {
            if (places <= bpt.get() && b != null) {
                if (packet.get()) {
                    PacketUtils.packetPlace(b, InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem()))), rotate.get(), swing.get());
                } else {
                    BlockUtils.place(b, block, rotate.get(), 50);
                }
                places++;
            }
        }
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof TeleportConfirmC2SPacket && disableOnTp.get()) toggle();
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get() || placePositions.isEmpty()) return;
        for (BlockPos pos : placePositions)
            event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }

    private void findPlacePos() {
        placePositions.clear();
        BlockPos pos = EntityInfo.getBlockPos(mc.player);

        switch (modes.get()) {
            case Full -> {
                add(pos.add(0, 2, 0));
                add(pos.add(1, 1, 0));
                add(pos.add(-1, 1, 0));
                add(pos.add(0, 1, 1));
                add(pos.add(0, 1, -1));
            }
            case AntiPiston -> {
                add(pos.add(0, 2, 0));
                add(pos.add(1, 1, 0));
                add(pos.add(-1, 1, 0));
                add(pos.add(0, 1, 1));
                add(pos.add(0, 1, -1));
                add(pos.add(1, 2, 0));
                add(pos.add(-1, 2, 0));
                add(pos.add(0, 2, 1));
                add(pos.add(0, 2, -1));
            }
            case Head -> add(pos.add(0, 2, 0));
            case antiFP -> {
                add(pos.add(1, 1, 0));
                add(pos.add(-1, 1, 0));
                add(pos.add(0, 1, 1));
                add(pos.add(0, 1, -1));
            }
            case antiCev -> {
                add(pos.add(0, 3, 0));
                add(pos.add(0, 2, 0));
                add(pos.add(1, 1, 0));
                add(pos.add(-1, 1, 0));
                add(pos.add(0, 1, 1));
                add(pos.add(0, 1, -1));
            }
            case antiCevMore -> {
                add(pos.add(0, 3, 0));
                add(pos.add(1, 2, 0));
                add(pos.add(-1, 2, 0));
                add(pos.add(0, 2, 1));
                add(pos.add(0, 2, -1));
            }
        }
    }

    private boolean blockFilter(Block block) {
        return BlockInfo.isCombatBlock(block);
    }

    private void add(BlockPos blockPos) {
        if (!placePositions.contains(blockPos) && mc.world.getBlockState(blockPos).getMaterial().isReplaceable() && mc.world.canPlace(Blocks.OBSIDIAN.getDefaultState(), blockPos, ShapeContext.absent()))
            placePositions.add(blockPos);
    }

    public enum TopMode {
        Full, Head, antiFP, antiCev, antiCevMore, AntiPiston
    }
}
