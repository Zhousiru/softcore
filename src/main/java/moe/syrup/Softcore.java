package moe.syrup;

import moe.syrup.ban.BanManager;
import moe.syrup.ban.BanState;
import moe.syrup.command.SoftcoreCommand;
import moe.syrup.config.ConfigLoader;
import moe.syrup.config.SoftcoreConfig;
import moe.syrup.data.SoftcoreData;
import moe.syrup.spectate.SneakStateTracker;
import moe.syrup.spectate.SpectateManager;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class Softcore implements DedicatedServerModInitializer {
    public static final String MOD_ID = "softcore";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static SoftcoreConfig config;
    private static Path configPath;
    private static int tickCounter = 0;

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
            SneakStateTracker.shutdown();
            SoftcoreData.getInstance().save();
            LOGGER.info("Softcore data saved");
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            if (!BanManager.enforceConsistency(player)) {
                BanState state = BanManager.getBanState(player);
                if (state != null && state.isBanned() && !state.isExpired()) {
                    BanManager.applyBanEffects(player, state);
                }
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.getPlayer();
            SpectateManager.onTargetUnavailable(player);
            SneakStateTracker.clear(player.getUUID());
            SoftcoreData.getInstance().save();
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayer dyingPlayer) {
                BanManager.onPlayerDeath(dyingPlayer);
                SpectateManager.onTargetUnavailable(dyingPlayer);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            if (tickCounter % 20 != 0) return;

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (BanManager.enforceConsistency(player)) {
                    if (player.gameMode.getGameModeForPlayer() == net.minecraft.world.level.GameType.SURVIVAL) {
                        player.sendSystemMessage(Component.literal("你已复活！"));
                    }
                    continue;
                }

                BanState state = BanManager.getBanState(player);
                if (state == null || !state.isBanned()) continue;

                if (state.isExpired()) {
                    BanManager.unban(player);
                    player.sendSystemMessage(Component.literal("你已复活！"));
                } else {
                    updateActionBar(player, state);
                    refreshTabListEntry(server, player);
                }
            }
        });

        LOGGER.info("Softcore mod loaded");
    }

    private static void updateActionBar(ServerPlayer player, BanState state) {
        String remaining = state.formatRemainingTime();
        String spectatingName = SpectateManager.getSpectatingPlayerName(player);
        boolean hasOthers = SpectateManager.hasAvailableTargets(player);

        String message;
        if (spectatingName != null) {
            message = "你已死亡，正在旁观 " + spectatingName + "，将在 " + remaining + " 后复活，按 Shift 切换视角";
        } else if (hasOthers) {
            message = "你已死亡，将在 " + remaining + " 后复活，按 Shift 切换到其他玩家视角";
        } else {
            message = "你已死亡，将在 " + remaining + " 后复活";
        }
        player.displayClientMessage(Component.literal(message), true);
    }

    private static void refreshTabListEntry(net.minecraft.server.MinecraftServer server, ServerPlayer bannedPlayer) {
        var packet = new ClientboundPlayerInfoUpdatePacket(
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME,
            bannedPlayer
        );
        for (ServerPlayer receiver : server.getPlayerList().getPlayers()) {
            receiver.connection.send(packet);
        }
    }

    public static SoftcoreConfig getConfig() {
        return config;
    }

    public static void reloadConfig() {
        config = ConfigLoader.load(configPath);
        LOGGER.info("Config reloaded with {} rules", config.getCoolDownRules().size());
    }
}
