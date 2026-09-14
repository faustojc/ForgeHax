package com.matt.forgehax.mods.commands;

import com.matt.forgehax.util.blocks.BlockOptionHelper;
import com.matt.forgehax.util.command.Command;
import com.matt.forgehax.util.command.CommandBuilders;
import com.matt.forgehax.util.mod.CommandMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created on 5/27/2017 by fr1kin
 */
@RegisterMod
public class BlocksCommand extends CommandMod {

  public BlocksCommand() {
    super("BlocksCommand");
  }

  @RegisterCommand
  public Command blocks(CommandBuilders builders) {
    return builders
        .newCommandBuilder()
        .name("blocks")
        .description("Find block(s) with matching name")
        .processor(
            data -> {
              data.requiredArguments(1);
              final String find = data.getArgumentAsString(0).toLowerCase();
              final StringBuilder builder = new StringBuilder("Search results:\n");
              BuiltInRegistries.BLOCK.forEach(
                  block -> {
                    final ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
                    final boolean resourceMatches =
                        key != null && key.toString().toLowerCase().contains(find);
                    final AtomicBoolean addedResource = new AtomicBoolean(resourceMatches);
                    // 1.20.1 has no sub-block metadata; each BlockState of a block shares the
                    // same name/description id, so this only differs by numeric state id.
                    BlockOptionHelper.getAllBlocks(block)
                                     .forEach(
                                         state -> {
                                           String localized = block.getName().getString();
                                           String unlocalized = block.getDescriptionId();
                                           if (resourceMatches
                                               || unlocalized.toLowerCase().contains(find)
                                               || localized.toLowerCase().contains(find)) {
                                             if (addedResource.compareAndSet(false, true)) {
                                               builder.append(String.format("[%s] ", key));
                                               builder.append(localized);
                                               builder.append('\n');
                                             }
                                             builder.append(
                                                 String.format("[%d]> ", Block.getId(state)));
                                             builder.append(localized);
                                             builder.append(" | ");
                                             builder.append(unlocalized);
                                             builder.append('\n');
                                           }
                                         });
                  });
              data.write(builder.toString());
              data.markSuccess();
            })
        .build();
  }
}
