package com.matt.forgehax.mods.services;

import com.matt.forgehax.events.Render2DEvent;
import com.matt.forgehax.events.RenderEvent;
import com.matt.forgehax.util.draw.SurfaceHelper;
import com.matt.forgehax.util.mod.ServiceMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import com.matt.forgehax.util.tesselation.GeometryTessellator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Created on 6/14/2017 by fr1kin
 */
@RegisterMod
public class RenderEventService extends ServiceMod {

  public RenderEventService() {
    super("RenderEventService");
  }

  @SubscribeEvent
  public void onRenderWorld(RenderLevelStageEvent event) {
    if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
      return;
    }

    PoseStack poseStack = event.getPoseStack();
    MultiBufferSource.BufferSource bufferSource = MC.renderBuffers().bufferSource();
    RenderType lines = getSeeThroughLines();
    VertexConsumer buffer = bufferSource.getBuffer(lines);

    GeometryTessellator tessellator = new GeometryTessellator(poseStack, buffer);
    // The pose stack only carries the camera rotation (LevelRenderer.renderLevel offsets world
    // coordinates by Camera#getPosition, which includes eye height), so translate by the camera.
    Vec3 renderPos = event.getCamera().getPosition();

    RenderEvent e = new RenderEvent(tessellator, renderPos, event.getPartialTick());
    e.resetTranslation();
    MinecraftForge.EVENT_BUS.post(e);

    bufferSource.endBatch(lines);
  }

  // RenderType.lines() is depth-tested (LEQUAL) and depth-writing with a window-scaled line
  // width, so the world pass was occluded by terrain. The 1.12.2 pass disabled depth and used
  // width 1; this is the same state as lines() minus depth test/write, with the default width.
  // Built lazily: RenderType's static init must not run during mod construction.
  private static RenderType seeThroughLines;

  private static RenderType getSeeThroughLines() {
    if (seeThroughLines == null) {
      seeThroughLines = RenderType.create(
          "forgehax_lines_see_through",
          DefaultVertexFormat.POSITION_COLOR_NORMAL,
          VertexFormat.Mode.LINES,
          256,
          false,
          false,
          RenderType.CompositeState.builder()
              .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeLinesShader))
              .setTransparencyState(new RenderStateShard.TransparencyStateShard(
                  "forgehax_translucent",
                  () -> {
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                  },
                  RenderSystem::disableBlend))
              .setDepthTestState(new RenderStateShard.DepthTestStateShard("always", 519))
              .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true, false))
              .setCullState(new RenderStateShard.CullStateShard(false))
              .createCompositeState(false));
    }
    return seeThroughLines;
  }

  // RenderGuiEvent.Post fires once per frame after the whole HUD. RenderGuiOverlayEvent.Post
  // fires once per registered overlay, which ran every 2D listener ~14 times a frame.
  @SubscribeEvent(priority = EventPriority.LOW)
  public void onRenderGuiOverlay(final RenderGuiEvent.Post event) {
    // SurfaceHelper/SurfaceBuilder item and text drawing is a no-op without an active GuiGraphics.
    SurfaceHelper.setGraphics(event.getGuiGraphics());
    try {
      MinecraftForge.EVENT_BUS.post(new Render2DEvent(event.getPartialTick()));
    } finally {
      SurfaceHelper.setGraphics(null);
    }
  }
}
