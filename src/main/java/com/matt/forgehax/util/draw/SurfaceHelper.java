package com.matt.forgehax.util.draw;

import com.matt.forgehax.Globals;
import com.matt.forgehax.util.draw.font.MinecraftFontRenderer;
import com.matt.forgehax.util.math.AlignHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

import static com.matt.forgehax.Globals.MC;
import static com.matt.forgehax.util.math.AlignHelper.getFlowDirY2;

/** Small rendering adapters used by the HUD and module code. */
public final class SurfaceHelper implements Globals {

  private static final ThreadLocal<GuiGraphics> ACTIVE_GRAPHICS = new ThreadLocal<>();

  private SurfaceHelper() {
  }

  /** Sets the GUI context used by static helpers during a render callback. */
  public static void setGraphics(@Nullable GuiGraphics graphics) {
    if (graphics == null) {
      ACTIVE_GRAPHICS.remove();
    } else {
      ACTIVE_GRAPHICS.set(graphics);
    }
  }

  @Nullable
  public static GuiGraphics getGraphics() {
    return ACTIVE_GRAPHICS.get();
  }

  public static void drawString(
      @Nullable MinecraftFontRenderer fontRenderer,
      String text,
      double x,
      double y,
      int color,
      boolean shadow
  ) {
    if (fontRenderer != null) {
      fontRenderer.drawString(text, x, y, color, shadow);
    } else {
      GuiGraphics graphics = getGraphics();
      if (graphics != null) {
        graphics.drawString(MC.font, text, (float) x, (float) y, color, shadow);
      } else {
        drawStringImmediate(text, x, y, color, shadow);
      }
    }
  }

  public static double getStringWidth(@Nullable MinecraftFontRenderer fontRenderer, String text) {
    return fontRenderer == null ? MC.font.width(text) : fontRenderer.getStringWidth(text);
  }

  public static double getStringHeight(@Nullable MinecraftFontRenderer fontRenderer) {
    return fontRenderer == null ? MC.font.lineHeight : fontRenderer.getHeight();
  }

  public static void drawRect(int x, int y, int w, int h, int color) {
    GuiGraphics graphics = getGraphics();
    if (graphics != null) {
      graphics.fill(x, y, x + w, y + h, color);
      return;
    }

    drawVertices(
        VertexFormat.Mode.QUADS,
        DefaultVertexFormat.POSITION_COLOR,
        builder -> {
          vertex(builder, x, y, 0, color);
          vertex(builder, x, y + h, 0, color);
          vertex(builder, x + w, y + h, 0, color);
          vertex(builder, x + w, y, 0, color);
        }
    );
  }

  public static void drawOutlinedRect(int x, int y, int w, int h, int color) {
    drawOutlinedRect(x, y, w, h, color, 1.f);
  }

  public static void drawOutlinedRectShaded(
      int x, int y, int w, int h, int colorOutline, int shade, float width) {
    int shaded = (0x00FFFFFF & colorOutline) | ((shade & 255) << 24);
    drawRect(x, y, w, h, shaded);
    drawOutlinedRect(x, y, w, h, colorOutline, width);
  }

  public static void drawOutlinedRect(int x, int y, int w, int h, int color, float width) {
    GuiGraphics graphics = getGraphics();
    if (graphics != null) {
      RenderSystem.lineWidth(width);
      graphics.hLine(x, x + w, y, color);
      graphics.hLine(x, x + w, y + h, color);
      graphics.vLine(x, y, y + h, color);
      graphics.vLine(x + w, y, y + h, color);
      return;
    }

    RenderSystem.lineWidth(width);
    drawVertices(
        VertexFormat.Mode.LINE_STRIP,
        DefaultVertexFormat.POSITION_COLOR,
        builder -> {
          vertex(builder, x, y, 0, color);
          vertex(builder, x, y + h, 0, color);
          vertex(builder, x + w, y + h, 0, color);
          vertex(builder, x + w, y, 0, color);
          vertex(builder, x, y, 0, color);
        }
    );
  }

  public static void drawTexturedRect(
      int x, int y, int textureX, int textureY, int width, int height, int zLevel) {
    drawVertices(
        VertexFormat.Mode.QUADS,
        DefaultVertexFormat.POSITION_TEX,
        builder -> {
          texturedVertex(builder, x, y + height, zLevel, textureX, textureY + height);
          texturedVertex(builder, x + width, y + height, zLevel,
              textureX + width, textureY + height);
          texturedVertex(builder, x + width, y, zLevel, textureX + width, textureY);
          texturedVertex(builder, x, y, zLevel, textureX, textureY);
        }
    );
  }

