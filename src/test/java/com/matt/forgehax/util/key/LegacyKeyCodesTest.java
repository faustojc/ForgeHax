package com.matt.forgehax.util.key;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.lwjgl.input.Keyboard;

public class LegacyKeyCodesTest {

  /**
   * {GLFW code (literal, from org.lwjgl.glfw.GLFW 3.2.2), expected LWJGL2 code} for every entry
   * of the 1.20.1 build's table.
   */
  private static final int[][] EXPECTED = {
      {256, Keyboard.KEY_ESCAPE},
      {49, Keyboard.KEY_1}, {50, Keyboard.KEY_2}, {51, Keyboard.KEY_3}, {52, Keyboard.KEY_4},
      {53, Keyboard.KEY_5}, {54, Keyboard.KEY_6}, {55, Keyboard.KEY_7}, {56, Keyboard.KEY_8},
      {57, Keyboard.KEY_9}, {48, Keyboard.KEY_0},
      {45, Keyboard.KEY_MINUS}, {61, Keyboard.KEY_EQUALS}, {259, Keyboard.KEY_BACK},
      {258, Keyboard.KEY_TAB},
      {81, Keyboard.KEY_Q}, {87, Keyboard.KEY_W}, {69, Keyboard.KEY_E}, {82, Keyboard.KEY_R},
      {84, Keyboard.KEY_T}, {89, Keyboard.KEY_Y}, {85, Keyboard.KEY_U}, {73, Keyboard.KEY_I},
      {79, Keyboard.KEY_O}, {80, Keyboard.KEY_P},
      {91, Keyboard.KEY_LBRACKET}, {93, Keyboard.KEY_RBRACKET}, {257, Keyboard.KEY_RETURN},
      {341, Keyboard.KEY_LCONTROL},
      {65, Keyboard.KEY_A}, {83, Keyboard.KEY_S}, {68, Keyboard.KEY_D}, {70, Keyboard.KEY_F},
      {71, Keyboard.KEY_G}, {72, Keyboard.KEY_H}, {74, Keyboard.KEY_J}, {75, Keyboard.KEY_K},
      {76, Keyboard.KEY_L},
      {59, Keyboard.KEY_SEMICOLON}, {39, Keyboard.KEY_APOSTROPHE}, {96, Keyboard.KEY_GRAVE},
      {340, Keyboard.KEY_LSHIFT}, {92, Keyboard.KEY_BACKSLASH},
      {90, Keyboard.KEY_Z}, {88, Keyboard.KEY_X}, {67, Keyboard.KEY_C}, {86, Keyboard.KEY_V},
      {66, Keyboard.KEY_B}, {78, Keyboard.KEY_N}, {77, Keyboard.KEY_M},
      {44, Keyboard.KEY_COMMA}, {46, Keyboard.KEY_PERIOD}, {47, Keyboard.KEY_SLASH},
      {344, Keyboard.KEY_RSHIFT}, {332, Keyboard.KEY_MULTIPLY}, {342, Keyboard.KEY_LMENU},
      {32, Keyboard.KEY_SPACE}, {280, Keyboard.KEY_CAPITAL},
      {290, Keyboard.KEY_F1}, {291, Keyboard.KEY_F2}, {292, Keyboard.KEY_F3},
      {293, Keyboard.KEY_F4}, {294, Keyboard.KEY_F5}, {295, Keyboard.KEY_F6},
      {296, Keyboard.KEY_F7}, {297, Keyboard.KEY_F8}, {298, Keyboard.KEY_F9},
      {299, Keyboard.KEY_F10},
      {282, Keyboard.KEY_NUMLOCK}, {281, Keyboard.KEY_SCROLL},
      {327, Keyboard.KEY_NUMPAD7}, {328, Keyboard.KEY_NUMPAD8}, {329, Keyboard.KEY_NUMPAD9},
      {333, Keyboard.KEY_SUBTRACT},
      {324, Keyboard.KEY_NUMPAD4}, {325, Keyboard.KEY_NUMPAD5}, {326, Keyboard.KEY_NUMPAD6},
      {334, Keyboard.KEY_ADD},
      {321, Keyboard.KEY_NUMPAD1}, {322, Keyboard.KEY_NUMPAD2}, {323, Keyboard.KEY_NUMPAD3},
      {320, Keyboard.KEY_NUMPAD0}, {330, Keyboard.KEY_DECIMAL},
      {300, Keyboard.KEY_F11}, {301, Keyboard.KEY_F12},
      {302, Keyboard.KEY_F13}, {303, Keyboard.KEY_F14}, {304, Keyboard.KEY_F15},
      {336, Keyboard.KEY_NUMPADEQUALS}, {335, Keyboard.KEY_NUMPADENTER},
      {345, Keyboard.KEY_RCONTROL},
      // deliberately DIVIDE, not the 1.20.1 table's mistaken NUMPADCOMMA
      {331, Keyboard.KEY_DIVIDE},
      {283, Keyboard.KEY_SYSRQ}, {346, Keyboard.KEY_RMENU}, {284, Keyboard.KEY_PAUSE},
      {268, Keyboard.KEY_HOME}, {265, Keyboard.KEY_UP}, {266, Keyboard.KEY_PRIOR},
      {263, Keyboard.KEY_LEFT}, {262, Keyboard.KEY_RIGHT}, {269, Keyboard.KEY_END},
      {264, Keyboard.KEY_DOWN}, {267, Keyboard.KEY_NEXT}, {260, Keyboard.KEY_INSERT},
      {261, Keyboard.KEY_DELETE}, {343, Keyboard.KEY_LMETA}, {347, Keyboard.KEY_RMETA},
      // additions the 1.20.1 table lacks
      {305, Keyboard.KEY_F16}, {306, Keyboard.KEY_F17}, {307, Keyboard.KEY_F18},
      {308, Keyboard.KEY_F19}, {348, Keyboard.KEY_APPS},
  };

  @Test
  public void translatesEveryTableEntry() {
    assertEquals(112, EXPECTED.length);
    assertEquals(112, LegacyKeyCodes.size());
    for (int[] pair : EXPECTED) {
      assertEquals("glfw " + pair[0], pair[1], LegacyKeyCodes.toLwjgl2(pair[0]));
    }
  }

  @Test
  public void unknownKeyBecomesUnbound() {
    assertEquals(Keyboard.KEY_NONE, LegacyKeyCodes.toLwjgl2(-1));
  }

  @Test
  public void mouseCodesPassThrough() {
    assertEquals(-100, LegacyKeyCodes.toLwjgl2(-100));
    assertEquals(-99, LegacyKeyCodes.toLwjgl2(-99));
    assertEquals(-98, LegacyKeyCodes.toLwjgl2(-98));
    // the 1.20.1 build saves mouse binds as bare button indexes
    for (int button = 0; button <= 7; button++) {
      assertEquals("button " + button, -100 + button, LegacyKeyCodes.toLwjgl2(button));
    }
  }

  @Test
  public void unmappedCodesBecomeUnbound() {
    assertEquals(Keyboard.KEY_NONE, LegacyKeyCodes.toLwjgl2(309)); // GLFW_KEY_F20
    for (int code = 8; code <= 31; code++) {
      assertEquals("code " + code, Keyboard.KEY_NONE, LegacyKeyCodes.toLwjgl2(code));
    }
  }
}
