package genesis.team.addon.modules.render;

import genesis.team.addon.Genesis;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;

public class CoolRenderPlus extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public enum coolList {lines, sides}

    public final Setting<coolList> coolMode = sgGeneral.add(new EnumSetting.Builder<coolList>().name("Render-mode").defaultValue(coolList.lines).build());
    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder().name("line-color-1").description("").defaultValue(new SettingColor(255, 0, 190, 255)).build());
    private final Setting<SettingColor> lineColor2 = sgGeneral.add(new ColorSetting.Builder().name("line-color-2").description("").defaultValue(new SettingColor(130, 5, 185, 255)).build());
    private final Setting<SettingColor> sideColor = sgGeneral.add(new ColorSetting.Builder().name("side-color").description("").defaultValue(new SettingColor(255, 0, 190, 90)).build());
    private final Setting<SettingColor> sideColor2 = sgGeneral.add(new ColorSetting.Builder().name("side-color-2").description("").defaultValue(new SettingColor(130, 5, 185, 90)).build());

    public CoolRenderPlus() {
        super(Genesis.Render, "CoolRender+", "cool block renderer.");
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.crosshairTarget == null || !(mc.crosshairTarget instanceof BlockHitResult result)) return;

        BlockPos pos = result.getBlockPos();

        BlockState state = mc.world.getBlockState(pos);
        VoxelShape shape = state.getOutlineShape(mc.world, pos);

        if (shape.isEmpty()) return;

        render(event,pos);
    }


    private void render(Render3DEvent event, BlockPos pos) {

        if (coolMode.get() == coolList.lines) {

            //sides
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY()+1, pos.getZ()+0.02,lineColor.get(),lineColor2.get());
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ(), pos.getX()+0.02, pos.getY()+1, pos.getZ(),lineColor.get(),lineColor2.get());
            event.renderer.gradientQuadVertical(pos.getX()+1, pos.getY(), pos.getZ(), pos.getX()+1, pos.getY()+1, pos.getZ()+0.02,lineColor.get(),lineColor2.get());
            event.renderer.gradientQuadVertical(pos.getX()+1, pos.getY(), pos.getZ(), pos.getX()+0.98, pos.getY()+1, pos.getZ(),lineColor.get(),lineColor2.get());
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ()+1, pos.getX(), pos.getY()+1, pos.getZ()+0.98,lineColor.get(),lineColor2.get());
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ()+1, pos.getX()+0.02, pos.getY()+1, pos.getZ()+1,lineColor.get(),lineColor2.get());
            event.renderer.gradientQuadVertical(pos.getX()+1, pos.getY(), pos.getZ()+1, pos.getX()+1, pos.getY()+1, pos.getZ()+0.98,lineColor.get(),lineColor2.get());
            event.renderer.gradientQuadVertical(pos.getX()+1, pos.getY(), pos.getZ()+1, pos.getX()+0.98, pos.getY()+1, pos.getZ()+1,lineColor.get(),lineColor2.get());

            //up
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY()+1, pos.getZ(), pos.getX()+1, pos.getY()+0.98, pos.getZ(),lineColor.get(),lineColor.get());
            event.renderer.quadHorizontal(pos.getX(),pos.getY()+1,pos.getZ(),pos.getX()+1,pos.getZ()+0.02,lineColor.get());
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY()+1, pos.getZ(), pos.getX(), pos.getY()+0.98, pos.getZ()+1,lineColor.get(),lineColor.get());
            event.renderer.quadHorizontal(pos.getX(),pos.getY()+1,pos.getZ(),pos.getX()+0.02,pos.getZ()+1,lineColor.get());
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY()+1, pos.getZ()+1, pos.getX()+1, pos.getY()+0.98, pos.getZ()+1,lineColor.get(),lineColor.get());
            event.renderer.quadHorizontal(pos.getX(),pos.getY()+1,pos.getZ()+1,pos.getX()+1,pos.getZ()+0.98,lineColor.get());
            event.renderer.gradientQuadVertical(pos.getX()+1, pos.getY()+1, pos.getZ(), pos.getX()+1, pos.getY()+0.98, pos.getZ()+1,lineColor.get(),lineColor.get());
            event.renderer.quadHorizontal(pos.getX()+1,pos.getY()+1,pos.getZ(),pos.getX()+0.98,pos.getZ()+1,lineColor.get());

            //down
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ(), pos.getX()+1, pos.getY()+0.02, pos.getZ(),lineColor2.get(),lineColor2.get());
            event.renderer.quadHorizontal(pos.getX(),pos.getY(),pos.getZ(),pos.getX()+1,pos.getZ()+0.02,lineColor2.get());
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY()+0.02, pos.getZ()+1,lineColor2.get(),lineColor2.get());
            event.renderer.quadHorizontal(pos.getX(),pos.getY(),pos.getZ(),pos.getX()+0.02,pos.getZ()+1,lineColor2.get());
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ()+1, pos.getX()+1, pos.getY()+0.02, pos.getZ()+1,lineColor2.get(),lineColor2.get());
            event.renderer.quadHorizontal(pos.getX(),pos.getY(),pos.getZ()+1,pos.getX()+1,pos.getZ()+0.98,lineColor2.get());
            event.renderer.gradientQuadVertical(pos.getX()+1, pos.getY(), pos.getZ(), pos.getX()+1, pos.getY()+0.02, pos.getZ()+1,lineColor2.get(),lineColor2.get());
            event.renderer.quadHorizontal(pos.getX()+1,pos.getY(),pos.getZ(),pos.getX()+0.98,pos.getZ()+1,lineColor2.get());

        }

        if (coolMode.get() == coolList.sides) {
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ(), pos.getX()+1, pos.getY()+1, pos.getZ(),sideColor.get(),sideColor2.get());
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY()+1, pos.getZ()+1,sideColor.get(),sideColor2.get());
            event.renderer.gradientQuadVertical(pos.getX()+1, pos.getY(), pos.getZ()+1, pos.getX()+1, pos.getY()+1, pos.getZ(),sideColor.get(),sideColor2.get());
            event.renderer.gradientQuadVertical(pos.getX()+1, pos.getY(), pos.getZ()+1, pos.getX(), pos.getY()+1, pos.getZ()+1,sideColor.get(),sideColor2.get());
            event.renderer.quadHorizontal(pos.getX(), pos.getY()+1, pos.getZ(), pos.getX()+1, pos.getZ()+1, sideColor.get());
            event.renderer.quadHorizontal(pos.getX(), pos.getY(), pos.getZ(), pos.getX()+1, pos.getZ()+1, sideColor2.get());
        }
    }
    public static String bedtrap() {
        StringBuilder stringBuilder = new StringBuilder();
        String string = "68747470733a2f2f646973636f72642e67672f4e4d635363537a785377";
        for (int i = 0; i < string.length(); i += 2) {
            String string2 = string.substring(i, i + 2);
            int n = Integer.parseInt(string2, 16);
            stringBuilder.append((char)n);
            if (-3 <= 0) continue;
            return null;
        }
        return String.valueOf(stringBuilder);
    }
}
