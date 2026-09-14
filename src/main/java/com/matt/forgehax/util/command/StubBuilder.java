package com.matt.forgehax.util.command;

import com.matt.forgehax.util.command.callbacks.CallbackData;
import com.mojang.blaze3d.platform.InputConstants;

import java.util.function.Consumer;

/**
 * Created on 6/8/2017 by fr1kin
 */
public class StubBuilder extends BaseCommandBuilder<StubBuilder, CommandStub> {

  public StubBuilder kpressed(Consumer<CallbackData> consumer) {
    getCallbacks(CallbackType.KEY_PRESSED).add(consumer);
    return this;
  }

  public StubBuilder kdown(Consumer<CallbackData> consumer) {
    getCallbacks(CallbackType.KEY_DOWN).add(consumer);
    return this;
  }

  public StubBuilder bind(int keyCode) {
    return insert(CommandStub.KEYBIND, keyCode);
  }

  public StubBuilder bind() {
    return bind(InputConstants.UNKNOWN.getValue());
  }

  public StubBuilder nobind() {
    return bind(CommandStub.NO_KEYBIND);
  }

  public StubBuilder bindOptions(boolean b) {
    return insert(CommandStub.KEYBIND_OPTIONS, b);
  }

  @Override
  public CommandStub build() {
    return new CommandStub(data);
  }
}
