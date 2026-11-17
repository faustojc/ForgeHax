package com.matt.forgehax.util.key;

import java.util.HashMap;
import java.util.Map;

/**
 * The 1.20.1 build stores binds as GLFW key codes and marks them with {@code "codes":"glfw"};
 * this build stores LWJGL2 {@code Keyboard} codes with no marker. The two overlap numerically but
 * mean different things (GLFW 54 is the digit 6, LWJGL2 54 is right shift), so only a marked value
 * may be run through this table, and only once.
 */
public class LegacyKeyCodes {

  /**
   * Marker the 1.20.1 build writes alongside every bind.
   */
  public static final String CODES_KEY = "codes";
  public static final String CODES_GLFW = "glfw";

  private static final int GLFW_KEY_UNKNOWN = -1;
  private static final int LWJGL2_KEY_NONE = 0;
  private static final int GLFW_MOUSE_BUTTON_LAST = 7;

  /**
   * {glfw, lwjgl2}: the inverse of the 1.20.1 build's LWJGL2-to-GLFW table, except KP_DIVIDE,
   * which maps to DIVIDE here (see its entry), plus F16-F19 and MENU, which that table lacks.
   */
  private static final int[][] PAIRS = {
      {256, 0x01}, // ESCAPE
      {49, 0x02}, {50, 0x03}, {51, 0x04}, {52, 0x05}, {53, 0x06}, // 1-5
      {54, 0x07}, {55, 0x08}, {56, 0x09}, {57, 0x0A}, {48, 0x0B}, // 6-9, 0
      {45, 0x0C}, // MINUS
      {61, 0x0D}, // EQUAL
      {259, 0x0E}, // BACKSPACE
      {258, 0x0F}, // TAB
      {81, 0x10}, {87, 0x11}, {69, 0x12}, {82, 0x13}, {84, 0x14}, // Q W E R T
      {89, 0x15}, {85, 0x16}, {73, 0x17}, {79, 0x18}, {80, 0x19}, // Y U I O P
      {91, 0x1A}, // LEFT_BRACKET
      {93, 0x1B}, // RIGHT_BRACKET
      {257, 0x1C}, // ENTER
      {341, 0x1D}, // LEFT_CONTROL
      {65, 0x1E}, {83, 0x1F}, {68, 0x20}, {70, 0x21}, {71, 0x22}, // A S D F G
      {72, 0x23}, {74, 0x24}, {75, 0x25}, {76, 0x26}, // H J K L
      {59, 0x27}, // SEMICOLON
      {39, 0x28}, // APOSTROPHE
      {96, 0x29}, // GRAVE_ACCENT
      {340, 0x2A}, // LEFT_SHIFT
      {92, 0x2B}, // BACKSLASH
      {90, 0x2C}, {88, 0x2D}, {67, 0x2E}, {86, 0x2F}, // Z X C V
      {66, 0x30}, {78, 0x31}, {77, 0x32}, // B N M
      {44, 0x33}, // COMMA
      {46, 0x34}, // PERIOD
      {47, 0x35}, // SLASH
      {344, 0x36}, // RIGHT_SHIFT
      {332, 0x37}, // KP_MULTIPLY
      {342, 0x38}, // LEFT_ALT
      {32, 0x39}, // SPACE
      {280, 0x3A}, // CAPS_LOCK
      {290, 0x3B}, {291, 0x3C}, {292, 0x3D}, {293, 0x3E}, {294, 0x3F}, // F1-F5
      {295, 0x40}, {296, 0x41}, {297, 0x42}, {298, 0x43}, {299, 0x44}, // F6-F10
      {282, 0x45}, // NUM_LOCK
      {281, 0x46}, // SCROLL_LOCK
      {327, 0x47}, {328, 0x48}, {329, 0x49}, // KP_7-KP_9
      {333, 0x4A}, // KP_SUBTRACT
      {324, 0x4B}, {325, 0x4C}, {326, 0x4D}, // KP_4-KP_6
      {334, 0x4E}, // KP_ADD
      {321, 0x4F}, {322, 0x50}, {323, 0x51}, // KP_1-KP_3
      {320, 0x52}, // KP_0
      {330, 0x53}, // KP_DECIMAL
      {300, 0x57}, {301, 0x58}, // F11, F12
      {302, 0x64}, {303, 0x65}, {304, 0x66}, // F13-F15
      {336, 0x8D}, // KP_EQUAL
      {335, 0x9C}, // KP_ENTER
      {345, 0x9D}, // RIGHT_CONTROL
      // KP_DIVIDE -> DIVIDE 0xB5. Intentionally differs from the 1.20.1 table, which pairs
      // KP_DIVIDE with NUMPADCOMMA 0xB3 by mistake; GLFW has no numpad-comma key.
      {331, 0xB5},
      {283, 0xB7}, // PRINT_SCREEN
      {346, 0xB8}, // RIGHT_ALT
      {284, 0xC5}, // PAUSE
      {268, 0xC7}, // HOME
      {265, 0xC8}, // UP
      {266, 0xC9}, // PAGE_UP
      {263, 0xCB}, // LEFT
      {262, 0xCD}, // RIGHT
      {269, 0xCF}, // END
      {264, 0xD0}, // DOWN
      {267, 0xD1}, // PAGE_DOWN
      {260, 0xD2}, // INSERT
      {261, 0xD3}, // DELETE
      {343, 0xDB}, // LEFT_SUPER
      {347, 0xDC}, // RIGHT_SUPER
      // additions the 1.20.1 table lacks
      {305, 0x67}, {306, 0x68}, {307, 0x69}, {308, 0x71}, // F16-F19
      {348, 0xDD}, // MENU -> APPS
  };

  private static final Map<Integer, Integer> GLFW_TO_LWJGL2 = new HashMap<>();

  static {
    for (int[] pair : PAIRS) {
      GLFW_TO_LWJGL2.put(pair[0], pair[1]);
    }
  }

  static int size() {
    return GLFW_TO_LWJGL2.size();
  }

  /**
   * Translates a GLFW-marked bind to this build's encoding. The 1.20.1 build saves a mouse bind
   * as its bare button index 0..7 (GLFW key codes start at 32), which becomes the 1.12.2 mouse
   * code {@code -100 + button}. Negative values other than GLFW's unknown key pass through as
   * 1.12.2 mouse codes; anything else without a counterpart becomes unbound.
   */
  public static int toLwjgl2(int glfwCode) {
    if (glfwCode == GLFW_KEY_UNKNOWN) {
      return LWJGL2_KEY_NONE;
    }
    if (glfwCode < 0) {
      return glfwCode;
    }
    if (glfwCode <= GLFW_MOUSE_BUTTON_LAST) {
      return -100 + glfwCode;
    }
    Integer code = GLFW_TO_LWJGL2.get(glfwCode);
    return code != null ? code : LWJGL2_KEY_NONE;
  }
}
