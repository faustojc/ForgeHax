package com.matt.forgehax.gui.elements;

import com.matt.forgehax.util.command.CommandStub;
import com.matt.forgehax.util.key.BindingHelper;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

/** Adapter for editing a CommandStub keybind without introducing a second bind store. */
public final class KeybindControl {

  private final CommandStub command;

  public KeybindControl(CommandStub command) {
    this.command = command;
  }

  public CommandStub getCommand() {
    return command;
  }

  public boolean isAvailable() {
    return command != null && command.getBind() != null;
  }

  public int getKeyCode() {
    return isAvailable() ? command.getBind().getKey().getValue() : InputConstants.UNKNOWN.getValue();
  }

  public String getDisplayName() {
    if (!isAvailable()) {
      return "UNAVAILABLE";
    }
    int keyCode = getKeyCode();
    if (keyCode == InputConstants.UNKNOWN.getValue()) {
      return "UNBOUND";
    }
    String name = BindingHelper.getIndexName(keyCode);
    return name == null || name.length() == 0 ? "UNBOUND" : name;
  }

  /** Applies a key code and serializes only when the authoritative key code changed. */
  public boolean setKeyCode(int keyCode) {
    if (!isAvailable()) {
      return false;
    }
    int before = getKeyCode();
    command.bind(keyCode);
    int after = getKeyCode();
    if (before != after) {
      command.serialize();
      return true;
    }
    return false;
  }

  public boolean unbind() {
    return setKeyCode(InputConstants.UNKNOWN.getValue());
  }

  /**
   * Completes a keyboard capture. Escape cancels; Delete and Backspace unbind; all other key codes
   * are accepted by the existing CommandStub binding.
   */
  public CaptureResult captureKeyboard(int keyCode) {
    if (!isAvailable()) {
      return CaptureResult.UNAVAILABLE;
    }
    if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
      return CaptureResult.CANCELED;
    }
    if (keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
      unbind();
      return CaptureResult.UNBOUND;
    }
    setKeyCode(keyCode);
    return CaptureResult.BOUND;
  }

  /** Completes a mouse capture using the project's negative mouse-button key codes. */
  public CaptureResult captureMouse(int button) {
    if (!isAvailable()) {
      return CaptureResult.UNAVAILABLE;
    }
    if (button < 0) {
      return CaptureResult.CANCELED;
    }
    setKeyCode(-100 + button);
    return CaptureResult.BOUND;
  }

  public enum CaptureResult {
    BOUND,
    UNBOUND,
    CANCELED,
    UNAVAILABLE
  }
}
