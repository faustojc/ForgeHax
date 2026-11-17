package com.matt.forgehax.mixin;

import com.matt.forgehax.asm.ForgeHaxHooks;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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

  /**
   * The {@code isUsingItem() && !isPassenger()} guard around the item-use {@code *= 0.2F} impulse
   * scaling (and the sprintTriggerTime reset). Ordinal 0 is that guard; ordinal 1 is the sprint
   * start check later in aiStep. Forge's MovementInputUpdateEvent fires before the scaling and
   * cannot skip it.
   */
  @Redirect(
      method = "aiStep",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z",
              ordinal = 0))
  private boolean forgehax$onItemUseSlowdown(LocalPlayer player) {
    return ForgeHaxHooks.isItemUseSlowdown(player);
  }
}
