package genesis.team.addon.modules.combat;

import genesis.team.addon.Genesis;
import genesis.team.addon.util.InfoUtil.BlockInfo;
import genesis.team.addon.util.InfoUtil.EntityInfo;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.misc.Vec3;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class AutoMinecart extends Module {
    private final SettingGroup sgDefault = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> range = sgDefault.add(new IntSetting.Builder().name("target-range").description("The range players can be targeted.").defaultValue(5).sliderRange(0, 7).build());
    private final Setting<Integer> delay = sgDefault.add(new IntSetting.Builder().name("delay").description("Resets the timer to 0 when minecraft was exploded.").defaultValue(40).sliderRange(15, 100).build());
    private final Setting<Boolean> rotate = sgDefault.add(new BoolSetting.Builder().name("rotate").description("Automatically faces towards the blocks being placed.").defaultValue(true).build());
    private final Setting<Boolean> swapBack = sgDefault.add(new BoolSetting.Builder().name("swap-back").description("Swaps to the previous slot after interact.").defaultValue(true).build());

    private final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder().name("swing").description("Whether to swing hand clientside clientside.").defaultValue(true).build());
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").description("Renders the block where it interacting.").defaultValue(true).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).visible(render::get).build());
    private final Setting<SettingColor> railSide = sgRender.add(new ColorSetting.Builder().name("rail-side").description("The side color of the target block rendering.").defaultValue(new SettingColor(144, 250, 255, 10)).visible(() -> render.get() && (shapeMode.get() == ShapeMode.Sides || shapeMode.get() == ShapeMode.Both)).build());
    private final Setting<SettingColor> railLine = sgRender.add(new ColorSetting.Builder().name("rail-line").description("The line color of the target block rendering.").defaultValue(new SettingColor(146, 255, 228)).visible(() -> render.get() && (shapeMode.get() == ShapeMode.Lines || shapeMode.get() == ShapeMode.Both)).build());
    private final Setting<SettingColor> cartSide = sgRender.add(new ColorSetting.Builder().name("cart-side").description("The side color of the target block rendering.").defaultValue(new SettingColor(197, 137, 232, 10)).visible(() -> render.get() && (shapeMode.get() == ShapeMode.Sides || shapeMode.get() == ShapeMode.Both)).build());
    private final Setting<SettingColor> cartLine = sgRender.add(new ColorSetting.Builder().name("cart-line").description("The line color of the target block rendering.").defaultValue(new SettingColor(197, 137, 232)).visible(() -> render.get() && (shapeMode.get() == ShapeMode.Lines || shapeMode.get() == ShapeMode.Both)).build());
    private final Setting<Boolean> delayRender = sgRender.add(new BoolSetting.Builder().name("render-delay").description("Render delay.").defaultValue(true).build());
    private final Setting<Double> delayScale = sgRender.add(new DoubleSetting.Builder().name("delay-scale").description("Scale of text.").defaultValue(1.5).min(0).sliderMax(3).build());
    private final Setting<SettingColor> delayColor = sgRender.add(new ColorSetting.Builder().name("delay-color").description("The color of the rendered text.").defaultValue(new SettingColor(255, 255, 255)).build());

    public AutoMinecart() {
        super(Genesis.Combat, "auto-minecart", "Places and blow's up minecarts in the target.");
    }

    // fields
    FindItemResult rails, tntMinecart, pickaxe, ignite;
    public PlayerEntity target;
    public BlockPos placePos;
    public int ticks = delay.get();

    @Override
    public void onActivate() {


        ticks = delay.get();
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        findRequiredItems(); // finds required items in inventory

        target = TargetUtils.getPlayerTarget(range.get(), SortPriority.LowestDistance); // finds target
        if (target == null) { // checks if target is null
            placePos = null;
            ticks = delay.get();
            return;
        }

        placePos = EntityInfo.getBlockPos(target); // set target pos
        //if (BlockUtilsNew.getBlock(placePos) != blo4ki.RAIL || BlockUtilsNew.getBlock(placePos) != blo4ki.AIR) return; // returns if pos is not air
        ticks--; // start counting ticks

        // safety
        if (!rails.found() || rails.slot() == -1 || !tntMinecart.found() || tntMinecart.slot() == -1 || !pickaxe.found() || pickaxe.slot() == -1 || !ignite.found() || ignite.slot() == -1) {
            ChatUtils.info(Utils.nameToTitle(title), "Items: " +
                (!rails.found() ? Formatting.RED + "rails, " : Formatting.GREEN + "rails, ") +
                (!tntMinecart.found() ? Formatting.RED + "tnt minecart, " : Formatting.GREEN + "tnt minecart, ") +
                (!pickaxe.found() ? Formatting.RED + "pickaxe, " : Formatting.GREEN + "pickaxe, ") +
                (!ignite.found() ? Formatting.RED + "ignite. " : Formatting.GREEN + "ignite. "));

            toggle();
            return;
        }

        BlockPos currentPos = placePos;

        // using if cuz variables in switch are not allowed
        if (ticks == delay.get() - 1) BlockUtils.place(currentPos, rails, rotate.get(), 50, swing.get(), false, swapBack.get());
        if (ticks == delay.get() - 2) Interaction.interactBlock(currentPos, tntMinecart.slot(), rotate.get(), swapBack.get(), swing.get(), false);
        if (ticks == delay.get() - 3) Interaction.breakBlock(currentPos, pickaxe.slot(), rotate.get(), false, swing.get(), false);
        if (ticks == delay.get() - 5) Interaction.interactBlock(currentPos.down(), ignite.slot(), rotate.get(), swapBack.get(), swing.get(), false);

        if (ticks <= 0) ticks = delay.get(); // resets timer
    }

    private void findRequiredItems() {
        rails = InvUtils.findInHotbar(Items.RAIL, Items.POWERED_RAIL, Items.DETECTOR_RAIL, Items.ACTIVATOR_RAIL);
        tntMinecart = InvUtils.findInHotbar(Items.TNT_MINECART);
        pickaxe = InvUtils.findInHotbar(itemStack -> itemStack.getItem() == Items.NETHERITE_PICKAXE || itemStack.getItem() == Items.DIAMOND_PICKAXE);
        ignite = InvUtils.findInHotbar(itemStack -> itemStack.getItem() == Items.FIRE_CHARGE || itemStack.getItem() == Items.FLINT_AND_STEEL);
    }

    private boolean isExploding() {
        for (Entity e : mc.world.getEntities()) {
            if (e instanceof TntMinecartEntity) {
                if (EntityInfo.getBlockPos(e).equals(placePos)) return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        if (!render.get() || target == null) return;
        int x = BlockInfo.X(placePos);
        int y = BlockInfo.Y(placePos);
        int z = BlockInfo.Z(placePos);

        event.renderer.box(x, y, z, x + 1, y + 0.15, z + 1, railSide.get(), railLine.get(), shapeMode.get(), 0); // rail render
        event.renderer.box(x, y + 1, z, x + 1, y + 1 - 0.85, z + 1, cartSide.get(), cartLine.get(), shapeMode.get(), 0); // cart render
    }

    @EventHandler
    private void onRender2D(final Render2DEvent event) {
        if (placePos == null || !delayRender.get() || target == null) return;

        Vec3 pos = new Vec3(placePos.getX() + 0.5, placePos.getY() + 0.5, placePos.getZ() + 0.5);
        if (NametagUtils.to2D(pos, delayScale.get())) {
            String progress;
            NametagUtils.begin(pos);
            TextRenderer.get().begin(1.0, false, true);
            progress = ticks + "";
            if (isExploding()) progress = "Exploding...";
            TextRenderer.get().render(progress, -TextRenderer.get().getWidth(progress) / 2.0, 0.0, delayColor.get());
            TextRenderer.get().end();
            NametagUtils.end();
        }
    }

    @Override
    public String getInfoString() {
        return target != null ? target.getGameProfile().getName() : null; // adds target name to the module array list
    }

    public static class Interaction {
        public static int prevSlot;
        public static MinecraftClient mc = MinecraftClient.getInstance();

        public static boolean interactBlock(BlockPos pos, int slot, boolean rotate, boolean swapBack, boolean swing, boolean debug) {
            prevSlot = mc.player.getInventory().selectedSlot;

            updateSlot(slot);
            if (debug) ChatUtils.info("tries to interact with " + pos);
            interact(pos, rotate);

            if (swing) mc.player.swingHand(Hand.MAIN_HAND);
            if (swapBack) updateSlot(prevSlot);
            return true;
        }

        private static boolean interact(BlockPos pos, boolean rotate) {
            if (rotate)
                Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), 50, () -> mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), Direction.UP, pos, true)));
            else
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), Direction.UP, pos, true));
            return true;
        }

        public static void updateSlot(FindItemResult result) {
            updateSlot(result.slot());
        }

        public static void updateSlot(int slot) {
            // updates slot on client and server side
            mc.player.getInventory().selectedSlot = slot;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        }

        public static boolean breakBlock(BlockPos pos, int slot, boolean rotate, boolean swapBack, boolean swing, boolean debug) {
            if (!(mc.player.getInventory().getStack(slot).getItem() instanceof PickaxeItem)) {
                if (debug) ChatUtils.info("Pickaxe not found.");
                return false;
            }
            updateSlot(slot);
            if (debug) ChatUtils.info("Starts breaking " + pos);
            if (rotate) Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), 50, () -> sendBreakPacket(pos));
            else sendBreakPacket(pos);

            if (swing) mc.player.swingHand(Hand.MAIN_HAND);
            if (swapBack) updateSlot(prevSlot);
            return true;
        }

        private static void sendBreakPacket(BlockPos pos) {
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, Direction.UP));
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP));
        }
    }
}
