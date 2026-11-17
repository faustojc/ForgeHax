package com.matt.forgehax.util.key;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.IKeyConflictContext;

public class BindingHelper {

  private static final IKeyConflictContext EMPTY =
      new IKeyConflictContext() {
        @Override
        public boolean isActive() {
          return false;
        }

        @Override
        public boolean conflicts(IKeyConflictContext other) {
          return false;
        }
      };

  /**
   * Resolves a stored bind code to an input. 1.12.2 configs wrote mouse buttons as negative LWJGL2
   * codes (-100 left, -99 right, -98 middle); feeding those to KEYSYM makes GLFW reject the key,
   * so they are mapped back onto the MOUSE type here.
   */
  public static InputConstants.Key getKey(int code) {
    if (code == InputConstants.UNKNOWN.getValue()) {
      return InputConstants.UNKNOWN;
    }
    return code < 0
        ? InputConstants.Type.MOUSE.getOrCreate(code + 100)
        : InputConstants.Type.KEYSYM.getOrCreate(code);
  }

  /**
   * Inverse of {@link #getKey(int)}: the code to store for an input. Mouse buttons are written in
   * the -100 based encoding; a raw {@code getValue()} would be the bare button index (0/1/2) and
   * reload as a keyboard key.
   */
  public static int getCode(InputConstants.Key key) {
    return key.getType() == InputConstants.Type.MOUSE ? key.getValue() - 100 : key.getValue();
  }

  public static String getIndexName(int code) {
    return getKey(code).getDisplayName().getString();
  }

  public static String getIndexName(KeyMapping binding) {
    return binding.getKey().getDisplayName().getString();
  }

  public static IKeyConflictContext getEmptyKeyConflictContext() {
    return EMPTY;
  }
}
