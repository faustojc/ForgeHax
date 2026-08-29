package com.matt.forgehax.asm.events;

import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

@Cancelable
public class ApplyCollisionMotionEvent extends Event {

  private final Entity entity;
  private final Entity collidedWithEntity;

  private final double motionX;
  private final double motionY;
  private final double motionZ;

  public ApplyCollisionMotionEvent(
      Entity entity, Entity collidedWithEntity, double mX, double mY, double mZ) {
    this.entity = entity;
    this.collidedWithEntity = collidedWithEntity;
    motionX = mX;
    motionY = mY;
    motionZ = mZ;
  }

  public Entity getEntity() {
    return entity;
  }

  public Entity getCollidedWithEntity() {
    return collidedWithEntity;
  }

  public double getMotionX() {
    return motionX;
  }

  public double getMotionY() {
    return motionY;
  }

  public double getMotionZ() {
    return motionZ;
  }
}
