package com.matt.forgehax.events;

import com.matt.forgehax.util.tesselation.GeometryTessellator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.Event;

/**
 * Created on 5/5/2017 by fr1kin
 */
public class RenderEvent extends Event {

  private final GeometryTessellator tessellator;
  private final Vec3 renderPos;
  private final double partialTicks;

  public RenderEvent(GeometryTessellator tessellator, Vec3 renderPos, double partialTicks) {
    this.tessellator = tessellator;
    this.renderPos = renderPos;
    this.partialTicks = partialTicks;
  }

  public GeometryTessellator getTessellator() {
    return tessellator;
  }

  public VertexConsumer getBuffer() {
    return tessellator.getBuffer();
  }

  public Vec3 getRenderPos() {
    return renderPos;
  }

  public void setTranslation(Vec3 translation) {
    tessellator.setTranslation(-translation.x, -translation.y, -translation.z);
  }

  public void resetTranslation() {
    setTranslation(renderPos);
  }

  public double getPartialTicks() {
    return partialTicks;
  }
}
