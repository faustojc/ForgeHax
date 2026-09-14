package com.matt.forgehax.util.projectile;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Objects;

/**
 * Created on 6/22/2017 by fr1kin
 */
public class SimulationResult {

  private final List<Vec3> points;
  private final double distanceTraveledSq;
  private final Entity hitEntity;

  public SimulationResult(List<Vec3> points, double distanceTraveledSq, Entity hitEntity) {
    this.points = points;
    this.distanceTraveledSq = distanceTraveledSq;
    this.hitEntity = hitEntity;
  }

  public Vec3 getShootPos() {
    try {
      return points.get(0);
    } catch (Throwable t) {
      return null;
    }
  }

  public Vec3 getHitPos() {
    try {
      return points.get(points.size() - 1);
    } catch (Throwable t) {
      return null;
    }
  }

  public Entity getHitEntity() {
    return hitEntity;
  }

  public boolean hasTraveled() {
    return !Objects.equals(getShootPos(), getHitPos());
  }

  public double getDistanceTraveledSq() {
    return distanceTraveledSq;
  }

  public double getDistanceApartSq() {
    Vec3 start = getShootPos();
    Vec3 hit = getHitPos();
    if (start != null && hit != null) {
      return start.distanceToSqr(hit);
    } else {
      return 0.D;
    }
  }

  public List<Vec3> getPathTraveled() {
    return points;
  }
}
