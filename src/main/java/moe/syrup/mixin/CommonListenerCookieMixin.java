package moe.syrup.mixin;

import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientboundLoginPacket.class)
public abstract class CommonListenerCookieMixin {

    @Inject(method = "hardcore", at = @At("HEAD"), cancellable = true)
    private void forceHardcore(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }
}
