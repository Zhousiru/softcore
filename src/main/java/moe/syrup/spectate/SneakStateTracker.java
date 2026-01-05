package moe.syrup.spectate;

import moe.syrup.ban.BanManager;
import moe.syrup.data.SoftcoreData;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SneakStateTracker {
    private static final Map<UUID, Boolean> lastSneakState = new HashMap<>();
    private static final Map<UUID, ScheduledFuture<?>> pendingTasks = new HashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final long DELAY_MS = 500;

    public static Boolean getLastState(UUID uuid) {
        return lastSneakState.get(uuid);
    }

    public static void setState(UUID uuid, boolean sneaking) {
        lastSneakState.put(uuid, sneaking);
    }

    public static void scheduleCycle(UUID uuid) {
        ScheduledFuture<?> existing = pendingTasks.remove(uuid);
        if (existing != null) {
            existing.cancel(false);
        }

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            var server = SoftcoreData.getServer();
            if (server == null) return;

            server.execute(() -> {
                pendingTasks.remove(uuid);
                ServerPlayer player = server.getPlayerList().getPlayer(uuid);
                if (player != null && BanManager.isBanned(player)) {
                    SpectateManager.cycleSpectateTarget(player);
                }
            });
        }, DELAY_MS, TimeUnit.MILLISECONDS);

        pendingTasks.put(uuid, future);
    }

    public static void clear(UUID uuid) {
        lastSneakState.remove(uuid);
        ScheduledFuture<?> task = pendingTasks.remove(uuid);
        if (task != null) {
            task.cancel(false);
        }
    }

    public static void shutdown() {
        scheduler.shutdownNow();
    }
}
