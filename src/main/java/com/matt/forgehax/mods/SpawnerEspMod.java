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
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.Helper.getWorld;

/** Shows mob spawners using a persistent, distance-indexed cache. */
@RegisterMod
public class SpawnerEspMod extends ToggleMod {
  private final Setting<Integer> refreshTicks =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("refresh-ticks")
          .description("Ticks between loaded spawner refreshes")
          .defaultTo(10)
          .min(1)
          .max(100)
          .build();

  private final Setting<Integer> distance =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("distance")
          .description("Maximum distance to display cached spawners")
          .defaultTo(256)
          .min(16)
          .max(1024)
          .build();

  private final PersistentBlockCache cache = new PersistentBlockCache();
  private Path cachePath;
  private int ticks;

  public SpawnerEspMod() {
    super(Category.RENDER, "SpawnerESP", false, "Spawner esp");
  }

  @Override
  protected void onEnabled() {
    ticks = refreshTicks.get();
  }

  @Override
  protected void onDisabled() {
    cache.close();
    cachePath = null;
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
    Path expected = PersistentBlockCache.pathFor("spawner", getWorld().provider.getDimension());
    if (!expected.equals(cachePath)) {
      cachePath = expected;
      cache.open(expected);
    }
  }

  private void refresh() {
    for (TileEntity tileEntity : getWorld().loadedTileEntityList) {
      if (tileEntity instanceof TileEntityMobSpawner) {
        cache.put(tileEntity.getPos(), Colors.RED.toBuffer());
      }
    }

    List<BlockPos> stale = new ArrayList<>();
    cache.forEachNearby(
        getLocalPlayer().getPosition(),
        distance.get(),
        (pos, color) -> {
          if (getWorld().isBlockLoaded(pos)
              && !(getWorld().getTileEntity(pos) instanceof TileEntityMobSpawner)) {
            stale.add(pos);
          }
        }
    );
    for (BlockPos pos : stale) {
      cache.remove(pos);
    }
  }

  @SubscribeEvent
  public void onWorldUnload(WorldEvent.Unload event) {
    cache.close();
    cachePath = null;
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
    event.getTessellator().draw();
  }
}
