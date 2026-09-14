package com.matt.forgehax.mixin;

import com.matt.forgehax.asm.ForgeHaxHooks;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.12.2 asked {@code Block#canRenderInLayer} per layer; 1.20.1 resolves a single chunk layer
 * here, so the event's layer and compare-to layer are the same value.
 */
@Mixin(ItemBlockRenderTypes.class)
public abstract class MixinItemBlockRenderTypes {

  @Inject(method = "getChunkRenderType", at = @At("RETURN"), cancellable = true)
  private static void forgehax$onRenderBlockInLayer(
      BlockState state, CallbackInfoReturnable<RenderType> cir) {
    RenderType layer = cir.getReturnValue();
    RenderType result =
        ForgeHaxHooks.onRenderBlockInLayer(state.getBlock(), state, layer, layer);
    if (result != layer) {
      cir.setReturnValue(result);
    }
  }
}
