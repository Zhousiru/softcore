package moe.syrup.data;

import moe.syrup.ban.BanState;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerData {
    private UUID playerId;
    private List<Instant> deathHistory;
    private BanState banState;

    public PlayerData(UUID playerId) {
        this.playerId = playerId;
        this.deathHistory = new ArrayList<>();
        this.banState = null;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public List<Instant> getDeathHistory() {
        return deathHistory;
    }

    public BanState getBanState() {
        return banState;
    }

    public void setBanState(BanState banState) {
        this.banState = banState;
    }

    public void recordDeath(Instant time) {
        deathHistory.add(time);
    }

    public void clearDeathHistory() {
        deathHistory.clear();
    }
}
