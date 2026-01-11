package moe.syrup.config;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import moe.syrup.Softcore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ConfigLoader {

    private static final String DEFAULT_CONFIG = """
            [respawn]
            # Rules are matched from top to bottom, first match wins
            # cond format: "N in Xh" means Nth death within X hours
            # cond format: "else" means default rule
            # time format: "Xd", "Xh", "Xm", "Xs" or "0" for no ban
            cool_down_rules = [
                { cond = "1 in 48h", time = "0" },
                { cond = "1 in 24h", time = "2h" },
                { cond = "2 in 24h", time = "4h" },
                { cond = "else", time = "1h" }
            ]
            # Immunity duration after respawn, deaths during this period won't count
            respawn_immunity = "20m"
            # Show next ban duration in action bar when player has low health
            enable_hints = true
            """;

    public static SoftcoreConfig load(Path configPath) {
        if (!Files.exists(configPath)) {
            createDefaultConfig(configPath);
        }

        try (FileConfig config = FileConfig.of(configPath)) {
            config.load();
            return parseConfig(config);
        } catch (Exception e) {
            Softcore.LOGGER.error("Failed to load config, using defaults", e);
            return createDefaultSoftcoreConfig();
        }
    }

    private static SoftcoreConfig parseConfig(FileConfig config) {
        List<CoolDownRule> rules = new ArrayList<>();
        List<Config> ruleConfigs = config.get("respawn.cool_down_rules");
        if (ruleConfigs != null) {
            for (Config ruleConfig : ruleConfigs) {
                String cond = ruleConfig.get("cond");
                String time = ruleConfig.get("time");
                try {
                    rules.add(CoolDownRule.parse(cond, time));
                } catch (IllegalArgumentException e) {
                    Softcore.LOGGER.warn("Invalid rule: cond={}, time={}", cond, time, e);
                }
            }
        }
        if (rules.isEmpty()) {
            Softcore.LOGGER.warn("No valid rules found, using defaults");
            return createDefaultSoftcoreConfig();
        }

        Duration respawnImmunity = Duration.ofMinutes(20);
        String immunityStr = config.get("respawn.respawn_immunity");
        if (immunityStr != null) {
            try {
                respawnImmunity = CoolDownRule.parseTime(immunityStr);
            } catch (IllegalArgumentException e) {
                Softcore.LOGGER.warn("Invalid respawn_immunity: {}, using default 20m", immunityStr);
            }
        }

        boolean enableHints = config.getOrElse("respawn.enable_hints", true);

        return new SoftcoreConfig(rules, respawnImmunity, enableHints);
    }

    public static void createDefaultConfig(Path configPath) {
        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, DEFAULT_CONFIG);
            Softcore.LOGGER.info("Created default config at {}", configPath);
        } catch (IOException e) {
            Softcore.LOGGER.error("Failed to create default config", e);
        }
    }

    private static SoftcoreConfig createDefaultSoftcoreConfig() {
        return new SoftcoreConfig(List.of(
            CoolDownRule.parse("1 in 48h", "0"),
            CoolDownRule.parse("1 in 24h", "2h"),
            CoolDownRule.parse("2 in 24h", "4h"),
            CoolDownRule.parse("else", "1h")
        ), Duration.ofMinutes(20), true);
    }
}
