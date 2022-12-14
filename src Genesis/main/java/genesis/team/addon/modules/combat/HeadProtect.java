package genesis.team.addon.modules.combat;

import genesis.team.addon.Genesis;
import genesis.team.addon.util.InfoUtil.EntityInfo;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class HeadProtect extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> placeOnSide = sgGeneral.add(new BoolSetting.Builder().name("place-on-side").description("Prevent from being killed by using CEV breaker on side blocks.").defaultValue(false).build());
    private final Setting<Boolean> crystalBreaker = sgGeneral.add(new BoolSetting.Builder().name("crystal-breaker").description("Places obsidian above head if target using cev breaker.").defaultValue(true).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").description("Automatically faces towards the blocks being placed.").defaultValue(false).build());

    public HeadProtect() {
        super(Genesis.Combat, "head-protect", "Prevent from getting fucked up by TNT-aura module.");
    }

    private BlockPos blockPos, crystalPos;
    private EndCrystalEntity crystalEntity;

    @Override
    public void onActivate() {
        blockPos = null;
        crystalEntity = null;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        blockPos = EntityInfo.getBlockPos(mc.player).up(2);

        if (placeOnSide.get()) {
            // places obsidian on each side
            for (Direction direction : Direction.values()) {
                if (direction.equals(Direction.DOWN)) continue;

                if (BlockUtils.canPlace(blockPos)) place(blockPos);
                if (BlockUtils.canPlace(blockPos.offset(direction))) place(blockPos.offset(direction));
            }
        }

        if (!crystalBreaker.get()) return; // returns if crystal breaker in turned off

        if (crystalEntity != null) { // attacks crystal if entity is not null
            crystalPos = crystalEntity.getBlockPos();

            attack(crystalEntity);
            crystalEntity = null;
        } else if (crystalPos != null) { // places obsidian on crystal pos
            place(crystalPos);
            crystalPos = null;
        }

        for (Entity e : mc.world.getEntities()) {
            if (e instanceof EndCrystalEntity) {
                BlockPos crystalPos = EntityInfo.getBlockPos(e);

                for (Direction direction : Direction.values()) {
                    if (direction.equals(Direction.DOWN)) continue;

                    if (crystalPos.equals(blockPos.offset(direction))) crystalEntity = (EndCrystalEntity) e;
                }
            }
        }
    }

    private void place(BlockPos blockPos) {
        BlockUtils.place(blockPos, InvUtils.find(Items.OBSIDIAN), rotate.get(), 50, true);
    }

    private void attack(Entity target) {
        mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
    }
}
