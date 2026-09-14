package com.matt.forgehax.mods.commands;

import com.matt.forgehax.util.command.Command;
import com.matt.forgehax.util.command.CommandBuilders;
import com.matt.forgehax.util.mod.CommandMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import java.util.Arrays;

import static com.matt.forgehax.Helper.getLocalPlayer;

@RegisterMod
public class SayCommand extends CommandMod {

  public SayCommand() {
    super("SayCommand");
  }

  @RegisterCommand
  public Command say(CommandBuilders builders) {
    return builders
        .newCommandBuilder()
        .name("say")
        .description("Send chat message")
        .options(
            parser -> {
              parser.acceptsAll(
                  Arrays.asList("fake", "f"),
                  "Send a fake message that won't be treated as command"
              );
              parser.acceptsAll(Arrays.asList("local", "l"), "Send message from local chat");
            }
        )
        .processor(
            data -> {
              boolean fake = data.hasOption("fake");
              // any emoji will work until 1.13
              final int fakePrefix = 0x1F921;
              String msg = data.getArgumentCount() > 0 ? data.getArgumentAsString(0) : "";

              if (getLocalPlayer() != null) {
                if (fake) {
                  msg = new StringBuilder().appendCodePoint(fakePrefix).append(msg).toString();
                }
                if (data.hasOption("local")) {
                  getLocalPlayer().connection.sendChat(msg);
                } else {
                  // TODO(1.20.1): raw CPacketChatMessage bypass is gone; chat packets now
                  // require signing (ServerboundChatPacket + LastSeenMessages state), so
                  // the "fake" no-echo send falls back to the normal signed path.
                  getLocalPlayer().connection.sendChat(msg);
                }
              }
            }
        )
        .build();
  }
}
