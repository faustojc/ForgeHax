package com.matt.forgehax.util.key;

import com.matt.forgehax.Globals;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

public class Bindings implements Globals {

  public static final List<KeyBindingHandler> KEY_LIST = getAllKeys();
  public static final KeyBindingHandler forward = new KeyBindingHandler(MC.options.keyUp);
  public static final KeyBindingHandler back = new KeyBindingHandler(MC.options.keyDown);
  public static final KeyBindingHandler left = new KeyBindingHandler(MC.options.keyLeft);
  public static final KeyBindingHandler right = new KeyBindingHandler(MC.options.keyRight);
  public static final KeyBindingHandler jump = new KeyBindingHandler(MC.options.keyJump);
  public static final KeyBindingHandler sprint = new KeyBindingHandler(MC.options.keySprint);
  public static final KeyBindingHandler sneak = new KeyBindingHandler(MC.options.keyShift);
  public static final KeyBindingHandler attack = new KeyBindingHandler(MC.options.keyAttack);
  public static final KeyBindingHandler use = new KeyBindingHandler(MC.options.keyUse);

  @Nullable
  public static KeyBindingHandler getKey(String name) {
    return Bindings.KEY_LIST
        .stream()
        .filter(k -> k.getBinding().getName().toLowerCase().contains(name.toLowerCase()))
        .findFirst()
        .orElse(null);
  }

  // reflectively get KeyBindingHandlers from Options
  @Nullable
  private static List<KeyBindingHandler> getAllKeys() {
    Field[] fields = Options.class.getFields();
    return Arrays.stream(fields)
                 .filter(f -> f.getType() == KeyMapping.class)
                 .map(Bindings::getBinding)
                 .filter(Objects::nonNull)
                 .map(KeyBindingHandler::new)
                 .collect(toList());
  }

  private static KeyMapping getBinding(Field field) {
    try {
      return (KeyMapping) field.get(MC.options);
    } catch (IllegalAccessException e) {
      e.printStackTrace();
      return null;
    }
  }
}
