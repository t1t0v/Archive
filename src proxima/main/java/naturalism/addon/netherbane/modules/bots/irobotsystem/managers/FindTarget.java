package naturalism.addon.netherbane.modules.bots.irobotsystem.managers;

import naturalism.addon.netherbane.utils.BlockUtil;
import naturalism.addon.netherbane.utils.EntityUtil;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BedItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class FindTarget {
    public static PlayerEntity init(TargetMode targetMode, String name, boolean ignoreNoob){
        switch (targetMode){
            case Name -> {
                return getTargetByName(name);
            }
            case Nearest -> {
                List<PlayerEntity> playerEntities = getValidTargets(ignoreNoob);
                playerEntities.sort(Comparator.comparing(player -> {
                    assert mc.player != null;
                    return BlockUtil.distance(mc.player.getBlockPos(), player.getBlockPos());
                }));
                if (playerEntities.isEmpty()) return null;
                return playerEntities.get(0);
            }
            case Top -> {
                List<PlayerEntity> playerEntities = getValidTargets(ignoreNoob);
                playerEntities = playerEntities.stream().filter(FindTarget::isTopPlayer).collect(Collectors.toList());
                if (playerEntities.isEmpty()){
                    playerEntities = getValidTargets(ignoreNoob);
                }
                if (playerEntities.isEmpty()) return null;
                return playerEntities.get(0);
            }
            case Nigger -> {
                List<PlayerEntity> playerEntities = getValidTargets(ignoreNoob);
                playerEntities = playerEntities.stream().filter(FindTarget::isNigger).collect(Collectors.toList());
                if (playerEntities.isEmpty()){
                    playerEntities = getValidTargets(ignoreNoob);
                }
                if (playerEntities.isEmpty()) return null;
                return playerEntities.get(0);
            }
        }
        return null;
    }

    private static boolean isTopPlayer(PlayerEntity player){
        for (ItemStack itemStack : player.getInventory().armor){
            if (itemStack.getItem().getMaxDamage() > 350 && EnchantmentHelper.getLevel(Enchantments.UNBREAKING, itemStack) > 2) return true;
        }
        return false;
    }

    private static boolean isNigger(PlayerEntity player){
        for (ItemStack itemStack : player.getInventory().armor){
            if (itemStack.getItem().getMaxDamage() > 150) return true;
        }
        if (player.getMainHandStack().getItem() instanceof BedItem) return true;
        if (player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) return true;
        return false;
    }

    public static PlayerEntity getTargetByName(String name){
        assert mc.world != null;
        for (PlayerEntity player : mc.world.getPlayers()){
            if (Objects.equals(player.getEntityName(), name)){
                return player;
            }
        }
        return null;
    }

    public static List<PlayerEntity> getValidTargets(boolean ignoreNoob){
        assert mc.world != null;
        List<PlayerEntity> players = EntityUtil.getTargetsInRange(128D);
        players = players.stream().filter(abstractClientPlayerEntity -> abstractClientPlayerEntity != mc.player).collect(Collectors.toList());
        if (ignoreNoob) players.stream().filter(abstractClientPlayerEntity -> !isNoobPlayer(abstractClientPlayerEntity));
        return players;
    }

    private static boolean isNoobPlayer(PlayerEntity player){
        for (ItemStack itemStack : player.getInventory().armor){
            if (!itemStack.isEmpty()) return false;
        }
        if (player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) return false;
        if (player.getMainHandStack().getItem() instanceof BedItem) return false;

        return true;
    }

    public enum TargetMode{
        Name,
        Nearest,
        Nigger,
        Top
    }
}
