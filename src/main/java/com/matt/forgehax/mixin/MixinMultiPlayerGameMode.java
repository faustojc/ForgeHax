package com.matt.forgehax.mixin;

import com.matt.forgehax.asm.ForgeHaxHooks;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MixinMultiPlayerGameMode {

  @Inject(method = "attack", at = @At("HEAD"))
  private void forgehax$onPlayerAttackEntity(Player attacker, Entity victim, CallbackInfo ci) {
    ForgeHaxHooks.onPlayerAttackEntity(
        (MultiPlayerGameMode) (Object) this, attacker, victim);
  }

  @Inject(method = "continueDestroyBlock", at = @At("HEAD"))
  private void forgehax$onPlayerBreakingBlock(
      BlockPos pos, Direction facing, CallbackInfoReturnable<Boolean> cir) {
    ForgeHaxHooks.onPlayerBreakingBlock((MultiPlayerGameMode) (Object) this, pos, facing);
  }

  @Inject(method = "ensureHasSentCarriedItem", at = @At("HEAD"))
  private void forgehax$onPlayerItemSync(CallbackInfo ci) {
    ForgeHaxHooks.onPlayerItemSync((MultiPlayerGameMode) (Object) this);
  }

  @Inject(method = "releaseUsingItem", at = @At("HEAD"), cancellable = true)
  private void forgehax$onPlayerStopUse(Player player, CallbackInfo ci) {
    if (ForgeHaxHooks.onPlayerStopUse((MultiPlayerGameMode) (Object) this, player)) {
      ci.cancel();
    }
  }
}
