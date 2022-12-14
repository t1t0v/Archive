package genesis.team.addon.modules.movement;

import genesis.team.addon.Genesis;
import genesis.team.addon.util.ProximaUtil.Timer;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.Hand;

public class ElytraHelper extends Module {
    public ElytraHelper(){
        super(Genesis.Move, "elytra-helper", "-");

    }

    public enum Bind{
        RClick,
        LClick,
        Bind
    }

    private final SettingGroup sgDefault = settings.createGroup("Default");
    private final Setting<Boolean> antiFall = sgDefault.add(new BoolSetting.Builder().name("anti-fall").description("Hold city pos.").defaultValue(true).build());
    private final Setting<Integer> fallDistance = sgDefault.add(new IntSetting.Builder().name("fall-distance").description("Distance.").defaultValue(10).min(0).sliderMax(36).visible(antiFall::get).build());
    public final Setting<Boolean> stopOnStart = sgDefault.add(new BoolSetting.Builder().name("stopOnStart").description("Try to prevent inventory desync.").defaultValue(false).build());
    public final Setting<Boolean> keepY = sgDefault.add(new BoolSetting.Builder().name("keepY").description("Try to prevent inventory desync.").defaultValue(false).build());
    private final Setting<Keybind> saveKey = sgDefault.add(new KeybindSetting.Builder().name("saveKey").description("Change the pickaxe slot to an iron one when pressing the button.").defaultValue(Keybind.fromKey(8)).visible(keepY::get).build());

    private final SettingGroup sgFireWork = settings.createGroup("Firework");
    public final Setting<Boolean> useFireworkBind = sgFireWork.add(new BoolSetting.Builder().name("use-firework-bind").description("Try to prevent inventory desync.").defaultValue(false).build());
    public final Setting<Bind> bind = sgFireWork.add(new EnumSetting.Builder<Bind>().name("bind").description("The method of calculating the damage.").defaultValue(Bind.RClick).visible(useFireworkBind::get).build());
    private final Setting<Keybind> bindKey = sgFireWork.add(new KeybindSetting.Builder().name("bind-key").description("Change the pickaxe slot to an iron one when pressing the button.").defaultValue(Keybind.fromKey(8)).visible(() -> bind.get().equals(Bind.Bind) && useFireworkBind.get()).build());

    public final Setting<Boolean> autoUse = sgFireWork.add(new BoolSetting.Builder().name("auto-use").description("Try to prevent inventory desync.").defaultValue(false).build());

    public final Setting<Boolean>  onLowSpeed = sgFireWork.add(new BoolSetting.Builder().name("on-low-speed").description("Try to prevent inventory desync.").defaultValue(false).visible(autoUse::get).build());
    private final Setting<Double> minSpeed = sgFireWork.add(new DoubleSetting.Builder().name("min-speed").description("The radius in which players get targeted.").defaultValue(5).min(0).sliderMax(10).visible(() -> autoUse.get() && onLowSpeed.get()).build());

    public final Setting<Boolean>  onLowY = sgFireWork.add(new BoolSetting.Builder().name("on-low-y").description("Try to prevent inventory desync.").defaultValue(false).visible(autoUse::get).build());
    private final Setting<Integer> minY = sgFireWork.add(new IntSetting.Builder().name("min-y").description("The delay between breaks.").defaultValue(70).min(0).sliderMax(300).visible(() -> autoUse.get() && onLowY.get()).build());
    private final Setting<Integer> maxY = sgFireWork.add(new IntSetting.Builder().name("max-y").description("The delay between breaks.").defaultValue(150).min(0).sliderMax(300).visible(() -> autoUse.get() && onLowY.get()).build());

    private final Timer useTimer = new Timer();
    private final Timer clickTimer = new Timer();
    private double y;
    private double keepedY;
    private boolean keeped;

    @Override
    public void onDeactivate() {
        keepedY = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event){
        if (!mc.player.isFallFlying() && !mc.player.isOnGround() && mc.player.fallDistance >= fallDistance.get() && mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA){
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        }

        if (mc.player.isFallFlying() && Utils.getPlayerSpeed() <= 0.1 && stopOnStart.get() && !mc.player.isUsingItem()){
            mc.player.setVelocity(mc.player.getVelocity().x, 0, mc.player.getVelocity().z);
            Modules.get().get(meteordevelopment.meteorclient.systems.modules.world.Timer.class).setOverride(0.7);
        }else  Modules.get().get(meteordevelopment.meteorclient.systems.modules.world.Timer.class).setOverride(1);

        if (mc.player.isFallFlying() && keepY.get()){
            if (saveKey.get().isPressed()) {
                keepedY = mc.player.getY();
                keeped = true;
            }
            if (keeped && keepedY != 0){
                mc.player.updatePosition(mc.player.getX(), keepedY, mc.player.getZ());
            }
        }

        if (useFireworkBind.get() && mc.player.isFallFlying()){
            if (((bind.get().equals(Bind.RClick) && mc.options.useKey.isPressed()) || (bind.get().equals(Bind.LClick) && mc.options.attackKey.isPressed()) || (bind.get().equals(Bind.Bind) && bindKey.get().isPressed())) && !(mc.player.getMainHandStack().getItem().isFood() || mc.player.getMainHandStack().getItem() instanceof PotionItem || mc.player.getMainHandStack().getItem() instanceof ExperienceBottleItem || mc.player.getMainHandStack().getItem() instanceof BowItem || mc.player.getMainHandStack().getItem() instanceof TridentItem || mc.player.getMainHandStack().getItem() instanceof ShieldItem || mc.player.getMainHandStack().getItem() instanceof CrossbowItem || mc.player.getMainHandStack().getItem() instanceof EnderPearlItem || mc.player.getMainHandStack().getItem() instanceof EnderEyeItem)){
                if (clickTimer.hasPassed(500)){
                    useFireWork();
                    clickTimer.reset();
                }
            }
        }

        if (autoUse.get()){
            if (onLowSpeed.get() && Utils.getPlayerSpeed() < minSpeed.get() && mc.player.isFallFlying()){
                if (useTimer.hasPassed(5000)){
                    useFireWork();
                    useTimer.reset();
                }
            }
            if (onLowY.get() && mc.player.getY() < minY.get() && mc.player.isFallFlying()){
                if (useTimer.hasPassed(5000)){
                    mc.player.setPitch(-40);
                    useFireWork();
                    useTimer.reset();
                }
            }
            if (mc.player.getY() > maxY.get() && mc.player.isFallFlying()){
                mc.player.setPitch(40);

            }
        }
    }

    private void useFireWork(){
        FindItemResult fireWork = InvUtils.findInHotbar(Items.FIREWORK_ROCKET);
        InvUtils.swap(fireWork.slot(), true);
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        InvUtils.swapBack();
    }

}
