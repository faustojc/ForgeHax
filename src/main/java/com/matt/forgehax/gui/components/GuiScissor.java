package com.matt.forgehax.gui.components;

import net.minecraft.client.gui.GuiGraphics;

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

  public static Clip begin(GuiGraphics graphics, GuiRect rect) {
    if (graphics == null) {
      throw new IllegalArgumentException("graphics");
    }
    if (rect == null) {
      throw new IllegalArgumentException("rect");
    }
    graphics.enableScissor(rect.getX(), rect.getY(), rect.getRight(), rect.getBottom());
    return new Clip(graphics);
  }

  public static final class Clip implements AutoCloseable {
    private final GuiGraphics graphics;
    private boolean closed;

    private Clip(GuiGraphics graphics) {
      this.graphics = graphics;
    }

    @Override
    public void close() {
      if (!closed) {
        closed = true;
        graphics.disableScissor();
      }
    }
  }
}
