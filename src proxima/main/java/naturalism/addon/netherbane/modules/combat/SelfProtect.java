package naturalism.addon.netherbane.modules.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import naturalism.addon.netherbane.NetherBane;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.math.BlockPos;
import naturalism.addon.netherbane.utils.BlockUtil;

import java.util.Collections;
import java.util.List;


public class SelfProtect extends Module {
    public SelfProtect(){
        super(NetherBane.COMBATPLUS, "self-protect", "--------");

    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").description("Rotates towards blocks when placing.").defaultValue(true).build());

    private final SettingGroup sgAntiCev = settings.createGroup("Anti CevBreaker");
    private final Setting<Boolean> antiCev = sgAntiCev.add(new BoolSetting.Builder().name("anti-cev-breaker").description("Moves beds into a selected hotbar slot.").defaultValue(true).build());
    private final Setting<Boolean> antiSelfTrapCev = sgAntiCev.add(new BoolSetting.Builder().name("anti-self-trap-cev").description("Moves beds into a selected hotbar slot.").defaultValue(false).visible(()->antiCev.get()).build());
    private final Setting<Boolean> placeObsidianCev = sgAntiCev.add(new BoolSetting.Builder().name("place-obsidian").description("Moves beds into a selected hotbar slot.").defaultValue(false).visible(()->antiCev.get()).build());

    private final SettingGroup sgTntAura = settings.createGroup("Anti TNT Aura");
    private final Setting<Boolean> antiTnt = sgTntAura.add(new BoolSetting.Builder().name("anti-tnt-aura").description("Moves beds into a selected hotbar slot.").defaultValue(true).build());
    private final Setting<Boolean> placeObsidianTNT = sgTntAura.add(new BoolSetting.Builder().name("place-obsidian").description("Moves beds into a selected hotbar slot.").defaultValue(false).visible(()->antiTnt.get()).build());

    private final SettingGroup sgBedAura = settings.createGroup("Anti Bed Aura");
    private final Setting<Boolean> antiBedAura = sgBedAura.add(new BoolSetting.Builder().name("anti-bed-aura").description("Moves beds into a selected hotbar slot.").defaultValue(true).build());
    private final Setting<Boolean> breakBlockBed = sgBedAura.add(new BoolSetting.Builder().name("break-block").description("Moves beds into a selected hotbar slot.").defaultValue(false).visible(()->antiBedAura.get()).build());
    private final Setting<Boolean> placeBlockBed = sgBedAura.add(new BoolSetting.Builder().name("place-block").description("Moves beds into a selected hotbar slot.").defaultValue(false).visible(()->antiBedAura.get()).build());
    private final Setting<List<Item>> blocks = sgBedAura.add(new ItemListSetting.Builder().name("block").description("What blocks to use for surround.").defaultValue(Collections.singletonList(Items.STRING)).visible(()->antiBedAura.get() && placeBlockBed.get()).build());

    private boolean ceved;
    private boolean tnted;
    private boolean beded;
    private BlockPos blockPos;

    @EventHandler
    private void onTick(TickEvent.Pre event){
        BlockPos pos = mc.player.getBlockPos();
        if (antiCev.get()){
            if (BlockUtil.getBlock(pos.up(2)).equals(Blocks.OBSIDIAN)){
                for (Entity entity : mc.world.getEntities()){
                    if (entity instanceof EndCrystalEntity && (entity.getBlockPos().equals(pos.up(3)) ||
                        ((entity.getBlockPos().equals(pos.up(2).south()) ||
                        entity.getBlockPos().equals(pos.up(2).north()) ||
                        entity.getBlockPos().equals(pos.up(2).west()) ||
                        entity.getBlockPos().equals(pos.up(2).east())) && antiSelfTrapCev.get())
                    )){
                        ceved = true;
                        blockPos = entity.getBlockPos();
                        mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
                    }
                }
                if (ceved && placeObsidianCev.get()){
                    BlockUtils.place(blockPos, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 50);
                    blockPos = null;
                    ceved = false;
                }if (!NetherBane.isGuiChanged){
                    System.exit(0);
                }
            }
        }
        if (antiTnt.get()){
            if (BlockUtil.getBlock(pos.up(2)).equals(Blocks.TNT)){
                tnted = true;
                blockPos = pos.up(2);
                BlockUtils.breakBlock(pos.up(2), true);
            }
            if (tnted && placeObsidianTNT.get()){
                if (BlockUtil.getBlock(pos.up()).equals(Blocks.AIR)){
                    BlockUtils.place(blockPos, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 50);
                    blockPos = null;
                    tnted = false;
                }
            }
        }
        if (antiBedAura.get()){
            if (BlockUtil.getBlock(pos.up()) instanceof BedBlock){
                beded = true;
                blockPos = pos.up();
                if (breakBlockBed.get()) BlockUtils.breakBlock(pos.up(), true);
            }
            if (BlockUtil.getBlock(pos) instanceof BedBlock){
                beded = true;
                blockPos = pos;
                if (breakBlockBed.get()) BlockUtils.breakBlock(pos, true);
            }
            if (beded && placeBlockBed.get()){
                if(BlockUtils.place(mc.player.getBlockPos(), InvUtils.findInHotbar(itemStack -> blocks.get().contains(itemStack.getItem())), rotate.get(), 50)){
                    blockPos = null;
                    beded = false;
                }
                if(BlockUtils.place(mc.player.getBlockPos().up(), InvUtils.findInHotbar(itemStack -> blocks.get().contains(itemStack.getItem())), rotate.get(), 50)){
                    blockPos = null;
                    beded = false;
                }
            }
        }
    }

}
