package com.matt.forgehax.util.key;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

import java.util.Map;

import static java.util.Map.entry;

/**
 * 1.12.2 stored binds as LWJGL2 {@code Keyboard} scancodes; 1.20.1 uses GLFW key codes. The two
 * overlap numerically but mean different things (LWJGL2 54 is right shift, GLFW 54 is the digit
 * 6), so a saved bind cannot be recognised as legacy by its value. Configs written by this build
 * carry a marker instead, and anything without one is run through this table once.
 */
public class LegacyKeyCodes {

  /**
   * Marker written alongside every bind so a config is only ever translated once.
   */
  public static final String CODES_KEY = "codes";
  public static final String CODES_GLFW = "glfw";

  private static final int LWJGL2_KEY_NONE = 0;

  private static final Map<Integer, Integer> LWJGL2_TO_GLFW =
      Map.ofEntries(
          entry(0x01, GLFW.GLFW_KEY_ESCAPE),
          entry(0x02, GLFW.GLFW_KEY_1),
          entry(0x03, GLFW.GLFW_KEY_2),
          entry(0x04, GLFW.GLFW_KEY_3),
          entry(0x05, GLFW.GLFW_KEY_4),
          entry(0x06, GLFW.GLFW_KEY_5),
          entry(0x07, GLFW.GLFW_KEY_6),
          entry(0x08, GLFW.GLFW_KEY_7),
          entry(0x09, GLFW.GLFW_KEY_8),
          entry(0x0A, GLFW.GLFW_KEY_9),
          entry(0x0B, GLFW.GLFW_KEY_0),
          entry(0x0C, GLFW.GLFW_KEY_MINUS),
          entry(0x0D, GLFW.GLFW_KEY_EQUAL),
          entry(0x0E, GLFW.GLFW_KEY_BACKSPACE),
          entry(0x0F, GLFW.GLFW_KEY_TAB),
          entry(0x10, GLFW.GLFW_KEY_Q),
          entry(0x11, GLFW.GLFW_KEY_W),
          entry(0x12, GLFW.GLFW_KEY_E),
          entry(0x13, GLFW.GLFW_KEY_R),
          entry(0x14, GLFW.GLFW_KEY_T),
          entry(0x15, GLFW.GLFW_KEY_Y),
          entry(0x16, GLFW.GLFW_KEY_U),
          entry(0x17, GLFW.GLFW_KEY_I),
          entry(0x18, GLFW.GLFW_KEY_O),
          entry(0x19, GLFW.GLFW_KEY_P),
          entry(0x1A, GLFW.GLFW_KEY_LEFT_BRACKET),
          entry(0x1B, GLFW.GLFW_KEY_RIGHT_BRACKET),
          entry(0x1C, GLFW.GLFW_KEY_ENTER),
          entry(0x1D, GLFW.GLFW_KEY_LEFT_CONTROL),
          entry(0x1E, GLFW.GLFW_KEY_A),
          entry(0x1F, GLFW.GLFW_KEY_S),
          entry(0x20, GLFW.GLFW_KEY_D),
          entry(0x21, GLFW.GLFW_KEY_F),
          entry(0x22, GLFW.GLFW_KEY_G),
          entry(0x23, GLFW.GLFW_KEY_H),
          entry(0x24, GLFW.GLFW_KEY_J),
          entry(0x25, GLFW.GLFW_KEY_K),
          entry(0x26, GLFW.GLFW_KEY_L),
          entry(0x27, GLFW.GLFW_KEY_SEMICOLON),
          entry(0x28, GLFW.GLFW_KEY_APOSTROPHE),
          entry(0x29, GLFW.GLFW_KEY_GRAVE_ACCENT),
          entry(0x2A, GLFW.GLFW_KEY_LEFT_SHIFT),
          entry(0x2B, GLFW.GLFW_KEY_BACKSLASH),
          entry(0x2C, GLFW.GLFW_KEY_Z),
          entry(0x2D, GLFW.GLFW_KEY_X),
          entry(0x2E, GLFW.GLFW_KEY_C),
          entry(0x2F, GLFW.GLFW_KEY_V),
          entry(0x30, GLFW.GLFW_KEY_B),
          entry(0x31, GLFW.GLFW_KEY_N),
          entry(0x32, GLFW.GLFW_KEY_M),
          entry(0x33, GLFW.GLFW_KEY_COMMA),
          entry(0x34, GLFW.GLFW_KEY_PERIOD),
          entry(0x35, GLFW.GLFW_KEY_SLASH),
          entry(0x36, GLFW.GLFW_KEY_RIGHT_SHIFT),
          entry(0x37, GLFW.GLFW_KEY_KP_MULTIPLY),
          entry(0x38, GLFW.GLFW_KEY_LEFT_ALT),
          entry(0x39, GLFW.GLFW_KEY_SPACE),
          entry(0x3A, GLFW.GLFW_KEY_CAPS_LOCK),
          entry(0x3B, GLFW.GLFW_KEY_F1),
          entry(0x3C, GLFW.GLFW_KEY_F2),
          entry(0x3D, GLFW.GLFW_KEY_F3),
          entry(0x3E, GLFW.GLFW_KEY_F4),
          entry(0x3F, GLFW.GLFW_KEY_F5),
          entry(0x40, GLFW.GLFW_KEY_F6),
          entry(0x41, GLFW.GLFW_KEY_F7),
          entry(0x42, GLFW.GLFW_KEY_F8),
          entry(0x43, GLFW.GLFW_KEY_F9),
          entry(0x44, GLFW.GLFW_KEY_F10),
          entry(0x45, GLFW.GLFW_KEY_NUM_LOCK),
          entry(0x46, GLFW.GLFW_KEY_SCROLL_LOCK),
          entry(0x47, GLFW.GLFW_KEY_KP_7),
          entry(0x48, GLFW.GLFW_KEY_KP_8),
          entry(0x49, GLFW.GLFW_KEY_KP_9),
          entry(0x4A, GLFW.GLFW_KEY_KP_SUBTRACT),
          entry(0x4B, GLFW.GLFW_KEY_KP_4),
          entry(0x4C, GLFW.GLFW_KEY_KP_5),
          entry(0x4D, GLFW.GLFW_KEY_KP_6),
          entry(0x4E, GLFW.GLFW_KEY_KP_ADD),
          entry(0x4F, GLFW.GLFW_KEY_KP_1),
          entry(0x50, GLFW.GLFW_KEY_KP_2),
          entry(0x51, GLFW.GLFW_KEY_KP_3),
          entry(0x52, GLFW.GLFW_KEY_KP_0),
          entry(0x53, GLFW.GLFW_KEY_KP_DECIMAL),
          entry(0x57, GLFW.GLFW_KEY_F11),
          entry(0x58, GLFW.GLFW_KEY_F12),
          entry(0x64, GLFW.GLFW_KEY_F13),
          entry(0x65, GLFW.GLFW_KEY_F14),
          entry(0x66, GLFW.GLFW_KEY_F15),
          entry(0x8D, GLFW.GLFW_KEY_KP_EQUAL),
          entry(0x9C, GLFW.GLFW_KEY_KP_ENTER),
          entry(0x9D, GLFW.GLFW_KEY_RIGHT_CONTROL),
          entry(0xB3, GLFW.GLFW_KEY_KP_DIVIDE),
          entry(0xB7, GLFW.GLFW_KEY_PRINT_SCREEN),
          entry(0xB8, GLFW.GLFW_KEY_RIGHT_ALT),
          entry(0xC5, GLFW.GLFW_KEY_PAUSE),
          entry(0xC7, GLFW.GLFW_KEY_HOME),
          entry(0xC8, GLFW.GLFW_KEY_UP),
          entry(0xC9, GLFW.GLFW_KEY_PAGE_UP),
          entry(0xCB, GLFW.GLFW_KEY_LEFT),
          entry(0xCD, GLFW.GLFW_KEY_RIGHT),
          entry(0xCF, GLFW.GLFW_KEY_END),
          entry(0xD0, GLFW.GLFW_KEY_DOWN),
          entry(0xD1, GLFW.GLFW_KEY_PAGE_DOWN),
          entry(0xD2, GLFW.GLFW_KEY_INSERT),
          entry(0xD3, GLFW.GLFW_KEY_DELETE),
          entry(0xDB, GLFW.GLFW_KEY_LEFT_SUPER),
          entry(0xDC, GLFW.GLFW_KEY_RIGHT_SUPER));

  private LegacyKeyCodes() {
  }

  /**
   * Translates a bind saved by the 1.12.2 build. Negative codes were already mouse buttons and are
   * passed through for {@link BindingHelper#getKey(int)} to resolve.
   */
  public static int toGlfw(int lwjgl2Code) {
    if (lwjgl2Code < 0) {
      return lwjgl2Code;
    }
    if (lwjgl2Code == LWJGL2_KEY_NONE) {
      return InputConstants.UNKNOWN.getValue();
    }
    return LWJGL2_TO_GLFW.getOrDefault(lwjgl2Code, InputConstants.UNKNOWN.getValue());
  }
}
