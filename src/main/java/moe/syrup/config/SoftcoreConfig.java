package moe.syrup.config;

import java.util.List;

public class SoftcoreConfig {
    private List<CoolDownRule> coolDownRules;

    public SoftcoreConfig(List<CoolDownRule> coolDownRules) {
        this.coolDownRules = coolDownRules;
    }

    public List<CoolDownRule> getCoolDownRules() {
        return coolDownRules;
    }
}
