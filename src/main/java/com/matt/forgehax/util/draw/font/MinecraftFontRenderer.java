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

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MinecraftFontRenderer extends CFont {

  private final int[] colorCode = new int[32];
  private final String colorcodeIdentifiers = "0123456789abcdefklmnor";
  protected CFont.CharData[] boldChars = new CFont.CharData[256];
  protected CFont.CharData[] italicChars = new CFont.CharData[256];
  protected CFont.CharData[] boldItalicChars = new CFont.CharData[256];
  protected DynamicTexture texBold;
  protected DynamicTexture texItalic;
  protected DynamicTexture texItalicBold;

  public MinecraftFontRenderer(Font font, boolean antiAlias, boolean fractionalMetrics) {
    super(font, antiAlias, fractionalMetrics);
    setupMinecraftColorcodes();
    setupBoldItalicIDs();
  }

  public float drawStringWithShadow(String text, double x, double y, int color) {
    float shadowWidth = drawString(text, x + 1.0D, y + 1.0D, color, true);
    return Math.max(shadowWidth, drawString(text, x, y, color, false));
  }

  public float drawString(String text, float x, float y, int color) {
    return drawString(text, x, y, color, false);
  }

  public float drawCenteredString(String text, float x, float y, int color) {
    return drawString(text, x - getStringWidth(text) / 2, y, color);
  }

  public float drawCenteredStringWithShadow(String text, float x, float y, int color) {
    float shadowWidth =
        drawString(text, x - getStringWidth(text) / 2 + 1.0D, y + 1.0D, color, true);
    return drawString(text, x - getStringWidth(text) / 2, y, color);
  }

  public float drawString(String text, double x, double y, int color, boolean shadow) {
    x -= 1;

    if (text == null) {
      return 0.0F;
    }

    if (color == 553648127) {
      color = 16777215;
    }

    if ((color & 0xFC000000) == 0) {
      color |= -16777216;
    }

    if (shadow) {
      color = (color & 0xFCFCFC) >> 2 | color & 0xFF000000;
    }

    CFont.CharData[] currentData = this.charData;
    float alpha = (color >> 24 & 0xFF) / 255.0F;
    boolean randomCase = false;
    boolean bold = false;
    boolean italic = false;
    boolean strikethrough = false;
    boolean underline = false;
    boolean render = true;
    x *= 2.0D;
    y = (y - 3.0D) * 2.0D;

    if (render) {
      // 1.20.1: no more fixed-function glPushMatrix/glScale - use a throwaway local PoseStack
      // scaled the same way the old code scaled its immediate-mode vertices.
      PoseStack poseStack = new PoseStack();
      poseStack.pushPose();
      poseStack.scale(0.5F, 0.5F, 0.5F);

      int drawColor = (color & 0xFFFFFF) | ((int) (alpha * 255.0F) << 24);
      DynamicTexture currentTexture = tex;

      int size = text.length();

      for (int i = 0; i < size; i++) {
        char character = text.charAt(i);

        if ((character == '\u00a7') && (i < size)) {
          int colorIndex = 21;

          try {
            colorIndex = "0123456789abcdefklmnor".indexOf(text.charAt(i + 1));
          } catch (Exception e) {
            e.printStackTrace();
          }

          if (colorIndex < 16) {
            bold = false;
            italic = false;
            randomCase = false;
            underline = false;
            strikethrough = false;
            currentTexture = tex;
            currentData = this.charData;

            if ((colorIndex < 0) || (colorIndex > 15)) {
              colorIndex = 15;
            }

            if (shadow) {
              colorIndex += 16;
            }

            int colorcode = this.colorCode[colorIndex];
            drawColor = (colorcode & 0xFFFFFF) | ((int) (alpha * 255.0F) << 24);
          } else if (colorIndex == 16) {
            randomCase = true;
          } else if (colorIndex == 17) {
            bold = true;

            if (italic) {
              currentTexture = texItalicBold;
              currentData = this.boldItalicChars;
            } else {
              currentTexture = texBold;
              currentData = this.boldChars;
            }
          } else if (colorIndex == 18) {
            strikethrough = true;
          } else if (colorIndex == 19) {
            underline = true;
          } else if (colorIndex == 20) {
            italic = true;

            if (bold) {
              currentTexture = texItalicBold;
              currentData = this.boldItalicChars;
            } else {
              currentTexture = texItalic;
              currentData = this.italicChars;
            }
          } else if (colorIndex == 21) {
            bold = false;
            italic = false;
            randomCase = false;
            underline = false;
            strikethrough = false;
            drawColor = (color & 0xFFFFFF) | ((int) (alpha * 255.0F) << 24);
            currentTexture = tex;
            currentData = this.charData;
          }

          i++;
        } else if ((character < currentData.length) && (character >= 0)) {
          drawChar(poseStack, currentTexture, currentData, character, (float) x, (float) y, drawColor);

          if (strikethrough) {
            drawLine(
                poseStack,
                x,
                y + currentData[character].height / 2,
                x + currentData[character].width - 8.0D,
                y + currentData[character].height / 2,
                1.0F,
                drawColor
            );
          }

          if (underline) {
            drawLine(
                poseStack,
                x,
                y + currentData[character].height - 2.0D,
                x + currentData[character].width - 8.0D,
                y + currentData[character].height - 2.0D,
                1.0F,
                drawColor
            );
          }

          x += currentData[character].width - 8 + this.charOffset;
        }
      }

      poseStack.popPose();
    }

    return (float) x / 2.0F;
  }

  @Override
  public int getStringWidth(String text) {
    if (text == null) {
      return 0;
    }

    int width = 0;
    CFont.CharData[] currentData = this.charData;
    boolean bold = false;
    boolean italic = false;
    int size = text.length();

    for (int i = 0; i < size; i++) {
      char character = text.charAt(i);

      if ((character == '\u00a7') && (i < size)) {
        int colorIndex = "0123456789abcdefklmnor".indexOf(character);

        if (colorIndex < 16) {
          bold = false;
          italic = false;
        } else if (colorIndex == 17) {
          bold = true;

          if (italic) {
            currentData = this.boldItalicChars;
          } else {
            currentData = this.boldChars;
          }
        } else if (colorIndex == 20) {
          italic = true;

          if (bold) {
            currentData = this.boldItalicChars;
          } else {
            currentData = this.italicChars;
          }
        } else if (colorIndex == 21) {
          bold = false;
          italic = false;
          currentData = this.charData;
        }

        i++;
      } else if ((character < currentData.length) && (character >= 0)) {
        width += currentData[character].width - 8 + this.charOffset;
      }
    }

    return width / 2;
  }

  public void setAntiAlias(boolean antiAlias) {
    super.setAntiAlias(antiAlias);
    setupBoldItalicIDs();
  }

  public void setFractionalMetrics(boolean fractionalMetrics) {
    super.setFractionalMetrics(fractionalMetrics);
    setupBoldItalicIDs();
  }

  public void setFont(Font font) {
    super.setFont(font);
    setupBoldItalicIDs();
  }

  private void setupBoldItalicIDs() {
    texBold =
        setupTexture(
            this.font.deriveFont(1), this.antiAlias, this.fractionalMetrics, this.boldChars);
    texItalic =
        setupTexture(
            this.font.deriveFont(2), this.antiAlias, this.fractionalMetrics, this.italicChars);
    texItalicBold =
        setupTexture(
            this.font.deriveFont(3), this.antiAlias, this.fractionalMetrics, this.boldItalicChars);
  }

  private void drawLine(
      PoseStack poseStack, double x, double y, double x1, double y1, float width, int argb) {
    float a = (argb >>> 24) / 255.f;
    float r = (argb >> 16 & 0xFF) / 255.f;
    float g = (argb >> 8 & 0xFF) / 255.f;
    float b = (argb & 0xFF) / 255.f;

    Matrix4f mat = poseStack.last().pose();
    BufferBuilder builder = Tesselator.getInstance().getBuilder();
    builder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
    builder.vertex(mat, (float) x, (float) y, 0).color(r, g, b, a).endVertex();
    builder.vertex(mat, (float) x1, (float) y1, 0).color(r, g, b, a).endVertex();

    RenderSystem.setShader(GameRenderer::getPositionColorShader);
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    RenderSystem.lineWidth(width);
    BufferUploader.drawWithShader(builder.end());
  }

  public List<String> wrapWords(String text, double width) {
    List finalWords = new ArrayList();

    if (getStringWidth(text) > width) {
      String[] words = text.split(" ");
      String currentWord = "";
      char lastColorCode = 65535;

      for (String word : words) {
        for (int i = 0; i < word.length(); i++) {
          char c = word.toCharArray()[i];

          if ((c == '\u00a7') && (i < word.length() - 1)) {
            lastColorCode = word.toCharArray()[(i + 1)];
          }
        }

        if (getStringWidth(currentWord + word + " ") < width) {
          currentWord = currentWord + word + " ";
        } else {
          finalWords.add(currentWord);
          currentWord = "\u00a7" + lastColorCode + word + " ";
        }
      }

      if (currentWord.length() > 0) {
        if (getStringWidth(currentWord) < width) {
          finalWords.add("\u00a7" + lastColorCode + currentWord + " ");
          currentWord = "";
        } else {
          for (String s : formatString(currentWord, width)) {
            finalWords.add(s);
          }
        }
      }
    } else {
      finalWords.add(text);
    }

    return finalWords;
  }

  public List<String> formatString(String string, double width) {
    List finalWords = new ArrayList();
    String currentWord = "";
    char lastColorCode = 65535;
    char[] chars = string.toCharArray();

    for (int i = 0; i < chars.length; i++) {
      char c = chars[i];

      if ((c == '\u00a7') && (i < chars.length - 1)) {
        lastColorCode = chars[(i + 1)];
      }

      if (getStringWidth(currentWord + c) < width) {
        currentWord = currentWord + c;
      } else {
        finalWords.add(currentWord);
        currentWord = "\u00a7" + lastColorCode + c;
      }
    }

    if (currentWord.length() > 0) {
      finalWords.add(currentWord);
    }

    return finalWords;
  }

  private void setupMinecraftColorcodes() {
    for (int index = 0; index < 32; index++) {
      int noClue = (index >> 3 & 0x1) * 85;
      int red = (index >> 2 & 0x1) * 170 + noClue;
      int green = (index >> 1 & 0x1) * 170 + noClue;
      int blue = (index >> 0 & 0x1) * 170 + noClue;

      if (index == 6) {
        red += 85;
      }

      if (index >= 16) {
        red /= 4;
        green /= 4;
        blue /= 4;
      }

      this.colorCode[index] = ((red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF);
    }
  }
}
