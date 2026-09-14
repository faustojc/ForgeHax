package com.matt.forgehax.mods.services;

import com.matt.forgehax.events.WorldChangeEvent;
import com.matt.forgehax.events.listeners.WorldListener;
import com.matt.forgehax.util.mod.ServiceMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Created on 6/14/2017 by fr1kin
 */
@RegisterMod
public class WorldEventService extends ServiceMod {

  private static final WorldListener WORLD_LISTENER = new WorldListener();

  public WorldEventService() {
    super("WorldEventService");
    // old IWorldEventListener is gone; WorldListener is a stable event-bus subscriber now,
    // so register it once instead of attaching it per-world.
    MinecraftForge.EVENT_BUS.register(WORLD_LISTENER);
  }

  @SubscribeEvent
  public void onWorldLoad(LevelEvent.Load event) {
    if (event.getLevel() instanceof Level level) {
      MinecraftForge.EVENT_BUS.post(new WorldChangeEvent(level));
    }
  }

  @SubscribeEvent
  public void onWorldUnload(LevelEvent.Unload event) {
    if (event.getLevel() instanceof Level level) {
      MinecraftForge.EVENT_BUS.post(new WorldChangeEvent(level));
    }
  }
}
