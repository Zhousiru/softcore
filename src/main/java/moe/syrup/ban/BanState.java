package moe.syrup.ban;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.time.Duration;
import java.time.Instant;

public class BanState {
    private boolean banned;
    private Instant banEndTime;
    private BlockPos spawnPosition;
    private ResourceKey<Level> spawnDimension;

    public BanState(Instant banEndTime, BlockPos spawnPosition, ResourceKey<Level> spawnDimension) {
        this.banned = true;
        this.banEndTime = banEndTime;
        this.spawnPosition = spawnPosition;
        this.spawnDimension = spawnDimension;
    }

    public boolean isBanned() {
        return banned;
    }

    public boolean isExpired() {
        return banned && Instant.now().isAfter(banEndTime);
    }

    public Duration getRemainingTime() {
        if (!banned || isExpired()) return Duration.ZERO;
        return Duration.between(Instant.now(), banEndTime);
    }

    public String formatRemainingTime() {
        Duration remaining = getRemainingTime();
        long days = remaining.toDays();
        long hours = remaining.toHours() % 24;
        long minutes = remaining.toMinutes() % 60;
        long seconds = remaining.getSeconds() % 60;
        if (days > 0) {
            return String.format("%d 天 %d 小时 %d 分 %d 秒", days, hours, minutes, seconds);
        } else if (hours > 0) {
            return String.format("%d 小时 %d 分 %d 秒", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%d 分 %d 秒", minutes, seconds);
        } else {
            return String.format("%d 秒", seconds);
        }
    }

    public Instant getBanEndTime() {
        return banEndTime;
    }

    public BlockPos getSpawnPosition() {
        return spawnPosition;
    }

    public ResourceKey<Level> getSpawnDimension() {
        return spawnDimension;
    }

    public void clearBan() {
        this.banned = false;
    }
}
