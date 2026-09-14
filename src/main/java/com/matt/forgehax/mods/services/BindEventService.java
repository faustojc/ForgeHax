package com.matt.forgehax.mods.services;

import com.matt.forgehax.gui.ClickGui;
import com.matt.forgehax.util.command.CommandStub;
import com.matt.forgehax.util.mod.ServiceMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.stream.Stream;

import static com.matt.forgehax.Helper.getGlobalCommand;

/**
 * Created on 6/14/2017 by fr1kin
 */
@RegisterMod
public class BindEventService extends ServiceMod {

  public BindEventService() {
    super("BindEventService");
  }

  @SubscribeEvent
  public void onKeyboardEvent(InputEvent.Key event) {
    dispatch(InputConstants.Type.KEYSYM.getOrCreate(event.getKey()), event.getAction());
  }

  @SubscribeEvent
  public void onMouseEvent(InputEvent.MouseButton.Pre event) {
    dispatch(InputConstants.Type.MOUSE.getOrCreate(event.getButton()), event.getAction());
  }

  /**
   * Vanilla only updates KeyMapping's pressed state while no screen is open, so the bind state is
   * read straight off the input event instead. That is also what lets the GUI's own bind close it.
   */
  private void dispatch(InputConstants.Key key, int action) {
    if (InputConstants.UNKNOWN.equals(key) || action == GLFW.GLFW_RELEASE || !acceptsInput()) {
      return;
    }

    boolean pressed = action == GLFW.GLFW_PRESS;
    binds()
        .filter(stub -> key.equals(stub.getBind().getKey()))
        .forEach(
            stub -> {
              if (pressed) {
                stub.onKeyPressed();
              }
              stub.onKeyDown();
            });
  }

  /**
   * Binds fire with no screen open, and inside our own GUI unless it is capturing text.
   */
  private boolean acceptsInput() {
    if (MC.screen == null) {
      return true;
    }
    return MC.screen instanceof ClickGui && !((ClickGui) MC.screen).isCapturingInput();
  }

  private Stream<CommandStub> binds() {
    return getGlobalCommand()
        .getChildrenDeep()
        .stream()
        .filter(command -> command instanceof CommandStub)
        .map(command -> (CommandStub) command)
        .filter(stub -> stub.getBind() != null);
  }
}
