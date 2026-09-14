package com.matt.forgehax.util.tesselation;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import org.lwjgl.opengl.GL11;

/** Geometry helpers backed by Minecraft's 1.20 vertex consumer pipeline. */
public class GeometryTessellator {

  private static GeometryTessellator instance;
  private static double staticDelta;

  private final Tesselator tesselator;
  private final PoseStack poseStack;
  private final VertexConsumer externalBuffer;
  private BufferBuilder builder;
  private VertexConsumer buffer;
  private VertexFormat.Mode mode;
  private double delta;
  private double translationX;
  private double translationY;
  private double translationZ;

  public GeometryTessellator() {
    this(0x200000);
  }

  public GeometryTessellator(final int size) {
    this.tesselator = new Tesselator(size);
    this.poseStack = null;
    this.externalBuffer = null;
    this.builder = tesselator.getBuilder();
    this.buffer = builder;
  }

  /** Creates an adapter for a consumer supplied by a render event. */
  public GeometryTessellator(PoseStack poseStack, VertexConsumer buffer) {
    this.tesselator = null;
    this.poseStack = poseStack;
    this.externalBuffer = buffer;
    this.buffer = buffer;
  }

  public static GeometryTessellator getInstance() {
    if (instance == null) {
      instance = new GeometryTessellator();
    }
    return instance;
  }

  public static void setStaticDelta(final double delta) {
    staticDelta = delta;
  }

  public VertexConsumer getBuffer() {
    return buffer;
  }

  public BufferBuilder getBuilder() {
    return builder;
  }

  public PoseStack getPoseStack() {
    return poseStack;
  }

  public static void drawCuboid(
      final VertexConsumer buffer, final BlockPos pos, final int sides, final int argb) {
    drawCuboid(buffer, null, pos, pos, sides, argb, staticDelta);
  }

  public static void drawCuboid(
      final PoseStack pose,
      final VertexConsumer buffer,
      final BlockPos pos,
      final int sides,
      final int argb) {
    drawCuboid(buffer, pose, pos, pos, sides, argb, staticDelta);
  }

  public static void drawCuboid(
      final VertexConsumer buffer,
      final BlockPos begin,
      final BlockPos end,
      final int sides,
      final int argb) {
    drawCuboid(buffer, null, begin, end, sides, argb, staticDelta);
  }

  public static void drawCuboid(
      final PoseStack pose,
      final VertexConsumer buffer,
      final BlockPos begin,
      final BlockPos end,
      final int sides,
      final int argb) {
    drawCuboid(buffer, pose, begin, end, sides, argb, staticDelta);
  }

  private static void drawCuboid(
      final VertexConsumer buffer,
      final PoseStack pose,
      final BlockPos begin,
      final BlockPos end,
      final int sides,
      final int argb,
      final double delta) {
    if (buffer == null || sides == 0) {
      return;
    }

    final double x0 = begin.getX() - delta;
    final double y0 = begin.getY() - delta;
    final double z0 = begin.getZ() - delta;
    final double x1 = end.getX() + 1 + delta;
    final double y1 = end.getY() + 1 + delta;
    final double z1 = end.getZ() + 1 + delta;

    drawLines(pose, buffer, x0, y0, z0, x1, y1, z1, sides, argb);
  }

  public static void drawQuads(
      final VertexConsumer buffer,
      final double x0,
      final double y0,
      final double z0,
      final double x1,
      final double y1,
      final double z1,
      final int sides,
      final int argb) {
    drawQuads(null, buffer, x0, y0, z0, x1, y1, z1, sides,
        argb >>> 24 & 0xFF, argb >>> 16 & 0xFF, argb >>> 8 & 0xFF, argb & 0xFF);
  }

  public static void drawQuads(
      final PoseStack pose,
      final VertexConsumer buffer,
      final double x0,
      final double y0,
      final double z0,
      final double x1,
      final double y1,
      final double z1,
      final int sides,
      final int argb) {
    drawQuads(pose, buffer, x0, y0, z0, x1, y1, z1, sides,
        argb >>> 24 & 0xFF, argb >>> 16 & 0xFF, argb >>> 8 & 0xFF, argb & 0xFF);
  }

  public static void drawQuads(
      final VertexConsumer buffer,
      final double x0,
      final double y0,
      final double z0,
      final double x1,
      final double y1,
      final double z1,
      final int sides,
      final int a,
      final int r,
      final int g,
      final int b) {
    drawQuads(null, buffer, x0, y0, z0, x1, y1, z1, sides, a, r, g, b);
  }

