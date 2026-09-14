package com.matt.forgehax.mods.commands;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.matt.forgehax.Helper;
import com.matt.forgehax.mods.services.ChatCommandService;
import com.matt.forgehax.util.command.Command;
import com.matt.forgehax.util.command.CommandBuilders;
import com.matt.forgehax.util.command.ExecuteData;
import com.matt.forgehax.util.command.Options;
import com.matt.forgehax.util.command.exception.CommandExecuteException;
import com.matt.forgehax.util.mod.CommandMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import com.matt.forgehax.util.serialization.ISerializableJson;
import com.matt.forgehax.util.key.BindingHelper;
import com.mojang.blaze3d.platform.InputConstants;
import joptsimple.OptionParser;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.commons.lang3.ArrayUtils;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

// TODO: hold macros
@RegisterMod
public class MacroCommand extends CommandMod {

  public final Options<MacroEntry> MACROS =
      GLOBAL_COMMAND
          .builders()
          .<MacroEntry>newOptionsBuilder()
          .name("macros")
          .description("Registered macros")
          .supplier(ArrayList::new)
          .factory(MacroEntry::new)
          .build();
  // which command in the list of commands to execute next
  private final Map<MacroEntry, Integer> macroIndex = new HashMap<>();

  public MacroCommand() {
    super("MacroCommand");
  }

  private static Iterator<String> stringIterator(JsonArray jsonArray) {
    return Streams.stream(jsonArray).map(JsonElement::getAsString).iterator();
  }

  // resolves a friendly key name (eg. "f", "space") to its GLFW key code
  private static int getKeyCode(String name) {
    try {
      return GLFW.class.getField("GLFW_KEY_" + name.toUpperCase()).getInt(null);
    } catch (ReflectiveOperationException e) {
      return InputConstants.UNKNOWN.getValue();
    }
  }

  @SubscribeEvent
  public void onKeyboardEvent(InputEvent.Key event) {
    MACROS
        .stream()
        .filter(macro -> !macro.isAnonymous())
        .filter(macro -> macro.getBind().consumeClick())
        .forEach(this::executeMacro);

    // execute anonymous macros
    if (event.getAction() != GLFW.GLFW_RELEASE) { // on press
      MACROS
          .stream()
          .filter(MacroEntry::isAnonymous)
          .filter(macro -> macro.getKey() == event.getKey())
          .forEach(this::executeMacro);
    }
  }

  private void removeMacro(MacroEntry macro) {
    MACROS.remove(macro);

    if (macro.name.isPresent()) {
      MC.options.keyMappings =
          ArrayUtils.remove(
              MC.options.keyMappings,
              ArrayUtils.indexOf(MC.options.keyMappings, macro.getBind())
          );
      KeyMapping.resetMapping();
    }
    // TODO(1.20.1): KeyMapping.getKeybinds() is gone; empty "Macros" category cleanup
    // for the controls screen is dropped for now (categories are keyed off
    // KeyMapping.CATEGORY_SORT_ORDER which we never touch here).
  }

  @Override
  public void onLoad() {
    super.onLoad();
    MACROS.deserializeAll();

    MACROS
        .builders()
        .newCommandBuilder()
        .name("remove")
        .description("Remove a macro (Usage: \".macros remove --name hack (and/or) --key f\") ")
        .options(MacroBuilders::keyOption)
        .options(MacroBuilders::nameOption)
        .processor(data -> {
          if (!data.hasOption("key") && !data.hasOption("name")) {
            // jopt doesn't seem to allow options to depend on each other
            throw new CommandExecuteException("Missing required option(s) [k/key], [n/name]");
          }

          if (data.hasOption("key")) {
            // remove by key
            final int key = getKeyCode(data.getOptionAsString("key"));
            MACROS
                .stream()
                .filter(macro -> macro.getKey() == key)
                .peek(
                    __ ->
                        Helper.printMessage(
                            "Removing bind for key \"%s\"", BindingHelper.getIndexName(key)))
                .forEach(this::removeMacro);
          }
          if (data.hasOption("name")) {
            // remove by name
            final String name = data.getOptionAsString("name");
            MACROS
                .stream()
                .filter(macro -> macro.getName().map(name::equals).orElseGet(name::isEmpty))
                .peek(__ -> Helper.printMessage("Removing bind \"%s\"", name))
                .forEach(this::removeMacro);
          }
        })
        .build();

    MACROS
        .builders()
        .newCommandBuilder()
        .name("list")
        .description("List all the macros")
        .options(MacroBuilders::fullOption)
        .processor(data -> {
          Helper.printMessage("Macros (%d):", MACROS.size());
          for (MacroEntry macro : MACROS) {
            data.write(
                macro.name.map(name -> '\"' + name + '\"').orElse("anonymous") + ": "
                    + BindingHelper.getIndexName(macro.key));
            if (data.hasOption("full")) {
              data.incrementIndent();
              data.write(this.rawMacroString(macro));
              data.decrementIndent();
            }
          }
        })
        .build();
  }

  @Override
  public void onUnload() {
    MACROS.serializeAll();
  }

  // TODO: split into separate lines if too long
  private String rawMacroString(MacroEntry macro) {
    return macro.commands.stream()
                         .map(nested ->
                             nested.stream()
                                   .collect(Collectors.joining(";", "(", ")"))
                         )
                         .collect(Collectors.joining(" "));
  }