  public static void drawLine(int x1, int y1, int x2, int y2, int color, float width) {
    RenderSystem.lineWidth(width);
    drawVertices(
        VertexFormat.Mode.DEBUG_LINES,
        DefaultVertexFormat.POSITION_COLOR,
        builder -> {
          vertex(builder, x1, y1, 0, color);
          vertex(builder, x2, y2, 0, color);
        }
    );
  }

  public static void drawText(String msg, int x, int y, int color) {
    drawString(null, msg, x, y, color, false);
  }

  public static void drawTextShadow(String msg, int x, int y, int color) {
    drawString(null, msg, x, y, color, true);
  }

  public static void drawTextShadowCentered(String msg, float x, float y, int color) {
    drawTextShadow(msg, Math.round(x - getTextWidth(msg) / 2.f),
        Math.round(y - getTextHeight() / 2.f), color);
  }

  public static void drawTextAlignH(
      String msg, int x, int y, int color, boolean shadow, int alignmask) {
    final int offsetX = AlignHelper.alignH(getTextWidth(msg), alignmask);
    drawString(null, msg, x - offsetX, y, color, shadow);
  }

  public static void drawTextShadowAlignH(String msg, int x, int y, int color, int alignmask) {
    drawTextAlignH(msg, x, y, color, true, alignmask);
  }

  public static void drawTextAlign(
      String msg, int x, int y, int color, boolean shadow, int alignmask) {
    final int offsetX = AlignHelper.alignH(getTextWidth(msg), alignmask);
    final int offsetY = AlignHelper.alignV(getTextHeight(), alignmask);
    drawString(null, msg, x - offsetX, y - offsetY, color, shadow);
  }

  public static void drawTextShadowAlign(String msg, int x, int y, int color, int alignmask) {
    drawTextAlign(msg, x, y, color, true, alignmask);
  }

  public static void drawTextAlign(
      String msg, int x, int y, int color, double scale, boolean shadow, int alignmask) {
    final int offsetX = AlignHelper.alignH((int) (getTextWidth(msg) * scale), alignmask);
    final int offsetY = AlignHelper.alignV((int) (getTextHeight() * scale), alignmask);
    if (scale == 1.0d) {
      drawString(null, msg, x - offsetX, y - offsetY, color, shadow);
    } else {
      drawText(msg, x - offsetX, y - offsetY, color, scale, shadow);
    }
  }

  public static void drawTextAlign(
      List<String> msgList, int x, int y, int color, double scale, boolean shadow, int alignmask) {
    PoseStack pose = getPoseStack();
    pose.pushPose();
    pose.scale((float) scale, (float) scale, 1.f);

    final int offsetY = AlignHelper.alignV((int) (getTextHeight() * scale), alignmask);
    final int height = (int) (getFlowDirY2(alignmask) * (getTextHeight() + 1) * scale);
    final float invScale = (float) (1 / scale);
    for (int i = 0; i < msgList.size(); i++) {
      final int offsetX = AlignHelper.alignH((int) (getTextWidth(msgList.get(i)) * scale), alignmask);
      drawString(null, msgList.get(i),
          (x - offsetX) * invScale,
          (y - offsetY + height * i) * invScale,
          color, shadow);
    }
    pose.popPose();
  }

  public static void drawText(String msg, int x, int y, int color, double scale, boolean shadow) {
    PoseStack pose = getPoseStack();
    pose.pushPose();
    pose.scale((float) scale, (float) scale, 1.f);
    drawString(null, msg, x / scale, y / scale, color, shadow);
    pose.popPose();
  }

  public static void drawText(String msg, int x, int y, int color, double scale) {
    drawText(msg, x, y, color, scale, false);
  }

  public static void drawTextShadow(String msg, int x, int y, int color, double scale) {
    drawText(msg, x, y, color, scale, true);
  }

  public static int getTextWidth(String text, double scale) {
    return (int) (MC.font.width(text) * scale);
  }

  public static int getTextWidth(String text) {
    return getTextWidth(text, 1.D);
  }

  public static int getTextHeight() {
    return MC.font.lineHeight;
  }

  public static int getTextHeight(double scale) {
    return (int) (MC.font.lineHeight * scale);
  }

  public static void drawItem(ItemStack item, int x, int y) {
    GuiGraphics graphics = getGraphics();
    if (graphics != null) {
      graphics.renderItem(item, x, y);
    }
  }

  public static void drawItemOverlay(ItemStack stack, int x, int y) {
    GuiGraphics graphics = getGraphics();
    if (graphics != null) {
      graphics.renderItemDecorations(MC.font, stack, x, y);
    }
  }

  public static void drawItem(ItemStack item, double x, double y) {
    renderItem(item, x, y, 16.D);
  }

  public static void drawItemWithOverlay(ItemStack item, double x, double y, double scale) {
    renderItem(item, x, y, 16.D);
    renderItemOverlayIntoGUI(MC.font, item, x, y, null, scale);
  }