  public static void drawQuads(
      final PoseStack pose,
      final VertexConsumer buffer,
      final double x0,
      final double y0,
      final double z0,
      final double x1,
      final double y1,
      final double z1,
      final int sides,
      final int a,
      final int r,
      final int g,
      final int b) {
    if (buffer == null) {
      return;
    }
    if ((sides & GeometryMasks.Quad.DOWN) != 0) {
      vertex(buffer, pose, x1, y0, z0, r, g, b, a);
      vertex(buffer, pose, x1, y0, z1, r, g, b, a);
      vertex(buffer, pose, x0, y0, z1, r, g, b, a);
      vertex(buffer, pose, x0, y0, z0, r, g, b, a);
    }
    if ((sides & GeometryMasks.Quad.UP) != 0) {
      vertex(buffer, pose, x1, y1, z0, r, g, b, a);
      vertex(buffer, pose, x0, y1, z0, r, g, b, a);
      vertex(buffer, pose, x0, y1, z1, r, g, b, a);
      vertex(buffer, pose, x1, y1, z1, r, g, b, a);
    }
    if ((sides & GeometryMasks.Quad.NORTH) != 0) {
      vertex(buffer, pose, x1, y0, z0, r, g, b, a);
      vertex(buffer, pose, x0, y0, z0, r, g, b, a);
      vertex(buffer, pose, x0, y1, z0, r, g, b, a);
      vertex(buffer, pose, x1, y1, z0, r, g, b, a);
    }
    if ((sides & GeometryMasks.Quad.SOUTH) != 0) {
      vertex(buffer, pose, x0, y0, z1, r, g, b, a);
      vertex(buffer, pose, x1, y0, z1, r, g, b, a);
      vertex(buffer, pose, x1, y1, z1, r, g, b, a);
      vertex(buffer, pose, x0, y1, z1, r, g, b, a);
    }
    if ((sides & GeometryMasks.Quad.WEST) != 0) {
      vertex(buffer, pose, x0, y0, z0, r, g, b, a);
      vertex(buffer, pose, x0, y0, z1, r, g, b, a);
      vertex(buffer, pose, x0, y1, z1, r, g, b, a);
      vertex(buffer, pose, x0, y1, z0, r, g, b, a);
    }
    if ((sides & GeometryMasks.Quad.EAST) != 0) {
      vertex(buffer, pose, x1, y0, z1, r, g, b, a);
      vertex(buffer, pose, x1, y0, z0, r, g, b, a);
      vertex(buffer, pose, x1, y1, z0, r, g, b, a);
      vertex(buffer, pose, x1, y1, z1, r, g, b, a);
    }
  }

  public static void drawLines(
      final VertexConsumer buffer,
      final double x0,
      final double y0,
      final double z0,
      final double x1,
      final double y1,
      final double z1,
      final int sides,
      final int argb) {
    drawLines(null, buffer, x0, y0, z0, x1, y1, z1, sides,
        argb >>> 24 & 0xFF, argb >>> 16 & 0xFF, argb >>> 8 & 0xFF, argb & 0xFF);
  }

  public static void drawLines(
      final PoseStack pose,
      final VertexConsumer buffer,
      final double x0,
      final double y0,
      final double z0,
      final double x1,
      final double y1,
      final double z1,
      final int sides,
      final int argb) {
    drawLines(pose, buffer, x0, y0, z0, x1, y1, z1, sides,
        argb >>> 24 & 0xFF, argb >>> 16 & 0xFF, argb >>> 8 & 0xFF, argb & 0xFF);
  }

  public static void drawLines(
      final VertexConsumer buffer,
      final double x0,
      final double y0,
      final double z0,
      final double x1,
      final double y1,
      final double z1,
      final int sides,
      final int a,
      final int r,
      final int g,
      final int b) {
    drawLines(null, buffer, x0, y0, z0, x1, y1, z1, sides, a, r, g, b);
  }

