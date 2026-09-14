package com.matt.forgehax.util.draw;

import com.matt.forgehax.util.color.Color;
import com.matt.forgehax.util.draw.font.MinecraftFontRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import static com.matt.forgehax.Globals.MC;

/** Fluent 2D drawing adapter retained for existing module render code. */
public class SurfaceBuilder {

  public static final int COLOR = 1;
  public static final int SCALE = 2;
  public static final int TRANSLATION = 4;
  public static final int ROTATION = 8;
  public static final int ALL = 15;

  private static final SurfaceBuilder INSTANCE = new SurfaceBuilder();
  private final Stack<RenderSettings> settings = new Stack<>();
  private final RenderSettings defaultSettings = new RenderSettings();
  private final List<Vertex> vertices = new ArrayList<>();
  private VertexFormat.Mode mode;

  public static SurfaceBuilder getBuilder() {
    return INSTANCE;
  }

  public static void disableTexture2D() {
    // Texture enable/disable is represented by the selected shader in 1.20.1.
  }

  public static void enableTexture2D() {
    // Texture enable/disable is represented by the selected shader in 1.20.1.
  }

  public static void enableBlend() {
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
  }

  public static void disableBlend() {
    RenderSystem.disableBlend();
  }

  public static void enableFontRendering() {
    RenderSystem.disableDepthTest();
  }

  public static void disableFontRendering() {
    RenderSystem.enableDepthTest();
  }

  public static void enableItemRendering() {
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
  }

  public static void disableItemRendering() {
    RenderSystem.disableBlend();
    RenderSystem.enableDepthTest();
  }

