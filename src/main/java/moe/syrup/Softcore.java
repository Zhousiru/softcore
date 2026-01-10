package moe.syrup;

import moe.syrup.ban.BanManager;
import moe.syrup.command.SoftcoreCommand;
import moe.syrup.config.ConfigLoader;
import moe.syrup.config.SoftcoreConfig;
import moe.syrup.data.SoftcoreData;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class Softcore implements DedicatedServerModInitializer {
    public static final String MOD_ID = "softcore";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static SoftcoreConfig config;
    private static Path configPath;

    @Override
    public void onInitializeServer() {
        configPath = FabricLoader.getInstance().getConfigDir().resolve("softcore.toml");

        CommandRegistrationCallback.EVENT.register(SoftcoreCommand::register);

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            config = ConfigLoader.load(configPath);
            SoftcoreData.init(server);
            LOGGER.info("Softcore initialized with {} rules", config.getCoolDownRules().size());
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            SoftcoreData.getInstance().save();
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayer player) {
                BanManager.onPlayerDeath(player);
            }
        });

        LOGGER.info("Softcore mod loaded");
    }

    public static SoftcoreConfig getConfig() {
        return config;
    }

    public static void reloadConfig() {
        config = ConfigLoader.load(configPath);
        LOGGER.info("Config reloaded with {} rules", config.getCoolDownRules().size());
    }
}
