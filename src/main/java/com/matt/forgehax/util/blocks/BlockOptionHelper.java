package com.matt.forgehax.util.blocks;

import com.google.common.collect.Sets;
import com.matt.forgehax.util.SafeConverter;
import com.matt.forgehax.util.blocks.exceptions.BadBlockEntryFormatException;
import com.matt.forgehax.util.blocks.exceptions.BlockDoesNotExistException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created on 5/18/2017 by fr1kin
 *
 * <p>1.20.1 has no metadata/sub-block model: each former metadata variant (e.g. wool colors)
 * is now its own registered {@link Block}, and what varies within a single Block is its
 * {@link BlockState}. "meta" here is repurposed as a global block-state id
 * ({@link Block#getId(BlockState)}/{@link Block#stateById(int)}), with {@code -1} still meaning
 * "no specific state / matches any state of this block".
 */
public class BlockOptionHelper {

  public static boolean isAir(String name) {
    ResourceLocation location = ResourceLocation.tryParse(name);
    return location != null && Objects.equals(BuiltInRegistries.BLOCK.getKey(Blocks.AIR), location);
  }

  public static boolean isAir(int id) {
    return id == 0;
  }

  public static Collection<BlockState> getAllBlocks(Block block) {
    return block != null
        ? Collections.unmodifiableCollection(block.getStateDefinition().getPossibleStates())
        : Collections.emptyList();
  }

  public static void getAllBlocksMatchingByUnlocalized(
      final Collection<BlockEntry> found, String regex) {
    final Pattern pattern = Pattern.compile(regex);
    BuiltInRegistries.BLOCK.forEach(
        block -> {
          ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
          Matcher matcher = pattern.matcher(key.getPath().toLowerCase());
          if (matcher.find()) {
            try {
              found.add(new BlockEntry(block, -1, false));
            } catch (BlockDoesNotExistException e) {
            }
          }
        });
  }

  public static Collection<BlockEntry> getAllBlocksMatchingByUnlocalized(String regex) {
    Collection<BlockEntry> map = Sets.newHashSet();
    getAllBlocksMatchingByUnlocalized(map, regex);
    return map;
  }

  public static void getAllBlocksMatchingByLocalized(
      final Collection<BlockEntry> found, String regex) {
    final Pattern pattern = Pattern.compile(regex);
    BuiltInRegistries.BLOCK.forEach(
        block -> {
          Matcher matcher =
              pattern.matcher(block.getName().getString().replaceAll(" ", "_").toLowerCase());
          if (matcher.find()) {
            try {
              found.add(new BlockEntry(block, -1, false));
            } catch (BlockDoesNotExistException e) {
            }
          }
        });
  }

  public static Collection<BlockEntry> getAllBlocksMatchingByLocalized(String regex) {
    Collection<BlockEntry> map = Sets.newHashSet();
    getAllBlocksMatchingByLocalized(map, regex);
    return map;
  }

  public static Collection<BlockEntry> getAllBlockMatching(String regex) {
    Collection<BlockEntry> map = Sets.newHashSet();
    getAllBlocksMatchingByUnlocalized(map, regex);
    getAllBlocksMatchingByLocalized(map, regex);
    return map;
  }

  public static boolean isValidMetadataValue(Block block, int stateId) {
    if (stateId < 0) {
      return true; // -1 = no specific state requested
    }
    BlockState state = Block.stateById(stateId);
    return state != null && state.getBlock() == block;
  }

  public static BlockData fromUniqueName(String uniqueName)
      throws BlockDoesNotExistException, BadBlockEntryFormatException {
    String[] split = uniqueName.split("::");
    if (split.length < 1) {
      throw new BadBlockEntryFormatException();
    }
    String name = split[0];
    int meta = SafeConverter.toInteger(split.length > 1 ? split[1] : -1, -1);
    ResourceLocation location = ResourceLocation.tryParse(name);
    Block block = location != null ? BuiltInRegistries.BLOCK.getOptional(location).orElse(null) : null;
    if (block == null) {
      throw new BlockDoesNotExistException(uniqueName + " is not a valid block");
    }
    BlockData data = new BlockData();
    data.block = block;
    data.meta = meta;
    return data;
  }

  public static void requiresValidBlock(Block block, int metadataId)
      throws BlockDoesNotExistException {
    if (block == null || block.equals(Blocks.AIR)) {
      throw new BlockDoesNotExistException("Attempted to create entry for a non-existent block");
    }
    if (!BlockOptionHelper.isValidMetadataValue(block, metadataId)) {
      throw new BlockDoesNotExistException(
          String.format(
              "Attempted to create entry for block \"%s\" with a invalid meta id of \"%d\"",
              BuiltInRegistries.BLOCK.getKey(block).toString(), metadataId
          ));
    }
  }

  public static class BlockData {

    public Block block = null;
    public int meta = -1;
  }
}
