package com.matt.forgehax.util.entity;

import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EntityUtilsDistanceTest {

  /** A 1x8x1 pillar, the kind of hitbox a large modded mob has. */
  private static final AxisAlignedBB TALL = new AxisAlignedBB(0.D, 0.D, 0.D, 1.D, 8.D, 1.D);

  @Test
  public void insideTheBoxIsZero() {
    assertEquals(0.D, EntityUtils.getDistanceSq(new Vec3d(0.5D, 4.D, 0.5D), TALL), 1E-9);
  }

  @Test
  public void measuresToTheNearestFaceNotTheCenter() {
    // eyes 2 blocks away horizontally, level with the bottom of the pillar. The box center is
    // 4 blocks up, so a center based check would read >4 and drop the target.
    Vec3d eyes = new Vec3d(3.D, 0.5D, 0.5D);
    assertEquals(4.D, EntityUtils.getDistanceSq(eyes, TALL), 1E-9);
  }

  @Test
  public void combinesAxesOnDiagonals() {
    // 3 away on x, 4 away on z, none on y -> 5 blocks
    Vec3d eyes = new Vec3d(4.D, 4.D, 5.D);
    assertEquals(25.D, EntityUtils.getDistanceSq(eyes, TALL), 1E-9);
  }
}
