package com.matt.forgehax.util.key;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

/**
 * Created on 6/10/2017 by fr1kin
 */
public interface IKeyBind {

  void bind(int keyCode);

  KeyMapping getBind();

  void onKeyPressed();

  void onKeyDown();

  default void unbind() {
    bind(InputConstants.UNKNOWN.getValue());
  }
}
