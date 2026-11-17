package com.matt.forgehax.util.key;

import com.mojang.blaze3d.platform.InputConstants;
import org.junit.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.Assert.assertEquals;

public class BindingHelperTest {

  @Test
  public void keyboardRoundTrips() {
    InputConstants.Key key = InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_K);
    assertEquals(GLFW.GLFW_KEY_K, BindingHelper.getCode(key));
    assertEquals(key, BindingHelper.getKey(BindingHelper.getCode(key)));
  }

  @Test
  public void mouseButtonsRoundTripThroughTheNegativeEncoding() {
    for (int button = 0; button <= 2; button++) {
      InputConstants.Key key = InputConstants.Type.MOUSE.getOrCreate(button);
      assertEquals(-100 + button, BindingHelper.getCode(key));
      assertEquals(key, BindingHelper.getKey(BindingHelper.getCode(key)));
    }
  }

  @Test
  public void unknownRoundTrips() {
    assertEquals(-1, BindingHelper.getCode(InputConstants.UNKNOWN));
    assertEquals(InputConstants.UNKNOWN, BindingHelper.getKey(-1));
  }
}
