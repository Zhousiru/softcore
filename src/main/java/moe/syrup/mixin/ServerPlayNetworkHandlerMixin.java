package moe.syrup.mixin;

import moe.syrup.ban.BanManager;
import moe.syrup.spectate.SneakStateTracker;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerPlayNetworkHandlerMixin {

    @Shadow
    public ServerPlayer player;

    @Inject(method = "handlePlayerInput", at = @At("HEAD"))
    private void onPlayerInput(ServerboundPlayerInputPacket packet, CallbackInfo ci) {
        if (player == null) return;

        UUID uuid = player.getUUID();
        boolean currentSneak = packet.input().shift();
        Boolean lastSneak = SneakStateTracker.getLastState(uuid);

        // 边缘检测：从未按到按下（null 视为未按）
        boolean wasNotSneaking = lastSneak == null || !lastSneak;
        if (wasNotSneaking && currentSneak) {
            if (BanManager.isBanned(player)) {
                // 安排延迟切换，而不是立即切换
                SneakStateTracker.scheduleCycle(uuid);
            }
        }
        SneakStateTracker.setState(uuid, currentSneak);
    }
}
