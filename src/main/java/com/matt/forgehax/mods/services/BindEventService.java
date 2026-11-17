package com.matt.forgehax.mods.services;

import com.matt.forgehax.gui.ClickGui;
import com.matt.forgehax.util.command.CommandStub;
import com.matt.forgehax.util.mod.ServiceMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import static com.matt.forgehax.Helper.getGlobalCommand;

/**
 * Created on 6/14/2017 by fr1kin
 */
@RegisterMod
public class BindEventService extends ServiceMod {

  /**
   * Whether the key event currently being handled by a screen arrived while binds were accepted.
   */
  private boolean keyAccepted;

  public BindEventService() {
    super("BindEventService");
  }

  // Minecraft.runTick only polls input itself (firing these two) while no screen is open, or the
  // open screen allows user input. Otherwise GuiScreen.handleInput drains the queue and posts the
  // GuiScreenEvent ones below, so every LWJGL event reaches exactly one of the two paths.

  @SubscribeEvent
  public void onKeyboardEvent(InputEvent.KeyInputEvent event) {
    dispatchKey(acceptsInput());
  }

  @SubscribeEvent
  public void onMouseEvent(InputEvent.MouseInputEvent event) {
    dispatchMouse();
  }

  @SubscribeEvent
  public void onGuiKeyboardEventPre(GuiScreenEvent.KeyboardInputEvent.Pre event) {
    // sampled before the screen sees the key, so the key that ends a text edit or bind capture
    // is not also dispatched as a bind
    keyAccepted = acceptsInput();
  }

  @SubscribeEvent
  public void onGuiKeyboardEvent(GuiScreenEvent.KeyboardInputEvent.Post event) {
    dispatchKey(keyAccepted && acceptsInput());
  }

  @SubscribeEvent
  public void onGuiMouseEvent(GuiScreenEvent.MouseInputEvent.Pre event) {
    GuiScreen before = MC.currentScreen;
    dispatchMouse();
    // a bind that closed or replaced the screen must not also click it
    if (MC.currentScreen != before) {
      event.setCanceled(true);
    }
  }

  private void dispatchKey(boolean accepted) {
    int key = Keyboard.getEventKey();
    // key 0 carries only a character, releases never fire a bind
    if (!accepted || key == Keyboard.KEY_NONE || !Keyboard.getEventKeyState()) {
      return;
    }
    dispatch(key, !Keyboard.isRepeatEvent());
  }

  private void dispatchMouse() {
    int button = Mouse.getEventButton();
    // button -1 is movement or the wheel
    if (button < 0 || !Mouse.getEventButtonState() || !acceptsInput()) {
      return;
    }
    dispatch(button - 100, true);
  }

  /**
   * Vanilla only updates KeyBinding's pressed state while no screen is open, so the bind state is
   * read straight off the input event instead. That is also what lets the GUI's own bind close it.
   */
  private void dispatch(int code, boolean pressed) {
    getGlobalCommand()
        .getChildrenDeep()
        .stream()
        .filter(command -> command instanceof CommandStub)
        .map(command -> (CommandStub) command)
        .filter(stub -> stub.getBind() != null && stub.getBind().getKeyCode() == code)
        .forEach(
            stub -> {
              if (pressed) {
                stub.onKeyPressed();
              }
              stub.onKeyDown();
            });
  }

  /**
   * Binds fire with no screen open, and inside our own GUI unless it is capturing input.
   */
  private boolean acceptsInput() {
    if (MC.currentScreen == null) {
      return true;
    }
    return MC.currentScreen instanceof ClickGui && !((ClickGui) MC.currentScreen).isCapturingInput();
  }
}
