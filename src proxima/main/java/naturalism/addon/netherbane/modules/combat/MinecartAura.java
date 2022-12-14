package naturalism.addon.netherbane.modules.combat;


import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import naturalism.addon.netherbane.NetherBane;
import naturalism.addon.netherbane.utils.CumDetector;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import naturalism.addon.netherbane.utils.BlockUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MinecartAura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgDelays = settings.createGroup("Delays");
    private final SettingGroup sgTarget = settings.createGroup("Target");
    private final SettingGroup sgAutoMove = settings.createGroup("AutoMove");
    private final SettingGroup sgPause = settings.createGroup("Pause");
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgDev = settings.createGroup("Dev");

    //General
    private final Setting<Boolean> itemCheck = sgGeneral.add(new BoolSetting.Builder().name("item-check").description("Checks if need items is in hotbar.").defaultValue(true).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").description("Doing rotate thing.").defaultValue(false).build());
    private final Setting<BurrowReact> burrowReact = sgGeneral.add(new EnumSetting.Builder<BurrowReact>().name("burrow-react").description("Which way to react to burrow.").defaultValue(BurrowReact.Break).build());
    private final Setting<SwapMode> swapMode = sgGeneral.add(new EnumSetting.Builder<SwapMode>().name("swap-mode").description("Which way to swap.").defaultValue(SwapMode.Silent).build());
    private final Setting<Double> minHealth = sgGeneral.add(new DoubleSetting.Builder().name("min-health").description("The minimum health you have to be for Minecart Aura to work.").defaultValue(0).sliderMax(36).build());

    //Delays
    private final Setting<Integer> railDelay = sgDelays.add(new IntSetting.Builder().name("rail-place-delay").description("The delay between placing rails in ticks.").defaultValue(2).sliderMax(30).build());
    private final Setting<Integer> minecartDelay = sgDelays.add(new IntSetting.Builder().name("minecart-place-delay").description("The delay between placing minecarts in ticks.").defaultValue(25).sliderMax(30).build());
    private final Setting<Integer> railMineDelay = sgDelays.add(new IntSetting.Builder().name("rail-mine-delay").description("The delay between mining rails in ticks.").defaultValue(2).sliderMax(30).build());
    private final Setting<Integer> ignitionDelay = sgDelays.add(new IntSetting.Builder().name("ignition-delay").description("The delay between ignition minecarts in ticks.").defaultValue(2).sliderMin(2).sliderMax(30).build());

    //Target
    private final Setting<Double> range = sgTarget.add(new DoubleSetting.Builder().name("target-range").description("The maximum distance to target players.").defaultValue(4).min(0).build());
    private final Setting<SortPriority> priority = sgTarget.add(new EnumSetting.Builder<SortPriority>().name("target-priority").description("How to select the player to target.").defaultValue(SortPriority.LowestDistance).build());

    //Auto move
    private final Setting<Boolean> autoMove = sgAutoMove.add(new BoolSetting.Builder().name("auto-move").description("Move minecarts to selected slot.").defaultValue(true).build());
    private final Setting<Integer> autoMoveSlot = sgAutoMove.add(new IntSetting.Builder().name("auto-move-slot").description("The slot auto move moves minecarts to.").defaultValue(9).min(1).max(9).sliderMin(1).sliderMax(9).visible(autoMove::get).build());

    //Pause
    private final Setting<Boolean> pauseOnMine = sgPause.add(new BoolSetting.Builder().name("pause-on-mine").description("Pauses Minecart Aura when mining.").defaultValue(false).build());
    private final Setting<Boolean> pauseOnDrink = sgPause.add(new BoolSetting.Builder().name("pause-on-drink").description("Pauses Minecart Aura when drinking.").defaultValue(true).build());
    private final Setting<Boolean> pauseOnEat = sgPause.add(new BoolSetting.Builder().name("pause-on-eat").description("Pauses Minecart Aura when eating.").defaultValue(true).build());
    private final Setting<Boolean> onlyInHole = sgPause.add(new BoolSetting.Builder().name("only-in-hole").description("Disables if enemy isn't in a hole.").defaultValue(true).build());
    private final Setting<Boolean> doubles = sgPause.add(new BoolSetting.Builder().name("doubles").description("Can count double hole as surround.").defaultValue(true).visible(onlyInHole::get).build());
    private final Setting<Boolean> ownHole = sgPause.add(new BoolSetting.Builder().name("own-hole").description("Disables if enemy is in your hole.").defaultValue(true).build());

    //Render
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").description("Render the exploding.").defaultValue(false).build());
    private final Setting<RenderMode> renderMode = sgRender.add(new EnumSetting.Builder<RenderMode>().name("render-mode").description("Which way to render.").defaultValue(RenderMode.Fade).build());
    private final Setting<Integer> fadeDuration = sgRender.add(new IntSetting.Builder().name("fade-duration").description("The fade duration in ticks.").defaultValue(10).sliderMax(20).visible(render::get).visible(() -> renderMode.get() == RenderMode.Fade).build());
    private final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder().name("swing").description("Render swing.").defaultValue(false).build());
    private final Setting<SwingHand> swingHand = sgRender.add(new EnumSetting.Builder<SwingHand>().name("swing-hand").description("Which hand to swing.").defaultValue(SwingHand.Main).visible(render::get).visible(swing::get).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Sides).visible(render::get).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The side color.").defaultValue(new SettingColor(15, 255, 100, 30)).visible(render::get).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The line color.").defaultValue(new SettingColor(0, 0, 0)).visible(render::get).build());

    //Dev
    private final Setting<Boolean> debug = sgDev.add(new BoolSetting.Builder().name("debug").description("Shows debug info (devs only).").defaultValue(false).build());

    public MinecartAura() {
        super(NetherBane.COMBATPLUS, "minecart-aura-", "Rapes players with tnt minecarts. (made by Kightinum)");

    }

    private PlayerEntity target;
    private Vec3d playerPos;
    private BlockPos targetPos;
    private int railDelayLeft;
    private int minecartDelayLeft;
    private int railMineDelayLeft;
    private int ignitionDelayLeft;
    private int timer;
    private static int duration;
    private boolean didRailPlace;
    private boolean mining;
    private boolean didMinecart;
    private boolean didRailMine;
    private boolean didIgnition;
    private boolean firstTime;
    int prevSlot;

    private final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderBlocks = new ArrayList<>();

    public enum RenderMode {
        Fade,
        Always
    }

    public enum SwingHand {
        Main,
        Off
    }

    public enum BurrowReact {
        Disable,
        Break,
        Ignore
    }

    public enum SwapMode {
        Normal,
        Silent
    }

    @Override
    public void onActivate() {
        didRailPlace = false;
        didMinecart = false;
        didRailMine = false;
        didIgnition = true;
        mining = false;
        //firstTime = true;
        railDelayLeft = 0;
        minecartDelayLeft = 0;
        railMineDelayLeft = 0;
        ignitionDelayLeft = 0;
        target = null;
        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();
    }

    @Override
    public void onDeactivate() {
        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();
        mining = false;
        //firstTime = true;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        //Target
        if (TargetUtils.isBadTarget(target, range.get()))
            target = TargetUtils.getPlayerTarget(range.get(), priority.get());
        if (TargetUtils.isBadTarget(target, range.get())) return;
        FindItemResult minecart = InvUtils.findInHotbar(Items.TNT_MINECART);
        //gavno zalupa penis her davalka hui blyadina
        if (mc.player == null) return;

        Vec3d playerPos = mc.player.getPos();
        prevSlot = mc.player.getInventory().selectedSlot;
        duration = fadeDuration.get();
        targetPos = target.getBlockPos();
        renderBlocks.forEach(RenderBlock::tick);
        FindItemResult pick = InvUtils.find(itemStack -> itemStack.getItem() == Items.DIAMOND_PICKAXE || itemStack.getItem() == Items.NETHERITE_PICKAXE);
        FindItemResult sword = InvUtils.find(itemStack -> itemStack.getItem() == Items.DIAMOND_SWORD || itemStack.getItem() == Items.NETHERITE_SWORD);
        renderBlocks.removeIf(renderBlock -> renderBlock.ticks <= 0);
        Hand hand;
        if (swingHand.get() == SwingHand.Main) {
            hand = Hand.MAIN_HAND;
        } else {
            hand = Hand.OFF_HAND;
        }

        //Sam auto minecart. Da.
        if (didIgnition && railDelayLeft >= railDelay.get() && !mining) {
            BlockUtils.place(targetPos, InvUtils.findInHotbar(Items.RAIL), rotate.get(), 0, false);
            if (debug.get()) {
                error("Placed rail.");
            }
            didRailPlace = true;
            didIgnition = false;
            renderBlocks.add(renderBlockPool.get().set(targetPos));
            if (swing.get()) {
                mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
            }
            railDelayLeft = 0;
        } else if (didIgnition) {
            railDelayLeft ++;
        }

        if (didRailPlace && minecartDelayLeft >= minecartDelay.get() && !mining/* && firstTime */) {
            if (BlockUtil.getBlock(targetPos) == Blocks.RAIL) {
                InvUtils.swap(minecart.slot(), false);
                if (debug.get()) {
                    error("Swaped to minecart.");
                }
                mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(playerPos, Direction.UP, targetPos, false)));
                if (debug.get()) {
                    error("Interacted with rail.");
                }
                if (swapMode.get() == SwapMode.Silent) {
                    mc.player.getInventory().selectedSlot = prevSlot;
                    if (debug.get()) {
                        error("Swaped back.");
                    }
                }
                didRailPlace = false;
                didMinecart = true;
                renderBlocks.add(renderBlockPool.get().set(targetPos));
                if (swing.get()) {
                    mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
                }
                minecartDelayLeft = 0;
            }
        } else if (didRailPlace) {
            minecartDelayLeft ++;
        }

        /*    doMinecart(playerPos, targetPos, hand);
            }

                firstTime = false;
            } else if (didRailPlace && minecartDelayLeft >= minecartDelay.get() && !mining && !firstTime) {
                for (Entity entity : mc.world.getEntities()) {
                    if (entity instanceof TntMinecartEntity) {
                        BlockPos minecartPos = entity.getBlockPos();
                        if (minecartPos.equals(targetPos) && entity.isAlive()) {
                            doMinecart(playerPos, targetPos, hand);
                        }
                    }
                }
            }*/

        if (didMinecart && railMineDelayLeft >= railMineDelay.get() && !mining) {
            if (BlockUtil.getBlock(targetPos) == Blocks.RAIL) {
                InvUtils.swap(pick.slot(), false);
                if (debug.get()) {
                    error("Swaped to pick.");
                }
                mine(targetPos);
                if (debug.get()) {
                    error("Mined rail.");
                }
                didMinecart = false;
                didRailMine = true;
                renderBlocks.add(renderBlockPool.get().set(targetPos));
                if (swapMode.get() == SwapMode.Silent) {
                    mc.player.getInventory().selectedSlot = prevSlot;
                    if (debug.get()) {
                        error("Swaped back.");
                    }
                }
                if (swing.get()) {
                    mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
                }
                railMineDelayLeft = 0;
            }
        } else if (didMinecart) {
            railMineDelayLeft ++;
        }

        if (didRailMine && ignitionDelayLeft >= ignitionDelay.get() && !mining) {
            BlockUtils.place(targetPos, InvUtils.findInHotbar(Items.FLINT_AND_STEEL), rotate.get(), 0, false);
            if (debug.get()) {
                error("Ignited.");
            }
            didRailMine = false;
            didIgnition = true;
            renderBlocks.add(renderBlockPool.get().set(targetPos));
            if (swing.get()) {
                mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
            }
            ignitionDelayLeft = 0;
        } else if (didRailMine) {
            ignitionDelayLeft ++;
        }

        if (onlyInHole.get() && !CumDetector.isInPiramidka(targetPos, doubles.get())) { error("Enemy isn't in a hole. Disabling..."); toggle(); return; }
        if (ownHole.get() && CumDetector.isNetvision(targetPos)) { error("Enemy is in your hole. Netvision mode activated."); toggle(); return; }

        //Burrow fucker
        if (burrowReact.get() == BurrowReact.Disable && CumDetector.isRetarded(target) || CumDetector.isAnal(targetPos)) {
            error("Enemy is burrowed. Disabling.");
            toggle();
            return;
        } else if (burrowReact.get() == BurrowReact.Break && CumDetector.isCum(targetPos) && !CumDetector.isAnal(targetPos)) {
            InvUtils.swap(sword.slot(), false);
            mining = true;
            firstTime = true;
            if (debug.get()) {
                error("Swaped to sword.");
            }
            mine(targetPos);
            if (debug.get()) {
                error("Burrow mined by sword.");
            }
            if (swapMode.get() == SwapMode.Silent) {
                mc.player.getInventory().selectedSlot = prevSlot;
                if (debug.get()) {
                    error("Swaped back.");
                }
            }
        } else if (burrowReact.get() == BurrowReact.Break && !CumDetector.isAnal(targetPos) && CumDetector.isSwallow(targetPos) || CumDetector.isCockHard(targetPos)) {
            InvUtils.swap(pick.slot(), false);
            mining = true;
            firstTime = true;
            if (debug.get()) {
                error("Swaped to pick.");
            }
            mine(targetPos);
            if (debug.get()) {
                error("Burrow mined by pickaxe.");
            }
            if (swapMode.get() == SwapMode.Silent) {
                mc.player.getInventory().selectedSlot = prevSlot;
                if (debug.get()) {
                    error("Swaped back.");
                }
            }
        } else if (!CumDetector.isAnal(targetPos) && !CumDetector.isSwallow(targetPos) && !CumDetector.isCockHard(targetPos) && !CumDetector.isCum(targetPos)) {
            mining = false;
        } else if (CumDetector.isAnal(targetPos)) { error("Your enemy is a bedrock pidoras."); toggle(); return; }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        //Item check
        if (itemCheck.get()) {
            FindItemResult minecart = InvUtils.findInHotbar(Items.TNT_MINECART);
            FindItemResult rail = InvUtils.findInHotbar(Items.RAIL);
            FindItemResult flint = InvUtils.findInHotbar(Items.FLINT_AND_STEEL);
            FindItemResult pick = InvUtils.find(itemStack -> itemStack.getItem() == Items.DIAMOND_PICKAXE || itemStack.getItem() == Items.NETHERITE_PICKAXE);
            if (!minecart.found()) { error("No minecarts in hotbar!"); toggle(); return; }
            if (!rail.found()) { error("No rails in hotbar!"); toggle(); return; }
            if (!flint.found()) { error("No flint and steel in hotbar!"); toggle(); return; }
            if (!pick.found()) { error("No pickaxe in hotbar!"); toggle(); return; }
        }

        //Pause/disable
        if (PlayerUtils.shouldPause(pauseOnMine.get(), pauseOnEat.get(), pauseOnDrink.get())) return;
        if (EntityUtils.getTotalHealth(mc.player) <= minHealth.get()) return;

        //Auto Move
        if (autoMove.get()) {
            FindItemResult minecart = InvUtils.find(Items.TNT_MINECART);

            if (minecart.found() && minecart.slot() != autoMoveSlot.get() - 1) {
                InvUtils.move().from(minecart.slot()).toHotbar(autoMoveSlot.get() - 1);
            }
        }
    }

    private void mine(BlockPos pos) {
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, Direction.UP));
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP));
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onRender(Render3DEvent event) {
        if (render.get()) {
            if (renderMode.get() == RenderMode.Fade) {
                renderBlocks.sort(Comparator.comparingInt(o -> -o.ticks));
                renderBlocks.forEach(renderBlock -> renderBlock.render(event, sideColor.get(), lineColor.get(), shapeMode.get()));
            } else {
                event.renderer.box(targetPos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            }
        }
    }

    public static class RenderBlock {
        public BlockPos.Mutable pos = new BlockPos.Mutable();
        public int ticks;

        public RenderBlock set(BlockPos blockPos) {
            pos.set(blockPos);
            ticks = duration;

            return this;
        }

        public void tick() {
            ticks--;
        }

        public void render(Render3DEvent event, Color sides, Color lines, ShapeMode shapeMode) {
            int preSideA = sides.a;
            int preLineA = lines.a;

            sides.a *= (double) ticks / duration;
            lines.a *= (double) ticks / duration;

            event.renderer.box(pos, sides, lines, shapeMode, 0);

            sides.a = preSideA;
            lines.a = preLineA;
        }
    }
}
