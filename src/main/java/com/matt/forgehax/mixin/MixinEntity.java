package com.matt.forgehax.mixin;

import com.matt.forgehax.asm.ForgeHaxHooks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Stand-in for the 1.12.2 {@code Entity#doBlockCollisions} patch. Soul sand no longer slows
 * through block collisions; it slows through {@code Block#getSpeedFactor}, which is read twice
 * (block at the feet, then the block below) in {@code Entity#getBlockSpeedFactor}. No ordinal is
 * given so both reads are redirected.
 */
@Mixin(Entity.class)
public abstract class MixinEntity {

  @Redirect(
      method = "getBlockSpeedFactor",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/world/level/block/Block;getSpeedFactor()F"))
  private float forgehax$onGetBlockSpeedFactor(Block block) {
    return ForgeHaxHooks.getBlockSpeedFactor((Entity) (Object) this, block);
  }
}