  public static void clearColor() {
    RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1.f);
  }

  private RenderSettings current() {
    return settings.isEmpty() ? defaultSettings : settings.peek();
  }

  public SurfaceBuilder begin(int glMode) {
    mode = modeFor(glMode);
    vertices.clear();
    return this;
  }

  public SurfaceBuilder beginLines() {
    return begin(GL11.GL_LINES);
  }

  public SurfaceBuilder beginLineLoop() {
    return begin(GL11.GL_LINE_LOOP);
  }

  public SurfaceBuilder beginQuads() {
    return begin(GL11.GL_QUADS);
  }

  public SurfaceBuilder beginPolygon() {
    return begin(GL11.GL_POLYGON);
  }

  public SurfaceBuilder end() {
    if (mode == null || vertices.isEmpty()) {
      return this;
    }

    BufferBuilder builder = Tesselator.getInstance().getBuilder();
    builder.begin(mode, DefaultVertexFormat.POSITION_COLOR);
    PoseStack.Pose pose = getPoseStack().last();
    for (Vertex vertex : vertices) {
      builder.vertex(pose.pose(), (float) vertex.x, (float) vertex.y, (float) vertex.z)
          .color(vertex.red, vertex.green, vertex.blue, vertex.alpha)
          .endVertex();
    }
    RenderSystem.setShader(GameRenderer::getPositionColorShader);
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    BufferUploader.drawWithShader(builder.end());
    vertices.clear();
    mode = null;
    return this;
  }

  public SurfaceBuilder autoApply(boolean enabled) {
    current().setAutoApply(enabled);
    return this;
  }

  public SurfaceBuilder apply() {
    return apply(ALL);
  }

  public SurfaceBuilder apply(int flags) {
    RenderSettings current = current();
    if ((flags & COLOR) == COLOR) current.applyColor();
    if ((flags & SCALE) == SCALE) current.applyScale();
    if ((flags & TRANSLATION) == TRANSLATION) current.applyTranslation();
    if ((flags & ROTATION) == ROTATION) current.applyRotation();
    return this;
  }

  public SurfaceBuilder reset() {
    return reset(ALL);
  }

  public SurfaceBuilder reset(int flags) {
    RenderSettings current = current();
    if ((flags & COLOR) == COLOR) current.resetColor();
    if ((flags & SCALE) == SCALE) current.resetScale();
    if ((flags & TRANSLATION) == TRANSLATION) current.resetTranslation();
    if ((flags & ROTATION) == ROTATION) current.resetRotation();
    return this;
  }

  public SurfaceBuilder push() {
    getPoseStack().pushPose();
    settings.push(new RenderSettings());
    return this;
  }

  public SurfaceBuilder pop() {
    if (!settings.isEmpty()) settings.pop();
    getPoseStack().popPose();
    return this;
  }

  public SurfaceBuilder color(double r, double g, double b, double a) {
    current().setColor4d(new double[]{
        clamp(r), clamp(g), clamp(b), clamp(a)
    });
    return this;
  }

  public SurfaceBuilder color(int buffer) {
    return color(
        (buffer >> 16 & 255) / 255.D,
        (buffer >> 8 & 255) / 255.D,
        (buffer & 255) / 255.D,
        (buffer >>> 24 & 255) / 255.D
    );
  }

  public SurfaceBuilder color(int r, int g, int b, int a) {
    return color(r / 255.D, g / 255.D, b / 255.D, a / 255.D);
  }

  public SurfaceBuilder scale(double x, double y, double z) {
    current().setScale3d(new double[]{x, y, z});
    return this;
  }

  public SurfaceBuilder scale(double s) {
    return scale(s, s, s);
  }

  public SurfaceBuilder scale() {
    return scale(0.D);
  }

  public SurfaceBuilder translate(double x, double y, double z) {
    current().setTranslate3d(new double[]{x, y, z});
    return this;
  }

  public SurfaceBuilder translate(double x, double y) {
    return translate(x, y, 0.D);
  }

  public SurfaceBuilder rotate(double angle, double x, double y, double z) {
    current().setRotated4d(new double[]{angle, x, y, z});
    return this;
  }

  public SurfaceBuilder width(double width) {
    RenderSystem.lineWidth((float) width);
    return this;
  }

  public SurfaceBuilder vertex(double x, double y, double z) {
    int color = current().hasColor() ? Color.of(current().getColor4d()).toBuffer() : 0xFFFFFFFF;
    vertices.add(new Vertex(x, y, z, color));
    return this;
  }

  public SurfaceBuilder vertex(double x, double y) {
    return vertex(x, y, 0.D);
  }

  public SurfaceBuilder line(double startX, double startY, double endX, double endY) {
    return vertex(startX, startY).vertex(endX, endY);
  }

  public SurfaceBuilder rectangle(double x, double y, double w, double h) {
    return vertex(x, y).vertex(x, y + h).vertex(x + w, y + h).vertex(x + w, y);
  }

  public SurfaceBuilder fontRenderer(MinecraftFontRenderer fontRenderer) {
    current().setFontRenderer(fontRenderer);
    return this;
  }

  private SurfaceBuilder text(String text, double x, double y, boolean shadow) {
    if (current().hasFontRenderer()) {
      current().getFontRenderer().drawString(
          text, x, y + 1, Color.of(current().getColor4d()).toBuffer(), shadow);
    } else {
      SurfaceHelper.drawString(null, text, x, y,
          current().hasColor() ? Color.of(current().getColor4d()).toBuffer() : 0xFFFFFFFF,
          shadow);
    }
    return this;
  }

  public SurfaceBuilder text(String text, double x, double y) {
    return text(text, x, y, false);
  }

  public SurfaceBuilder textWithShadow(String text, double x, double y) {
    return text(text, x, y, true);
  }

  public SurfaceBuilder task(Runnable task) {
    task.run();
    return this;
  }

  public SurfaceBuilder item(ItemStack stack, double x, double y) {
    SurfaceHelper.renderItemAndEffectIntoGUI(null, stack, x, y,
        current().hasScale() ? current().getScale3d()[0] : 16.D);
    return this;
  }

  public SurfaceBuilder itemOverlay(ItemStack stack, double x, double y) {
    SurfaceHelper.renderItemOverlayIntoGUI(
        MC.font, stack, x, y, null,
        current().hasScale() ? current().getScale3d()[0] : 16.D
    );
    return this;
  }

  public SurfaceBuilder head(ResourceLocation resource, double x, double y) {
    double scale = current().hasScale() ? current().getScale3d()[0] : 12.D;
    SurfaceHelper.drawHead(resource, x, y, (float) scale / 12.f);
    return this;
  }

  public int getFontWidth(String text) {
    return current().hasFontRenderer()
        ? current().getFontRenderer().getStringWidth(text)
        : MC.font.width(text);
  }

  public int getFontHeight() {
    return current().hasFontRenderer()
        ? current().getFontRenderer().getHeight()
        : MC.font.lineHeight;
  }

  public int getFontHeight(String text) {
    return getFontHeight();
  }

  private PoseStack getPoseStack() {
    return SurfaceHelper.getGraphics() == null
        ? RenderSystem.getModelViewStack()
        : SurfaceHelper.getGraphics().pose();
  }

  private static VertexFormat.Mode modeFor(int mode) {
    switch (mode) {
      case GL11.GL_LINES:
        return VertexFormat.Mode.LINES;
      case GL11.GL_LINE_LOOP:
        return VertexFormat.Mode.LINE_STRIP;
      case GL11.GL_QUADS:
        return VertexFormat.Mode.QUADS;
      case GL11.GL_POLYGON:
        return VertexFormat.Mode.TRIANGLE_FAN;
      default:
        throw new IllegalArgumentException("Unsupported drawing mode: " + mode);
    }
  }

  private static double clamp(double value) {
    return Math.max(0.D, Math.min(1.D, value));
  }

  private static final class Vertex {
    private final double x;
    private final double y;
    private final double z;
    private final int red;
    private final int green;
    private final int blue;
    private final int alpha;

    private Vertex(double x, double y, double z, int argb) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.red = argb >> 16 & 255;
      this.green = argb >> 8 & 255;
      this.blue = argb & 255;
      this.alpha = argb >>> 24 & 255;
    }
  }

  private static final class RenderSettings {
    private double[] color4d;
    private double[] scale3d;
    private double[] translate3d;
    private double[] rotated4d;
    private boolean autoApply = true;
    private MinecraftFontRenderer fontRenderer;

    private double[] getColor4d() { return color4d; }
    private double[] getScale3d() { return scale3d; }
    private boolean hasColor() { return color4d != null; }
    private boolean hasScale() { return scale3d != null; }
    private boolean hasTranslation() { return translate3d != null; }
    private boolean hasRotation() { return rotated4d != null; }
    private boolean hasFontRenderer() { return fontRenderer != null; }
    private MinecraftFontRenderer getFontRenderer() { return fontRenderer; }
    private void setFontRenderer(MinecraftFontRenderer value) { fontRenderer = value; }
    private void setAutoApply(boolean value) { autoApply = value; }

    private void setColor4d(double[] value) {
      color4d = value;
      if (autoApply) applyColor();
    }

    private void setScale3d(double[] value) {
      scale3d = value;
      if (autoApply) applyScale();
    }

    private void setTranslate3d(double[] value) {
      translate3d = value;
      if (autoApply) applyTranslation();
    }

    private void setRotated4d(double[] value) {
      rotated4d = value;
      if (autoApply) applyRotation();
    }

    private PoseStack pose() {
      return SurfaceHelper.getGraphics() == null
          ? RenderSystem.getModelViewStack()
          : SurfaceHelper.getGraphics().pose();
    }

    private void applyColor() {
      if (hasColor()) {
        RenderSystem.setShaderColor((float) color4d[0], (float) color4d[1],
            (float) color4d[2], (float) color4d[3]);
      }
    }

    private void applyScale() {
      if (hasScale()) pose().scale((float) scale3d[0], (float) scale3d[1], (float) scale3d[2]);
    }

    private void applyTranslation() {
      if (hasTranslation()) pose().translate(translate3d[0], translate3d[1], translate3d[2]);
    }

    private void applyRotation() {
      if (hasRotation()) {
        pose().mulPose(new Quaternionf().rotateAxis(
            (float) Math.toRadians(rotated4d[0]),
            (float) rotated4d[1], (float) rotated4d[2], (float) rotated4d[3]));
      }
    }

    private void resetColor() {
      if (hasColor()) {
        color4d = null;
        RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1.f);
      }
    }

    private void resetScale() {
      if (hasScale()) {
        pose().scale((float) (1.D / scale3d[0]), (float) (1.D / scale3d[1]),
            (float) (1.D / scale3d[2]));
        scale3d = null;
      }
    }

    private void resetTranslation() {
      if (hasTranslation()) {
        pose().translate(-translate3d[0], -translate3d[1], -translate3d[2]);
        translate3d = null;
      }
    }

    private void resetRotation() {
      if (hasRotation()) {
        pose().mulPose(new Quaternionf().rotateAxis(
            (float) -Math.toRadians(rotated4d[0]),
            (float) rotated4d[1], (float) rotated4d[2], (float) rotated4d[3]));
        rotated4d = null;
      }
    }
  }
}
