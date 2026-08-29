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
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.item.EntityMinecartChest;
import net.minecraft.item.ItemShulkerBox;
import net.minecraft.tileentity.*;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

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

  private int getTileEntityColor(TileEntity tileEntity) {
    if (tileEntity instanceof TileEntityChest
        || tileEntity instanceof TileEntityDispenser
        || tileEntity instanceof TileEntityShulkerBox) {
      return Colors.ORANGE.toBuffer();
    } else if (tileEntity instanceof TileEntityEnderChest) {
      return Colors.PURPLE.toBuffer();
    } else if (tileEntity instanceof TileEntityFurnace) {
      return Colors.GRAY.toBuffer();
    } else if (tileEntity instanceof TileEntityHopper) {
      return Colors.DARK_RED.toBuffer();
    }
    return -1;
  }

  private int getEntityColor(Entity entity) {
    if (entity instanceof EntityMinecartChest) {
      return Colors.ORANGE.toBuffer();
    } else if (entity instanceof EntityItemFrame
        && ((EntityItemFrame) entity).getDisplayedItem().getItem() instanceof ItemShulkerBox) {
      return Colors.YELLOW.toBuffer();
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
    if (getWorld().getTotalWorldTime() % 100L < refreshTicks.get()) {
      cache.flush();
    }
  }

  private void ensureCache() {
    Path expected = PersistentBlockCache.pathFor("storage", getWorld().provider.getDimension());
    if (!expected.equals(cachePath)) {
      cachePath = expected;
      cache.open(expected);
    }
  }

  private void refresh() {
    for (TileEntity tileEntity : getWorld().loadedTileEntityList) {
      int color = getTileEntityColor(tileEntity);
      if (color != -1) {
        cache.put(tileEntity.getPos(), color);
      }
    }

    List<BlockPos> stale = new ArrayList<>();
    cache.forEachNearby(
        getLocalPlayer().getPosition(),
        distance.get(),
        (pos, cachedColor) -> {
          if (getWorld().isBlockLoaded(pos)) {
            int currentColor = getTileEntityColor(getWorld().getTileEntity(pos));
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
    for (Entity entity : getWorld().loadedEntityList) {
      if (getEntityColor(entity) != -1) {
        dynamicStorage.add(entity);
      }
    }
  }

  @SubscribeEvent
  public void onWorldUnload(WorldEvent.Unload event) {
    cache.close();
    cachePath = null;
    dynamicStorage.clear();
  }

  @SubscribeEvent
  public void onRender(RenderEvent event) {
    event.getBuffer().begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
    cache.forEachNearby(
        getLocalPlayer().getPosition(),
        distance.get(),
        (pos, color) ->
            GeometryTessellator.drawCuboid(
                event.getBuffer(), pos, GeometryMasks.Line.ALL, color)
    );

    double distanceSquared = (double) distance.get() * distance.get();
    for (Entity entity : dynamicStorage) {
      int color = getEntityColor(entity);
      if (!entity.isDead
          && color != -1
          && entity.getDistanceSq(getLocalPlayer()) <= distanceSquared) {
        BlockPos pos = entity.getPosition();
        GeometryTessellator.drawCuboid(
            event.getBuffer(),
            entity instanceof EntityItemFrame ? pos.add(0, -1, 0) : pos,
            GeometryMasks.Line.ALL,
            color
        );
      }
    }
    event.getTessellator().draw();
  }
}
