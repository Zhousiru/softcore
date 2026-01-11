package moe.syrup.ban;

import moe.syrup.Softcore;
import moe.syrup.config.CoolDownRule;
import moe.syrup.data.PlayerData;
import moe.syrup.data.SoftcoreData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BanManager {
    private static final String BAN_MESSAGE_TEMPLATE = "你已死亡，暂时无法进入服务器\n\n重生时间: %s";

    public static BanState checkBanState(UUID playerId) {
        BanState state = SoftcoreData.getInstance().getPlayerData(playerId).getBanState();
        if (state == null) return null;

        if (state.isExpired()) {
            clearBan(playerId);
            Softcore.LOGGER.info("Ban expired for player {}", playerId);
            return null;
        }

        return state;
    }

    public static Component createBanMessage(BanState state) {
        return Component.literal(String.format(BAN_MESSAGE_TEMPLATE, state.formatAbsoluteTime()));
    }

    public static boolean isInImmunity(PlayerData data) {
        Instant lastRespawn = data.getLastRespawnTime();
        if (lastRespawn == null) return false;
        Duration immunity = Softcore.getConfig().getRespawnImmunity();
        return Instant.now().isBefore(lastRespawn.plus(immunity));
    }

    public static void onPlayerDeath(ServerPlayer player) {
        UUID playerId = player.getUUID();
        String playerName = player.getName().getString();

        PlayerData data = SoftcoreData.getInstance().getPlayerData(playerId);

        if (isInImmunity(data)) {
            Softcore.LOGGER.info("Player {} died during immunity period, skipping ban", playerName);
            return;
        }

        Instant now = Instant.now();
        data.recordDeath(now);

        Duration banDuration = calculateBanDuration(data);
        if (banDuration.isZero()) {
            data.setBanState(null);
            return;
        }

        BanState banState = new BanState(now.plus(banDuration));
        data.setBanState(banState);
        SoftcoreData.getInstance().save();

        Softcore.LOGGER.info("Player {} (UUID: {}) banned for {}", playerName, playerId, banState.formatRemainingTime());

        // 公屏广播重生时间
        SoftcoreData.getServer().getPlayerList().broadcastSystemMessage(
            Component.literal("§e" + playerName + " 将在 " + banState.formatAbsoluteTime() + " 重生"),
            false
        );
        player.connection.disconnect(createBanMessage(banState));
    }

    public static Duration calculateBanDuration(PlayerData data) {
        for (CoolDownRule rule : Softcore.getConfig().getCoolDownRules()) {
            if (rule.matches(data.getDeathHistory())) {
                return rule.banDuration();
            }
        }
        return Duration.ofHours(1);
    }

    public static Duration calculateNextBanDuration(PlayerData data) {
        List<Instant> simulatedHistory = new ArrayList<>(data.getDeathHistory());
        simulatedHistory.add(Instant.now());
        for (CoolDownRule rule : Softcore.getConfig().getCoolDownRules()) {
            if (rule.matches(simulatedHistory)) {
                return rule.banDuration();
            }
        }
        return Duration.ofHours(1);
    }

    public static void clearBan(UUID playerId) {
        SoftcoreData.getInstance().getPlayerData(playerId).setBanState(null);
        SoftcoreData.getInstance().save();
    }

    public static void clearPlayer(UUID playerId) {
        PlayerData data = SoftcoreData.getInstance().getPlayerData(playerId);
        data.clearDeathHistory();
        data.setBanState(null);
        SoftcoreData.getInstance().save();
        Softcore.LOGGER.info("Cleared data for player {}", playerId);
    }

    public static int clearAllPlayers() {
        SoftcoreData instance = SoftcoreData.getInstance();
        int count = instance.getAllPlayers().size();
        instance.clearAllPlayers();
        instance.save();
        Softcore.LOGGER.info("Cleared data for {} players", count);
        return count;
    }
}
