package moe.syrup.spectate;

import moe.syrup.Softcore;
import moe.syrup.ban.BanManager;
import moe.syrup.data.SoftcoreData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

public class SpectateManager {

    public static void cycleSpectateTarget(ServerPlayer spectator) {
        List<ServerPlayer> targets = getAvailableTargets(spectator);
        Softcore.LOGGER.info("[Spectate] cycleSpectateTarget for {}, available: {}",
            spectator.getName().getString(), targets.size());

        if (targets.isEmpty()) {
            clearSpectateTarget(spectator);
            return;
        }

        ServerPlayer currentTarget = SpectateState.getTarget(spectator.getUUID());
        int currentIndex = currentTarget != null ? targets.indexOf(currentTarget) : -1;
        int nextIndex = (currentIndex + 1) % targets.size();

        if (currentIndex >= 0 && nextIndex == 0) {
            Softcore.LOGGER.info("[Spectate] Cycling back to self view");
            clearSpectateTarget(spectator);
        } else {
            ServerPlayer target = targets.get(nextIndex);
            Softcore.LOGGER.info("[Spectate] Setting target to {}", target.getName().getString());
            SpectateState.setTarget(spectator.getUUID(), target.getUUID());
            spectator.setCamera(target);
        }
    }

    public static void clearSpectateTarget(ServerPlayer spectator) {
        SpectateState.clear(spectator.getUUID());
        spectator.setCamera(spectator);
    }

    public static void onTargetUnavailable(ServerPlayer unavailablePlayer) {
        MinecraftServer server = SoftcoreData.getServer();
        if (server == null) return;

        UUID unavailableUuid = unavailablePlayer.getUUID();
        for (ServerPlayer spectator : server.getPlayerList().getPlayers()) {
            if (!BanManager.isBanned(spectator)) continue;

            UUID targetUuid = SpectateState.getTargetUuid(spectator.getUUID());
            if (unavailableUuid.equals(targetUuid)) {
                List<ServerPlayer> targets = getAvailableTargets(spectator);
                if (targets.isEmpty()) {
                    clearSpectateTarget(spectator);
                } else {
                    ServerPlayer newTarget = targets.get(0);
                    SpectateState.setTarget(spectator.getUUID(), newTarget.getUUID());
                    spectator.setCamera(newTarget);
                }
                Softcore.LOGGER.info("[Spectate] {}'s target {} unavailable, switched",
                    spectator.getName().getString(), unavailablePlayer.getName().getString());
            }
        }
    }

    public static ServerPlayer getSpectatingPlayer(ServerPlayer spectator) {
        return SpectateState.getTarget(spectator.getUUID());
    }

    public static String getSpectatingPlayerName(ServerPlayer spectator) {
        ServerPlayer target = getSpectatingPlayer(spectator);
        return target != null ? target.getName().getString() : null;
    }

    public static boolean hasAvailableTargets(ServerPlayer spectator) {
        return !getAvailableTargets(spectator).isEmpty();
    }

    private static List<ServerPlayer> getAvailableTargets(ServerPlayer spectator) {
        MinecraftServer server = SoftcoreData.getServer();
        if (server == null) return List.of();

        return server.getPlayerList().getPlayers().stream()
            .filter(p -> !p.equals(spectator) && !BanManager.isBanned(p))
            .toList();
    }
}
