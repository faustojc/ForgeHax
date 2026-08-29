package com.matt.forgehax.gui.components;

/**
 * Colours used by the ForgeHax control centre.  Values are ARGB integers in
 * the format expected by Minecraft's GUI drawing helpers.
 */
public final class GuiPalette {

  public static final int BACKDROP = rgba(0x08, 0x0A, 0x0D, 0xB8);
  public static final int MODAL = rgba(0x12, 0x17, 0x1D, 0xFF);
  public static final int SURFACE = rgba(0x19, 0x21, 0x2A, 0xFF);
  public static final int DIVIDER = rgba(0x2B, 0x36, 0x42, 0xFF);
  public static final int TEXT = rgba(0xED, 0xF1, 0xF5, 0xFF);
  public static final int ACCENT = rgba(0xD9, 0x77, 0x3F, 0xFF);
  public static final int TEXT_MUTED = rgba(0xA4, 0xAF, 0xBA, 0xFF);
  public static final int HOVER = rgba(0x20, 0x2A, 0x34, 0xFF);

  private GuiPalette() {
  }

  public static int rgba(int red, int green, int blue, int alpha) {
    return ((alpha & 0xFF) << 24)
        | ((red & 0xFF) << 16)
        | ((green & 0xFF) << 8)
        | (blue & 0xFF);
  }
}
