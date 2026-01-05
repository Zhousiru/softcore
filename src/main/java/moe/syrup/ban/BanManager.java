package moe.syrup.ban;

import moe.syrup.Softcore;
import moe.syrup.config.CoolDownRule;
import moe.syrup.data.PlayerData;
import moe.syrup.data.SoftcoreData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public class BanManager {

    public static boolean isBanned(ServerPlayer player) {
        return isBanned(player.getUUID());
    }

    public static boolean isBanned(UUID playerId) {
        PlayerData data = SoftcoreData.getInstance().getPlayerData(playerId);
        BanState state = data.getBanState();
        return state != null && state.isBanned() && !state.isExpired();
    }

    public static BanState getBanState(ServerPlayer player) {
        PlayerData data = SoftcoreData.getInstance().getPlayerData(player.getUUID());
        return data.getBanState();
    }

    public static BanState getBanState(UUID playerId) {
        PlayerData data = SoftcoreData.getInstance().getPlayerData(playerId);
        return data.getBanState();
    }

    public static void onPlayerDeath(ServerPlayer player) {
        PlayerData data = SoftcoreData.getInstance().getPlayerData(player.getUUID());
        Instant now = Instant.now();
        data.recordDeath(now);

        Duration banDuration = calculateBanDuration(player.getUUID());
        if (banDuration.isZero()) {
            Softcore.LOGGER.info("Player {} died but no ban required", player.getName().getString());
            data.setBanState(null);
        } else {
            ServerLevel overworld = SoftcoreData.getServer().overworld();
            BlockPos spawnPos = overworld.getLevelData().getRespawnData().pos();
            BanState banState = new BanState(
                now.plus(banDuration),
                spawnPos,
                overworld.dimension()
            );
            data.setBanState(banState);
            Softcore.LOGGER.info("Player {} banned for {}", player.getName().getString(), formatDuration(banDuration));
        }
        SoftcoreData.getInstance().save();
    }

    public static Duration calculateBanDuration(UUID playerId) {
        PlayerData data = SoftcoreData.getInstance().getPlayerData(playerId);
        List<Instant> deathHistory = data.getDeathHistory();
        List<CoolDownRule> rules = Softcore.getConfig().getCoolDownRules();

        for (CoolDownRule rule : rules) {
            if (rule.matches(deathHistory)) {
                return rule.banDuration();
            }
        }
        return Duration.ofHours(1);
    }

    public static void unban(ServerPlayer player) {
        PlayerData data = SoftcoreData.getInstance().getPlayerData(player.getUUID());
        BanState state = data.getBanState();
        if (state != null) {
            state.clearBan();
        }
        data.setBanState(null);
        player.setGameMode(GameType.SURVIVAL);
        player.setCamera(player);
        SoftcoreData.getInstance().save();
        Softcore.LOGGER.info("Player {} has been unbanned", player.getName().getString());
    }

    public static boolean enforceConsistency(ServerPlayer player) {
        BanState state = getBanState(player);
        boolean isSpectator = player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR;
        boolean hasValidBan = state != null && state.isBanned() && !state.isExpired();

        if (isSpectator && !hasValidBan) {
            unban(player);
            Softcore.LOGGER.info("Consistency fix: {} restored to survival", player.getName().getString());
            return true;
        }

        if (hasValidBan && !isSpectator) {
            applyBanEffects(player, state);
            Softcore.LOGGER.info("Consistency fix: {} ban effects applied", player.getName().getString());
            return true;
        }

        return false;
    }

    public static void manualBan(ServerPlayer player, Duration duration) {
        PlayerData data = SoftcoreData.getInstance().getPlayerData(player.getUUID());
        ServerLevel overworld = SoftcoreData.getServer().overworld();
        BlockPos spawnPos = overworld.getLevelData().getRespawnData().pos();
        BanState banState = new BanState(
            Instant.now().plus(duration),
            spawnPos,
            overworld.dimension()
        );
        data.setBanState(banState);
        applyBanEffects(player, banState);
        SoftcoreData.getInstance().save();
        Softcore.LOGGER.info("Player {} manually banned for {}", player.getName().getString(), formatDuration(duration));
    }

    public static void applyBanEffects(ServerPlayer player, BanState banState) {
        player.setGameMode(GameType.SPECTATOR);
        ServerLevel level = SoftcoreData.getServer().getLevel(banState.getSpawnDimension());
        if (level != null) {
            BlockPos pos = banState.getSpawnPosition();
            player.teleportTo(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, EnumSet.noneOf(net.minecraft.world.entity.Relative.class), player.getYRot(), player.getXRot(), false);
        }
    }

    public static void clearDeathHistory(UUID playerId) {
        PlayerData data = SoftcoreData.getInstance().getPlayerData(playerId);
        data.clearDeathHistory();
        SoftcoreData.getInstance().save();
    }

    private static String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return minutes + "m";
    }
}
