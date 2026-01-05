package moe.syrup.mixin;

import moe.syrup.ban.BanManager;
import moe.syrup.ban.BanState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class TabListMixin {

    @Inject(method = "getTabListDisplayName", at = @At("HEAD"), cancellable = true)
    private void modifyTabListName(CallbackInfoReturnable<Component> cir) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        BanState state = BanManager.getBanState(player);
        if (state != null && state.isBanned() && !state.isExpired()) {
            String remaining = state.formatRemainingTime();
            String name = player.getGameProfile().name();
            cir.setReturnValue(Component.literal(name + " §7[§c" + remaining + "§7]"));
        }
    }
}
