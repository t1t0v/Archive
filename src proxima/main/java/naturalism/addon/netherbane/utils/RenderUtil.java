package naturalism.addon.netherbane.utils;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.util.math.BlockPos;

public class RenderUtil {
    public static void X(Render3DEvent event, BlockPos pos, double x, double z, Color c1, Color c2){

        event.renderer.line(pos.getX(), pos.getY(), pos.getZ(), pos.getX()+1, pos.getY()+1, pos.getZ() ,c1,c2);
        event.renderer.line(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX()+1, pos.getY(), pos.getZ() ,c1,c2);

        event.renderer.line(pos.getX() + 1, pos.getY(), pos.getZ(), pos.getX()+1, pos.getY()+1, pos.getZ() + 1 ,c1,c2);
        event.renderer.line(pos.getX() + 1, pos.getY() + 1, pos.getZ(), pos.getX()+1, pos.getY(), pos.getZ() + 1 ,c1,c2);

        event.renderer.line(pos.getX(), pos.getY(), pos.getZ()+1, pos.getX()+1, pos.getY()+1, pos.getZ()+1 ,c1,c2);
        event.renderer.line(pos.getX(), pos.getY() + 1, pos.getZ()+1, pos.getX()+1, pos.getY(), pos.getZ()+ 1,c1,c2);

        event.renderer.line(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY()+1, pos.getZ() + 1 ,c1,c2);
        event.renderer.line(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX(), pos.getY(), pos.getZ() + 1 ,c1,c2);
    }

    public static void FS(Render3DEvent event, BlockPos pos, double y, boolean top, boolean bottom, Color c1, Color c2){
        event.renderer.gradientQuadVertical(pos.getX(), pos.getY() + y, pos.getZ(), pos.getX()+1, pos.getY()+1, pos.getZ(),c1,c2);
        event.renderer.gradientQuadVertical(pos.getX(), pos.getY() + y, pos.getZ(), pos.getX(), pos.getY()+1, pos.getZ()+1,c1,c2);
        event.renderer.gradientQuadVertical(pos.getX()+1, pos.getY() + y, pos.getZ()+1, pos.getX()+1, pos.getY()+1, pos.getZ(),c1,c2);
        event.renderer.gradientQuadVertical(pos.getX()+1, pos.getY() + y, pos.getZ()+1, pos.getX(), pos.getY()+1, pos.getZ()+1,c1,c2);
        if (top){
            event.renderer.quadHorizontal(pos.getX(), pos.getY()+1, pos.getZ(), pos.getX()+1, pos.getZ()+1, c1);
        }
        if (bottom){
            event.renderer.quadHorizontal(pos.getX(), pos.getY(), pos.getZ(), pos.getX()+1, pos.getZ()+1, c1);
        }
    }

    public static void S(Render3DEvent event, BlockPos pos, double x, double y, double z , Color c1, Color c2){
        event.renderer.gradientQuadVertical(pos.getX(), pos.getY() + y, pos.getZ(), pos.getX(), pos.getY()+1, pos.getZ()+z,c1,c2);
       event.renderer.gradientQuadVertical(pos.getX(), pos.getY() + y, pos.getZ(), pos.getX()+z, pos.getY()+1, pos.getZ(),c1,c2);

        event.renderer.gradientQuadVertical(pos.getX()+1, pos.getY() + y, pos.getZ(), pos.getX()+1, pos.getY()+1, pos.getZ()+z,c1,c2);
        event.renderer.gradientQuadVertical(pos.getX()+1, pos.getY() + y, pos.getZ(), pos.getX()+x, pos.getY()+1, pos.getZ(),c1,c2);

        event.renderer.gradientQuadVertical(pos.getX(), pos.getY() + y, pos.getZ()+1, pos.getX(), pos.getY()+1, pos.getZ()+x,c1,c2);
        event.renderer.gradientQuadVertical(pos.getX(), pos.getY() + y, pos.getZ()+1, pos.getX()+z, pos.getY()+1, pos.getZ()+1,c1,c2);

        event.renderer.gradientQuadVertical(pos.getX()+1, pos.getY() + y, pos.getZ()+1, pos.getX()+1, pos.getY()+1, pos.getZ()+x,c1,c2);
        event.renderer.gradientQuadVertical(pos.getX()+1, pos.getY() + y, pos.getZ()+1, pos.getX()+x, pos.getY()+1, pos.getZ()+1,c1,c2);
    }

    public static void TABV2(Render3DEvent event, BlockPos pos, double x, double z, boolean top, boolean down, Color c1, Color c2){

    }

    public static void TAB(Render3DEvent event, BlockPos pos ,double x, double z, boolean top, boolean down, Color c1, Color c2){
        if (top){
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY()+1, pos.getZ(), pos.getX()+1, pos.getY()+x, pos.getZ(),c1,c1);
            event.renderer.quadHorizontal(pos.getX(),pos.getY()+1,pos.getZ(),pos.getX()+1,pos.getZ()+z,c1);
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY()+1, pos.getZ(), pos.getX(), pos.getY()+x, pos.getZ()+1,c1,c1);
            event.renderer.quadHorizontal(pos.getX(),pos.getY()+1,pos.getZ(),pos.getX()+z,pos.getZ()+1,c1);
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY()+1, pos.getZ()+1, pos.getX()+1, pos.getY()+x, pos.getZ()+1,c1,c1);
            event.renderer.quadHorizontal(pos.getX(),pos.getY()+1,pos.getZ()+1,pos.getX()+1,pos.getZ()+x,c1);
            event.renderer.gradientQuadVertical(pos.getX()+1, pos.getY()+1, pos.getZ(), pos.getX()+1, pos.getY()+x, pos.getZ()+1,c1,c1);
            event.renderer.quadHorizontal(pos.getX()+1,pos.getY()+1,pos.getZ(),pos.getX()+x,pos.getZ()+1,c1);

        }
        if (down){
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ(), pos.getX()+1, pos.getY()+z, pos.getZ(),c2,c2);
            event.renderer.quadHorizontal(pos.getX(),pos.getY(),pos.getZ(),pos.getX()+1,pos.getZ()+z,c2);
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY()+z, pos.getZ()+1,c2,c2);
            event.renderer.quadHorizontal(pos.getX(),pos.getY(),pos.getZ(),pos.getX()+z,pos.getZ()+1,c2);
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ()+1, pos.getX()+1, pos.getY()+z, pos.getZ()+1,c2,c2);
            event.renderer.quadHorizontal(pos.getX(),pos.getY(),pos.getZ()+1,pos.getX()+1,pos.getZ()+x,c2);
            event.renderer.gradientQuadVertical(pos.getX()+1, pos.getY(), pos.getZ(), pos.getX()+1, pos.getY()+z, pos.getZ()+1,c2,c2);
            event.renderer.quadHorizontal(pos.getX()+1,pos.getY(),pos.getZ(),pos.getX()+x,pos.getZ()+1,c2);
        }

        //down

    }
}
