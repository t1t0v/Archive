package naturalism.addon.netherbane.modules.player;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import naturalism.addon.netherbane.events.SetBlockStateEvent;
import naturalism.addon.netherbane.utils.TimeUtil;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import naturalism.addon.netherbane.NetherBane;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;



public class NoDesync extends Module {
    public NoDesync(){
        super(NetherBane.PLAYERPLUS, "no-desync", "L1te no desync");
        this.sg_general = this.settings.getDefaultGroup();
        this.cfg_predict_block_state = this.sg_general.add((new BoolSetting.Builder()).name("predict-block-state").description("...").defaultValue(true).build());
    }

    AtomicBoolean manual = new AtomicBoolean(false);
    private final Map<BlockPos, BlockState> synced_blocks = new ConcurrentHashMap();
    private final Map<BlockPos, Long> desynced_blocks = new ConcurrentHashMap();
    private final Map<BlockPos, BlockState> bypass_blocks = new ConcurrentHashMap();
    private final SettingGroup sg_general;
    private final Setting<Boolean> cfg_predict_block_state;

    @Override
    public void onActivate() {
        this.ClearInfo();
    }

    private void ClearInfo() {
        this.synced_blocks.clear();
        this.desynced_blocks.clear();
        this.bypass_blocks.clear();
    }

    @EventHandler(priority = 200)
    private void onTick(TickEvent.Pre event) {
        this.desynced_blocks.entrySet().stream().filter((entry) -> {
            return (Long)entry.getValue() > TimeUtil.GetCurTime();
        }).map(Map.Entry::getKey).forEach(this::SyncBlockState);
    }

    private void onSetBlockStateOld(SetBlockStateEvent event) {
        if (!this.manual.get()) {
            BlockPos block = event.pos;
            BlockState synced_state = (BlockState)this.synced_blocks.get(block);
            if (synced_state != null && synced_state == event.newState) {
                this.desynced_blocks.remove(block);
                if (event.oldState != null && event.oldState.getBlock() instanceof BedBlock && event.newState.getMaterial().isReplaceable()) {
                    BlockState state = event.oldState;
                    Direction dir = BedBlock.getOppositePartDirection(state);
                    this.SyncBlockState(block.offset(dir));
                }
            } else if ((Boolean)this.cfg_predict_block_state.get()) {
                this.desynced_blocks.put(block, TimeUtil.GetResponseTime());
            } else {
                event.setCancelled(true);
            }

        }
    }

    @EventHandler(priority = 200)
    private void onSetBlockState(SetBlockStateEvent event) {
        if (!this.manual.get()) {
            BlockPos block = event.pos;
            BlockState synced_state = (BlockState)this.synced_blocks.get(block);
            if (synced_state == null && event.oldState != null) {
                this.synced_blocks.put(block, event.oldState);
            } else if (synced_state == event.newState) {
                this.desynced_blocks.remove(block);
                if (event.oldState != null && event.oldState.getBlock() instanceof BedBlock && event.newState.isAir()) {
                    this.RemoveBlock(block.offset(BedBlock.getOppositePartDirection(event.oldState)), false);
                }
            } else if ((Boolean)this.cfg_predict_block_state.get()) {
                this.desynced_blocks.put(block, TimeUtil.GetResponseTime());
            } else {
                event.setCancelled(true);
            }

        }
    }

    @EventHandler(
        priority = 200
    )
    private void onPacketReceive(PacketEvent.Receive event) {
        assert this.mc.world != null;

        assert this.mc.player != null;

        if (event.packet instanceof BlockUpdateS2CPacket) {
            BlockUpdateS2CPacket packet = (BlockUpdateS2CPacket) event.packet;
            this.synced_blocks.put(packet.getPos(), packet.getState());
        } else if (event.packet instanceof ChunkDeltaUpdateS2CPacket) {
            ChunkDeltaUpdateS2CPacket packet = (ChunkDeltaUpdateS2CPacket)event.packet;
            Map var10001 = this.synced_blocks;
            Objects.requireNonNull(var10001);
            packet.visitUpdates(var10001::put);
        } else if (event.packet instanceof PlayerActionResponseS2CPacket) {
            PlayerActionResponseS2CPacket packet = (PlayerActionResponseS2CPacket)event.packet;
            this.synced_blocks.put(packet.pos(), packet.state());
        } else if (event.packet instanceof BlockEventS2CPacket) {
            BlockEventS2CPacket packet = (BlockEventS2CPacket)event.packet;
            BlockPos pos = packet.getPos();
            Block block = packet.getBlock();
            BlockState synced_state = (BlockState)this.synced_blocks.get(pos);
            if (synced_state == null) {
                return;
            }

            if (synced_state.isOf(block)) {
                return;
            }

            this.mc.world.setBlockStateWithoutNeighborUpdates(pos, block.getDefaultState());
        } else if (event.packet instanceof UnloadChunkS2CPacket) {
            UnloadChunkS2CPacket packet = (UnloadChunkS2CPacket)event.packet;
            ChunkPos unload_chunk_pos = new ChunkPos(packet.getX(), packet.getZ());
            this.synced_blocks.keySet().removeIf((b) -> {
                return (new ChunkPos(b)).equals(unload_chunk_pos);
            });
            this.desynced_blocks.keySet().removeIf((b) -> {
                return (new ChunkPos(b)).equals(unload_chunk_pos);
            });
            this.bypass_blocks.keySet().removeIf((b) -> {
                return (new ChunkPos(b)).equals(unload_chunk_pos);
            });
        } else if (event.packet instanceof GameJoinS2CPacket) {
            this.ClearInfo();
        }

    }

    private boolean CustomEquals(BlockState state1, BlockState state2) {
        return state1.equals(state2);
    }

    public void SyncBlockState(BlockPos block) {
        assert this.mc.world != null;

        BlockState client_state = this.mc.world.getBlockState(block);
        BlockState server_state = (BlockState)this.synced_blocks.get(block);
        if (server_state == null) {
            this.RemoveBlock(block, false);
        } else if (server_state != client_state) {
            this.mc.world.setBlockStateWithoutNeighborUpdates(block, server_state);
        }

    }

    public void RemoveBlock(BlockPos block, boolean move) {
        assert this.mc.world != null;

        this.manual.set(true);
        this.mc.world.removeBlock(block, move);
        this.manual.set(false);
    }

    public void SetBlockState(BlockPos block, BlockState state) {
        assert this.mc.world != null;

        this.manual.set(true);
        this.mc.world.setBlockStateWithoutNeighborUpdates(block, state);
        this.manual.set(false);
    }

    public boolean IsBlockSynced(BlockPos block) {
        return !this.desynced_blocks.containsKey(block);
    }


}
