package com.matt.forgehax.mixin;

import com.matt.forgehax.asm.ForgeHaxHooks;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class MixinLocalPlayer {

  @Inject(method = "sendPosition()V", at = @At("HEAD"), cancellable = true)
  private void forgehax$onUpdateWalkingPlayerPre(CallbackInfo ci) {
    if (ForgeHaxHooks.onUpdateWalkingPlayerPre((LocalPlayer) (Object) this)) {
      ci.cancel();
    }
  }

  @Inject(method = "sendPosition()V", at = @At("RETURN"))
  private void forgehax$onUpdateWalkingPlayerPost(CallbackInfo ci) {
    ForgeHaxHooks.onUpdateWalkingPlayerPost((LocalPlayer) (Object) this);
  }
}
