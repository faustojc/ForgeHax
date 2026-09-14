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
  public void translationIsNotAccidentallyIdempotent() {
    // documents why the codes marker exists: re-running the table on a GLFW value destroys it
    assertEquals(UNBOUND, LegacyKeyCodes.toGlfw(GLFW.GLFW_KEY_RIGHT_SHIFT));
  }
}
