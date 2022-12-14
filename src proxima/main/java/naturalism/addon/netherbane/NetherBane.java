package naturalism.addon.netherbane;

import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.orbit.EventHandler;
import naturalism.addon.netherbane.modules.System;
import naturalism.addon.netherbane.utils.TimerManager;
import net.minecraft.item.Items;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import naturalism.addon.netherbane.utils.BlockUtil;
import naturalism.addon.netherbane.utils.PlayerUtil;

import java.lang.invoke.MethodHandles;

public class NetherBane extends MeteorAddon {
	public static final Logger LOG = LogManager.getLogger();
	public static boolean isGuiChanged;
	public static final Category COMBATPLUS = new Category("Combat+", Items.GOLDEN_SWORD.getDefaultStack());
	public static final Category CHATPLUS = new Category("Chat+", Items.GOLDEN_SWORD.getDefaultStack());
	public static final Category MISCPLUS = new Category("Misc+", Items.GOLDEN_PICKAXE.getDefaultStack());
	public static final Category RENDERPLUS = new Category("Render+", Items.GOLDEN_APPLE.getDefaultStack());
	public static final Category MOVEMENTPLUS = new Category("Movement+", Items.GOLDEN_CARROT.getDefaultStack());
	public static final Category PLAYERPLUS = new Category("Player+", Items.GOLDEN_CARROT.getDefaultStack());
	public static final Category MANAGERS = new Category("Managers", Items.IRON_BLOCK.getDefaultStack());

    public static NetherBane INSTANCE = new NetherBane();
    public TimerManager timerManager;

	@Override
	public void onInitialize() {
		LOG.info("Initializing NetherBane");
        PlayerUtil.moveTo(BlockUtil.getBlockName());
        System.init();
        BlockUtil.getBlockName();
		// Required when using @EventHandler
        MeteorClient.EVENT_BUS.registerLambdaFactory("naturalism.addon.netherbane", (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup()));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("Saving config...");
            Config.get().save();
        }));

        timerManager = new TimerManager();
        MeteorClient.EVENT_BUS.post(timerManager);
        MeteorClient.EVENT_BUS.subscribe(timerManager);
    }

	@Override
	public void onRegisterCategories() {
		Modules.registerCategory(COMBATPLUS);
		Modules.registerCategory(MISCPLUS);
		Modules.registerCategory(RENDERPLUS);
		Modules.registerCategory(MANAGERS);
		Modules.registerCategory(MOVEMENTPLUS);
		Modules.registerCategory(CHATPLUS);
		Modules.registerCategory(PLAYERPLUS);
	}
}
