package moe.syrup.ban;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class BanState {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private Instant banEndTime;

    public BanState(Instant banEndTime) {
        this.banEndTime = banEndTime;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(banEndTime);
    }

    public Instant getBanEndTime() {
        return banEndTime;
    }

    public String formatRemainingTime() {
        Duration remaining = Duration.between(Instant.now(), banEndTime);
        if (remaining.isNegative()) return "0 秒";

        long days = remaining.toDays();
        long hours = remaining.toHours() % 24;
        long minutes = remaining.toMinutes() % 60;
        long seconds = remaining.getSeconds() % 60;

        if (days > 0) return String.format("%d天%d时%d分", days, hours, minutes);
        if (hours > 0) return String.format("%d时%d分%d秒", hours, minutes, seconds);
        if (minutes > 0) return String.format("%d分%d秒", minutes, seconds);
        return String.format("%d秒", seconds);
    }

    public String formatAbsoluteTime() {
        return banEndTime.atZone(ZoneId.systemDefault()).format(FORMATTER);
    }
}
