package com.matt.forgehax.events;

import com.matt.forgehax.util.draw.SurfaceBuilder;
import net.minecraftforge.eventbus.api.Event;

import static com.matt.forgehax.Globals.MC;

/**
 * Created on 9/2/2017 by fr1kin
 */
public class Render2DEvent extends Event {

  private final int screenWidth = MC.getWindow().getGuiScaledWidth();
  private final int screenHeight = MC.getWindow().getGuiScaledHeight();
  private final SurfaceBuilder surfaceBuilder = new SurfaceBuilder();
  private final float partialTicks;

  public Render2DEvent(float partialTicks) {
    this.partialTicks = partialTicks;
  }

  public float getPartialTicks() {
    return partialTicks;
  }

  public double getScreenWidth() {
    return screenWidth;
  }

  public double getScreenHeight() {
    return screenHeight;
  }

  public SurfaceBuilder getSurfaceBuilder() {
    return surfaceBuilder;
  }
}
