package com.matt.forgehax.gui.components;

import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;

/**
 * Scoped scissor helper for scaled Minecraft GUI coordinates.
 *
 * <p>The scissor test and box are restored on close, allowing the sidebar and
 * settings pane to clip independently without leaking GL state to the screen
 * or to one another.
 */
public final class GuiScissor {

  private GuiScissor() {
  }

  public static Clip begin(GuiRect rect, ScaledResolution resolution) {
    if (resolution == null) {
      throw new IllegalArgumentException("resolution");
    }
    return begin(
        rect,
        resolution.getScaledHeight(),
        resolution.getScaleFactor()
    );
  }

  public static Clip begin(GuiRect rect, int scaledScreenHeight, int scaleFactor) {
    if (rect == null) {
      throw new IllegalArgumentException("rect");
    }
    int scale = Math.max(1, scaleFactor);
    int physicalX = rect.getX() * scale;
    int physicalY = (scaledScreenHeight - rect.getY() - rect.getHeight()) * scale;
    int physicalWidth = rect.getWidth() * scale;
    int physicalHeight = rect.getHeight() * scale;

    GL11.glPushAttrib(GL11.GL_SCISSOR_BIT);
    GL11.glEnable(GL11.GL_SCISSOR_TEST);
    GL11.glScissor(
        Math.max(0, physicalX),
        Math.max(0, physicalY),
        Math.max(0, physicalWidth),
        Math.max(0, physicalHeight)
    );
    return new Clip();
  }

  public static final class Clip implements AutoCloseable {
    private boolean closed;

    private Clip() {
    }

    @Override
    public void close() {
      if (!closed) {
        closed = true;
        GL11.glPopAttrib();
      }
    }
  }
}
