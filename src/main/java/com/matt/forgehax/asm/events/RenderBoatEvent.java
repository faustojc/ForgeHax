package com.matt.forgehax.asm.events;

import net.minecraft.world.entity.vehicle.Boat;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/**
 * Created by Babbaj on 9/2/2017.
 */
@Cancelable
public class RenderBoatEvent extends Event {

  private final Boat boat;
  private float yaw;

  public RenderBoatEvent(Boat boatIn, float entityYaw) {
    this.boat = boatIn;
    this.yaw = entityYaw;
  }

  public float getYaw() {
    return this.yaw;
  }

  public void setYaw(float yawIn) {
    this.yaw = yawIn;
  }

  public Boat getBoat() {
    return this.boat;
  }
}
