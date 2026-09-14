package com.matt.forgehax.util.draw.font;

/*
       DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
                   Version 2, December 2004

Copyright (C) 2004 Sam Hocevar <sam@hocevar.net>

Everyone is permitted to copy and distribute verbatim or modified
copies of this license document, and changing it is allowed as long
as the name is changed.

           DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
  TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION

 0. You just DO WHAT THE FUCK YOU WANT TO.
*/

/**
 * Created by Hexeption on 18/12/2016.
 */

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.FastColor;
import org.joml.Matrix4f;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class CFont {

  private final float imgSize = 512;
  protected CharData[] charData = new CharData[256];
  protected Font font;
  protected boolean antiAlias;
  protected boolean fractionalMetrics;
  protected int fontHeight = -1;
  protected int charOffset = 0;
  protected DynamicTexture tex;

  public CFont(Font font, boolean antiAlias, boolean fractionalMetrics) {
    this.font = font;
    this.antiAlias = antiAlias;
    this.fractionalMetrics = fractionalMetrics;
    tex = setupTexture(font, antiAlias, fractionalMetrics, this.charData);
  }

  protected DynamicTexture setupTexture(
      Font font, boolean antiAlias, boolean fractionalMetrics, CharData[] chars) {
    BufferedImage img = generateFontImage(font, antiAlias, fractionalMetrics, chars);

    try {
      return new DynamicTexture(toNativeImage(img));
    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }

  /** 1.20.1: DynamicTexture takes a NativeImage (ABGR32), not an AWT BufferedImage (ARGB). */
  private static NativeImage toNativeImage(BufferedImage img) {
    int width = img.getWidth();
    int height = img.getHeight();
    NativeImage image = new NativeImage(NativeImage.Format.RGBA, width, height, false);
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int argb = img.getRGB(x, y);
        image.setPixelRGBA(
            x,
            y,
            FastColor.ABGR32.color(
                FastColor.ARGB32.alpha(argb),
                FastColor.ARGB32.blue(argb),
                FastColor.ARGB32.green(argb),
                FastColor.ARGB32.red(argb)
            )
        );
      }
    }
    return image;
  }

  protected BufferedImage generateFontImage(
      Font font, boolean antiAlias, boolean fractionalMetrics, CharData[] chars) {
    int imgSize = (int) this.imgSize;
    BufferedImage bufferedImage = new BufferedImage(imgSize, imgSize, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = (Graphics2D) bufferedImage.getGraphics();
    g.setFont(font);
    g.setColor(new Color(255, 255, 255, 0));
    g.fillRect(0, 0, imgSize, imgSize);
    g.setColor(Color.WHITE);
    g.setRenderingHint(
        RenderingHints.KEY_FRACTIONALMETRICS,
        fractionalMetrics
            ? RenderingHints.VALUE_FRACTIONALMETRICS_ON
            : RenderingHints.VALUE_FRACTIONALMETRICS_OFF
    );
    g.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING,
        antiAlias
            ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON
            : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF
    );
    g.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        antiAlias ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF
    );
    FontMetrics fontMetrics = g.getFontMetrics();
    int charHeight = 0;
    int positionX = 0;
    int positionY = 1;

    for (int i = 0; i < chars.length; i++) {
      char ch = (char) i;
      CharData charData = new CharData();
      Rectangle2D dimensions = fontMetrics.getStringBounds(String.valueOf(ch), g);
      charData.width = (dimensions.getBounds().width + 8);
      charData.height = dimensions.getBounds().height;

      if (positionX + charData.width >= imgSize) {
        positionX = 0;
        positionY += charHeight;
        charHeight = 0;
      }

      if (charData.height > charHeight) {
        charHeight = charData.height;
      }

      charData.storedX = positionX;
      charData.storedY = positionY;

      if (charData.height > this.fontHeight) {
        this.fontHeight = charData.height;
      }

      chars[i] = charData;
      g.drawString(String.valueOf(ch), positionX + 2, positionY + fontMetrics.getAscent());
      positionX += charData.width;
    }

    return bufferedImage;
  }

  public void drawChar(
      PoseStack poseStack, DynamicTexture texture, CharData[] chars, char c, float x, float y, int argb)
      throws ArrayIndexOutOfBoundsException {
    try {
      drawQuad(
          poseStack,
          texture,
          x,
          y,
          chars[c].width,
          chars[c].height,
          chars[c].storedX,
          chars[c].storedY,
          chars[c].width,
          chars[c].height,
          argb
      );
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * 1.20.1: no more fixed-function immediate mode (glBegin/glTexCoord2f/glVertex2d). Each glyph is
   * its own draw call through a BufferBuilder + shader, same one-quad-per-call granularity as the
   * old GL11 code.
   */
  protected void drawQuad(
      PoseStack poseStack,
      DynamicTexture texture,
      float x,
      float y,
      float width,
      float height,
      float srcX,
      float srcY,
      float srcWidth,
      float srcHeight,
      int argb
  ) {
    float renderSRCX = srcX / imgSize;
    float renderSRCY = srcY / imgSize;
    float renderSRCWidth = srcWidth / imgSize;
    float renderSRCHeight = srcHeight / imgSize;

    float a = (argb >>> 24) / 255.f;
    float r = (argb >> 16 & 0xFF) / 255.f;
    float g = (argb >> 8 & 0xFF) / 255.f;
    float b = (argb & 0xFF) / 255.f;

    Matrix4f mat = poseStack.last().pose();
    BufferBuilder builder = Tesselator.getInstance().getBuilder();
    builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR);
    quadVertex(builder, mat, x + width, y, renderSRCX + renderSRCWidth, renderSRCY, r, g, b, a);
    quadVertex(builder, mat, x, y, renderSRCX, renderSRCY, r, g, b, a);
    quadVertex(builder, mat, x, y + height, renderSRCX, renderSRCY + renderSRCHeight, r, g, b, a);
    quadVertex(builder, mat, x, y + height, renderSRCX, renderSRCY + renderSRCHeight, r, g, b, a);
    quadVertex(builder, mat, x + width, y + height, renderSRCX + renderSRCWidth,
        renderSRCY + renderSRCHeight, r, g, b, a);
    quadVertex(builder, mat, x + width, y, renderSRCX + renderSRCWidth, renderSRCY, r, g, b, a);

    RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
    RenderSystem.setShaderTexture(0, texture.getId());
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    BufferUploader.drawWithShader(builder.end());
  }

  private static void quadVertex(
      BufferBuilder builder, Matrix4f mat, float x, float y, float u, float v,
      float r, float g, float b, float a) {
    builder.vertex(mat, x, y, 0).uv(u, v).color(r, g, b, a).endVertex();
  }

  public int getStringHeight(String text) {
    return getHeight();
  }

  public int getHeight() {
    return (this.fontHeight - 8) / 2;
  }

  public int getStringWidth(String text) {
    int width = 0;

    for (char c : text.toCharArray()) {
      if ((c < this.charData.length) && (c >= 0)) {
        width += this.charData[c].width - 8 + this.charOffset;
      }
    }

    return width / 2;
  }

  public boolean isAntiAlias() {
    return this.antiAlias;
  }

  public void setAntiAlias(boolean antiAlias) {
    if (this.antiAlias != antiAlias) {
      this.antiAlias = antiAlias;
      tex = setupTexture(this.font, antiAlias, this.fractionalMetrics, this.charData);
    }
  }

  public boolean isFractionalMetrics() {
    return this.fractionalMetrics;
  }

  public void setFractionalMetrics(boolean fractionalMetrics) {
    if (this.fractionalMetrics != fractionalMetrics) {
      this.fractionalMetrics = fractionalMetrics;
      tex = setupTexture(this.font, this.antiAlias, fractionalMetrics, this.charData);
    }
  }

  public Font getFont() {
    return this.font;
  }

  public void setFont(Font font) {
    this.font = font;
    tex = setupTexture(font, this.antiAlias, this.fractionalMetrics, this.charData);
  }

  protected class CharData {

    public int width;
    public int height;
    public int storedX;
    public int storedY;

    protected CharData() {
    }
  }
}
