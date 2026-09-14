package com.matt.forgehax.mixin;

import com.matt.forgehax.asm.ForgeHaxHooks;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {

  @ModifyVariable(method = "continueAttack(Z)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
  private boolean forgehax$onSendClickBlockToController(boolean clicked) {
    return ForgeHaxHooks.onSendClickBlockToController((Minecraft) (Object) this, clicked);
  }
}
