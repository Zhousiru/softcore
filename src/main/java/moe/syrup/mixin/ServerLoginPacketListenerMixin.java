package moe.syrup.mixin;

import com.mojang.authlib.GameProfile;
import moe.syrup.ban.BanManager;
import moe.syrup.ban.BanState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLoginPacketListenerImpl.class)
public class ServerLoginPacketListenerMixin {

    @Shadow
    private GameProfile authenticatedProfile;

    @Inject(method = "verifyLoginAndFinishConnectionSetup", at = @At("HEAD"), cancellable = true)
    private void onVerifyLogin(GameProfile profile, CallbackInfo ci) {
        if (profile == null || profile.id() == null) return;

        BanState state = BanManager.checkBanState(profile.id());
        if (state == null) return;

        ServerLoginPacketListenerImpl self = (ServerLoginPacketListenerImpl) (Object) this;
        self.disconnect(BanManager.createBanMessage(state));
        ci.cancel();
    }
}
