package com.matt.forgehax.util.math;

import com.matt.forgehax.Globals;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class VectorUtils implements Globals {
  // Credits to Gregor and P47R1CK for the 3D vector transformation code

  /**
   * Convert 3D coordinates into a 2D coordinate projected onto the screen.
   *
   * <p>The model-view and projection matrices are read from the active render
   * state, which is the supported replacement for the old ActiveRenderInfo
   * reflection fields.
   */
  public static Plane toScreen(double x, double y, double z) {
    Entity view = MC.getCameraEntity();
    Camera camera = MC.gameRenderer.getMainCamera();

    if (view == null || camera == null) {
      return new Plane(0.D, 0.D, false);
    }

    Vec3 cameraPos = camera.getPosition();
    Vector4f pos = new Vector4f(
        (float) (x - cameraPos.x),
        (float) (y - cameraPos.y),
        (float) (z - cameraPos.z),
        1.f
    );

    pos.mul(new Matrix4f(RenderSystem.getModelViewMatrix()));
    pos.mul(new Matrix4f(RenderSystem.getProjectionMatrix()));

    if (pos.w > 0.f) {
      pos.x *= -100000;
      pos.y *= -100000;
    } else {
      float invert = 1.f / pos.w;
      pos.x *= invert;
      pos.y *= invert;
    }

    float screenWidth = MC.getWindow().getGuiScaledWidth();
    float screenHeight = MC.getWindow().getGuiScaledHeight();
    float halfWidth = screenWidth / 2.f;
    float halfHeight = screenHeight / 2.f;

    pos.x = halfWidth + (0.5f * pos.x * screenWidth + 0.5f);
    pos.y = halfHeight - (0.5f * pos.y * screenHeight + 0.5f);

    boolean visible = pos.x >= 0 && pos.y >= 0
        && pos.x <= screenWidth && pos.y <= screenHeight;
    return new Plane(pos.x, pos.y, visible);
  }

  public static Plane toScreen(Vec3 vec) {
    return toScreen(vec.x, vec.y, vec.z);
  }

  @Deprecated
  public static ScreenPos _toScreen(double x, double y, double z) {
    Plane plane = toScreen(x, y, z);
    return new ScreenPos(plane.getX(), plane.getY(), plane.isVisible());
  }

  @Deprecated
  public static ScreenPos _toScreen(Vec3 vec) {
    return _toScreen(vec.x, vec.y, vec.z);
  }

  /**
   * Convert a vector to an angle. Kept for source compatibility with the old
   * API; callers should use AngleHelper directly.
   */
  @Deprecated
  public static Object vectorAngle(Vec3 vec) {
    return null;
  }

  public static Vec3 multiplyBy(Vec3 vec1, Vec3 vec2) {
    return new Vec3(vec1.x * vec2.x, vec1.y * vec2.y, vec1.z * vec2.z);
  }

  public static Vec3 copy(Vec3 toCopy) {
    return new Vec3(toCopy.x, toCopy.y, toCopy.z);
  }

  public static double getCrosshairDistance(Vec3 eyes, Vec3 directionVec, Vec3 pos) {
    return pos.subtract(eyes).normalize().subtract(directionVec).lengthSqr();
  }

  @Deprecated
  public static class ScreenPos {

    public final int x;
    public final int y;
    public final boolean isVisible;

    public final double xD;
    public final double yD;

    public ScreenPos(double x, double y, boolean isVisible) {
      this.x = (int) x;
      this.y = (int) y;
      this.xD = x;
      this.yD = y;
      this.isVisible = isVisible;
    }
  }
}
