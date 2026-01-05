package moe.syrup.mixin;

import moe.syrup.ban.BanManager;
import moe.syrup.ban.BanState;
import moe.syrup.data.SoftcoreData;
import moe.syrup.spectate.SpectateState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.EnumSet;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerEntityMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        BanState state = BanManager.getBanState(player);
        if (state == null || !state.isBanned() || state.isExpired()) {
            SpectateState.clear(player.getUUID());
            return;
        }

        ServerPlayer spectateTarget = SpectateState.getTarget(player.getUUID());

        if (spectateTarget != null && !BanManager.isBanned(spectateTarget)) {
            player.setCamera(spectateTarget);

            double dx = player.getX() - spectateTarget.getX();
            double dy = player.getY() - spectateTarget.getY();
            double dz = player.getZ() - spectateTarget.getZ();
            double distSq = dx * dx + dy * dy + dz * dz;

            if (distSq > 100.0) {
                player.teleportTo(
                    (ServerLevel) spectateTarget.level(),
                    spectateTarget.getX(),
                    spectateTarget.getY(),
                    spectateTarget.getZ(),
                    EnumSet.noneOf(Relative.class),
                    spectateTarget.getYRot(),
                    spectateTarget.getXRot(),
                    false
                );
            }
        } else {
            if (SpectateState.getTargetUuid(player.getUUID()) != null) {
                SpectateState.clear(player.getUUID());
                player.setCamera(player);
            }

            ServerLevel level = SoftcoreData.getServer().getLevel(state.getSpawnDimension());
            if (level == null || player.level() != level) return;

            BlockPos pos = state.getSpawnPosition();
            double dx = player.getX() - (pos.getX() + 0.5);
            double dy = player.getY() - pos.getY();
            double dz = player.getZ() - (pos.getZ() + 0.5);
            double distSq = dx * dx + dy * dy + dz * dz;

            if (distSq > 4.0) {
                player.teleportTo(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                    EnumSet.noneOf(Relative.class), player.getYRot(), player.getXRot(), false);
            }
        }
    }
}
