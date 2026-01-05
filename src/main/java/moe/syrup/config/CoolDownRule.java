package moe.syrup.config;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record CoolDownRule(RuleCondition condition, Duration banDuration) {

    private static final Pattern COUNT_IN_TIME_PATTERN = Pattern.compile("(\\d+)\\s+in\\s+(\\d+)([hms])");
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)([dhms])");

    public sealed interface RuleCondition permits RuleCondition.CountInTime, RuleCondition.Else {
        record CountInTime(int count, Duration timeWindow) implements RuleCondition {}
        record Else() implements RuleCondition {}
    }

    public boolean matches(List<Instant> deathHistory) {
        return switch (condition) {
            case RuleCondition.CountInTime(int count, Duration window) -> {
                Instant cutoff = Instant.now().minus(window);
                long recentDeaths = deathHistory.stream()
                    .filter(t -> t.isAfter(cutoff))
                    .count();
                yield recentDeaths == count;
            }
            case RuleCondition.Else() -> true;
        };
    }

    public static CoolDownRule parse(String condStr, String timeStr) {
        RuleCondition condition = parseCondition(condStr.trim());
        Duration duration = parseTime(timeStr.trim());
        return new CoolDownRule(condition, duration);
    }

    private static RuleCondition parseCondition(String condStr) {
        if (condStr.equalsIgnoreCase("else")) {
            return new RuleCondition.Else();
        }
        Matcher matcher = COUNT_IN_TIME_PATTERN.matcher(condStr);
        if (matcher.matches()) {
            int count = Integer.parseInt(matcher.group(1));
            int value = Integer.parseInt(matcher.group(2));
            String unit = matcher.group(3);
            Duration window = switch (unit) {
                case "h" -> Duration.ofHours(value);
                case "m" -> Duration.ofMinutes(value);
                case "s" -> Duration.ofSeconds(value);
                default -> throw new IllegalArgumentException("Unknown time unit: " + unit);
            };
            return new RuleCondition.CountInTime(count, window);
        }
        throw new IllegalArgumentException("Invalid condition format: " + condStr);
    }

    public static Duration parseTime(String timeStr) {
        if (timeStr.equals("0")) {
            return Duration.ZERO;
        }
        Matcher matcher = TIME_PATTERN.matcher(timeStr);
        if (matcher.matches()) {
            int value = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);
            return switch (unit) {
                case "d" -> Duration.ofDays(value);
                case "h" -> Duration.ofHours(value);
                case "m" -> Duration.ofMinutes(value);
                case "s" -> Duration.ofSeconds(value);
                default -> throw new IllegalArgumentException("Unknown time unit: " + unit);
            };
        }
        throw new IllegalArgumentException("Invalid time format: " + timeStr);
    }
}
