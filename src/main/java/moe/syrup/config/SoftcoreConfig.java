package moe.syrup.config;

import java.time.Duration;
import java.util.List;

public class SoftcoreConfig {
    private List<CoolDownRule> coolDownRules;
    private Duration respawnImmunity;
    private boolean enableHints;

    public SoftcoreConfig(List<CoolDownRule> coolDownRules, Duration respawnImmunity, boolean enableHints) {
        this.coolDownRules = coolDownRules;
        this.respawnImmunity = respawnImmunity;
        this.enableHints = enableHints;
    }

    public List<CoolDownRule> getCoolDownRules() {
        return coolDownRules;
    }

    public Duration getRespawnImmunity() {
        return respawnImmunity;
    }

    public boolean isEnableHints() {
        return enableHints;
    }
}
