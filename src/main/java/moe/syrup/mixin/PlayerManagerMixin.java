package moe.syrup.mixin;

import moe.syrup.ban.BanManager;
import moe.syrup.ban.BanState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerList.class)
public abstract class PlayerManagerMixin {

    @Inject(method = "respawn", at = @At("RETURN"))
    private void onRespawn(ServerPlayer player, boolean alive, Entity.RemovalReason removalReason, CallbackInfoReturnable<ServerPlayer> cir) {
        ServerPlayer newPlayer = cir.getReturnValue();
        if (!alive) {
            // 重生时应用封禁效果（封禁已在死亡时开始）
            BanState state = BanManager.getBanState(newPlayer);
            if (state != null && state.isBanned() && !state.isExpired()) {
                newPlayer.setGameMode(GameType.SPECTATOR);
                BanManager.applyBanEffects(newPlayer, state);
            }
        }
    }
}
