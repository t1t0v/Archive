package genesis.team.addon.modules.render;

import genesis.team.addon.Genesis;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Vec3;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;

import java.util.Random;

public class BlockSelectionV3 extends Module {
    public BlockSelectionV3(){
        super(Genesis.Render, "block-highlight", "*");

    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<String> logoText = sgGeneral.add(new StringSetting.Builder().name("logo-text").description("Name to be replaced with.").defaultValue("uOu").build());
    private final Setting<Integer> updateDelay = sgGeneral.add(new IntSetting.Builder().name("update-delay").description("The delay between placing beds in ticks.").defaultValue(5).min(0).sliderMax(100).build());
    private final Setting<Integer> alpha = sgGeneral.add(new IntSetting.Builder().name("alpha").description("The delay between placing beds in ticks.").defaultValue(255).min(0).sliderMax(255).build());
    private final Setting<Boolean> randomColor = sgGeneral.add(new BoolSetting.Builder().name("random-color").description("Will not place and break beds if they will kill you.").defaultValue(true).build());
    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder().name("line-color-1").description("").defaultValue(new SettingColor(255, 0, 190, 255)).build());
    private final Setting<SettingColor> lineColor2 = sgGeneral.add(new ColorSetting.Builder().name("line-color-2").description("").defaultValue(new SettingColor(130, 5, 185, 255)).build());
    private final Setting<SettingColor> lineColor3 = sgGeneral.add(new ColorSetting.Builder().name("line-color-3").description("").defaultValue(new SettingColor(255, 0, 190, 255)).build());
    private final Setting<SettingColor> lineColor4 = sgGeneral.add(new ColorSetting.Builder().name("line-color-4").description("").defaultValue(new SettingColor(130, 5, 185, 255)).build());
    private final Setting<Double> damageTextScale = sgGeneral.add(new DoubleSetting.Builder().name("damage-scale").description("How big the damage text should be.").defaultValue(1.25).min(1).sliderMax(4).build());

    private Color side1 = new Color(0, 0, 0, 10);
    private Color side2 = new Color(0, 0, 0, 10);
    private Color side3 = new Color(0, 0, 0, 10);
    private Color side4 = new Color(0, 0, 0, 10);
    private Color side5 = new Color(0, 0, 0, 10);
    private Color side6 = new Color(0, 0, 0, 10);
    private Color side7 = new Color(0, 0, 0, 10);
    private Color side8 = new Color(0, 0, 0, 10);
    private Color side9 = new Color(0, 0, 0, 10);
    private Color side10 = new Color(0, 0, 0, 10);
    private Color side11 = new Color(0, 0, 0, 10);
    private Color side12 = new Color(0, 0, 0, 10);

    private int updateTimer;
    private BlockPos selectedPos;