  @RegisterCommand
  public Command executeMacro(CommandBuilders builder) {
    return builder
        .newCommandBuilder()
        .name("exec")
        .description("Execute a named macro")
        .requiredArgs(1)
        .processor(
            data -> {
              final String name = data.getArgumentAsString(0);
              final MacroEntry macro =
                  MACROS
                      .stream()
                      .filter(entry -> entry.getName().map(name::equals).orElse(false))
                      .findFirst()
                      .orElseThrow(
                          () -> new CommandExecuteException(String.format("Unknown macro: \"%s\"", name)));

              executeMacro(macro);
            })
        .build();
  }

  private void executeMacro(MacroEntry macro) {
    final int currentIndex = Optional.ofNullable(macroIndex.putIfAbsent(macro, 0)).orElse(0);
    macro.getCommands().get(currentIndex).forEach(ChatCommandService::handleCommand);
    macroIndex.replace(macro, rotate(currentIndex, 0, macro.getCommands().size() - 1));
  }

  private int rotate(int i, int min, int max) {
    return (i >= max) ? min : i + 1;
  }

  @RegisterCommand
  public Command bindMacro(CommandBuilders builder) {
    return builder
        .newCommandBuilder()
        .name("bindmacro")
        .description("Usage: .bindmacro f \"clip 5; <command>; ...\" --name cool_macro")
        .options(MacroBuilders::nameOption)
        .processor(MacroBuilders::parseName)
        .processor(
            data -> {
              data.requiredArguments(2);
              final int key = getKeyCode(data.getArgumentAsString(0));
              if (data.getOption("name") == null && key == InputConstants.UNKNOWN.getValue()) {
                throw new CommandExecuteException("A macro must have a name and/or a valid key");
              }

              final List<ImmutableList<String>> commands =
                  data.arguments()
                      .stream()
                      .skip(1) // skip key
                      .map(Object::toString)
                      .map(this::parseCommand)
                      .collect(Collectors.toList());

              final MacroEntry macro =
                  new MacroEntry((String) data.getOption("name"), key, commands);
              MACROS
                  .stream()
                  .filter(m -> m.getName().isPresent() && m.getName().equals(macro.getName()))
                  .findFirst()
                  .ifPresent(alreadyExists -> {
                    throw new CommandExecuteException(
                        String.format("Command \"%s\" already exists!", alreadyExists.getName().get()));
                  });
              MACROS.add(macro);

              if (!macro.isAnonymous()) {
                macro.registerBind();
              }

              Helper.printMessage("Successfully bound to %s", BindingHelper.getIndexName(key));
            })
        .build();
  }

  private ImmutableList<String> parseCommand(String input) {
    return ImmutableList.copyOf(Arrays.asList(
        input.split(";"))); // TODO: don't split semicolons in quotes and allow escaped semicolons
  }

  public static class MacroEntry implements ISerializableJson {

    private final List<ImmutableList<String>> commands = new ArrayList<>();
    private final Optional<String> name;
    private int key = InputConstants.UNKNOWN.getValue();
    @Nullable // null if this is an anonymous macro (ie !name.isPresent())
    private transient KeyMapping bind;

    public MacroEntry(String name) {
      this.name = name.isEmpty() ? Optional.empty() : Optional.of(name);
    }

    public MacroEntry(@Nullable String name, int key, List<ImmutableList<String>> commands) {
      this.name = Optional.ofNullable(name);
      this.key = key;
      this.commands.addAll(commands);
    }

    public int getKey() {
      return Optional.ofNullable(bind).map(km -> km.getKey().getValue()).orElse(this.key);
    }

    public Optional<String> getName() {
      return this.name;
    }

    public boolean isAnonymous() {
      return !getName().isPresent();
    }

    public List<ImmutableList<String>> getCommands() {
      return commands;
    }

    public KeyMapping getBind() {
      return this.bind;
    }

    // only done for named macros
    private void registerBind() {
      KeyMapping bind = new KeyMapping(name.get(), this.getKey(), "Macros");
      // TODO: listen for key pressed for anonymous macros
      // No more ClientRegistry.registerKeyBinding for dynamic runtime binds (that API now
      // expects static registration via RegisterKeyMappingsEvent); append directly instead,
      // same as CommandStub/removeMacro already do for the options key list.
      MC.options.keyMappings = ArrayUtils.add(MC.options.keyMappings, bind);
      KeyMapping.resetMapping();
      this.bind = bind;
    }

    @Override
    public void serialize(JsonWriter writer) throws IOException {
      writer.beginObject();
      writer.name("key");
      writer.value(getKey());

      writer.name("commands");
      writer.beginArray();
      for (final List<String> list : commands) {
        writer.beginArray();
        for (final String cmd : list) {
          writer.value(cmd);
        }
        writer.endArray();
      }
      writer.endArray();
      writer.endObject();
    }

    @Override
    public void deserialize(JsonReader reader) throws IOException {
      JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
      this.key = root.get("key").getAsInt();

      Streams.stream(root.get("commands").getAsJsonArray())
             .map(JsonElement::getAsJsonArray)
             .map(jArray -> ImmutableList.copyOf(stringIterator(jArray)))
             .forEach(commands::add);

      if (!this.isAnonymous()) {
        this.registerBind();
      }
    }

    @Override
    public String toString() {
      return name.orElse("");
    }
  }

  private static class MacroBuilders {

    static void nameOption(OptionParser parser) {
      parser.acceptsAll(Arrays.asList("name", "n"), "name").withRequiredArg();
    }

    static void keyOption(OptionParser parser) {
      parser.acceptsAll(Arrays.asList("key", "k"), "key").withRequiredArg();
    }

    static void fullOption(OptionParser parser) {
      parser.accepts("full").withOptionalArg();
    }

    static void parseName(ExecuteData data) {
      final @Nullable String name = (String) data.getOption("name");
      data.set("name", Optional.ofNullable(name));
    }
  }
}