  public static void drawLines(
      final PoseStack pose,
      final VertexConsumer buffer,
      final double x0,
      final double y0,
      final double z0,
      final double x1,
      final double y1,
      final double z1,
      final int sides,
      final int a,
      final int r,
      final int g,
      final int b) {
    if (buffer == null) {
      return;
    }
    if ((sides & GeometryMasks.Line.DOWN_WEST) != 0) {
      line(buffer, pose, x0, y0, z0, x0, y0, z1, r, g, b, a);
    }
    if ((sides & GeometryMasks.Line.UP_WEST) != 0) {
      line(buffer, pose, x0, y1, z0, x0, y1, z1, r, g, b, a);
    }
    if ((sides & GeometryMasks.Line.DOWN_EAST) != 0) {
      line(buffer, pose, x1, y0, z0, x1, y0, z1, r, g, b, a);
    }
    if ((sides & GeometryMasks.Line.UP_EAST) != 0) {
      line(buffer, pose, x1, y1, z0, x1, y1, z1, r, g, b, a);
    }
    if ((sides & GeometryMasks.Line.DOWN_NORTH) != 0) {
      line(buffer, pose, x0, y0, z0, x1, y0, z0, r, g, b, a);
    }
    if ((sides & GeometryMasks.Line.UP_NORTH) != 0) {
      line(buffer, pose, x0, y1, z0, x1, y1, z0, r, g, b, a);
    }
    if ((sides & GeometryMasks.Line.DOWN_SOUTH) != 0) {
      line(buffer, pose, x0, y0, z1, x1, y0, z1, r, g, b, a);
    }
    if ((sides & GeometryMasks.Line.UP_SOUTH) != 0) {
      line(buffer, pose, x0, y1, z1, x1, y1, z1, r, g, b, a);
    }
    if ((sides & GeometryMasks.Line.NORTH_WEST) != 0) {
      line(buffer, pose, x0, y0, z0, x0, y1, z0, r, g, b, a);
    }
    if ((sides & GeometryMasks.Line.NORTH_EAST) != 0) {
      line(buffer, pose, x1, y0, z0, x1, y1, z0, r, g, b, a);
    }
    if ((sides & GeometryMasks.Line.SOUTH_WEST) != 0) {
      line(buffer, pose, x0, y0, z1, x0, y1, z1, r, g, b, a);
    }
    if ((sides & GeometryMasks.Line.SOUTH_EAST) != 0) {
      line(buffer, pose, x1, y0, z1, x1, y1, z1, r, g, b, a);
    }
  }

  public void setTranslation(final double x, final double y, final double z) {
    translationX = x;
    translationY = y;
    translationZ = z;
  }

  public void beginQuads() {
    begin(GL11.GL_QUADS);
  }

  public void beginLines() {
    begin(GL11.GL_LINES);
  }

  public void begin(final int glMode) {
    if (externalBuffer != null) {
      mode = modeFor(glMode);
      return;
    }
    mode = modeFor(glMode);
    builder.begin(mode, DefaultVertexFormat.POSITION_COLOR_NORMAL);
    buffer = builder;
  }

  public void draw() {
    if (externalBuffer != null || mode == null || builder == null || !builder.building()) {
      return;
    }
    RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
    BufferUploader.drawWithShader(builder.end());
    mode = null;
    buffer = builder;
  }

  public void setDelta(final double delta) {
    this.delta = delta;
  }

  public void drawCuboid(final BlockPos pos, final int sides, final int argb) {
    drawCuboid(pos, pos, sides, argb);
  }

  public void drawCuboid(
      final BlockPos begin, final BlockPos end, final int sides, final int argb) {
    PoseStack transform = poseStack == null ? new PoseStack() : poseStack;
    transform.pushPose();
    transform.translate(translationX, translationY, translationZ);
    drawCuboid(buffer, transform, begin, end, sides, argb, delta);
    transform.popPose();
  }

  private static VertexFormat.Mode modeFor(int mode) {
    switch (mode) {
      case GL11.GL_LINES:
        return VertexFormat.Mode.LINES;
      case GL11.GL_QUADS:
        return VertexFormat.Mode.QUADS;
      case GL11.GL_LINE_STRIP:
        return VertexFormat.Mode.LINE_STRIP;
      default:
        throw new IllegalArgumentException("Unsupported drawing mode: " + mode);
    }
  }

  private static void line(
      VertexConsumer buffer,
      PoseStack pose,
      double x0,
      double y0,
      double z0,
      double x1,
      double y1,
      double z1,
      int r,
      int g,
      int b,
      int a) {
    vertex(buffer, pose, x0, y0, z0, r, g, b, a);
    vertex(buffer, pose, x1, y1, z1, r, g, b, a);
  }

  private static void vertex(
      VertexConsumer buffer,
      PoseStack pose,
      double x,
      double y,
      double z,
      int r,
      int g,
      int b,
      int a) {
    if (pose == null) {
      buffer.vertex(x, y, z).color(r, g, b, a).normal(0.f, 1.f, 0.f).endVertex();
    } else {
      PoseStack.Pose current = pose.last();
      buffer.vertex(current.pose(), (float) x, (float) y, (float) z)
          .color(r, g, b, a)
          .normal(current.normal(), 0.f, 1.f, 0.f)
          .endVertex();
    }
  }
}
