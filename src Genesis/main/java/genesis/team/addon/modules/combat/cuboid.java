package genesis.team.addon.modules.combat;

import genesis.team.addon.Genesis;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

import java.util.Collections;
import java.util.List;

public class cuboid extends Module {
    public enum SurroundMode {
        Simple,
        DoubleHeight,
        AntiCity,
        ClassicPyramid,
        AdvancedPyramid,
        Cube,
        None
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<SurroundMode> surroundMode = sgGeneral.add(new EnumSetting.Builder<SurroundMode>()
        .name("sarround-mode")
        .description("The way sarround will place.")
        .defaultValue(SurroundMode.Simple)
        .build()
    );

    private final Setting<Boolean> onlyOnGround = sgGeneral.add(new BoolSetting.Builder()
            .name("only-on-ground")
            .description("Works only when you standing on blocks.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> onlyWhenSneaking = sgGeneral.add(new BoolSetting.Builder()
            .name("only-when-sneaking")
            .description("Places blocks only after sneaking.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> turnOff = sgGeneral.add(new BoolSetting.Builder()
            .name("turn-off")
            .description("Toggles off when all blocks are placed.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> center = sgGeneral.add(new BoolSetting.Builder()
            .name("center")
            .description("Teleports you to the center of the block.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> disableOnJump = sgGeneral.add(new BoolSetting.Builder()
            .name("disable-on-jump")
            .description("Automatically disables when you jump.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> disableOnYChange = sgGeneral.add(new BoolSetting.Builder()
            .name("disable-on-y-change")
            .description("Automatically disables when your y level (step, jumping, atc).")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Automatically faces towards the obsidian being placed.")
            .defaultValue(true)
            .build()
    );

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
            .name("block")
            .description("What blocks to use for sarround.")
            .defaultValue(Collections.singletonList(Blocks.OBSIDIAN))
            .filter(this::blockFilter)
            .build()
    );

    private final BlockPos.Mutable blockPos = new BlockPos.Mutable();
    private boolean return_;

    public cuboid() {
        super(Genesis.Combat, "cuboid", "Sarrounds you in blocks to prevent you from taking lots of damage.");
    }

    @Override
    public void onActivate() {
        if (center.get()) PlayerUtils.centerPlayer();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if ((disableOnJump.get() && (mc.options.jumpKey.isPressed() || mc.player.input.jumping)) || (disableOnYChange.get() && mc.player.prevY < mc.player.getY())) {
            toggle();
            return;
        }

        if (onlyOnGround.get() && !mc.player.isOnGround()) return;
        if (onlyWhenSneaking.get() && !mc.options.sneakKey.isPressed()) return;

        // Place
        return_ = false;

        // Bottom
        boolean p1 = place(0, -1, 0);
        if (return_) return;

        if (surroundMode.get() == SurroundMode.Simple) {
            // Sides
            boolean p2 = place(1, 0, 0);
            if (return_) return;
            boolean p3 = place(-1, 0, 0);
            if (return_) return;
            boolean p4 = place(0, 0, 1);
            if (return_) return;
            boolean p5 = place(0, 0, -1);
            if (return_) return;
        }
        // Bottom
        else if (surroundMode.get() == SurroundMode.DoubleHeight) {
            boolean p2 = place(1, 0, 0);
            if (return_) return;
            boolean p3 = place(-1, 0, 0);
            if (return_) return;
            boolean p4 = place(0, 0, 1);
            if (return_) return;
            boolean p5 = place(0, 0, -1);
            if (return_) return;
            boolean p6 = place(1, 1, 0);
            if (return_) return;
            boolean p7 = place(-1, 1, 0);
            if (return_) return;
            boolean p8 = place(0, 1, 1);
            if (return_) return;
            boolean p9 = place(0, 1, -1);
            if (return_) return;
        }

        else if (surroundMode.get() == SurroundMode.AntiCity) {
            boolean p2 = place(1, 0, 0);
            if (return_) return;
            boolean p3 = place(0, 0, 1);
            if (return_) return;
            boolean p4 = place(-1, 0, 0);
            if (return_) return;
            boolean p5 = place(0, 0, -1);
            if (return_) return;
            boolean p6 = place(2, 0, 0);
            if (return_) return;
            boolean p7 = place(-2, 0, 0);
            if (return_) return;
            boolean p8 = place(0, 0, 2);
            if (return_) return;
            boolean p9 = place(0, 0, -2);
            if (return_) return;
            boolean p10 = place(1, 0, -1);
            if (return_) return;
            boolean p11 = place(1, 0, 1);
            if (return_) return;
            boolean p12 = place(-1, 0, 1);
            if (return_) return;
            boolean p13 = place(1, 0, -1);
            if (return_) return;
            boolean p14 = place(-1, 0, -1);
            if (return_) return;
        }

        else if (surroundMode.get() == SurroundMode.ClassicPyramid) {
            boolean p2 = place(1, 0, 0);
            if (return_) return;
            boolean p3 = place(0, 0, 1);
            if (return_) return;
            boolean p4 = place(-1, 0, 0);
            if (return_) return;
            boolean p5 = place(0, 0, -1);
            if (return_) return;
            boolean p6 = place(2, 0, 0);
            if (return_) return;
            boolean p7 = place(-2, 0, 0);
            if (return_) return;
            boolean p8 = place(0, 0, 2);
            if (return_) return;
            boolean p9 = place(0, 0, -2);
            if (return_) return;
            boolean p10 = place(1, 0, -1);
            if (return_) return;
            boolean p11 = place(1, 0, 1);
            if (return_) return;
            boolean p12 = place(-1, 0, 1);
            if (return_) return;
            boolean p13 = place(1, 0, -1);
            if (return_) return;
            boolean p14 = place(-1, 0, -1);
            if (return_) return;
            boolean p15 = place(0, 1, 1);
            if (return_) return;
            boolean p16 = place(0, 1, -1);
            if (return_) return;
            boolean p17 = place(1, 1, 0);
            if (return_) return;
            boolean p18 = place(-1, 1, 0);
            if (return_) return;
            boolean p19 = place(0, 2, 0);
            if (return_) return;
            boolean p20 = place(0, -2, 0);
            if (return_) return;
            boolean p21 = place(-1, 0, -1);
            if (return_) return;
            boolean p22 = place(0, -1, -1);
            if (return_) return;
            boolean p23 = place(-1, -1, 0);
            if (return_) return;
            boolean p24 = place(0, -1, 1);
            if (return_) return;
            boolean p25 = place(1, -1, 0);
            if (return_) return;
        }

        else if (surroundMode.get() == SurroundMode.AdvancedPyramid) {
            boolean p2 = place(1, 0, 0);
            if (return_) return;
            boolean p3 = place(0, 0, 1);
            if (return_) return;
            boolean p4 = place(-1, 0, 0);
            if (return_) return;
            boolean p5 = place(0, 0, -1);
            if (return_) return;
            boolean p6 = place(2, 0, 0);
            if (return_) return;
            boolean p7 = place(-2, 0, 0);
            if (return_) return;
            boolean p8 = place(0, 0, 2);
            if (return_) return;
            boolean p9 = place(0, 0, -2);
            if (return_) return;
            boolean p10 = place(1, 0, -1);
            if (return_) return;
            boolean p11 = place(1, 0, 1);
            if (return_) return;
            boolean p12 = place(-1, 0, 1);
            if (return_) return;
            boolean p13 = place(1, 0, -1);
            if (return_) return;
            boolean p14 = place(-1, 0, -1);
            if (return_) return;
            boolean p15 = place(0, 1, 1);
            if (return_) return;
            boolean p16 = place(0, 1, -1);
            if (return_) return;
            boolean p17 = place(1, 1, 0);
            if (return_) return;
            boolean p18 = place(-1, 1, 0);
            if (return_) return;
            boolean p19 = place(0, 2, 0);
            if (return_) return;
            boolean p20 = place(0, -2, 0);
            if (return_) return;
            boolean p21 = place(-1, 0, -1);
            if (return_) return;
            boolean p22 = place(0, -1, -1);
            if (return_) return;
            boolean p23 = place(-1, -1, 0);
            if (return_) return;
            boolean p24 = place(0, -1, 1);
            if (return_) return;
            boolean p25 = place(1, -1, 0);
            if (return_) return;
            boolean p26 = place(0, 2, 1);
            if (return_) return;
            boolean p27 = place(0, 2, -1);
            if (return_) return;
            boolean p28 = place(1, 2, 0);
            if (return_) return;
            boolean p29 = place(-1, 2, 0);
            if (return_) return;
            boolean p30 = place(0, 2, 0);
            if (return_) return;
            boolean p31 = place(0, 3, 0);
            if (return_) return;
            boolean p32 = place(0, -2, 0);
            if (return_) return;
        }

        else if (surroundMode.get() == SurroundMode.Cube) {
            boolean p2 = place(1, 0, 0);
            if (return_) return;
            boolean p3 = place(0, 0, 1);
            if (return_) return;
            boolean p4 = place(-1, 0, 0);
            if (return_) return;
            boolean p5 = place(0, 0, -1);
            if (return_) return;
            boolean p6 = place(1, -1, 0);
            if (return_) return;
            boolean p7 = place(0, -1, 1);
            if (return_) return;
            boolean p8 = place(-1, -1, 0);
            if (return_) return;
            boolean p9 = place(0, -1, -1);
            if (return_) return;
            boolean p10 = place(0, -1, 0);
            if (return_) return;
            boolean p11 = place(1, 0, -1);
            if (return_) return;
            boolean p12 = place(1, 0, 1);
            if (return_) return;
            boolean p13 = place(-1, 0, 1);
            if (return_) return;
            boolean p14 = place(-1, 0, -1);
            if (return_) return;
            boolean p15 = place(1, 0, -1);
            if (return_) return;
            boolean p16 = place(2, 0, 1);
            if (return_) return;
            boolean p17 = place(2, 0, -1);
            if (return_) return;
            boolean p18 = place(1, 0, 2);
            if (return_) return;
            boolean p19 = place(-1, 0, 2);
            if (return_) return;
            boolean p20 = place(-1, 0, -2);
            if (return_) return;
            boolean p21 = place(1, 0, -2);
            if (return_) return;
            boolean p22 = place(-2, 0, -1);
            if (return_) return;
            boolean p23 = place(-2, 0, 1);
            if (return_) return;
            boolean p24 = place(2, 0, 2);
            if (return_) return;
            boolean p25 = place(2, 0, 0);
            if (return_) return;
            boolean p26 = place(0, 0, 2);
            if (return_) return;
            boolean p27 = place(0, 0, -2);
            if (return_) return;
            boolean p28 = place(-2, 0, 0);
            if (return_) return;
            boolean p29 = place(-2, 0, 2);
            if (return_) return;
            boolean p30 = place(-2, 0, -2);
            if (return_) return;
            boolean p31 = place(2, 0, -2);
            if (return_) return;
            boolean p33 = place(1, 1, 1);
            if (return_) return;
            boolean p34 = place(1, 1, -1);
            if (return_) return;
            boolean p35 = place(-1, 1, -1);
            if (return_) return;
            boolean p36 = place(-1, 1, 1);
            if (return_) return;
            boolean p37 = place(0, 1, -2);
            if (return_) return;
            boolean p38 = place(0, 1, 2);
            if (return_) return;
            boolean p39 = place(2, 1, 0);
            if (return_) return;
            boolean p40 = place(-2, 1, 0);
            if (return_) return;
            boolean p41 = place(-1, 1, 2);
            if (return_) return;
            boolean p42 = place(-1, 1, -2);
            if (return_) return;
            boolean p43 = place(1, 1, 2);
            if (return_) return;
            boolean p44 = place(1, 1, -2);
            if (return_) return;
            boolean p45 = place(2, 1, -1);
            if (return_) return;
            boolean p46 = place(2, 1, 1);
            if (return_) return;
            boolean p47 = place(2, 1, 2);
            if (return_) return;
            boolean p48 = place(2, 1, -2);
            if (return_) return;
            boolean p49 = place(-2, 1, 1);
            if (return_) return;
            boolean p50 = place(-2, 1, -1);
            if (return_) return;
            boolean p51 = place(-2, 1, 2);
            if (return_) return;
            boolean p52 = place(-2, 1, -2);
            if (return_) return;
            boolean p53 = place(1, 2, 0);
            if (return_) return;
            boolean p54 = place(0, 2, 1);
            if (return_) return;
            boolean p55 = place(-1, 2, 0);
            if (return_) return;
            boolean p56 = place(0, 2, -1);
            if (return_) return;
            boolean p57 = place(-1, 2, -1);
            if (return_) return;
            boolean p58 = place(-1, 2, 1);
            if (return_) return;
            boolean p59 = place(1, 2, -1);
            if (return_) return;
            boolean p60 = place(1, 2, 1);
            if (return_) return;
            boolean p61 = place(2, 2, 0);
            if (return_) return;
            boolean p62 = place(0, 2, 2);
            if (return_) return;
            boolean p63 = place(-2, 2, 0);
            if (return_) return;
            boolean p64 = place(0, 2, -2);
            if (return_) return;
            boolean p65 = place(2, 2, 1);
            if (return_) return;
            boolean p66 = place(1, 2, 2);
            if (return_) return;
            boolean p67 = place(-1, 2, 2);
            if (return_) return;
            boolean p68 = place(-2, 2, -1);
            if (return_) return;
            boolean p69 = place(-1, 2, -2);
            if (return_) return;
            boolean p70 = place(1, 2, -2);
            if (return_) return;
            boolean p71 = place(2, 2, -1);
            if (return_) return;
            boolean p72= place(-2, 2, 1);
            if (return_) return;
            boolean p73 = place(2, 2, 2);
            if (return_) return;
            boolean p74 = place(-2, 2, 2);
            if (return_) return;
            boolean p75 = place(-2, 2, -2);
            if (return_) return;
            boolean p76 = place(2, 2, -2);
            if (return_) return;
            boolean p77 = place(0, 2, 0);
            if (return_) return;
            boolean p78 = place(1, 3, 0);
            if (return_) return;
            boolean p79 = place(0, 3, 1);
            if (return_) return;
            boolean p80 = place(-1, 3, 0);
            if (return_) return;
            boolean p81 = place(0, 3, -1);
            if (return_) return;
            boolean p82 = place(-1, 3, -1);
            if (return_) return;
            boolean p83 = place(-1, 3, 1);
            if (return_) return;
            boolean p84 = place(1, 3, -1);
            if (return_) return;
            boolean p85 = place(1, 3, 1);
            if (return_) return;
            boolean p86 = place(2, 3, 0);
            if (return_) return;
            boolean p87 = place(0, 3, 2);
            if (return_) return;
            boolean p88 = place(-2, 3, 0);
            if (return_) return;
            boolean p89 = place(0, 3, -2);
            if (return_) return;
            boolean p90 = place(2, 3, 1);
            if (return_) return;
            boolean p91 = place(1, 3, 2);
            if (return_) return;
            boolean p92 = place(-1, 3, 2);
            if (return_) return;
            boolean p93 = place(-2, 3, -1);
            if (return_) return;
            boolean p94 = place(-1, 3, -2);
            if (return_) return;
            boolean p95 = place(1, 3, -2);
            if (return_) return;
            boolean p96 = place(2, 3, -1);
            if (return_) return;
            boolean p97 = place(-2, 3, 1);
            if (return_) return;
            boolean p98 = place(2, 3, 2);
            if (return_) return;
            boolean p99 = place(-2, 3, 2);
            if (return_) return;
            boolean p100 = place(-2, 3, -2);
            if (return_) return;
            boolean p101 = place(2, 3, -2);
            if (return_) return;
            boolean p102 = place(0, 3, 0);
            if (return_) return;
            boolean p103 = place(1, -1, 1);
            if (return_) return;
            boolean p104 = place(1, -1, -1);
            if (return_) return;
            boolean p105 = place(-1, -1, -1);
            if (return_) return;
            boolean p106 = place(-1, -1, 1);
            if (return_) return;
            boolean p107 = place(0, -1, -2);
            if (return_) return;
            boolean p108 = place(0, -1, 2);
            if (return_) return;
            boolean p109 = place(2, -1, 0);
            if (return_) return;
            boolean p110 = place(-2, -1, 0);
            if (return_) return;
            boolean p111 = place(-1, -1, 2);
            if (return_) return;
            boolean p112 = place(-1, -1, -2);
            if (return_) return;
            boolean p113 = place(1, -1, 2);
            if (return_) return;
            boolean p114 = place(1, -1, -2);
            if (return_) return;
            boolean p115 = place(2, -1, -1);
            if (return_) return;
            boolean p116 = place(2, -1, 1);
            if (return_) return;
            boolean p117 = place(2, -1, 2);
            if (return_) return;
            boolean p118 = place(2, -1, -2);
            if (return_) return;
            boolean p119 = place(-2, -1, 1);
            if (return_) return;
            boolean p120 = place(-2, -1, -1);
            if (return_) return;
            boolean p121 = place(-2, -1, 2);
            if (return_) return;
            boolean p122 = place(-2, -1, -2);
            if (return_) return;
            boolean p123 = place(1, -2, 0);
            if (return_) return;
            boolean p124 = place(0, -2, 1);
            if (return_) return;
            boolean p125 = place(-1, -2, 0);
            if (return_) return;
            boolean p126 = place(0, -2, -1);
            if (return_) return;
            boolean p127 = place(0, 1, 1);
            if (return_) return;
            boolean p128 = place(0, 1, -1);
            if (return_) return;
            boolean p129 = place(1, 1, 0);
            if (return_) return;
            boolean p130 = place(-1, 1, 0);
            if (return_) return;
            boolean p131 = place(0, 2, 1);
            if (return_) return;
            boolean p132 = place(0, 2, -1);
            if (return_) return;
            boolean p133 = place(1, 2, 0);
            if (return_) return;
            boolean p134 = place(-1, 2, 0);
            if (return_) return;
        }
    }
    
    private boolean blockFilter(Block block) {
        return block == Blocks.OBSIDIAN ||
            block == Blocks.CRYING_OBSIDIAN ||
            block == Blocks.NETHERITE_BLOCK ||
            block == Blocks.ENDER_CHEST ||
            block == Blocks.RESPAWN_ANCHOR;
    }

    private boolean place(int x, int y, int z) {
        setBlockPos(x, y, z);
        BlockState blockState = mc.world.getBlockState(blockPos);

        if (!blockState.getMaterial().isReplaceable()) return true;

        if (BlockUtils.place(blockPos, InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem()))), rotate.get(), 100, true)) {
            return_ = true;
        }

        return false;
    }

    private void setBlockPos(int x, int y, int z) {
        blockPos.set(mc.player.getX() + x, mc.player.getY() + y, mc.player.getZ() + z);
    }
}