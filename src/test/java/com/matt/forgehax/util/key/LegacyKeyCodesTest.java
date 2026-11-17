package com.matt.forgehax.util.key;

import org.junit.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.Assert.assertEquals;

public class LegacyKeyCodesTest {

  private static final int UNBOUND = -1;

  @Test
  public void translatesTheDefaultGuiBind() {
    // 54 is LWJGL2 KEY_RSHIFT; read as a GLFW code it would be the digit 6
    assertEquals(GLFW.GLFW_KEY_RIGHT_SHIFT, LegacyKeyCodes.toGlfw(54));
  }

  @Test
  public void legacyKeyNoneBecomesUnbound() {
    assertEquals(UNBOUND, LegacyKeyCodes.toGlfw(0));
  }

  @Test
  public void unknownLegacyCodeBecomesUnbound() {
    assertEquals(UNBOUND, LegacyKeyCodes.toGlfw(0xFE));
  }

  @Test
  public void mouseButtonsPassThroughForBindingHelper() {
    assertEquals(-100, LegacyKeyCodes.toGlfw(-100));
    assertEquals(UNBOUND, LegacyKeyCodes.toGlfw(UNBOUND));
  }

  @Test
  public void translatesAcrossTheKeyboard() {
    assertEquals(GLFW.GLFW_KEY_ESCAPE, LegacyKeyCodes.toGlfw(0x01));
    assertEquals(GLFW.GLFW_KEY_W, LegacyKeyCodes.toGlfw(0x11));
    assertEquals(GLFW.GLFW_KEY_SPACE, LegacyKeyCodes.toGlfw(0x39));
    assertEquals(GLFW.GLFW_KEY_F12, LegacyKeyCodes.toGlfw(0x58));
    assertEquals(GLFW.GLFW_KEY_DELETE, LegacyKeyCodes.toGlfw(0xD3));
  }

  @Test
  public void keypadDivideIsLwjgl2Divide() {
    // LWJGL2 KEY_DIVIDE is 0xB5; 0xB3 is KEY_NUMPADCOMMA, which GLFW has no key for
    assertEquals(GLFW.GLFW_KEY_KP_DIVIDE, LegacyKeyCodes.toGlfw(0xB5));
    assertEquals(UNBOUND, LegacyKeyCodes.toGlfw(0xB3));
  }

  @Test
  public void translatesExtendedFunctionKeysAndMenu() {
    assertEquals(GLFW.GLFW_KEY_F16, LegacyKeyCodes.toGlfw(0x67));
    assertEquals(GLFW.GLFW_KEY_F17, LegacyKeyCodes.toGlfw(0x68));
    assertEquals(GLFW.GLFW_KEY_F18, LegacyKeyCodes.toGlfw(0x69));
    assertEquals(GLFW.GLFW_KEY_F19, LegacyKeyCodes.toGlfw(0x71));
    assertEquals(GLFW.GLFW_KEY_MENU, LegacyKeyCodes.toGlfw(0xDD));
  }

  @Test
  public void unmarkedStoredValuesAreTranslated() {
    assertEquals(GLFW.GLFW_KEY_RIGHT_SHIFT, LegacyKeyCodes.fromStored(54, false));
    assertEquals(-100, LegacyKeyCodes.fromStored(-100, false));
  }

  @Test
  public void markedStoredValuesAreNotTranslated() {
    assertEquals(GLFW.GLFW_KEY_RIGHT_SHIFT, LegacyKeyCodes.fromStored(GLFW.GLFW_KEY_RIGHT_SHIFT, true));
    assertEquals(GLFW.GLFW_KEY_SPACE, LegacyKeyCodes.fromStored(GLFW.GLFW_KEY_SPACE, true));
    assertEquals(-100, LegacyKeyCodes.fromStored(-100, true));
    assertEquals(UNBOUND, LegacyKeyCodes.fromStored(UNBOUND, true));
  }

  @Test
  public void markedMouseButtonIndexesFromTheOldBugAreRecovered() {
    // GLFW key codes start at 32, so a marked 0..7 is a raw mouse button index
    assertEquals(-100, LegacyKeyCodes.fromStored(0, true));
    assertEquals(-99, LegacyKeyCodes.fromStored(1, true));
    assertEquals(-98, LegacyKeyCodes.fromStored(2, true));
    assertEquals(-93, LegacyKeyCodes.fromStored(7, true));
    assertEquals(GLFW.GLFW_KEY_SPACE, LegacyKeyCodes.fromStored(8 + 24, true));
  }

  @Test
  public void translationIsNotAccidentallyIdempotent() {
    // documents why the codes marker exists: re-running the table on a GLFW value destroys it
    assertEquals(UNBOUND, LegacyKeyCodes.toGlfw(GLFW.GLFW_KEY_RIGHT_SHIFT));
  }
}
