package com.matt.forgehax.asm.events;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

@Cancelable
public class WaterMovementEvent extends Event {

  private final Entity entity;
  private final Vec3 movement;

  public WaterMovementEvent(Entity entity, Vec3 movement) {
    this.entity = entity;
    this.movement = movement;
  }

  public Entity getEntity() {
    return entity;
  }

  public Vec3 getMoveDir() {
    return movement;
  }
}
