package com.matt.forgehax.mods.services;

import com.google.common.util.concurrent.AtomicDouble;
import com.matt.forgehax.util.color.Colors;
import com.matt.forgehax.util.mod.ServiceMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import static net.minecraft.ChatFormatting.RED;

/**
 * Created by Babbaj on 4/10/2018.
 */
@RegisterMod
public class MainMenuGuiService extends ServiceMod {

  public MainMenuGuiService() {
    super("MainMenuGuiService");
  }

  @SubscribeEvent
  public void onGui(ScreenEvent.Init.Post event) {
    if (event.getScreen() instanceof TitleScreen) {
      Screen gui = event.getScreen();

      List<? extends GuiEventListener> children = gui.children();
      children
          .stream()
          .filter(w -> w instanceof Button)
          .skip(4) // skip first 4 buttons
          .forEach(
              w -> {
                AbstractWidget widget = (AbstractWidget) w;
                widget.setY(widget.getY() + 24);
              }); // lower the rest of the buttons to make room for ours

      Button customButton =
          Button.builder(Component.literal("Command Input"), b -> MC.setScreen(new CommandInputGui()))
              .bounds(
                  gui.width / 2 - 100,
                  gui.height / 4 + 48 + (24 * 3), // put button in 4th row
                  200,
                  20
              )
              .build();
      event.addListener(customButton);
    }
  }

  private enum ClientMode {
    FORGEHAX("Forgehax"),
    FUTURE("Future");

    private final String name;

    ClientMode(String nameIn) {
      this.name = nameIn;
    }

    public String getName() {
      return this.name;
    }
  }

  public class CommandInputGui extends Screen {

    EditBox inputField;
    Button modeButton;
    ClientMode mode = ClientMode.FORGEHAX;
    Deque<String> messageHistory = new LinkedList<>();

    // ordered from oldest to newest
    List<String> inputHistory = new ArrayList<>();
    int sentHistoryCursor = 0;
    String historyBuffer = "";

    protected CommandInputGui() {
      super(Component.literal("Command Input"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      this.renderBackground(graphics);
      graphics.fill(
          2, this.height - 16, this.width - 104, this.height - 4, Integer.MIN_VALUE); // input field
      graphics.fill(2, 2, this.width - 2, this.height - 38, 70 << 24); // messageHistory box
      this.drawHistory(graphics);
      super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
        MC.setScreen(null);
        return true;
      } else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
        String str = this.inputField.getValue().trim();

        if (!str.isEmpty()) {
          this.inputField.setValue("");
          if (this.inputHistory.isEmpty()
              || !this.inputHistory.get(this.inputHistory.size() - 1).equals(str)) {
            this.inputHistory.add(str);
          }
          this.sentHistoryCursor = inputHistory.size();
          this.runCommand(str);
        }
        return true;
      } else if (keyCode == GLFW.GLFW_KEY_UP) {
        // older
        String sent = getSentHistory(-1);
        if (sent != null) {
          inputField.setValue(sent);
        }
        return true;
      } else if (keyCode == GLFW.GLFW_KEY_DOWN) {
        // newer
        String sent = getSentHistory(1);
        if (sent != null) {
          inputField.setValue(sent);
        }
        return true;
      } else if (keyCode == GLFW.GLFW_KEY_PAGE_UP || keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
        return true;
      }
      return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void init() {
      this.inputField =
          new EditBox(this.font, 4, this.height - 12, this.width - 4, 12, Component.empty());
      inputField.setMaxLength(Integer.MAX_VALUE);
      inputField.setBordered(false);
      inputField.setFocused(true);
      inputField.setCanLoseFocus(false);
      addRenderableWidget(inputField);
      setInitialFocus(inputField);

      this.modeButton =
          Button.builder(
                  Component.literal(mode.getName()),
                  b -> {
                    if (mode.ordinal() == ClientMode.values().length - 1) {
                      mode = ClientMode.values()[0];
                    } else {
                      mode = ClientMode.values()[mode.ordinal() + 1];
                    }
                    b.setMessage(Component.literal(mode.getName()));
                  })
              .bounds(this.width - 100 - 2, this.height - 20 - 2, 100, 20)
              .build();
      addRenderableWidget(modeButton);
    }

    private void drawHistory(GuiGraphics graphics) {
      AtomicDouble offset = new AtomicDouble();
      messageHistory
          .stream()
          .limit(100)
          .forEach(
              str -> {
                graphics.drawString(
                    MC.font, str, 5, (this.height - 50 - offset.intValue()), Colors.WHITE.toBuffer());
                offset.addAndGet(10);
              });
    }

    @Nullable
    private String getSentHistory(int offset) {
      int pos = this.sentHistoryCursor + offset;
      final int max = this.inputHistory.size();
      pos = Mth.clamp(pos, 0, max);
      if (pos != sentHistoryCursor) {
        if (pos == max) {
          this.sentHistoryCursor = max;
          return this.historyBuffer;
        }
        if (this.sentHistoryCursor == max) {
          this.historyBuffer = inputField.getValue();
        }
        this.sentHistoryCursor = pos;
        return inputHistory.get(pos);
      }
      return null; // if cursor is out of bounds or there is no history
    }

    public void print(String message) {
      if (!message.isEmpty()) {
        for (String str : message.split("\n")) {
          messageHistory.push(str);
        }
      }
    }

    private void runCommand(String s) {
      try {
        // TODO: Future client api
        switch (mode) {
          case FORGEHAX:
            ChatCommandService.handleCommand(s);
            break;
          case FUTURE:
            print(RED + "Unsupported");
            break;
        }
      } catch (Throwable t) {
        print(RED + t.toString());
      }
    }

    @Override
    public boolean isPauseScreen() {
      return false;
    }
  }
}
