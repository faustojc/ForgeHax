package com.matt.forgehax.asm.events;

import com.matt.forgehax.mixin.accessor.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

@Cancelable
public class LeftClickCounterUpdateEvent extends Event {

  private final Minecraft minecraft;
  private int value;

  public LeftClickCounterUpdateEvent(Minecraft minecraft, int value) {
    this.minecraft = minecraft;
    this.value = value;
  }

  public Minecraft getMinecraft() {
    return minecraft;
  }

  public int getCurrentValue() {
    return ((MinecraftAccessor) minecraft).getMissTime();
  }

  public int getValue() {
    return value;
  }

  public void setValue(int value) {
    this.value = value;
  }
}
