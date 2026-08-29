package com.matt.forgehax.mods.commands;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.matt.forgehax.util.command.Options;
import com.matt.forgehax.util.command.exception.CommandExecuteException;
import com.matt.forgehax.util.mod.CommandMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import com.matt.forgehax.util.serialization.ISerializableJson;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;

/**
 * Global friend list. Mods should go through PlayerUtils::isFriend instead of using this directly.
 */
@RegisterMod
public class FriendsCommand extends CommandMod {

  @Nullable
  private static Options<FriendEntry> instance = null;
  public final Options<FriendEntry> FRIENDS =
      GLOBAL_COMMAND
          .builders()
          .<FriendEntry>newOptionsBuilder()
          .name("friends")
          .description("Players that mods will not target")
          .supplier(Sets::newConcurrentHashSet)
          .factory(FriendEntry::new)
          .build();

  public FriendsCommand() {
    super("FriendsCommand", "Global friend list");
    instance = FRIENDS;
    buildSubCommands();
  }

  public static boolean isFriend(String name) {
    return instance != null
        && !Strings.isNullOrEmpty(name)
        && instance.get(name.toLowerCase()) != null;
  }

  private void buildSubCommands() {
    FRIENDS
        .builders()
        .newCommandBuilder()
        .name("add")
        .description("Add a player by name")
        .processor(
            data -> {
              data.requiredArguments(1);
              final String name = data.getArgumentAsString(0);

              if (Strings.isNullOrEmpty(name)) {
                throw new CommandExecuteException("Empty or null argument");
              }

              if (FRIENDS.add(new FriendEntry(name))) {
                data.write(String.format("Added friend \"%s\"", name));
                data.markSuccess();
              } else {
                data.write(String.format("\"%s\" is already a friend", name));
                data.markFailed();
              }
            })
        .success(cb -> FRIENDS.serialize())
        .build();

    FRIENDS
        .builders()
        .newCommandBuilder()
        .name("remove")
        .description("Remove a player by name")
        .processor(
            data -> {
              data.requiredArguments(1);
              final String name = data.getArgumentAsString(0);

              if (Strings.isNullOrEmpty(name)) {
                throw new CommandExecuteException("Empty or null argument");
              }

              Optional<FriendEntry> match = Optional.ofNullable(FRIENDS.get(name.toLowerCase()));

              if (match.isPresent() && FRIENDS.remove(match.get())) {
                data.write(String.format("Removed friend \"%s\"", match.get().getName()));
                data.markSuccess();
              } else {
                data.write(String.format("\"%s\" is not a friend", name));
                data.markFailed();
              }
            })
        .success(cb -> FRIENDS.serialize())
        .build();

    FRIENDS
        .builders()
        .newCommandBuilder()
        .name("list")
        .description("List current friends")
        .processor(
            data -> {
              Iterator<FriendEntry> it = FRIENDS.iterator();
              StringBuilder builder = new StringBuilder();
              while (it.hasNext()) {
                builder.append(it.next().getName());
                if (it.hasNext()) {
                  builder.append(", ");
                }
              }
              data.write(builder.length() > 0 ? builder.toString() : "empty");
              data.markSuccess();
            })
        .build();
  }

  /**
   * Names are matched case insensitively, so the entry is stored lowercase
   */
  public static class FriendEntry implements ISerializableJson {

    private final String name;

    public FriendEntry(String name) {
      this.name = name.toLowerCase();
    }

    public String getName() {
      return name;
    }

    @Override
    public void serialize(JsonWriter writer) throws IOException {
      writer.beginObject();
      writer.endObject();
    }

    @Override
    public void deserialize(JsonReader reader) throws IOException {
      reader.beginObject();
      reader.endObject();
    }

    @Override
    public int hashCode() {
      return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      return obj == this
          || (obj instanceof FriendEntry && name.equals(((FriendEntry) obj).name))
          || (obj instanceof String && name.equalsIgnoreCase((String) obj));
    }

    @Override
    public String toString() {
      return name;
    }
  }
}
