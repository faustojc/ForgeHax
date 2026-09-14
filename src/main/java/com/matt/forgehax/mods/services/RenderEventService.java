package com.matt.forgehax.mods.services;

import com.matt.forgehax.events.Render2DEvent;
import com.matt.forgehax.events.RenderEvent;
import com.matt.forgehax.util.entity.EntityUtils;
import com.matt.forgehax.util.mod.ServiceMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import com.matt.forgehax.util.tesselation.GeometryTessellator;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import static com.matt.forgehax.Helper.getRenderEntity;

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
    VertexConsumer buffer = bufferSource.getBuffer(RenderType.lines());

    GeometryTessellator tessellator = new GeometryTessellator(poseStack, buffer);
    Vec3 renderPos = EntityUtils.getInterpolatedPos(getRenderEntity(), event.getPartialTick());

    RenderEvent e = new RenderEvent(tessellator, renderPos, event.getPartialTick());
    e.resetTranslation();
    MinecraftForge.EVENT_BUS.post(e);

    bufferSource.endBatch(RenderType.lines());
  }

  // RenderGuiEvent.Post fires once per frame after the whole HUD. RenderGuiOverlayEvent.Post
  // fires once per registered overlay, which ran every 2D listener ~14 times a frame.
  @SubscribeEvent(priority = EventPriority.LOW)
  public void onRenderGuiOverlay(final RenderGuiEvent.Post event) {
    MinecraftForge.EVENT_BUS.post(new Render2DEvent(event.getPartialTick()));
  }
}
