package com.matt.forgehax.util.command;

import com.google.common.base.Strings;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.matt.forgehax.util.command.callbacks.CallbackData;
import com.matt.forgehax.util.command.exception.CommandBuildException;
import com.matt.forgehax.util.command.exception.CommandExecuteException;
import com.matt.forgehax.util.key.BindingHelper;
import com.matt.forgehax.util.key.LegacyKeyCodes;
import com.matt.forgehax.util.key.IKeyBind;
import com.matt.forgehax.util.serialization.ISerializableJson;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Map;

/**
 * Created on 6/8/2017 by fr1kin
 */
public class CommandStub extends Command implements IKeyBind, ISerializableJson {

  public static final String KEYBIND = "Command.keybind";
  public static final String KEYBIND_OPTIONS = "Command.keybind_options";

  /**
   * Marks a command that takes no keybind at all. This cannot be InputConstants.UNKNOWN (-1),
   * which is a real, valid state meaning "bindable but currently unbound".
   */
  public static final int NO_KEYBIND = Integer.MIN_VALUE;

  private final KeyMapping bind;

  /**
   * Set when a bind was read from a pre-1.20.1 config, so the translated code is written straight
   * back out. Without that the next load would translate the already-GLFW value again and lose it.
   */
  private boolean bindNeedsRewrite = false;

  protected CommandStub(Map<String, Object> data) throws CommandBuildException {
    super(data);

    // key binding
    int keyCode = (Integer) data.getOrDefault(KEYBIND, NO_KEYBIND);
    if (keyCode != NO_KEYBIND) {
      InputConstants.Key bound = BindingHelper.getKey(keyCode);
      bind = new KeyMapping(getAbsoluteName(), bound.getType(), bound.getValue(), "ForgeHax");

      Boolean genOptions = (Boolean) data.getOrDefault(KEYBIND_OPTIONS, true);
      if (genOptions) {
        parser.accepts("bind", "Bind to the given key").withRequiredArg();
        parser.accepts("unbind", "Sets bind to KEY_NONE");

        this.processors.add(
            dt -> {
              if (dt.hasOption("bind")) {
                String key = dt.getOptionAsString("bind").toLowerCase();

                int kc;
                try {
                  kc = InputConstants.getKey("key.keyboard." + key).getValue();
                } catch (IllegalArgumentException e) {
                  kc = InputConstants.UNKNOWN.getValue();
                }
                if (kc == InputConstants.UNKNOWN.getValue()) {
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
    writer.value(bind != null ? bind.getKey().getValue() : InputConstants.UNKNOWN.getValue());
    writer.name(LegacyKeyCodes.CODES_KEY);
    writer.value(LegacyKeyCodes.CODES_GLFW);

    writer.endObject();
  }

  @Override
  public void deserialize(JsonReader reader) throws IOException {
    reader.beginObject();

    reader.nextName();
    int kc = reader.nextInt();

    // configs written by the 1.12.2 build carry no codes marker and hold LWJGL2 scancodes
    boolean glfw = false;
    while (reader.hasNext()) {
      if (LegacyKeyCodes.CODES_KEY.equals(reader.nextName())) {
        glfw = LegacyKeyCodes.CODES_GLFW.equals(reader.nextString());
      } else {
        reader.skipValue();
      }
    }

    if (bind != null) {
      bind(glfw ? kc : LegacyKeyCodes.toGlfw(kc));
      bindNeedsRewrite = !glfw;
    }

    reader.endObject();
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
      bind.setKey(BindingHelper.getKey(keyCode));
      KeyMapping.resetMapping();
    }
  }

  @Nullable
  public KeyMapping getBind() {
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
