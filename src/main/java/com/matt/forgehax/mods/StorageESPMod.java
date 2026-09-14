package com.matt.forgehax.mods;

import com.matt.forgehax.events.LocalPlayerUpdateEvent;
import com.matt.forgehax.events.RenderEvent;
import com.matt.forgehax.util.blocks.PersistentBlockCache;
import com.matt.forgehax.util.color.Colors;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import com.matt.forgehax.util.tesselation.GeometryMasks;
import com.matt.forgehax.util.tesselation.GeometryTessellator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.Helper.getWorld;

/** Shows storage positions using a persistent, distance-indexed cache. */
@RegisterMod
public class StorageESPMod extends ToggleMod {
  private final Setting<Integer> refreshTicks =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("refresh-ticks")
          .description("Ticks between loaded storage refreshes")
          .defaultTo(10)
          .min(1)
          .max(100)
          .build();

  private final Setting<Integer> distance =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("distance")
          .description("Maximum distance to display cached storage")
          .defaultTo(256)
          .min(16)
          .max(1024)
          .build();

  private final PersistentBlockCache cache = new PersistentBlockCache();
  private final List<Entity> dynamicStorage = new ArrayList<>();
  private Path cachePath;
  private int ticks;

  public StorageESPMod() {
    super(Category.RENDER, "StorageESP", false, "Shows storage");
  }

  private int getTileEntityColor(BlockEntity tileEntity) {
    if (tileEntity instanceof ChestBlockEntity
        || tileEntity instanceof DispenserBlockEntity
        || tileEntity instanceof ShulkerBoxBlockEntity) {
      return Colors.ORANGE.toBuffer();
    } else if (tileEntity instanceof EnderChestBlockEntity) {
      return Colors.PURPLE.toBuffer();
    } else if (tileEntity instanceof FurnaceBlockEntity) {
      return Colors.GRAY.toBuffer();
    } else if (tileEntity instanceof HopperBlockEntity) {
      return Colors.DARK_RED.toBuffer();
    }
    return -1;
  }

  private int getEntityColor(Entity entity) {
    if (entity instanceof MinecartChest) {
      return Colors.ORANGE.toBuffer();
    } else if (entity instanceof ItemFrame) {
      Item item = ((ItemFrame) entity).getItem().getItem();
      if (item instanceof BlockItem && ((BlockItem) item).getBlock() instanceof ShulkerBoxBlock) {
        return Colors.YELLOW.toBuffer();
      }
    }
    return -1;
  }

  @Override
  protected void onEnabled() {
    ticks = refreshTicks.get();
  }

  @Override
  protected void onDisabled() {
    cache.close();
    cachePath = null;
    dynamicStorage.clear();
  }

  @SubscribeEvent
  public void onUpdate(LocalPlayerUpdateEvent event) {
    ensureCache();
    if (++ticks < refreshTicks.get()) {
      return;
    }
    ticks = 0;
    refresh();
    if (getWorld().getGameTime() % 100L < refreshTicks.get()) {
      cache.flush();
    }
  }

  private void ensureCache() {
    // Dimension no longer has an int id in 1.20.1; use the dimension key's hash as a stable
    // per-dimension bucket for the on-disk cache file name.
    Path expected =
        PersistentBlockCache.pathFor("storage", getWorld().dimension().location().hashCode());
    if (!expected.equals(cachePath)) {
      cachePath = expected;
      cache.open(expected);
    }
  }

  private void refresh() {
    // ClientChunkCache exposes no "all loaded chunks" iterator in 1.20.1, so walk the
    // cache/view radius around the player instead, same as the nearby-cache scan below.
    ChunkPos center = new ChunkPos(getLocalPlayer().blockPosition());
    int radius = (distance.get() >> 4) + 1;
    for (int cx = center.x - radius; cx <= center.x + radius; cx++) {
      for (int cz = center.z - radius; cz <= center.z + radius; cz++) {
        if (!getWorld().getChunkSource().hasChunk(cx, cz)) {
          continue;
        }
        LevelChunk chunk = getWorld().getChunkSource().getChunkNow(cx, cz);
        if (chunk == null) {
          continue;
        }
        for (BlockEntity tileEntity : chunk.getBlockEntities().values()) {
          int color = getTileEntityColor(tileEntity);
          if (color != -1) {
            cache.put(tileEntity.getBlockPos(), color);
          }
        }
      }
    }

    List<BlockPos> stale = new ArrayList<>();
    cache.forEachNearby(
        getLocalPlayer().blockPosition(),
        distance.get(),
        (pos, cachedColor) -> {
          if (getWorld().hasChunkAt(pos)) {
            int currentColor = getTileEntityColor(getWorld().getBlockEntity(pos));
            if (currentColor == -1) {
              stale.add(pos);
            } else if (currentColor != cachedColor) {
              cache.put(pos, currentColor);
            }
          }
        }
    );
    for (BlockPos pos : stale) {
      cache.remove(pos);
    }

    dynamicStorage.clear();
    for (Entity entity : getWorld().entitiesForRendering()) {
      if (getEntityColor(entity) != -1) {
        dynamicStorage.add(entity);
      }
    }
  }

  @SubscribeEvent
  public void onWorldUnload(LevelEvent.Unload event) {
    cache.close();
    cachePath = null;
    dynamicStorage.clear();
  }

  @SubscribeEvent
  public void onRender(RenderEvent event) {
    cache.forEachNearby(
        getLocalPlayer().blockPosition(),
        distance.get(),
        (pos, color) ->
            GeometryTessellator.drawCuboid(
                event.getBuffer(), pos, GeometryMasks.Line.ALL, color)
    );

    double distanceSquared = (double) distance.get() * distance.get();
    for (Entity entity : dynamicStorage) {
      int color = getEntityColor(entity);
      if (entity.isAlive()
          && color != -1
          && entity.distanceToSqr(getLocalPlayer()) <= distanceSquared) {
        BlockPos pos = entity.blockPosition();
        GeometryTessellator.drawCuboid(
            event.getBuffer(),
            entity instanceof ItemFrame ? pos.below() : pos,
            GeometryMasks.Line.ALL,
            color
        );
      }
    }
    event.getTessellator().draw();
  }
}
