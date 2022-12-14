package genesis.team.addon.modules.misc;

import genesis.team.addon.Genesis;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.fluid.FluidState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/*
    Ported from: https://github.com/BleachDrinker420/BleachHack/blob/master/BleachHack-Fabric-1.16/src/main/java/bleach/hack/module/mods/NewChunks.java
*/
public class NewChunks extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> remove = sgGeneral.add(new BoolSetting.Builder()
            .name("remove")
            .description("Removes the cached chunks when disabling the module.")
            .defaultValue(true)
            .build()
    );

    private final Setting<SettingColor> newChunksColor = sgGeneral.add(new ColorSetting.Builder()
            .name("new-chunks-color")
            .description("Color of the chunks that are (most likely) completely new.")
            .defaultValue(new SettingColor(204, 153, 217))
            .build()
    );

    private final Setting<SettingColor> oldChunksColor = sgGeneral.add(new ColorSetting.Builder()
            .name("old-chunks-color")
            .description("Color of the chunks that have (most likely) been loaded before.")
            .defaultValue(new SettingColor(230, 51, 51))
            .build()
    );

    private final Set<ChunkPos> newChunks = Collections.synchronizedSet(new HashSet<>());
    private final Set<ChunkPos> oldChunks = Collections.synchronizedSet(new HashSet<>());
    private static final Direction[] searchDirs = new Direction[] { Direction.EAST, Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.UP };

    public NewChunks() {
        super(Genesis.Misc,"new-chunks", "Detects completely new chunks using certain traits of them");
    }

    @Override
    public void onDeactivate() {
        if (remove.get()) {
            newChunks.clear();
            oldChunks.clear();
        }
        super.onDeactivate();
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (newChunksColor.get().a > 5) {
            synchronized (newChunks) {
                for (ChunkPos c : newChunks) {
                    if (mc.getCameraEntity().getBlockPos().isWithinDistance(c.getStartPos(), 1024)) {
                        drawBoxOutline(new Box(c.getStartPos(), c.getStartPos().add(16, 0, 16)), newChunksColor.get(), event);
                    }
                }
            }
        }

        if (oldChunksColor.get().a > 5){
            synchronized (oldChunks) {
                for (ChunkPos c : oldChunks) {
                    if (mc.getCameraEntity().getBlockPos().isWithinDistance(c.getStartPos(), 1024)) {
                        drawBoxOutline(new Box(c.getStartPos(), c.getStartPos().add(16, 0, 16)), oldChunksColor.get(), event);
                    }
                }
            }
        }
    }

    private void drawBoxOutline(Box box, Color color, Render3DEvent event) {
        event.renderer.box(
                box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
                new Color(0,0,0,0), color, ShapeMode.Lines, 0
                );
    }

    @EventHandler
    private void onReadPacket(PacketEvent.Receive event) {
        if (event.packet instanceof ChunkDeltaUpdateS2CPacket) {
            ChunkDeltaUpdateS2CPacket packet = (ChunkDeltaUpdateS2CPacket) event.packet;

            packet.visitUpdates((pos, state) -> {
                if (!state.getFluidState().isEmpty() && !state.getFluidState().isStill()) {
                    ChunkPos chunkPos = new ChunkPos(pos);

                    for (Direction dir: searchDirs) {
                        if (mc.world.getBlockState(pos.offset(dir)).getFluidState().isStill() && !oldChunks.contains(chunkPos)) {
                            newChunks.add(chunkPos);
                            return;
                        }
                    }
                }
            });
        }

        else if (event.packet instanceof BlockUpdateS2CPacket) {
            BlockUpdateS2CPacket packet = (BlockUpdateS2CPacket) event.packet;

            if (!packet.getState().getFluidState().isEmpty() && !packet.getState().getFluidState().isStill()) {
                ChunkPos chunkPos = new ChunkPos(packet.getPos());

                for (Direction dir: searchDirs) {
                    if (mc.world.getBlockState(packet.getPos().offset(dir)).getFluidState().isStill() && !oldChunks.contains(chunkPos)) {
                        newChunks.add(chunkPos);
                        return;
                    }
                }
            }
        }

        else if (event.packet instanceof ChunkDataS2CPacket && mc.world != null) {
            ChunkDataS2CPacket packet = (ChunkDataS2CPacket) event.packet;

            ChunkPos pos = new ChunkPos(packet.getX(), packet.getZ());

            if (!newChunks.contains(pos) && mc.world.getChunkManager().getChunk(packet.getX(), packet.getZ()) == null) {
                WorldChunk chunk = new WorldChunk(mc.world, pos);
                try {
                    chunk.loadFromPacket(packet.getChunkData().getSectionsDataBuf(), new NbtCompound(), packet.getChunkData().getBlockEntities(packet.getX(), packet.getZ()));
                } catch (ArrayIndexOutOfBoundsException e) {
                    return;
                }


                for (int x = 0; x < 16; x++) {
                    for (int y = mc.world.getBottomY(); y < mc.world.getTopY(); y++) {
                        for (int z = 0; z < 16; z++) {
                            FluidState fluid = chunk.getFluidState(x, y, z);

                            if (!fluid.isEmpty() && !fluid.isStill()) {
                                oldChunks.add(pos);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }
}
