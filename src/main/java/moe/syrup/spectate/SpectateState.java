package moe.syrup.spectate;

import moe.syrup.data.SoftcoreData;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpectateState {
    private static final Map<UUID, UUID> spectateTargets = new HashMap<>();

    public static void setTarget(UUID spectator, UUID target) {
        if (target == null) {
            spectateTargets.remove(spectator);
        } else {
            spectateTargets.put(spectator, target);
        }
    }

    public static UUID getTargetUuid(UUID spectator) {
        return spectateTargets.get(spectator);
    }

    public static ServerPlayer getTarget(UUID spectator) {
        UUID targetUuid = spectateTargets.get(spectator);
        if (targetUuid == null) return null;
        var server = SoftcoreData.getServer();
        return server != null ? server.getPlayerList().getPlayer(targetUuid) : null;
    }

    public static void clear(UUID spectator) {
        spectateTargets.remove(spectator);
    }
}