    @Override
    public void onActivate() {
        updateTimer = updateDelay.get();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event){
        if (!randomColor.get()){
            if (updateTimer <= 0){
                side1 = lineColor.get();
                side2 = lineColor.get();
                side3 = lineColor.get();

                side4 = lineColor2.get();
                side5 = lineColor2.get();
                side6 = lineColor2.get();

                side7 = lineColor3.get();
                side8 = lineColor3.get();
                side9 = lineColor3.get();

                side10 = lineColor4.get();
                side11 = lineColor4.get();
                side12 = lineColor4.get();
                updateTimer = updateDelay.get();
            }
            else updateTimer--;
            if (updateTimer * 2 <= 0){
                side1 = lineColor4.get();
                side2 = lineColor4.get();
                side3 = lineColor4.get();

                side4 = lineColor.get();
                side5 = lineColor.get();
                side6 = lineColor.get();

                side7 = lineColor2.get();
                side8 = lineColor2.get();
                side9 = lineColor2.get();

                side10 = lineColor3.get();
                side11 = lineColor3.get();
                side12 = lineColor3.get();
                updateTimer = updateDelay.get();
            }
            else updateTimer--;
            if (updateTimer * 3 <= 0){
                side1 = lineColor3.get();
                side2 = lineColor3.get();
                side3 = lineColor3.get();

                side4 = lineColor4.get();
                side5 = lineColor4.get();
                side6 = lineColor4.get();

                side7 = lineColor.get();
                side8 = lineColor.get();
                side9 = lineColor.get();

                side10 = lineColor2.get();
                side11 = lineColor2.get();
                side12 = lineColor2.get();
                updateTimer = updateDelay.get();
            }
            else updateTimer--;
            if (updateTimer * 4 <= 0){
                side1 = lineColor2.get();
                side2 = lineColor2.get();
                side3 = lineColor2.get();

                side4 = lineColor3.get();
                side5 = lineColor3.get();
                side6 = lineColor3.get();

                side7 = lineColor4.get();
                side8 = lineColor4.get();
                side9 = lineColor4.get();

                side10 = lineColor.get();
                side11 = lineColor.get();
                side12 = lineColor.get();
                updateTimer = updateDelay.get();
            }
            else updateTimer--;
        }

        if (randomColor.get()){
            if (updateTimer <= 0){
                side1 = new Color(new Random().ints(1, 1, 255).findFirst().getAsInt(), new Random().ints(1, 1, 255).findFirst().getAsInt(), new Random().ints(1, 1, 255).findFirst().getAsInt(), alpha.get());
                side2 = new Color(new Random().ints(1, 1, 255).findFirst().getAsInt(), new Random().ints(1, 1, 255).findFirst().getAsInt(), new Random().ints(1, 1, 255).findFirst().getAsInt(), alpha.get());
                side3 = new Color(new Random().ints(1, 1, 255).findFirst().getAsInt(), new Random().ints(1, 1, 255).findFirst().getAsInt(), new Random().ints(1, 1, 255).findFirst().getAsInt(), alpha.get());
                side4 = new Color(new Random().ints(1, 1, 255).findFirst().getAsInt(), new Random().ints(1, 1, 255).findFirst().getAsInt(), new Random().ints(1, 1, 255).findFirst().getAsInt(), alpha.get());
                side5 = new Color(new Random().ints(1, 1, 255).findFirst().getAsInt(), new Random().ints(1, 1, 255).findFirst().getAsInt(), new Random().ints(1, 1, 255).findFirst().getAsInt(), alpha.get());
                side6 = new Color(new Random().ints(1, 1, 255).findFirst().getAsInt(), new Random().ints(1, 1, 255).findFirst().getAsInt(), new Random().ints(1, 1, 255).findFirst().getAsInt(), alpha.get());
                side7 = new Color(new Random().ints(1, 1, 255).findFirst().getAsInt(), new Random().ints(1, 1, 255).findFirst().getAsInt(), new Random().ints(1, 1, 255).findFirst().getAsInt(), alpha.get());
                side8 = new Color(new Random().ints(1, 1, 255).findFirst().getAsInt(), new Random().ints(1, 1, 255).findFirst().getAsInt(), new Random().ints(1, 1, 255).findFirst().getAsInt(), alpha.get());
                side9 = new Color(new Random().ints(1, 1, 255).findFirst().getAsInt(), new Random().ints(1, 1, 255).findFirst().getAsInt(), new Random().ints(1, 1, 255).findFirst().getAsInt(), alpha.get());
                side10 = new Color(new Random().ints(1, 1, 255).findFirst().getAsInt(), new Random().ints(1, 1, 255).findFirst().getAsInt(), new Random().ints(1, 1, 255).findFirst().getAsInt(), alpha.get());
                side11 = new Color(new Random().ints(1, 1, 255).findFirst().getAsInt(), new Random().ints(1, 1, 255).findFirst().getAsInt(), new Random().ints(1, 1, 255).findFirst().getAsInt(), alpha.get());
                side12 = new Color(new Random().ints(1, 1, 255).findFirst().getAsInt(), new Random().ints(1, 1, 255).findFirst().getAsInt(), new Random().ints(1, 1, 255).findFirst().getAsInt(), alpha.get());
                updateTimer = updateDelay.get();
            }else updateTimer--;
        }
    }
    private final Vec3 vec31 = new Vec3();

    @EventHandler
    private void onRender2D(Render2DEvent event){
        if (mc.crosshairTarget == null || !(mc.crosshairTarget instanceof BlockHitResult result)) return;

        if (selectedPos != result.getBlockPos()){
            selectedPos = null;
        }

        if (selectedPos != null) {
            vec31.set(selectedPos.getX() + 0.5, selectedPos.getY() + 0.5, selectedPos.getZ() + 0.5);
            render(vec31);
        }
    }

    private void render(Vec3 vec3){
        if (NametagUtils.to2D(vec3, damageTextScale.get())) {
            NametagUtils.begin(vec3);
            TextRenderer.get().begin(1, true, true);

            String text = logoText.get();
            double w = TextRenderer.get().getWidth(text) / 2;
            TextRenderer.get().render(text, -w, 0, side1, true);

            TextRenderer.get().end();
            NametagUtils.end();
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.crosshairTarget == null || !(mc.crosshairTarget instanceof BlockHitResult result)) return;

        BlockPos pos = result.getBlockPos();

        BlockState state = mc.world.getBlockState(pos);
        VoxelShape shape = state.getOutlineShape(mc.world, pos);

        if (shape.isEmpty()) return;
        selectedPos = pos;
        //down
        event.renderer.line(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY(), pos.getZ(), side1, side1);

        event.renderer.line(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ() + 1, side2, side2);

        event.renderer.line(pos.getX(), pos.getY(), pos.getZ() + 1, pos.getX() + 1, pos.getY(), pos.getZ() + 1, side3, side3);

        event.renderer.line(pos.getX() + 1, pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY(), pos.getZ() + 1, side4, side4);


        //up
        event.renderer.line(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ(), side5, side5);

        event.renderer.line(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX(), pos.getY() + 1, pos.getZ() + 1, side6, side6);

        event.renderer.line(pos.getX(), pos.getY() + 1, pos.getZ() + 1, pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1, side7, side7);

        event.renderer.line(pos.getX() + 1, pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1, side8, side8);


        //sides
        event.renderer.line(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY() + 1, pos.getZ(), side9, side9);

        event.renderer.line(pos.getX() + 1, pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ(), side10, side10);

        event.renderer.line(pos.getX() + 1, pos.getY(), pos.getZ() + 1, pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1, side11, side11);

        event.renderer.line(pos.getX(), pos.getY(), pos.getZ() + 1, pos.getX(), pos.getY() + 1, pos.getZ() + 1, side12, side12);
    }

}
