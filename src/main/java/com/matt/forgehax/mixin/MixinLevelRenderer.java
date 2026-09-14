package com.matt.forgehax.mixin;

import com.matt.forgehax.asm.ForgeHaxHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.renderer.LevelRenderer.class)
public abstract class MixinLevelRenderer {

  // renderChunkLayer has no partial-tick argument in 1.20.1, so the event reports 0.
  @Inject(method = "renderChunkLayer", at = @At("HEAD"), cancellable = true)
  private void forgehax$onPreRenderBlockLayer(
      RenderType layer, PoseStack poseStack, double camX, double camY, double camZ,
      Matrix4f projection, CallbackInfo ci) {
    if (ForgeHaxHooks.onPreRenderBlockLayer(layer, 0.D)) {
      ci.cancel();
    }
  }

  @Inject(method = "renderChunkLayer", at = @At("RETURN"))
  private void forgehax$onPostRenderBlockLayer(
      RenderType layer, PoseStack poseStack, double camX, double camY, double camZ,
      Matrix4f projection, CallbackInfo ci) {
    ForgeHaxHooks.onPostRenderBlockLayer(layer, 0.D);
  }
}
