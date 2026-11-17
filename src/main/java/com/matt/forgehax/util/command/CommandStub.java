package com.matt.forgehax.util.command;

import com.google.common.base.Strings;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.matt.forgehax.util.command.callbacks.CallbackData;
import com.matt.forgehax.util.command.exception.CommandBuildException;
import com.matt.forgehax.util.command.exception.CommandExecuteException;
import com.matt.forgehax.util.key.IKeyBind;
import com.matt.forgehax.util.key.LegacyKeyCodes;
import com.matt.forgehax.util.serialization.ISerializableJson;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Map;

/**
 * Created on 6/8/2017 by fr1kin
 */
public class CommandStub extends Command implements IKeyBind, ISerializableJson {

  public static final String KEYBIND = "Command.keybind";
  public static final String KEYBIND_OPTIONS = "Command.keybind_options";

  private final KeyBinding bind;

  private boolean bindNeedsRewrite;

  protected CommandStub(Map<String, Object> data) throws CommandBuildException {
    super(data);

    // key binding
    Integer keyCode = (Integer) data.getOrDefault(KEYBIND, -1);
    if (keyCode != -1) {
      bind = new KeyBinding(getAbsoluteName(), keyCode, "ForgeHax");
      ClientRegistry.registerKeyBinding(bind);

      Boolean genOptions = (Boolean) data.getOrDefault(KEYBIND_OPTIONS, true);
      if (genOptions) {
        parser.accepts("bind", "Bind to the given key").withRequiredArg();
        parser.accepts("unbind", "Sets bind to KEY_NONE");

        this.processors.add(
            dt -> {
              if (dt.hasOption("bind")) {
                String key = dt.getOptionAsString("bind").toUpperCase();

                int kc = Keyboard.getKeyIndex(key);
                if (Keyboard.getKeyIndex(key) == Keyboard.KEY_NONE) {
                  throw new CommandExecuteException(
                      String.format("\"%s\" is not a valid key name", key));
                }

                bind(kc);
                serialize();

                dt.write(String.format("Bound %s to key %s [code=%d]", getAbsoluteName(), key, kc));
                dt.stopProcessing();
              } else if (dt.hasOption("unbind")) {
                unbind();
                serialize();

                dt.write(String.format("Unbound %s", getAbsoluteName()));
                dt.stopProcessing();
              }
            });
        this.processors.add(
            dt -> {
              if (!dt.options().hasOptions() && dt.getArgumentCount() > 0) {
                dt.write(
                    String.format(
                        "Unknown command \"%s\"", Strings.nullToEmpty(dt.getArgumentAsString(0))));
              }
            });
      }
    } else {
      bind = null;
    }
  }

  @Override
  public void serialize(JsonWriter writer) throws IOException {
    writer.beginObject();

    writer.name("bind");
    if (bind != null) {
      writer.value(bind.getKeyCode());
    } else {
      writer.value(-1);
    }

    writer.endObject();
  }

  @Override
  public void deserialize(JsonReader reader) throws IOException {
    boolean[] glfw = new boolean[1];
    Integer kc = BindJson.read(reader, glfw);
    if (bind != null && kc != null) {
      bind(kc);
      bindNeedsRewrite = glfw[0];
    }
  }

  /**
   * Nested so it can be tested without initializing Command, whose static state needs the game.
   */
  static final class BindJson {

    /**
     * Reads one bind object as a native LWJGL2 code, or null when it holds no bind. Configs written
     * by the 1.20.1 build mark their GLFW codes, which are translated once; glfwOut[0] reports it.
     */
    @Nullable
    static Integer read(JsonReader reader, boolean[] glfwOut) throws IOException {
      Integer kc = null;
      boolean glfw = false;
      reader.beginObject();
      while (reader.hasNext()) {
        String name = reader.nextName();
        if ("bind".equals(name)) {
          kc = reader.nextInt();
        } else if (LegacyKeyCodes.CODES_KEY.equals(name)) {
          glfw = LegacyKeyCodes.CODES_GLFW.equals(reader.nextString());
        } else {
          reader.skipValue();
        }
      }
      reader.endObject();
      glfwOut[0] = glfw;
      return kc != null && glfw ? Integer.valueOf(LegacyKeyCodes.toLwjgl2(kc)) : kc;
    }
  }

  @Override
  public void deserialize() {
    super.deserialize();
    if (bindNeedsRewrite) {
      bindNeedsRewrite = false;
      serialize();
    }
  }

  @Override
  public void bind(int keyCode) {
    if (bind != null) {
      bind.setKeyCode(keyCode);
      KeyBinding.resetKeyBindingArrayAndHash();
    }
  }

  @Nullable
  public KeyBinding getBind() {
    return bind;
  }

  @Override
  public void onKeyPressed() {
    invokeCallbacks(CallbackType.KEY_PRESSED, new CallbackData(this));
  }

  @Override
  public void onKeyDown() {
    invokeCallbacks(CallbackType.KEY_DOWN, new CallbackData(this));
  }
}
