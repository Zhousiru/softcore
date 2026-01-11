package moe.syrup;

import moe.syrup.ban.BanManager;
import moe.syrup.command.SoftcoreCommand;
import moe.syrup.config.ConfigLoader;
import moe.syrup.config.SoftcoreConfig;
import moe.syrup.data.PlayerData;
import moe.syrup.data.SoftcoreData;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Softcore implements DedicatedServerModInitializer {
    public static final String MOD_ID = "softcore";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static SoftcoreConfig config;
    private static Path configPath;
    private static final Set<UUID> lowHealthPlayers = new HashSet<>();

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

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            PlayerData data = SoftcoreData.getInstance().getPlayerData(newPlayer.getUUID());
            data.setLastRespawnTime(Instant.now());
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!config.isEnableHints()) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                UUID uuid = player.getUUID();
                boolean isLowHealth = player.getHealth() <= 8.0f;
                boolean wasLowHealth = lowHealthPlayers.contains(uuid);

                if (isLowHealth && !wasLowHealth) {
                    lowHealthPlayers.add(uuid);
                    PlayerData data = SoftcoreData.getInstance().getPlayerData(uuid);
                    String msg;
                    if (BanManager.isInImmunity(data)) {
                        msg = "§a现在死亡将会立即重生";
                    } else {
                        Duration nextBan = BanManager.calculateNextBanDuration(data);
                        msg = nextBan.isZero() ? "§a现在死亡将会立即重生" : "§c现在死亡将在 " + formatDuration(nextBan) + " 后重生";
                    }
                    player.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(msg)));
                } else if (!isLowHealth && wasLowHealth) {
                    lowHealthPlayers.remove(uuid);
                }
            }
        });

        LOGGER.info("Softcore mod loaded");
    }

    private static String formatDuration(Duration d) {
        long hours = d.toHours();
        long minutes = d.toMinutes() % 60;
        if (hours > 0) return hours + "时" + minutes + "分";
        return minutes + "分";
    }

    public static SoftcoreConfig getConfig() {
        return config;
    }

    public static void reloadConfig() {
        config = ConfigLoader.load(configPath);
        LOGGER.info("Config reloaded with {} rules", config.getCoolDownRules().size());
    }
}