  public static void drawPotionEffect(MobEffectInstance potion, int x, int y) {
    // Modern HUD rendering owns effect icons; preserve the old entry point for callers.
  }

  public static void drawHead(ResourceLocation skinResource, double x, double y, float scale) {
    GuiGraphics graphics = getGraphics();
    if (graphics == null) {
      return;
    }
    int width = (int) (12 * scale);
    graphics.blit(skinResource, (int) x, (int) y, 0, 8, 8, width, width, 64, 64);
    graphics.blit(skinResource, (int) x, (int) y, 0, 40, 8, width, width, 64, 64);
  }

  protected static void renderItemAndEffectIntoGUI(
      @Nullable LivingEntity living, final ItemStack stack, double x, double y, double scale) {
    if (stack.isEmpty()) {
      return;
    }
    GuiGraphics graphics = getGraphics();
    if (graphics != null) {
      graphics.renderItem(stack, (int) x, (int) y);
    }
  }

  protected static void renderItemOverlayIntoGUI(
      Font font,
      ItemStack stack,
      double xPosition,
      double yPosition,
      @Nullable String text,
      double scale
  ) {
    GuiGraphics graphics = getGraphics();
    if (graphics != null && !stack.isEmpty()) {
      graphics.renderItemDecorations(font, stack, (int) xPosition, (int) yPosition, text);
    }
  }

  protected static void drawScaledCustomSizeModalRect(
      double x,
      double y,
      float u,
      float v,
      double uWidth,
      double vHeight,
      double width,
      double height,
      double tileWidth,
      double tileHeight
  ) {
    drawVertices(
        VertexFormat.Mode.QUADS,
        DefaultVertexFormat.POSITION_TEX,
        builder -> {
          texturedVertex(builder, x, y + height, 0, u, v + vHeight, tileWidth, tileHeight);
          texturedVertex(builder, x + width, y + height, 0,
              u + (float) uWidth, v + vHeight, tileWidth, tileHeight);
          texturedVertex(builder, x + width, y, 0,
              u + (float) uWidth, v, tileWidth, tileHeight);
          texturedVertex(builder, x, y, 0, u, v, tileWidth, tileHeight);
        }
    );
  }

  public static int getHeadWidth(float scale) {
    return (int) (scale * 12);
  }

  public static int getHeadWidth() {
    return getHeadWidth(1.f);
  }

  public static int getHeadHeight(float scale) {
    return (int) (scale * 12);
  }

  public static int getHeadHeight() {
    return getHeadHeight(1.f);
  }

  private static void renderItem(ItemStack item, double x, double y, double scale) {
    GuiGraphics graphics = getGraphics();
    if (graphics != null) {
      graphics.renderItem(item, (int) x, (int) y);
    }
  }

  private static PoseStack getPoseStack() {
    GuiGraphics graphics = getGraphics();
    return graphics == null ? RenderSystem.getModelViewStack() : graphics.pose();
  }

  private static void drawStringImmediate(
      String text, double x, double y, int color, boolean shadow) {
    Tesselator tesselator = Tesselator.getInstance();
    BufferBuilder builder = tesselator.getBuilder();
    net.minecraft.client.renderer.MultiBufferSource.BufferSource source =
        net.minecraft.client.renderer.MultiBufferSource.immediate(builder);
    MC.font.drawInBatch(
        text,
        (float) x,
        (float) y,
        color,
        shadow,
        new org.joml.Matrix4f(),
        source,
        Font.DisplayMode.NORMAL,
        0,
        0xF000F0
    );
    source.endBatch();
  }

  private static void drawVertices(
      VertexFormat.Mode mode,
      VertexFormat format,
      Consumer<BufferBuilder> emitter) {
    BufferBuilder builder = Tesselator.getInstance().getBuilder();
    builder.begin(mode, format);
    emitter.accept(builder);
    RenderSystem.setShader(
        format == DefaultVertexFormat.POSITION_TEX
            ? GameRenderer::getPositionTexShader
            : GameRenderer::getPositionColorShader
    );
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    BufferUploader.drawWithShader(builder.end());
  }

  private static void vertex(BufferBuilder builder, double x, double y, double z, int argb) {
    builder.vertex(x, y, z)
        .color(argb >> 16 & 255, argb >> 8 & 255, argb & 255, argb >>> 24 & 255)
        .endVertex();
  }

  private static void texturedVertex(
      BufferBuilder builder, double x, double y, double z, double u, double v) {
    texturedVertex(builder, x, y, z, u, v, 256.D, 256.D);
  }

  private static void texturedVertex(
      BufferBuilder builder,
      double x,
      double y,
      double z,
      double u,
      double v,
      double tileWidth,
      double tileHeight) {
    builder.vertex(x, y, z)
        .uv((float) (u / tileWidth), (float) (v / tileHeight))
        .endVertex();
  }
}
