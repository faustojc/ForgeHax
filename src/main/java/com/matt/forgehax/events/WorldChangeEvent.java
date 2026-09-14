package com.matt.forgehax.events;

import net.minecraftforge.eventbus.api.Event;
import net.minecraft.world.level.Level;

/**
 * Created on 5/29/2017 by fr1kin
 */
public class WorldChangeEvent extends Event {

  private final Level world;

  public WorldChangeEvent(Level world) {
    this.world = world;
  }

  public Level getWorld() {
    return world;
  }

  public boolean isWorldNull() {
    return getWorld() == null;
  }
}
