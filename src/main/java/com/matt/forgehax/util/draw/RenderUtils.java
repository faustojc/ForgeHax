package com.matt.forgehax.util.draw;

import com.matt.forgehax.Globals;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;

/** World-space line and box helpers for the modern vertex pipeline. */
public final class RenderUtils implements Globals {

  private RenderUtils() {
  }

  public static Vec3 getRenderPos() {
    Entity entity = MC.getCameraEntity();
    if (entity == null) {
      return Vec3.ZERO;
    }
    double partialTicks = MC.getFrameTime();
    return new Vec3(
        entity.xo + (entity.getX() - entity.xo) * partialTicks,
        entity.yo + (entity.getY() - entity.yo) * partialTicks,
        entity.zo + (entity.getZ() - entity.zo) * partialTicks
    );
  }

  public static void drawLine(
      Vec3 startPos, Vec3 endPos, int color, boolean smooth, float width) {
    Vec3 renderPos = getRenderPos();
    Vec3 start = startPos.subtract(renderPos);
    Vec3 end = endPos.subtract(renderPos);

    if (smooth) {
      GL11.glEnable(GL11.GL_LINE_SMOOTH);
    }
    RenderSystem.lineWidth(width);
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    RenderSystem.disableDepthTest();

    BufferBuilder builder = Tesselator.getInstance().getBuilder();
    builder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
    vertex(builder, start.x, start.y, start.z, color);
    vertex(builder, end.x, end.y, end.z, color);
    draw(builder);

    RenderSystem.enableDepthTest();
    RenderSystem.disableBlend();
    if (smooth) {
      GL11.glDisable(GL11.GL_LINE_SMOOTH);
    }
  }

  public static void drawBox(
      Vec3 startPos, Vec3 endPos, int color, float width, boolean ignoreZ) {
    Vec3 renderPos = getRenderPos();
    Vec3 min = startPos.subtract(renderPos);
    Vec3 max = endPos.subtract(renderPos);

    RenderSystem.lineWidth(width);
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    if (ignoreZ) {
      RenderSystem.disableDepthTest();
    }

    BufferBuilder builder = Tesselator.getInstance().getBuilder();
    builder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

    line(builder, min.x, min.y, min.z, max.x, min.y, min.z, color);
    line(builder, max.x, min.y, min.z, max.x, min.y, max.z, color);
    line(builder, max.x, min.y, max.z, min.x, min.y, max.z, color);
    line(builder, min.x, min.y, max.z, min.x, min.y, min.z, color);

    line(builder, min.x, max.y, min.z, max.x, max.y, min.z, color);
    line(builder, max.x, max.y, min.z, max.x, max.y, max.z, color);
    line(builder, max.x, max.y, max.z, min.x, max.y, max.z, color);
    line(builder, min.x, max.y, max.z, min.x, max.y, min.z, color);

    line(builder, min.x, min.y, min.z, min.x, max.y, min.z, color);
    line(builder, max.x, min.y, min.z, max.x, max.y, min.z, color);
    line(builder, max.x, min.y, max.z, max.x, max.y, max.z, color);
    line(builder, min.x, min.y, max.z, min.x, max.y, max.z, color);

    draw(builder);
    RenderSystem.enableDepthTest();
    RenderSystem.disableBlend();
    RenderSystem.enableCull();
  }

  public static void drawBox(
      BlockPos startPos, BlockPos endPos, int color, float width, boolean ignoreZ) {
    drawBox(
        new Vec3(startPos.getX(), startPos.getY(), startPos.getZ()),
        new Vec3(endPos.getX(), endPos.getY(), endPos.getZ()),
        color,
        width,
        ignoreZ
    );
  }

  private static void line(
      BufferBuilder builder,
      double x0,
      double y0,
      double z0,
      double x1,
      double y1,
      double z1,
      int color) {
    vertex(builder, x0, y0, z0, color);
    vertex(builder, x1, y1, z1, color);
  }

  private static void vertex(BufferBuilder builder, double x, double y, double z, int color) {
    builder.vertex(x, y, z)
        .color(color >> 16 & 255, color >> 8 & 255, color & 255, color >>> 24 & 255)
        .endVertex();
  }

  private static void draw(BufferBuilder builder) {
    RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);
    BufferUploader.drawWithShader(builder.end());
  }
}
